package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceUhlmashTest {

    private final String SUPPLIER_NAME = "uhlmash";
    private final String SUPPLIER_URL = "https://uhl-mash.com.ua/ua/";
    private final String DISPLAY_NAME = "99 - УХЛ-МАШ";
    private final String URL_PART_PAGE = "?PAGEN_1=";
    private final String MANUFACTURER_NAME = "УХЛ-МАШ";

    @Autowired
    private ParserServiceUhlmash parserServiceUhlmash;

    @Test
    void doProcess() {
        parserServiceUhlmash.doProcess();
    }

    @Test
    void getFullProductsData() {
        SupplierApp supplierApp = parserServiceUhlmash.buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);

        ProductOpencart productOpencart = new ProductOpencart.Builder()
                .withUrlProduct("https://uhl-mash.com.ua/ua/products/instrument_v_lozhementi/nabir_instrumentu_1_4_72od_v_lozhementi.php").build();
        List<ProductOpencart> productOpencartList = new ArrayList<>();
        productOpencartList.add(productOpencart);
        parserServiceUhlmash.getFullProductsData(productOpencartList, supplierApp);

    }
}