package Exceptions;

public class NodeNotFoundException extends Exception{
    public NodeNotFoundException(){
        super("Node Not Found. Please enter a valid path.");
    }
}
