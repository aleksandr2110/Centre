package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceAstimTest {
    private final String SUPPLIER_NAME = "astim";
    private final String SUPPLIER_URL = "https://astim.in.ua/";
    private final String DISPLAY_NAME = "74 - АС-ТІМ";
    private final String SUPPLIER_URL_XML = "https://astim.in.ua/export/astim.xml";
    private final String SUPPLIER_URL_EXCEL = "https://astim.in.ua/datawork/downbasexls";
    private final String URL_PART_PAGE = "&p=";


    @Autowired
    private ParserServiceAstim parserServiceAstim;

    @Test
    void doProcess() {
        parserServiceAstim.doProcess();
    }


    @Test
    void updateModel(){
        parserServiceAstim.updateModel();
    }

//    @Test
//    void testAttribute(){
//        parserServiceAstim.testAttribute();
//    }

    @Test
    void getOffersFromSiteAsExcel() {
        parserServiceAstim.getOffersFromSiteAsExcel();
    }
    @Test
    void getAttributeWrapper(){
        SupplierApp supplierApp = parserServiceAstim.buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        String html = "<p>виробник&nbsp;- TECASA<br />температура вимкнення: 230 &deg;C<br />діаметр датчика - 3 мм <br />довжинана датчика - 155 мм <br />довжина капілярної трубки - 890 мм <br />довжина капілярной трубки окремо - 600 мм <br />датчик - CNS&nbsp;</p>";
        List<AttributeWrapper> attributeWrapper = parserServiceAstim.getAttributeWrapper(html, supplierApp);
//        attributeWrapper.forEach(a -> log.info("Key {}, Value: {}", a.getKeySite(), a.getValueSite()));
    }

    @Test
    void getProductsInitDataByCategory(){
        SupplierApp supplierApp = parserServiceAstim.buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductOpencart> productsInitDataByCategory = parserServiceAstim.getProductsInitDataByCategory(null, supplierApp);
//        productsInitDataByCategory
//                .forEach(p -> log.info("\n\n\tProduct opencart: {}", p));
    }

//    @Test
//    void updateDescription(){
//        parserServiceAstim.updateDescription();
//    }


}