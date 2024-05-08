package Run;
import FileSystem.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
public class Main {
    public static void main(String[] args){
        FileSystem fs;
        Path createPath = Paths.get("");
        Path mountPath = Paths.get("Aqua");
        try {
            fs = FileSystem.createFileSystem(createPath, "Aqua");
//            fs = FileSystem.mount(mountPath);
//            fs.createDirectory("/","Obama1");
//            fs.createDirectory("/","Osama1");
//            fs.createDirectory("/","Iqbal");
//            fs.createDirectory("/Iqbal","Iqbal2");
//            fs.createDirectory("/","Obama");
//            fs.createDirectory("/Obama","Obama3");
//            fs.createDirectory("/Obama1","Obama2");
//             LS needs to be fixed.
//            fs.createDirectory("/", "Obama");
            Path path = Paths.get("file.jpg");
            if (!path.toFile().exists())
                throw new Exception("NO SUCH FILE.");
            fs.addFile(path);
//            fs.ls("/");
            fs.ls("/");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
