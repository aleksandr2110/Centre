package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.AttributeDescriptionOpencart;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class AttributeDescriptionOpencartDaoTest {

    @Autowired
    private AttributeDescriptionOpencartDao attributeDescriptionOpencartDao;

    @Test
    void save() {
        AttributeDescriptionOpencart attributeDescriptionOpencart = new AttributeDescriptionOpencart.Builder()
                .withAttributeId(13)
                .withName("test attribute desc")
                .build();
        attributeDescriptionOpencartDao.save(attributeDescriptionOpencart);
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