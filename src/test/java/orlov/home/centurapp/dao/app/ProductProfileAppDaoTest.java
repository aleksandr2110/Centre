package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.ProductProfileApp;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ProductProfileAppDaoTest {

    @Autowired
    private ProductProfileAppDao productProfileAppDao;

    @Test
    void save() {
        productProfileAppDao.save(new ProductProfileApp.Builder()
                .withSupplierId(8)
                .withUrl("https://www.test.ua")
                .withSku("test sku")
                .withTitle("test title")

                .build());
    }

    @Test
    void getById() {
    }

    @Test
    void deleteById() {
    }

    @Test
    void update() {
        productProfileAppDao.update(new ProductProfileApp.Builder()
                .withProductProfileId(1)
                .withSupplierId(8)
                .withUrl("https://www.test.ua/")
                .withSku("test sku")
                .withTitle("test title")
                .build());
    }

    @Test
    void getAll() {
        List<ProductProfileApp> all =
                productProfileAppDao.getAll();
            all.forEach(p -> log.info("Product: {}", p));
    }



    @Test
    void getAllBySupplierId() {
        List<ProductProfileApp> all =
                productProfileAppDao.getAllBySupplierId(8);
        all.forEach(p -> log.info("Product: {}", p));
    }
}