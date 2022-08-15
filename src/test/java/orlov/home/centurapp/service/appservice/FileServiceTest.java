package orlov.home.centurapp.service.appservice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.ImageOpencart;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class FileServiceTest {

    @Autowired
    private FileService fileService;


    @Test
    void writeToFile() {
    }

    @Test
    void downloadImg() {
        fileService.downloadImg("http://www.rp.ua/images/img/cooleq/SM-10.jpg", "catalog/app/SM-10.jpg");
    }

    @Test
    void updateImageUseZipFile() {
    }

    @Test
    void createZipImage() {
    }

    @Test
    void createExcelProduct() {
    }

    @Test
    void updateProductUseFile() {
    }

    @Test
    void deleteImageFile() {
        ImageOpencart image = new ImageOpencart();
        image.setImage("catalog/app/______01_1.jpg");
        fileService.deleteImageFile(image);
    }
}