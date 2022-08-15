package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.ManufacturerApp;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ManufacturerAppDaoTest {

    @Autowired
    private ManufacturerAppDao manufacturerAppDao;

    @Test
    void save() {
        int id = manufacturerAppDao.save(new ManufacturerApp.Builder()
                .withSupplierId(8)
                .withSupplierTitle("NOKIA")
                .withOpencartTitle("Opencart manufacturer name")
                .withMarkup(13)
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
        ManufacturerApp opencartManufacturerName = manufacturerAppDao.update(new ManufacturerApp.Builder()
                .withManufacturerId(3)
                .withSupplierId(8)
                .withSupplierTitle("NOKIA")
                .withOpencartTitle("2. Opencart manufacturer name")
                .withMarkup(13)
                .build());
        log.info("Manufacture updated: {}", opencartManufacturerName);
    }

    @Test
    void getAll() {
        List<ManufacturerApp> manufacturersApp = manufacturerAppDao.getAll();
        manufacturersApp
                .forEach(m -> log.info("Manufacture app: {}", m));
    }
}