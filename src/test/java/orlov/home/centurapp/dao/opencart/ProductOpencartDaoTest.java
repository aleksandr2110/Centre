package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.dto.ManufacturerUpdateDto;
import orlov.home.centurapp.dto.ProductInfoDto;
import orlov.home.centurapp.dto.ProductToAttributeDto;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ProductOpencartDaoTest {



    @Autowired
    private ProductOpencartDao productOpencartDao;

    @Test
    void save() {
        ProductOpencart productOpencart = new ProductOpencart.Builder()
                .withModel("test model cust")
                .build();
        long id = productOpencartDao.save(productOpencart);
        log.info("Id product: {}", id);
    }



    @Test
    void getModels() {
        List<ProductInfoDto> allModelByAttributeId = productOpencartDao.getAllModelByAttributeId(2554);
        log.info("Size: {}", allModelByAttributeId.size());
    }

    @Test
    void getLastProductModel() {
        int lastProductModel = productOpencartDao.getLastProductModel();
        log.info("lastProductModel: {}", lastProductModel);
    }

    @Test
    void getProductWithDescriptionById() {
        ProductOpencart p = productOpencartDao.getProductWithDescriptionById(188344488);
        log.info("Product. Id: {}, Jan: {}, Sku: {}, Name: {}", p.getId(), p.getJan(), p.getSku(), p.getProductsDescriptionOpencart().get(0).getName());
    }

    @Test
    void getCategoryIdByProductId() {
        List<Integer> categoryIdByProductId = productOpencartDao.getCategoryIdByProductId(188352382);
        log.info("categoryIdByProductId: {}", categoryIdByProductId);
    }

    @Test
    void deleteProductToCategoryByProductId() {
        productOpencartDao.deleteProductToCategoryByProductId(188352382);
    }

    @Test
    void deleteById() {
        productOpencartDao.deleteById(1509433);
    }

    @Test
    void update() {
    }

    @Test
    void getProductToAttributeBySupplierName() {
        List<ProductToAttributeDto> all = productOpencartDao.getProductToAttributeBySupplierName("НОВИЙ СТИЛЬ");
        log.info("All size: {}", all.size());
    }

    @Test
    void getAll() {
        List<ProductOpencart> products = productOpencartDao.getAll();
        products.forEach(p -> log.info("Product: {}", p));
    }

    @Test
    void updateProductManufacturer() {
        productOpencartDao.updateProductManufacturer(new ManufacturerUpdateDto("indigowood", "smennyiy_komplekt_v_detskuyu_krovatku_locomotives", 777));
    }

    @Test
    void deleteProductToStore() {
        productOpencartDao.deleteProductToStore(1509433);
    }

    @Test
    void deleteProductToCategory() {
        productOpencartDao.deleteProductToCategory(1509433);
    }

    @Test
    void deleteProductAttribute() {
        productOpencartDao.deleteProductAttribute(1509433);
    }

    @Test
    void deleteProductDescription() {
        productOpencartDao.deleteProductDescription(1509433);
    }


    @Test
    void getProductsSameTitle() {
        String title = "Млинниця електрична VP-";
        List<ProductOpencart> productsSameTitle = productOpencartDao.getProductsSameTitle(title);
        log.info("Product title: {} same title: {}",title, productsSameTitle.size());
    }

    @Test
    void getSupplierProducts() {
        String supplierName = "";
        List<ProductOpencart> supplierProducts = productOpencartDao.getSupplierProducts(supplierName);
    }
}