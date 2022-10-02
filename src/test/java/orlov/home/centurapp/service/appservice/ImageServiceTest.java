package orlov.home.centurapp.service.appservice;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ImageServiceTest {

    @Autowired
    private ImageService imageService;

    @Test
    void createImageByHex() {
        String hexColor = "#9fa6a5";
        imageService.createOptionImage(hexColor);
    }
}