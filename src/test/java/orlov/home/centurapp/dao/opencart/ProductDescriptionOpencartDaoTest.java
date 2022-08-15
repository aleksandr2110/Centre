package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ProductDescriptionOpencartDaoTest {

    @Autowired
    private ProductDescriptionOpencartDao productDescriptionOpencartDao;

    @Test
    void save() {
        ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                .withProductId(50)
                .withLanguageId(1)
                .withName("test description name")
                .withDescription("test product description")
                .withMetaTitle("test product meta title")
                .withMetaDescription("test product meta description")
                .withMetaKeyword("test product meta keyword")
                .build();
        productDescriptionOpencartDao.save(productDescriptionOpencart);
    }

    @Test
    void getById() {
    }

    @Test
    void deleteById() {
    }

    @Test
    void update() {
    }

    @Test
    void getAll() {
    }
}