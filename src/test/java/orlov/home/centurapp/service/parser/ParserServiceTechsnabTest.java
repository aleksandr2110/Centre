package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.ProductProfileApp;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceTechsnabTest {
    private final String SUPPLIER_NAME = "techsnab";
    private final String SUPPLIER_URL = "https://techsnab.com.ua/ua/";
    private final String DISPLAY_NAME = "156 - Техснаб";
    private final String URL_PART_PAGE = "?page=";
    @Autowired
    private ParserServiceTechsnab parserServiceTechsnab;

    @Test
    void doProcess() {
        parserServiceTechsnab.doProcess();
    }

    @Test
    void getFullProductsData() {
        SupplierApp supplierApp = parserServiceTechsnab.buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);

        ProductOpencart productOpencart = new ProductOpencart.Builder()
                .withUrlProduct("https://techsnab.com.ua/ua/shkaf-holodilnyy-cm114-s-1400l-polair--663").build();
        List<ProductOpencart> productOpencartList = new ArrayList<>();
        productOpencartList.add(productOpencart);
        parserServiceTechsnab.getFullProductsData(productOpencartList, supplierApp);
    }
}