package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.ImageOpencart;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ImageOpencartDaoTest {

    @Autowired
    private ImageOpencartDao imageOpencartDao;

    @Test
    void save() {
        ImageOpencart imageOpencart = new ImageOpencart.Builder()
                .withProductId(50)
                .withImage("catalog/app/playstation-ps5-ps4-sony.jpg")
                .build();
        imageOpencartDao.save(imageOpencart);
    }

    @Test
    void getById() {
    }

    @Test
    void deleteById() {
    }

    @Test
    void update() {
        ImageOpencart imageOpencart = new ImageOpencart();
        imageOpencart.setImage("123123");
        imageOpencart.setProductId(188377267);
        imageOpencart.setProductImageId(375749);
        imageOpencartDao.update(imageOpencart);
    }

    @Test
    void getImageByProductId() {
        List<ImageOpencart> imageByProductId = imageOpencartDao.getImageByProductId(188353005);
        imageByProductId.forEach(i -> log.info("Image oc: {}", i));

    }

    @Test
    void deleteByProductId() {
        imageOpencartDao.deleteByProductId(39771500);
    }
}