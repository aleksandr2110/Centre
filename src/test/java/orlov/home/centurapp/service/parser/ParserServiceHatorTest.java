package orlov.home.centurapp.service.parser;

import org.apache.commons.math3.stat.descriptive.summary.Product;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ParserServiceHatorTest {

    @Autowired
    private ParserServiceHator parserServiceHator;
    private final String SUPPLIER_NAME = "hator";
    private final String SUPPLIER_URL = "https://hator-m.com/";
    private final String SUPPLIER_URL_CATEGORY = "";
    private final String DISPLAY_NAME = "179 - ХАТОР-М";
    private final String MANUFACTURER_NAME = "";
    private final String URL_PART_PAGE = "?page=";


    @Test
    void doProcess() {
        parserServiceHator.doProcess();
    }

    @Test
    void getSiteCategories() {
    }

    @Test
    void getProductsInitDataByCategory() {
    }

    @Test
    void getFullProductsData() {
        SupplierApp supplierApp = parserServiceHator.buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);

//        List<CategoryOpencart> siteCategories = parserServiceHator.getSiteCategories(supplierApp);
        ProductOpencart productOpencart = new ProductOpencart.Builder()
                .withUrlProduct("https://hator-m.com/veshalka-sekcionnaja-buk-cvet")
                .build();
        List<ProductOpencart> fullProductsData = parserServiceHator.getFullProductsData(Collections.singletonList(productOpencart), supplierApp);


    }
}