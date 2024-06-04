package Run;
import DiskUtility.Crypto;
import FileSystem.FileSystem;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.SecretKey;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

public class Main {
    public static void main(String[] args){
        FileSystem fs;
        Path createPath = Paths.get("");
        Path mountPath = Paths.get("Aqua");
        try {
//            fs = FileSystem.createFileSystem(createPath, "Aqua", "kratos123");
            fs = FileSystem.mount(mountPath, "kratos123");
//            fs.createDirectory("/", "sheep");
//            fs.createDirectory("/","Obama1");
//            fs.createDirectory("/","Iqbal");
//            fs.createDirectory("/Iqbal","Iqbal2");
//            fs.createDirectory("/Obama1","Obama2");
//            fs.createDirectory("/Obama1/Obama2","Obama3");
//            fs.createDirectory("/Obama1/Obama2/Obama3","Obama4");
            Path path = Paths.get("file3.jpg");
            if (!path.toFile().exists())
                throw new Exception("NO SUCH FILE.");
//            fs.addFile(path);
//            fs.removeNode("/home/rogue/Desktop/Files/FYP/Project/FYP/file2.jpg");
//            fs.removeDirectory("/Obama1", true);
//            fs.ls("/");
//            fs.ls("/");
            fs.printTree();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
