package Exceptions;

public class INodeFileNotFoundException extends Exception{
    public INodeFileNotFoundException(){
        super("INodeStore File Not Found Or Is Not Accessible.");
    }
}
