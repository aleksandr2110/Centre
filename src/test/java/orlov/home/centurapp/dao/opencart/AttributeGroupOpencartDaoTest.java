package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.AttributeGroupOpencart;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class AttributeGroupOpencartDaoTest {

    @Autowired
    private AttributeGroupOpencartDao attributeGroupOpencartDao;

    @Test
    void save() {
        AttributeGroupOpencart attributeGroupOpencart = new AttributeGroupOpencart.Builder()
                .withSortOrder(77)
                .build();
        int id = attributeGroupOpencartDao.save(attributeGroupOpencart);
        log.info("id att group: {}", id);
    }

    @Test
    void getAttributeGroupByName(){
        AttributeGroupOpencart attribute = attributeGroupOpencartDao.getDefaultGlobalAttributeGroupByName("Характеристики");
        log.info("Attribute: {}", attribute);
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