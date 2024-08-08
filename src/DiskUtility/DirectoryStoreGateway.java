/**
 * CREATE METHODS FOR THE FOLLOWING:
 * --- Create byte frame for a single entry using values as parameters
 * --- Take a byte array and modify a particular entry
 * --- Write to the store
 * --- Read from the store
 * --- Update the store
 */
package DiskUtility;
import Constants.FLAGS;
import Constants.DIRECTORY_STORE_FRAME;
import Constants.VALUES;
import FileSystem.Node;
import FileSystem.NodeTree;
import Utilities.BinaryUtilities;
import Utilities.GeneralUtilities;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Path;
import java.util.LinkedList;
/**
 * This class provides an interface between the DirectoryStore file and the rest of the filesystem.
 Magic Value                 -       4 bytes                                 || Starting Index: 0
 index                       -       8 bytes                                 || Starting Index: 4
 parentIndex                 -       8 bytes                                 || Starting Index: 12
 previousSiblingIndex        -       8 bytes                                 || Starting Index: 20
 nextSiblingIndex            -       8 bytes                                 || Starting Index: 28
 childIndex                  -       8 bytes                                 || Starting Index: 36
 iNode                       -       8 bytes                                 || Starting Index: 44
 parentINode                 -       8 bytes                                 || Starting Index: 52
 flags                       -       1 byte                                  || Starting Index: 60
 Magic Value                 -       4 bytes                                 || Starting Index: 61
 name                        -       256 bytes                               || Starting Index: 65
 Magic Value                 -       4 bytes                                 || Starting Index: 321
 */
public class DirectoryStoreGateway{
    private static class DirectoryFrame{
        String name;
        long index;
        long parentIndex;
        long iNodeAddress;
        long parentINodeAddress;
        long previousSiblingIndex;
        long nextSiblingIndex;
        long childIndex;
        byte flags;
        DirectoryFrame(String name, long index, long parentIndex, long iNodeAddress, long parentINodeAddress,
                       long previousSiblingIndex, long nextSiblingIndex, long childIndex, byte flags){
            this.name = name;
            this.index = index;
            this.parentIndex = parentIndex;
            this.iNodeAddress = iNodeAddress;
            this.parentINodeAddress = parentINodeAddress;
            this.previousSiblingIndex = previousSiblingIndex;
            this.nextSiblingIndex = nextSiblingIndex;
            this.childIndex = childIndex;
            this.flags = flags;
        }
        byte[] getBytes(){
            byte[] byteArray = new byte[DIRECTORY_STORE_FRAME.SIZE];
            final byte[] magicValueBytes = VALUES.MAGIC_VALUE_BYTES;
            System.arraycopy(magicValueBytes, 0, byteArray, DIRECTORY_STORE_FRAME.MAGIC_VALUE_1_INDEX, 4);
            System.arraycopy(magicValueBytes, 0, byteArray, DIRECTORY_STORE_FRAME.MAGIC_VALUE_2_INDEX, 4);
            System.arraycopy(magicValueBytes, 0, byteArray, DIRECTORY_STORE_FRAME.MAGIC_VALUE_3_INDEX, 4);
            System.arraycopy(BinaryUtilities.convertLongToBytes(index), 0, byteArray, DIRECTORY_STORE_FRAME.DIRECTORY_STORE_INDEX_INDEX, 8);
            System.arraycopy(BinaryUtilities.convertLongToBytes(parentIndex), 0, byteArray, DIRECTORY_STORE_FRAME.PARENT_INDEX, 8);
            System.arraycopy(BinaryUtilities.convertLongToBytes(previousSiblingIndex), 0, byteArray, DIRECTORY_STORE_FRAME.PREVIOUS_SIBLING_INDEX, 8);
            System.arraycopy(BinaryUtilities.convertLongToBytes(nextSiblingIndex), 0, byteArray, DIRECTORY_STORE_FRAME.NEXT_SIBLING_INDEX, 8);
            System.arraycopy(BinaryUtilities.convertLongToBytes(childIndex), 0, byteArray, DIRECTORY_STORE_FRAME.CHILD_INDEX, 8);
            System.arraycopy(BinaryUtilities.convertLongToBytes(iNodeAddress), 0, byteArray, DIRECTORY_STORE_FRAME.INODE_INDEX, 8);
            System.arraycopy(BinaryUtilities.convertLongToBytes(parentINodeAddress), 0, byteArray, DIRECTORY_STORE_FRAME.PARENT_INODE_INDEX, 8);
            byteArray[DIRECTORY_STORE_FRAME.FLAGS_INDEX] = flags;
            System.arraycopy(GeneralUtilities.getFixedSizeUTF8StringBytes(name, 256), 0, byteArray, DIRECTORY_STORE_FRAME.NAME_INDEX, 256);
            return byteArray;
        }
        boolean isDirectory(){
            return (Node.DIRECTORY_FLAG_MASK & flags) != 0;
        }
        boolean isEmpty(){
            return (childIndex == index);
        }
        boolean hasSiblings(){
            return (nextSiblingIndex != index);
        }
    }
    private final File directoryStoreFile;
    private final BitMapUtility bitMapUtility;
    private final SecretKey key;
    public DirectoryStoreGateway(File baseFile, BitMapUtility bitMapUtility, SecretKey key) throws Exception {
        File file;
        try {
            file = Gateway.getFileInBaseDirectory(baseFile, Store.DirectoryStore.fileName);
        } catch (Exception e){
            throw new Exception("Unable to Initialize DirectoryStore: DirectoryStoreFile Inaccessible -- " + e.getMessage());
        }
        directoryStoreFile = file;
        this.bitMapUtility = bitMapUtility;
        this.key = key;
    }

    /**
     * This method mounts the DirectoryTree from the directory-store file. When the directory tree is mounted, only the
     * root node is completely read. The child nodes of root are not read to check for any children. Their CNR
     * (Children Not Read) flags are set to true. At any further operation on those nodes, their children will first
     * need to be read before the operations can proceed.
     * @return NodeTree object containing the rote node.
     */
    public NodeTree mount() throws Exception {
        NodeTree nodeTree = new NodeTree(true);
        DirectoryFrame rootFrame = getDirectoryFrame(0);
        Node root = new Node(rootFrame.name, null, 0, rootFrame.flags);
        root.setIndex(0);
        nodeTree.setRoot(root);
        root.setFlag(Node.CNR_FLAG_MASK, true);
        // Check to see whether root has any children. If not, simply return the root after setting CNR flag to false.
        if (rootFrame.childIndex == 0){
            root.setFlag(Node.CNR_FLAG_MASK, false);
            return nodeTree;
        }
        // Get the directory frame of a child.
        readChildren(root);
        return nodeTree;
    }
    private DirectoryFrame getDirectoryFrame(long index) throws Exception{
        byte[] byteArray = __getDirectoryFrameBytes(index);
        String name = BinaryUtilities.convertBytesToUTF8String(byteArray, DIRECTORY_STORE_FRAME.NAME_INDEX, 256);
        long i = BinaryUtilities.convertBytesToLong(byteArray, DIRECTORY_STORE_FRAME.DIRECTORY_STORE_INDEX_INDEX);
        if (i != index)
            throw new RuntimeException("DirectoryFrame Error: Index is not what is is supposed to be.\n" +
                    "Expected Value: " + index + "\nCurrent Value: " + i);
        long parentIndex = BinaryUtilities.convertBytesToLong(byteArray, DIRECTORY_STORE_FRAME.PARENT_INDEX);
        long iNodeAddress = BinaryUtilities.convertBytesToLong(byteArray, DIRECTORY_STORE_FRAME.INODE_INDEX);
        long parentINodeAddress = BinaryUtilities.convertBytesToLong(byteArray, DIRECTORY_STORE_FRAME.PARENT_INODE_INDEX);
        long previousSiblingIndex = BinaryUtilities.convertBytesToLong(byteArray, DIRECTORY_STORE_FRAME.PREVIOUS_SIBLING_INDEX);
        long nextSiblingIndex = BinaryUtilities.convertBytesToLong(byteArray, DIRECTORY_STORE_FRAME.NEXT_SIBLING_INDEX);
        long childIndex = BinaryUtilities.convertBytesToLong(byteArray, DIRECTORY_STORE_FRAME.CHILD_INDEX);
        byte flags = byteArray[DIRECTORY_STORE_FRAME.FLAGS_INDEX];
        return new DirectoryFrame(name.trim(), index, parentIndex, iNodeAddress, parentINodeAddress, previousSiblingIndex,
                nextSiblingIndex, childIndex, flags);
    }
    /**
     * Reads a DirectoryFrame from the DirectoryStore File.
     * @param index Target Index
     * @return byte array of the target directory frame
     * @throws Exception In case of IOExceptions; or in case the file has been deleted; or in case of decryption errors.
     */
    private byte[] __getDirectoryFrameBytes(long index) throws Exception {
        RandomAccessFile file;
        int frameSize = DIRECTORY_STORE_FRAME.FULL_SIZE;
        byte[] byteArray = new byte[frameSize];
        try {
            file = new RandomAccessFile(directoryStoreFile, "r");
            file.seek(index * frameSize);
            file.read(byteArray);
            file.close();
        } catch (FileNotFoundException e){
            throw new Exception("Unexpected error occurred during getting directoryFrame. " +
                    "DirectoryStoreFile does not exist.");
        }
        byteArray = Crypto.decryptBlock(byteArray, key, DIRECTORY_STORE_FRAME.SIZE);
        return byteArray;
    }
    /**
     * This method takes a node and places that node within the directory store file at a free index.
     * @param node The desired node to be added to the directory store file.
     * @return The index of the node
     * @throws Exception In case the node is not successfully added to the directory-store.
     */
    public long addNode(Node node) throws Exception{
        if (node.getName().equals("root"))
            return __addRootNode(node);
        Node parentNode = node.getParentNode();
        LinkedList<Node> siblings = parentNode.getChildNodes(this);
        int siblingsSize = siblings.size();
        long index = bitMapUtility.getFreeIndexDirectoryStore();
        DirectoryFrame frame = new DirectoryFrame(
                node.getName(),
                index,
                parentNode.getIndex(),
                node.getiNodeAddress(),
                parentNode.getiNodeAddress(),
                index,
                index,
                index,
                node.checkFlag(Node.DIRECTORY_FLAG_MASK) ? FLAGS.DEFAULT_DIRECTORY_FRAME_DIR : FLAGS.DEFAULT_DIRECTORY_FRAME_FILE
        );
        if (siblingsSize < 2){
            frame.previousSiblingIndex = index;
            frame.nextSiblingIndex = index;
            DirectoryFrame parentFrame = getDirectoryFrame(frame.parentIndex);
            parentFrame.childIndex = index;
            __updateDirectoryFrame(parentFrame);
        }
        else{
            int selfIndex = siblings.indexOf(node);
            DirectoryFrame previousSibling = null;
            DirectoryFrame nextSibling = null;
            int nextIndex = (selfIndex == 0) ? 1: 0;
            previousSibling = getDirectoryFrame(siblings.get(nextIndex).getIndex());
            if (previousSibling.previousSiblingIndex == previousSibling.index){ // There is only 1 sibling.
                previousSibling.previousSiblingIndex = index;
                previousSibling.nextSiblingIndex = index;
                frame.nextSiblingIndex = previousSibling.index;
                frame.previousSiblingIndex = previousSibling.index;
                __updateDirectoryFrame(previousSibling);
            } else { // There are at least 2 siblings.
                nextSibling = getDirectoryFrame(previousSibling.nextSiblingIndex);
                previousSibling.nextSiblingIndex = index;
                frame.previousSiblingIndex = previousSibling.index;
                nextSibling.previousSiblingIndex = index;
                frame.nextSiblingIndex = nextSibling.index;
                __updateDirectoryFrame(previousSibling);
                __updateDirectoryFrame(nextSibling);
            }
        }
        __writeDirectoryFrame(frame, index);
        bitMapUtility.setIndexDirectoryStore(index, true);
        node.setIndex(index);
        return index;
    }

    /**
     * This method updates a Node already present in the directory store
     * @param node Target Node
     * @return Index of the node.
     */
    public long updateNode(Node node) throws Exception{
        long index = node.getIndex();
        DirectoryFrame frame = getDirectoryFrame(index);
        Node parentNode = node.getParentNode();
        boolean nameChanged, parentChanged, flagsChanged;
        /*
         * Cases:
         *  1. Name Changed:
         *      Only change the name and return.
         *  2. Parent Changed:
         *      Change the child index of the previous parent, possibly the new parent. Change the sibling index of the past siblings. New possibly.
         *  3. Flags Changed:
         *      Write Flags.
         */
        // CASE 1: Name Changed.
        nameChanged = !node.getName().equals(frame.name);
        // CASE 2: Parent Changed.
        if (parentNode != null){
            // Only handling this case as the parent of Root can never be changed.
            parentChanged = parentNode.getIndex() != frame.parentIndex;
        } else {
            // If root, parentChanged is false.
            parentChanged = false;
        }
        // CASE 3: Flags Changed.
        flagsChanged = node.getFlags() != frame.flags;
        ///////////////////////////////////////////
        // HANDLE ALL THE CHANGES ONE BY ONE.
        ///////////////////////////////////////////
        // CASE 1: NAME CHANGED.
        if (nameChanged){
            frame.name = node.getName();
        }
        // CASE 2: PARENT CHANGED.
        if (parentChanged){
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ///////// PREVIOUS CONNECTIONS
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // First Take Care of All the changes needed to the previous parent and possibly siblings.
            DirectoryFrame previousParentFrame = getDirectoryFrame(frame.parentIndex);
            // Take Care of previous parent's childIndex.
            if (previousParentFrame.childIndex == index){
                // Node is set as the ChildIndex of the previous parent.
                // In case there are more siblings, set the index to one of them else set it to the parent's own index.
                if (frame.previousSiblingIndex != frame.index){
                    // It had a sibling, hence assign the sibling as the new child.
                    previousParentFrame.childIndex = frame.previousSiblingIndex;
                } else {
                    previousParentFrame.childIndex = previousParentFrame.index;
                }
                __writeDirectoryFrame(previousParentFrame, previousParentFrame.index);
            }
            // Take care of previous siblings, if any.
            if (frame.previousSiblingIndex != frame.index){
                DirectoryFrame previousSiblingFrame;
                // At least one sibling exists.
                previousSiblingFrame = getDirectoryFrame(frame.previousSiblingIndex);
                if (frame.previousSiblingIndex != frame.nextSiblingIndex){
                    DirectoryFrame nextSiblingFrame = getDirectoryFrame(frame.nextSiblingIndex);
                    // At least two siblings exist.
                    previousSiblingFrame.nextSiblingIndex = nextSiblingFrame.index;
                    nextSiblingFrame.previousSiblingIndex = previousSiblingFrame.index;
                    __writeDirectoryFrame(nextSiblingFrame, nextSiblingFrame.index);
                } else {
                    // Only 1 sibling.
                    previousSiblingFrame.previousSiblingIndex = previousSiblingFrame.index;
                    previousSiblingFrame.nextSiblingIndex = previousSiblingFrame.index;
                }
                __writeDirectoryFrame(previousSiblingFrame, previousSiblingFrame.index);
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            //////// NEW CONNECTION
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Turn for next parent.
            DirectoryFrame nextParentFrame = getDirectoryFrame(parentNode.getIndex());
            if (nextParentFrame.childIndex == nextParentFrame.index){
                // Next Parent Currently has no children.
                nextParentFrame.childIndex = frame.index;
                frame.nextSiblingIndex = frame.index;
                frame.previousSiblingIndex = frame.index;
                __writeDirectoryFrame(nextParentFrame, nextParentFrame.index);
            } else {
                // There is at least 1 child already.
                DirectoryFrame nextParentChildFrame = getDirectoryFrame(nextParentFrame.childIndex);
                if (nextParentChildFrame.previousSiblingIndex == nextParentChildFrame.index){
                    // The child does not have any siblings.
                    nextParentChildFrame.nextSiblingIndex = frame.index;
                    nextParentChildFrame.previousSiblingIndex = frame.index;
                    frame.nextSiblingIndex = nextParentChildFrame.index;
                    frame.previousSiblingIndex = nextParentChildFrame.index;
                } else {
                    // The child does have at least one sibling already.
                    DirectoryFrame nextParentChildSiblingFrame = getDirectoryFrame(nextParentChildFrame.previousSiblingIndex);
                    nextParentChildSiblingFrame.nextSiblingIndex = frame.index;
                    frame.previousSiblingIndex = nextParentChildSiblingFrame.index;
                    frame.nextSiblingIndex = nextParentChildFrame.index;
                    nextParentChildFrame.previousSiblingIndex = frame.index;
                    __writeDirectoryFrame(nextParentChildSiblingFrame, nextParentChildSiblingFrame.index);
                }
                __writeDirectoryFrame(nextParentChildFrame, nextParentChildFrame.index);
            }
        }
        // CASE 3: FLAGS CHANGED
        if (flagsChanged){
            frame.flags = node.getFlags();
        }
        if (nameChanged || parentChanged || flagsChanged){
            __writeDirectoryFrame(frame, index);
        }
        return index;
    }



    /**
     * This method takes a node and removes the node from the FileSystem
     * @param node Target Node
     */
    public void removeNode(Node node) throws Exception{
        Node parentNode = node.getParentNode();
        DirectoryFrame nodeFrame = getDirectoryFrame(node.getIndex());
        DirectoryFrame parentFrame = getDirectoryFrame(parentNode.getIndex());
        if (nodeFrame.isDirectory() && nodeFrame.childIndex != nodeFrame.index){
            throw new Exception("Cannot Remove Non Empty Directory");
        }
        if (nodeFrame.nextSiblingIndex == nodeFrame.index){
            // Node has no siblings
            parentFrame.childIndex = parentFrame.index;
            __updateDirectoryFrame(parentFrame);
        } else {
            DirectoryFrame previousSiblingFrame = getDirectoryFrame(nodeFrame.previousSiblingIndex);
            if (nodeFrame.nextSiblingIndex == nodeFrame.previousSiblingIndex){
                // Node only has 1 sibling
                previousSiblingFrame.nextSiblingIndex = previousSiblingFrame.index;
                previousSiblingFrame.previousSiblingIndex = previousSiblingFrame.index;
                __updateDirectoryFrame(previousSiblingFrame);
            } else {
                // Node has at least 2 siblings
                DirectoryFrame nextSiblingFrame = getDirectoryFrame(nodeFrame.nextSiblingIndex);
                previousSiblingFrame.nextSiblingIndex = nextSiblingFrame.index;
                nextSiblingFrame.previousSiblingIndex = previousSiblingFrame.index;
                __updateDirectoryFrame(previousSiblingFrame);
                __updateDirectoryFrame(nextSiblingFrame);
            }
            // If ParentFrame points to node, change it.
            if (parentFrame.childIndex == nodeFrame.index){
                parentFrame.childIndex = previousSiblingFrame.index;
                __updateDirectoryFrame(parentFrame);
            }
        }
        // Set Bitmap to 0
        bitMapUtility.setIndexDirectoryStore(nodeFrame.index, false);
        parentNode.getChildNodes(this).remove(node);
    }
    private long __addRootNode(Node node) throws Exception{
        long index = bitMapUtility.getFreeIndexDirectoryStore();
        DirectoryFrame frame = new DirectoryFrame(
                node.getName(),
                index,
                index,
                0,
                0,
                index,
                index,
                index,
                FLAGS.DEFAULT_DIRECTORY_FRAME_DIR
        );
        __writeDirectoryFrame(frame, index);
        bitMapUtility.setIndexDirectoryStore(index, true);
        node.setIndex(index);
        return index;
    }
    private void __updateDirectoryFrame(DirectoryFrame frame) throws Exception{
        __writeDirectoryFrame(frame, frame.index);
    }
    /**
     * Writes the provided DirectoryFrame to the designated index.
     * @param frame The input DirectoryFrame
     * @param index The desired index
     * @throws Exception If the operation was unsuccessful
     */
    private void __writeDirectoryFrame(DirectoryFrame frame, long index) throws Exception{
        int frameSize = DIRECTORY_STORE_FRAME.FULL_SIZE;
        byte[] byteArray = frame.getBytes();
        byteArray = Crypto.encryptBlock(byteArray, key, DIRECTORY_STORE_FRAME.SIZE);
        RandomAccessFile file;
        try {
            file = new RandomAccessFile(directoryStoreFile, "rw");
            file.seek(index * frameSize);
            file.write(byteArray, 0, byteArray.length);
            file.close();
        } catch (FileNotFoundException e){
            throw new Exception("Unexpected error occurred during getting directoryFrame. " +
                    "DirectoryStoreFile does not exist.");
        }
    }
    /**
     * This method takes a node with the CNR flag set to true and reads the children for that node.
     * @param node The required node.
     */
    public void readChildren(Node node) throws Exception{
        DirectoryFrame frame = getDirectoryFrame(node.getIndex());
        if (frame.childIndex == frame.index)
            return;
        if (!node.isDirectory())
            return;
        // Get the directory frame of a child.
        DirectoryFrame childFrame = getDirectoryFrame(frame.childIndex);
        DirectoryFrame runningChildFrame = childFrame;
        if (childFrame.hasSiblings()){
            do {
                Node childNode = new Node(runningChildFrame.name,  node, runningChildFrame.iNodeAddress, runningChildFrame.flags);
                if (childNode.isDirectory())
                    childNode.setFlag(Node.CNR_FLAG_MASK, true);
                childNode.setIndex(runningChildFrame.index);
                node.addChild(childNode);
                runningChildFrame = getDirectoryFrame(runningChildFrame.nextSiblingIndex);
            } while (runningChildFrame.index != childFrame.index);
        } else {
            Node childNode = new Node(runningChildFrame.name, node, runningChildFrame.iNodeAddress, runningChildFrame.flags);
            childNode.setIndex(runningChildFrame.index);
            if (childNode.isDirectory())
                childNode.setFlag(Node.CNR_FLAG_MASK, true);
            node.addChild(childNode);
        }
        node.setFlag(Node.CNR_FLAG_MASK, false);
    }
    /**
     * This method provides a byte array containing the data within a new DirectoryStore file.
     * @return a byte array containing the bytes in a new DirectoryStore file.
     */
    protected static byte[] getDefaultBytes(){
        DirectoryFrame frame = new DirectoryFrame("root", 0, 0, 0, 0, 0, 0, 0, FLAGS.DEFAULT_DIRECTORY_FRAME_DIR);
        return frame.getBytes();
    }
}
