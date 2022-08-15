package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.ProductAttributeApp;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ProductAttributeAppDaoTest {

    @Autowired
    private ProductAttributeAppDao productAttributeAppDao;

    @Test
    void save() {
        ProductAttributeApp productAttributeApp = new ProductAttributeApp();
        productAttributeApp.setProductProfileAppId(12502);
        productAttributeApp.setAttributeAppId(460);
        productAttributeApp.setAttributeValue("test bob");
        productAttributeAppDao.save(productAttributeApp);
    }

    @Test
    void getById() {
    }

    @Test
    void getProductAttributeId() {
        ProductAttributeApp productAttributeId = productAttributeAppDao.getProductAttributeId(12502, 460);
        log.info("Product attribute: {}", productAttributeId);
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