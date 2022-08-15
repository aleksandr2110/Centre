package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.ProductApp;

import javax.swing.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ProductAppDaoTest {

    @Autowired
    private ProductAppDao dao;

    @Test
    void save() {
        ProductApp productApp = new ProductApp();
        productApp.setOrderProcessId(1);
        productApp.setName("test name");
        productApp.setUrl("http://test.app");
        productApp.setStatus("ord");
        productApp.setNewPrice(new BigDecimal("12.17"));
        productApp.setOldPrice(new BigDecimal("9.03"));
        int id = dao.save(productApp);
        log.info("product app id: {}", id);
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