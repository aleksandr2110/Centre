package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.entity.opencart.AttributeOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class AttributeOpencartDaoTest {

    @Autowired
    private AttributeOpencartDao attributeOpencartDao;

    @Test
    void save() {
        AttributeOpencart attributeOpencart = new AttributeOpencart.Builder()
                .withAttributeGroupId(6)
                .withSortOrder(9)
                .build();
        int id = attributeOpencartDao.save(attributeOpencart);
        log.info("id att: {}", id);
    }

    @Test
    void getById() {
    }

    @Test
    void getAttributesByProduct() {
//        List<AttributeWrapper> attributesByProduct = attributeOpencartDao.getAttributesByProduct(new ProductOpencart.Builder().withProductId(188352561).build());
        List<AttributeWrapper> attributesByProduct = attributeOpencartDao.getAttributesByProduct(new ProductOpencart.Builder().withProductId(188340527).build());

        attributesByProduct.forEach(a -> log.info("Attr: {}", a));
    }

    @Test
    void getByName(){
        AttributeOpencart attributeOpencart = attributeOpencartDao.getByName("asdgadshcgfcvfsgbdfad dfs  dsfgdsfg");
        log.info("attributeOpencart: {}", attributeOpencart);
    }

    @Test
    void getAllWithDesc() {
        List<AttributeOpencart> allWithDesc = attributeOpencartDao.getAllWithDesc();
        allWithDesc.forEach(a -> log.info("Attribute: {}", a));
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