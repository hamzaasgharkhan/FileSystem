package Run;

import Exceptions.FileSystemCreationFailedException;
import FileSystem.FileSystem;

public class Main {
    public static void main(String[] args){
        FileSystem fs;
        try {
            fs = FileSystem.createFileSystem("Aqua File System");
//             LS needs to be fixed.
            fs.createDirectory("/", "Obama");
            fs.ls("/");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
