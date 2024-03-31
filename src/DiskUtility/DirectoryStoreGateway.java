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
            byte[] byteArray = new byte[DIRECTORY_STORE_FRAME.DIRECTORY_STORE_FRAME_SIZE];
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
    File directoryStoreFile;
    final BitMapUtility bitMapUtility;
    public DirectoryStoreGateway(Path path, BitMapUtility bitMapUtility) throws Exception {
        File file = path.resolve("directory-store").toFile();
        if (!file.exists())
            throw new Exception("Directory Store File Does Not Exist.");
        directoryStoreFile = file;
        this.bitMapUtility = bitMapUtility;
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
        nodeTree.setRoot(root);
        root.setFlag(Node.CNR_FLAG_MASK, true);
        // Check to see whether root has any children. If not, simply return the root after setting CNR flag to false.
        if (rootFrame.childIndex == 0){
            root.setFlag(Node.CNR_FLAG_MASK, false);
            return nodeTree;
        }
        // Get the directory frame of a child.
        DirectoryFrame childFrame = getDirectoryFrame(rootFrame.childIndex);
        DirectoryFrame runningChildFrame = childFrame;
        if (childFrame.hasSiblings()){
            do {
                Node childNode = new Node(runningChildFrame.name,  root, runningChildFrame.iNodeAddress, runningChildFrame.flags);
                childNode.setIndex(runningChildFrame.index);
                if (childNode.isDirectory())
                    childNode.setFlag(Node.CNR_FLAG_MASK, true);
                root.addChild(childNode);
                runningChildFrame = getDirectoryFrame(runningChildFrame.nextSiblingIndex);
            } while (runningChildFrame.index != childFrame.index);
        } else {
            Node childNode = new Node(runningChildFrame.name, root, runningChildFrame.iNodeAddress, runningChildFrame.flags);
            childNode.setIndex(runningChildFrame.index);
            if (childNode.isDirectory())
                childNode.setFlag(Node.CNR_FLAG_MASK, true);
            root.addChild(childNode);
        }
        // Set the CNR flag of root to false after all its children have been read.
        root.setFlag(Node.CNR_FLAG_MASK, false);
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

    private byte[] __getDirectoryFrameBytes(long index) throws Exception {
        RandomAccessFile file;
        int frameSize = DIRECTORY_STORE_FRAME.DIRECTORY_STORE_FRAME_SIZE;
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
        LinkedList<Node> siblings = parentNode.getChildNodes();
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
                FLAGS.DEFAULT_DIRECTORY_FRAME_DIR
        );
        if (siblingsSize == 1){
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
        int frameSize = DIRECTORY_STORE_FRAME.DIRECTORY_STORE_FRAME_SIZE;
        RandomAccessFile file;
        try {
            file = new RandomAccessFile(directoryStoreFile, "rw");
            file.seek(index * frameSize);
            file.write(frame.getBytes(), 0, frameSize);
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
        // Get the directory frame of a child.
        DirectoryFrame childFrame = getDirectoryFrame(frame.childIndex);
        DirectoryFrame runningChildFrame = childFrame;
        if (childFrame.hasSiblings()){
            do {
                Node childNode = new Node(runningChildFrame.name,  node, runningChildFrame.iNodeAddress, runningChildFrame.flags);
                if (childNode.isDirectory())
                    childNode.setFlag(Node.CNR_FLAG_MASK, true);
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

    public boolean writeObject(NodeTree tree){
        // IMPLEMENT
        return false;
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
