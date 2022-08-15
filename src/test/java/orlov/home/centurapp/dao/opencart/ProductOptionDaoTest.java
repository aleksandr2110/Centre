package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.ProductOptionOpencart;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ProductOptionDaoTest {

    @Autowired
    private ProductOptionDao productOptionDao;

    @Test
    void save() {
        ProductOptionOpencart productOption = new ProductOptionOpencart();
        productOption.setProductId(188328115);
        productOption.setValue("test");
        productOption.setOptionId(28);
        int save = productOptionDao.save(productOption);
        log.info("ID: {}", save);
    }




    @Test
    void getProductOptionsById() {
        List<ProductOptionOpencart> options = productOptionDao.getProductOptionsById(188405644);
        options.forEach(o -> log.info("Option product: {}", o));
    }
}