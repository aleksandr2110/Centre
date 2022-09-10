package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.ProductOptionValueOpencart;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ProductOptionValueDaoTest {

    @Autowired
    private ProductOptionValueDao productOptionValueDao;

    @Test
    void save() {
        ProductOptionValueOpencart value = new ProductOptionValueOpencart();
        value.setProductOptionId(9401);
        value.setProductId(188327316);
        value.setOptionId(36);
        value.setOptionValueId(418);
        value.setOptsku("test x");

        int save = productOptionValueDao.save(value);
        log.info("ID: {}", save);
    }

    @Test
    void update() {
        ProductOptionValueOpencart value = new ProductOptionValueOpencart();
        value.setProductOptionValueId(14243);
        value.setQuantity(99);
        value.setProductOptionId(6433);
        value.setProductId(188303177);
        value.setOptionId(75);
        value.setOptionValueId(1671);
        value.setOptsku("test x");
        productOptionValueDao.update(value);
    }

    @Test
    void deleteProductOptionValue(){
        productOptionValueDao.deleteProductOptionValue(188381099, 245, 3859);
    }
}