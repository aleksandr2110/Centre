package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.AttributeGroupDescriptionOpencart;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class AttributeGroupDescriptionOpencartDaoTest {
    @Autowired
    private AttributeGroupDescriptionOpencartDao attributeGroupDescriptionOpencartDao;

    @Test
    void save() {
        AttributeGroupDescriptionOpencart attributeGroupDescriptionOpencart = new AttributeGroupDescriptionOpencart.Builder()
                .withAttributeGroupId(7)
                .withLanguageId(1)
                .withName("test att group desc")
                .build();
        attributeGroupDescriptionOpencartDao.save(attributeGroupDescriptionOpencart);
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