package Run;
import net.coobird.thumbnailator.Thumbnails;
import DiskUtility.CustomInputStream;
import FileSystem.FileSystem;
import FileSystem.InputFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

public class Main {
    public static void main(String[] args){
        FileSystem fs;
        Path parentPath = Paths.get("");
        File createPath = parentPath.toAbsolutePath().toFile();
        File mountPath = new File("Aqua");
        try {
            Path path1 = Paths.get("file1.jpg");
            Path path2 = Paths.get("file2.jpg");
            Path path3 = Paths.get("file3.jpg");
            Path path4 = Paths.get("file4.jpg");
            Path path5 = Paths.get("1.png");
            Path path6 = Paths.get("1.mp4");
            Path largePath = Paths.get("large.mp4");
            Path pdfPath = Paths.get("cv.pdf");
            if (!path1.toFile().exists()) {
                throw new Exception("NO SUCH FILE.");
            }
            if (!path2.toFile().exists()) {
                throw new Exception("NO SUCH FILE.");
            }
            if (!path3.toFile().exists()) {
                throw new Exception("NO SUCH FILE.");
            }
            if (!path4.toFile().exists()) {
                throw new Exception("NO SUCH FILE.");
            }
            if (!path5.toFile().exists()) {
                throw new Exception("NO SUCH FILE.");
            }
            if (!path6.toFile().exists()) {
                throw new Exception("NO SUCH FILE.");
            }
            if (!largePath.toFile().exists()) {
                throw new Exception("NO SUCH FILE.");
            }
            if (!pdfPath.toFile().exists()) {
                throw new Exception("NO SUCH FILE.");
            }
            InputFile file5 = getInputFile(path5, Path.of("testThumbnail.jpg"));
            InputFile pdf = getInputFile(pdfPath);
            fs = FileSystem.createFileSystem(createPath, "Aqua", "kratos123");
//            fs = FileSystem.mount(mountPath, "kratos123");
            fs.addFile(pdf);
            fs.moveNode("/home/reikhan/Desktop/Files/FYP/Project/FYP/cv.pdf", "/home");
            fs.removeNode("/home/cv.pdf");
            fs.addFile(file5);
            fs.moveNode("/home/reikhan/Desktop/Files/FYP/Project/FYP/1.png", "/home");
            fs.removeNode("/home/1.png");
            fs.addFile(getInputFile(path5, Path.of("testThumbnail.jpg")));
//            fs.copyNode("/1.png", "/home");
//            writeFileToDisk(fs, "/cv.pdf", "cv.pdf");
//            fs.removeNode("/home", true);
//            writeFileToDisk(fs, "/1.png", "moved-1.png");
//            writeFileToDisk(fs, "/home/1.png", "copied-1.png");
//            fs.createDirectory("/", "sheep");
//            fs.renameNode("/sheep", "sheep123");
//            fs.createDirectory("/","Obama1");
//            fs.createDirectory("/","Iqbal");
//            fs.createDirectory("/Iqbal","Iqbal2");
//            fs.createDirectory("/Obama1","Obama2");
//            fs.createDirectory("/Obama1/Obama2","Obama3");
//            fs.createDirectory("/Obama1/Obama2/Obama3","Obama4");
//            fs.addFile(file5);
//            fs.addFile(path2);
//            fs.removeNode("/home/reikhan/Desktop/Files/FYP/Project/FYP/file1.jpg");
//            fs.addFile(path3);
//            fs.removeNode("/home/reikhan/Desktop/Files/FYP/Project/FYP/file2.jpg");
//            fs.addFile(path4);
//            fs.addFile(path5);
//            fs.addFile(path6);
//            fs.addFile(largePath);
//            writeFileToDisk(fs, "/home/reikhan/Desktop/Files/FYP/Project/FYP/file1.jpg", "output1.jpg");
//            writeFileToDisk(fs, "/home/reikhan/Desktop/Files/FYP/Project/FYP/file2.jpg", "output2.jpg");
//            writeFileToDisk(fs, "/home/reikhan/Desktop/Files/FYP/Project/FYP/file3.jpg", "output3.jpg");
//            writeFileToDisk(fs, "/home/reikhan/Desktop/Files/FYP/Project/FYP/file4.jpg", "output4.jpg");
//            writeFileToDisk(fs, "/home/reikhan/Desktop/Files/FYP/Project/FYP/1.png", "output5.png");
//            writeFileToDisk(fs, "/home/reikhan/Desktop/Files/FYP/Project/FYP/1.mp4", "output6.mp4");
//            writeFileToDisk(fs, "/home/reikhan/Desktop/Files/FYP/Project/FYP/large.mp4", "outputLarge.mp4");
//            fs.removeNode("/Obama1", true);
//            fs.ls("/");
//            fs.ls("/");
            fs.printTree();
//            thumbnailGeneration();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static InputFile getInputFile(Path path, Path thumbnailPath) throws Exception{
        Path parentPath = Paths.get("");
        long fileCreationTime, fileLastModifiedTime, fileSize;
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            fileCreationTime = attributes.creationTime().toMillis();
            fileLastModifiedTime = attributes.lastModifiedTime().toMillis();
            fileSize = attributes.size();
        } catch (IOException e){
            throw new Exception("Unable to access the attributes of the requested file." + e.getMessage());
        }
        return new InputFile(
                path.getFileName().toString(),
                parentPath.toAbsolutePath().toString(),
                fileSize,
                fileCreationTime,
                fileLastModifiedTime,
                new FileInputStream(path.toFile()),
                new FileInputStream(thumbnailPath.toFile()),
                thumbnailPath.toFile().length()
        );
    }

    public static InputFile getInputFile(Path path) throws Exception{
        Path parentPath = Paths.get("");
        long fileCreationTime, fileLastModifiedTime, fileSize;
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            fileCreationTime = attributes.creationTime().toMillis();
            fileLastModifiedTime = attributes.lastModifiedTime().toMillis();
            fileSize = attributes.size();
        } catch (IOException e){
            throw new Exception("Unable to access the attributes of the requested file." + e.getMessage());
        }
        return new InputFile(
                path.getFileName().toString(),
                parentPath.toAbsolutePath().toString(),
                fileSize,
                fileCreationTime,
                fileLastModifiedTime,
                new FileInputStream(path.toFile())
        );
    }


    public static void writeFileToDisk(FileSystem fs, String path, String outputName) throws Exception{
        CustomInputStream fin = fs.openFile(path);
        FileOutputStream fout = new FileOutputStream(outputName);
        byte[] buffer = new byte[4096];
        int len;
        while ((len = fin.read(buffer)) != -1) {
            fout.write(buffer, 0, len);
        }
        fin.close();
        fout.close();
        CustomInputStream thumbnail = fs.openThumbnail(path);
        if (thumbnail == null) return;
        fout = new FileOutputStream("thumbnail-" + outputName.split("\\.")[0] + ".jpg");
        len = 0;
        buffer = new byte[4096];
        while ((len = thumbnail.read(buffer)) != -1) {
            fout.write(buffer, 0, len);
        }
        thumbnail.close();
        fout.close();
    }
    public static void thumbnailGeneration(){
        try {
            // Example with FileInputStream
            FileInputStream fis = new FileInputStream("1.jpg");

            // Create a ByteArrayOutputStream to hold the thumbnail bytes
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Specify the size of the thumbnail
            int thumbnailSize = 640;

            // Create a thumbnail and write it to the ByteArrayOutputStream
            Thumbnails.of(fis)
                    .outputQuality(0.5)
                    .size(thumbnailSize, thumbnailSize)
                    .outputFormat("jpg") // Optional
//                    .toOutputStream(baos);
                    .toFile("testThumbnail");

            // Close the FileInputStream
            fis.close();

            // Convert the ByteArrayOutputStream to an InputStream
//            InputStream thumbnailInputStream = new ByteArrayInputStream(baos.toByteArray());

            // Now you can use thumbnailInputStream as needed, for example:
            // 1. Upload to a storage service
            // 2. Process further in-memory
            // 3. Return as a response in a web application, etc.

            // Close the ByteArrayOutputStream
//            baos.close();

            System.out.println("Thumbnail created successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
