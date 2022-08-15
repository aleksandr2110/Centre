package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.ManufacturerOpencart;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ManufacturerOpencartDaoTest {

    @Autowired
    private ManufacturerOpencartDao manufacturerOpencartDao;

    @Test
    void save() {
        ManufacturerOpencart manufacturerOpencart = new ManufacturerOpencart.Builder()
                .withName("test manu name")
                .withImage("test manu img")
                .withSortOrder(0)
                .withNoindex(true)
                .build();
        long id = manufacturerOpencartDao.save(manufacturerOpencart);
        log.info("Id manu: {}", id);

    }

    @Test
    void getById() {
    }

    @Test
    void getByName() {
        ManufacturerOpencart htc = manufacturerOpencartDao.getByName("HTC");
        log.info("Manufacturer: {}", htc);
    }

    @Test
    void deleteById() {
    }

    @Test
    void update() {
    }

    @Test
    void getAll() {
        List<ManufacturerOpencart> all = manufacturerOpencartDao.getAll();
        all.forEach(m -> log.info("Manufacturer: {}", m));
    }
}