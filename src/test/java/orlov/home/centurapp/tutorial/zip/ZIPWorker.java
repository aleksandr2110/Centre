package orlov.home.centurapp.tutorial.zip;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
public class ZIPWorker {
    public static void main(String[] args) throws IOException {

        ZipFile zipFile = new ZipFile(new File("zipFile.zip"));
//        ZipEntry entry = zipFile.getEntry("test_zip.txt");
//        log.info("entry: {}", entry);


    }
}
