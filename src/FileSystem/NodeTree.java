/**
 * FEATURES TO IMPLEMENT:
 * Create a Node
 * Delete a Node
 * Open a Node
 * Get information about a Node
 * Retrieve a node.
 * Read a node.
 * Rename a node.
 */
package FileSystem;

import Constants.FLAGS;
import DiskUtility.DirectoryStoreGateway;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

/**
 * This class is a NodeTree that contains the overall structure of the DirectoryStore within the filesystem.
 * It starts from the root note. Each node has a LinkedList of childNodes (of the class INode).
 * It provides address parsing functionalities (to go to a particular node using an address e.g. /test.txt)
 * All paths start from '/'. '/' is considered as the root.
 *
 * @see INode
 */
public class NodeTree {

    private Node root;
    private final LinkedList<Node> dirtyNodes;
    /**
     * At the initialization of the NodeTree, a reference is created to the rootNode.
     */
    public NodeTree(){
        root = new Node("root", null,0, (byte)0b1000000);
        dirtyNodes = new LinkedList<Node>();
    }
    public Node getRoot() {
        return root;
    }

    /**
     * This method parses a path to get a Node.
     * @param path The path of the requested Node in the FileSystem (e.g. /files/img1.jpeg)
     * @return  The requested node
     * @throws IllegalArgumentException In case the path is invalid.
     */
    public Node getNodeFromPath(String path, DirectoryStoreGateway gateway) throws Exception {
        String[] nodes = path.split("/");
        Node n = root;
        for (int i = 0; i < nodes.length; i++){
            if (nodes[i].isEmpty())
                continue;
            if (n.checkFlag(Node.CNR_FLAG_MASK))
                gateway.readChildren(n);
            n = n.getChildNode(nodes[i]);
        }
        if (n == null)
            throw new IllegalArgumentException("Unable to getNodeFromPath: Invalid Path");
        return n;
    }

    /**
     * This method functions the same way as the UNIX ls command. It takes a path and returns the Nodes at that path.
     * @param path The path of the requested Node in the FileSystem (e.g. /files/img1.jpeg)
     * @return A LinkedList of the Nodes at the requested path
     * @throws IllegalArgumentException In case the path does not lead to a valid Node, a RuntimeException is thrown.
     */
    public LinkedList<Node> ls(String path, DirectoryStoreGateway gateway) throws Exception{
        Node node = getNodeFromPath(path, gateway);
        if (node.checkFlag(Node.CNR_FLAG_MASK))
            gateway.readChildren(node);
        if (node.isDirectory())
            return node.childNodes;
        return null;
    }

    /**
     * Add a Node to a specific path. The parentNode attribute may be set to null as the method will assign it to the
     * appropriate node. The method will throw a RuntimeException if the path is not valid.
     * @param name The name of the node (<= 256 bytes)
     * @param path The destination path
     * @return The newly created Node
     * @throws IllegalArgumentException If the path does not exist; If the path does not point to a directory; If a node
     * with the same name exists
     */
    public Node addNode(String path, String name, long iNodeAddress, DirectoryStoreGateway gateway) throws Exception{
        _validNodeName(name);
        Node parentNode = getNodeFromPath(path, gateway);
        if (!parentNode.isDirectory())
            throw new IllegalArgumentException("Path does not point to a directory");
        if (parentNode.getChildNode(name) != null)
            throw new IllegalArgumentException("Node with the same name exists");
        Node node = new Node(name, parentNode, iNodeAddress, FLAGS.DEFAULT_NODE_FILE);
        node.parentNode = node;
        parentNode.childNodes.add(node);
        return node;
    }


    // DIRECTORY
    public Node addNode(String path, String name, DirectoryStoreGateway gateway) throws Exception{
        _validNodeName(name);
        Node parentNode = getNodeFromPath(path, gateway);
        Node node = new Node(name, parentNode);
        parentNode.childNodes.add(node);
        dirtyNodes.add(node);
        return node;
    }

    /**
     * Returns if and only if node name is valid. Otherwise, throws relevant exceptions.
     * @param name The node name to be tested.
     */
    public void _validNodeName(String name){
        int isNameValid = __validNodeName(name);
        if (isNameValid != 0)
            if (isNameValid == 1)
                throw new IllegalArgumentException("Name cannot be blank");
            else if (isNameValid == 2)
                throw new IllegalArgumentException("Name cannot exceed 256 bytes");
            else if (isNameValid == 3)
                throw new IllegalArgumentException("Name cannot contain / or \\");
    }

    /**
     * This method checks whether a potential node name is valid or not.
     * @param name Name of node to be tested.
     * @return 0 if and only if the Name is valid. Other integers correspond to specific errors.
     * Integer  |   Error
     * 1        |   Name empty
     * 2        |   Name exceeds 256 bytes
     * 3        |   Name contains forbidden characters
     */
    public int __validNodeName(String name){
        if (name.isEmpty())
            return 1;
        byte[] byteArray = name.getBytes(StandardCharsets.UTF_8);
        if (byteArray.length > 256)
            return 2;
        if (name.contains("\\") || name.contains("/"))
            return 3;
        return 0;

    }
    /**
     * Checks whether a proposed node can be added to the NodeTree. Returns only if the node can be added or throws an
     * exception
     * @param path Destination of the proposed node.
     * @param name Name of the proposed node
     */
    public void __nodeCanBeAdded(String path, String name, DirectoryStoreGateway gateway) throws Exception{
        _validNodeName(name);
        getNodeFromPath(path, gateway);
    }

    public void setRoot(Node root) {
        this.root = root;
    }
}
