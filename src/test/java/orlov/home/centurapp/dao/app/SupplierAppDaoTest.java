package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class SupplierAppDaoTest {

    @Autowired
    private SupplierAppDao dao;

    @Test
    void save() {
        SupplierApp supplierApp = new SupplierApp("https://nowystyl.ua/", "nowystyl", "NOWY STYL", 20);
        int id = dao.save(supplierApp);
        log.info("Supplier id: {}", id);
    }

    @Test
    void getByName() {
        SupplierApp nowystyl = dao.getByName("nowystyl");
        log.info("supplier by name nowystyl : {}", nowystyl);
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