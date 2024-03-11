package Exceptions;

public class FileSystemCreationFailedException extends Exception{
    public FileSystemCreationFailedException(String msg){
        super("FileSystemCreationFailed: " + msg);
    }
}
