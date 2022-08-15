package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.AttributeApp;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class AttributeAppDaoTest {

    @Autowired
    private AttributeAppDao attributeAppDao;

    @Test
    void save() {
        AttributeApp attr = new AttributeApp.Builder()
                .withSupplierId(8)
                .withSupplierTitle("Samsung")
                .build();
        int id = attributeAppDao.save(attr);
        log.info("Id save attribute: {}",id);
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
    void deleteAll() {
        attributeAppDao.deleteAll();
    }

    @Test
    void getAllBySupplierId() {
        List<AttributeApp> allBySupplierId = attributeAppDao.getAllBySupplierId(8);
        allBySupplierId
                .forEach(a -> log.info("Attribute app: {}", a));
    }
}