package Exceptions;

public class INodeNotFoundException extends Exception{
    public INodeNotFoundException(){
        super("INode Not Found. Invalid iNode Address.");
    }
}
