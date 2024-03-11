package FileSystem;

import Constants.FLAGS;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;

/**
 * This class represents a Node in the FileSystem.
 */
public class Node {
    public static final byte CNR_FLAG_MASK = 0b00000001;
    public static final byte DIRECTORY_FLAG_MASK = (byte)0b10000000;
    /**
     * Contains the name of the Node. Maximum 256 bytes.
     */
    private String name;
    /**
     * Points to the iNode reference within the iNodeStore.
     */
    long iNodeAddress;
    /**
     * Contains a reference to the parentNode. Value set to null only in case of the root node.
     */
    Node parentNode;
    /**
     * A LinkedList of child Node references.
     */
    LinkedList<Node> childNodes = new LinkedList<Node>();
    /**
     * Flags that are stored on disk.
     */
    byte flags;
    /**
     * The index of the node within the file system.
     */
    long index;


    /**
     * This constructor creates a Node provided that the name does not exceed 256 bytes. If it does, an exception is
     * thrown.
     * @param name  Name of the node (<= 256 bytes)
     * @param iNodeAddress iNodeAddress of the node
     * @param parentNode parentNode. Only root will have no parentNodes.
     * @param flags the flags for the Node.
     * @throws IllegalArgumentException If the name of the Node exceeds 256 bytes.
     */
    public Node(String name, Node parentNode, long iNodeAddress, byte flags){
        if (name.getBytes(StandardCharsets.UTF_8).length > 256)
            throw new IllegalArgumentException("Node name cannot exceed 256 bytes.");
        this.name = name;
        this.parentNode = parentNode;
        this.flags = flags;
        if (this.isDirectory())
            this.childNodes = new LinkedList<Node>();
        else
            this.iNodeAddress = iNodeAddress;
    }

    public Node(String name, Node parentNode){
        this(name, parentNode, 0, FLAGS.DEFAULT_NODE_DIRECTORY);
    }

    public Node(String name, Node parentNode, long iNodeAddress){
        this(name, parentNode, iNodeAddress, FLAGS.DEFAULT_NODE_FILE);
    }



    public boolean isDirectory(){
        return checkFlag(DIRECTORY_FLAG_MASK);
    }

    /**
     * This method returns the path of a given node by traversing the tree through its parent till it reaches
     * the root node.
     * @return The absolute path of the node in the filesystem.
     */
    public String getPath(){
        if (name.equals("root"))
            return "/";
        if (isDirectory())
            return parentNode.getPath() + name +"/";
        return parentNode.getPath() + name;
    }

    public Node getChildNode(String name){
        if (!isDirectory())
            return null;
        for (int i = 0; i < childNodes.size(); i++){
            Node node = childNodes.get(i);
            if (node.getName().equals(name))
                return node;
        }
        return null;
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Node node = (Node) obj;
        return name.equals(node.name);
    }

    public boolean checkFlag(byte flagMask){
        return (flagMask & flags) != 0;
    }

    public void setFlag(byte flagMask, boolean value){
        if (value)
            flags = (byte) (flags | flagMask);
        else
            flags = (byte)(flags & (0xff ^ flagMask));
    }

    public void addChild(Node child){
        if (!isDirectory())
            throw new RuntimeException("Cannot Add Child. Parent Node is not a directory.");
        childNodes.add(child);
        child.parentNode = this;
    }

    public String getName(){
        return name;
    }

    public long getIndex(){
        return index;
    }

    public Node getParentNode(){
        return parentNode;
    }

    public long getiNodeAddress(){
        return iNodeAddress;
    }

    public LinkedList<Node> getChildNodes(){
        return this.childNodes;
    }

    public void setIndex(long index) {
        this.index = index;
    }
}
