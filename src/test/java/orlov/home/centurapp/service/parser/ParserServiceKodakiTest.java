package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;
import orlov.home.centurapp.service.daoservice.app.SupplierAppService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceKodakiTest {

    @Autowired
    private SupplierAppService supplierAppService;

    @Autowired
    private ParserServiceKodaki parserServiceKodaki;

    @Test
    void doProcess() {
        parserServiceKodaki.doProcess();
    }

    @Test
    void updateDescription() {
        parserServiceKodaki.updateDescription();
    }

    @Test
    void updateModel(){
        parserServiceKodaki.updateModel();
    }

    @Test
    void getDescription() {
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk-ua");
        Document webDocument = parserServiceKodaki.getWebDocument("https://kodaki.ua/vspomogatelnoe-oborudovanie/avtoholodilniki/avtoholodilnik-a-18x", cookies);
        String description = parserServiceKodaki.getDescription(webDocument);
        log.info("Result: {}", description);
    }


    @Test
    void updateCategoryOpencart() {
//        parserServiceKodaki.updateCategoryOpencart();
    }

    @Test
    void getFullProductsData() {
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk-ua");
        Document webDocument = parserServiceKodaki.getWebDocument("https://kodaki.ua/barnoe-i-kofejnoe-oborudovanie/blendery/blender-professionalnyj-010e", cookies);
        Elements manuElem = webDocument.select("li:contains(Виробник: )");
        String manufacturer = "";
        if (!manuElem.isEmpty()){
            manufacturer = manuElem.select("a").text().trim();
        }
        log.info("Manu elem: {}", manufacturer);
    }

    @Test
    void saveNewProduct() {
    }
}