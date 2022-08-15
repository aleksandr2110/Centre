package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.ProductSupplierOpencart;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ProductSupplierOpencartDaoTest {

    @Autowired
    private ProductSupplierOpencartDao productSupplierOpencartDao;

    @Test
    void save() {
        ProductSupplierOpencart productSupplierOpencart = new ProductSupplierOpencart();
        productSupplierOpencart.setProductId(188359913);
        productSupplierOpencart.setSupCode("3 - МАРЕСТО");
        productSupplierOpencart.setPrice(new BigDecimal("25.77"));
        productSupplierOpencart.setIsPdv("готівка, К, БбПДВ, БзПДВ");
        productSupplierOpencart.setCurrency("UAH");
        productSupplierOpencart.setAvailability("під замовлення (27.09.2021)");
        productSupplierOpencartDao.save(productSupplierOpencart);
    }

    @Test
    void getAllProductSupplierBySupCode() {
        List<ProductSupplierOpencart> allProductSupplierBySupCode = productSupplierOpencartDao.getAllProductSupplierBySupCode("3 - МАРЕСТО");
        allProductSupplierBySupCode.forEach(p -> log.info("Product supplier: {}", p));
    }

    @Test
    void updatePDVProductSupplier() {

    }

    @Test
    void updatePriceProductSupplier() {
        ProductSupplierOpencart productSupplierOpencart = new ProductSupplierOpencart();
        productSupplierOpencart.setProductId(188359913);
        productSupplierOpencart.setSupCode("3 - МАРЕСТО");
        productSupplierOpencart.setPrice(new BigDecimal("7.77"));
        productSupplierOpencart.setIsPdv("готівка, К, БбПДВ, БзПДВ");
        productSupplierOpencart.setCurrency("UAH");
        productSupplierOpencart.setAvailability("під замовлення (27.09.2021)");
        productSupplierOpencartDao.updatePriceProductSupplier(productSupplierOpencart);
    }
}