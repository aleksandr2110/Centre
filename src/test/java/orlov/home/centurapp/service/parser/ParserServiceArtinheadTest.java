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
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceArtinheadTest {
    private final String SUPPLIER_NAME = "artinhead";
    private final String SUPPLIER_URL = "https://artinhead.com/";
    private final String SUPPLIER_URL_CATEGORY = "https://artinhead.com/product-category/all/";
    private final String DISPLAY_NAME = "20 - ART IN HEAD";
    private final String MANUFACTURER_NAME = "ART IN HEAD";
    private final String URL_PART_PAGE = "page/";
    @Autowired
    private ParserServiceArtinhead parserServiceArtinhead;

    @Test
    void doProcess() {
        parserServiceArtinhead.doProcess();
//        parserServiceArtinhead.doProcess();
    }


    @Test
    public void updateAttributeValue() {
        parserServiceArtinhead.updateAttributeValue();
    }

    @Test
    void getProductSupplierModel(){
        String url = "https://artinhead.com/product-category/all/beds/";
        Document doc = parserServiceArtinhead.getWebDocument(url, new HashMap<>());
        Elements productsElement = doc.select("div.item.product-item");
        productsElement
                .forEach(p -> {
                    String sku = p.select("a[data-product_id]").attr("data-product_id");
                    String modelSupplier = p.select("a[data-product_sku]").attr("data-product_sku");
                    String model = parserServiceArtinhead.generateModel(modelSupplier, sku);
                    log.info("Model: {}", model);
                });

    }

    @Test
    void settingOptionsOpencart(){
        String url = "https://artinhead.com/product/krovatka-cherdak-detskaya-binky-ds37a-dub-taho-belyj-supermat-mdf/";
        Document webDocument = parserServiceArtinhead.getWebDocument(url, new HashMap<>());
        SupplierApp supplierApp = parserServiceArtinhead.buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        ProductOpencart jsonDataFromDocument = parserServiceArtinhead.settingOptionsOpencart(webDocument, new ProductOpencart(), supplierApp);
        jsonDataFromDocument.getOptionsOpencart().forEach(o -> log.info("Option into product: {}", o));
    }

    @Test
    void updateModel(){
        parserServiceArtinhead.updateModel();
    }
}