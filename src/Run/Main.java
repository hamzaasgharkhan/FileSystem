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
            Path path = Paths.get("file2.jpg");
            if (!path.toFile().exists())
                throw new Exception("NO SUCH FILE.");
            fs.addFile(path);
//            fs.removeNode("/home/rogue/Desktop/Files/FYP/Project/FYP/file4.jpg");
//            fs.removeDirectory("/home/rogue/", true);
//            fs.ls("/");
//            fs.ls("/");
            fs.printTree();


            // TESTING ENCRYPTION
//            Security.addProvider(new BouncyCastleProvider());
//            Crypto.init();
//            String password = "securepassword";
//            byte[] salt = new byte[16];
//            SecureRandom random = new SecureRandom();
//            random.nextBytes(salt);
//            SecretKey key = Crypto.deriveKeyFromPassword(password, salt);
//            byte[] plaintext = new byte[20];
//            new SecureRandom().nextBytes(plaintext);
//
//            byte[] encryptedData = Crypto.encryptBlock(plaintext, key, 20);
//            System.out.println("Encryption Successful!");
//            System.out.println("Encrypted Data Length: " + encryptedData.length);
//
//
//            byte[] decryptedData = Crypto.decryptBlock(encryptedData, key, 20);
//            System.out.println("Decryption Successful!");
//            System.out.println("Decrypted Data Length: " + decryptedData.length);
//
//            boolean isEqual = Arrays.equals(plaintext, decryptedData);
//            System.out.println("Decryption Verification: " + (isEqual ? "Successful" : "Failed"));



        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
