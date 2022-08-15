package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.ManufacturerDescriptionOpencart;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ManufacturerDescriptionOpencartDaoTest {
    @Autowired
    private ManufacturerDescriptionOpencartDao manufacturerOpencartDescriptionDao;

    @Test
    void save() {
        ManufacturerDescriptionOpencart manufacturerDescriptionOpencart = new ManufacturerDescriptionOpencart.Builder()
                .withManufacturerId(11)
                .withLanguageId(1)
                .withDescription("test manu desc")
                .withMetaDescription("test manu metadesc")
                .withMetaKeyword("test manu keyword")
                .withMetaTitle("test manu metatitle")
                .withMetaH1("test manu metah1")
                .build();
        manufacturerOpencartDescriptionDao.save(manufacturerDescriptionOpencart);
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