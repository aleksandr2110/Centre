package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceRPTest {
    private final String SUPPLIER_NAME = "rp";
    private final String SUPPLIER_URL = "http://www.rp.ua/";
    private final String DISPLAY_NAME = "115 - РП УКРАЇНА";
    private final String URL_PART_PAGE = "&p=";
    private final String URL_FILE_LINK = "http://www.rp.ua/ru/prais";

    @Autowired
    private ParserServiceRP parserServiceRP;

    @Test
    void doProcess() {
        parserServiceRP.doProcess();
    }

    @Test
    void getProductFile() {
        parserServiceRP.getProductsFromFile();
    }

    @Test
    void downloadImages() {
        parserServiceRP.downloadImages();
    }


    @Test
    void updateMainImage() {
        parserServiceRP.updateMainImage();
    }

    @Test
    void translateSupplierProducts(){
        parserServiceRP.translateSupplierProducts();
    }

    @Test
    void getFullProductData() {
        SupplierApp supplierApp = parserServiceRP.buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<CategoryOpencart> siteCategories = parserServiceRP.getSiteCategories(supplierApp);
        List<ProductOpencart> productsFromSite = parserServiceRP.getProductsInitDataByCategory(siteCategories, supplierApp);
        log.info("Supplier products count: {}", productsFromSite.size());
        ProductOpencart productOpencart = productsFromSite
                .stream()
                .filter(p -> p.getSku().equals("850"))
                .findFirst()
                .orElse(null);
        log.info("productOpencart: {}", productOpencart);
        List<ProductOpencart> fullProductsData = parserServiceRP.getFullProductsData(Collections.singletonList(productOpencart), supplierApp);

    }
}