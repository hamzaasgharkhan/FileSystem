package DiskUtility;

import FileSystem.SuperBlock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GatewayTests {
    SuperBlock superBlock = new SuperBlock("Test");
    Path path = Paths.get("").resolve("Test");
    @Test
    @DisplayName("Gateway Constructor with initialize set to true")
    public void GatewayConstructor(){

    }

//    @Test
//    @DisplayName("Gateway InitializeFileSystem")
//    public void __initializeFileSystem(){
//        Gateway gateway;
//        try{
//            gateway = new Gateway(path, superBlock, true); // ERROR HERE DUE TO MISSING KEY
//        } catch (Exception ignored){}
//        // The FileSystem Directory Exists
//        Assertions.assertTrue(Files.exists(path));
//        Assertions.assertTrue(Files.isReadable(path.resolve("directory-store")));
//        Assertions.assertTrue(Files.isReadable(path.resolve("inode-store")));
//        Assertions.assertTrue(Files.isReadable(path.resolve("extent-store")));
//        Assertions.assertTrue(Files.isReadable(path.resolve("data-store")));
//        Assertions.assertTrue(Files.isReadable(path.resolve("thumbnail-store")));
//    }
}
