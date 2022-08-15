package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;
import orlov.home.centurapp.service.daoservice.app.SupplierAppService;
import orlov.home.centurapp.service.daoservice.opencart.CategoryOpencartService;
import orlov.home.centurapp.service.daoservice.validator.HttpsUrlValidator;
import orlov.home.centurapp.util.AppConstant;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceMarestoTest {

    @Autowired
    private ParserServiceMaresto parserServiceMaresto;
    @Autowired
    private CategoryOpencartService categoryOpencartService;
    @Autowired
    private SupplierAppService supplierAppService;

    @Test
    void doProcess() {
        parserServiceMaresto.doProcess();
    }


    @Test
    void getDescription() {
        String urlProduct = "https://maresto.ua/ua/catalog/elektromekhanicheskoe_oborudovanie/ovoshcherezki/ovoshcherezka_el_reednee_vc65ms.html";
        Document doc = parserServiceMaresto.getWebDocument(urlProduct, new HashMap<>());
        String description = parserServiceMaresto.getDescription(doc);
        log.warn("RESULT: \n{}", description);

    }

    @Test
    void updateModel(){
        parserServiceMaresto.updateModel();
    }

    @Test
    void updateCategoryOpencart() {
//        parserServiceMaresto.updateCategoryOpencart();
    }

    @Test
    void cleanDescription() {
        String url = "https://maresto.ua/ua/catalog/teplovoe_oborudovanie/";
        Document webDocument = getWebDocument(url, new HashMap<>());


    }

    public Document getWebDocument(String url, Map<String, String> cookies) {
        Document document = null;
        int countBadConnection = 0;
        boolean hasConnection = false;

        while (!hasConnection && countBadConnection < AppConstant.BAD_CONNECTION_LIMIT) {
            try {
                HttpsUrlValidator.retrieveResponseFromServer(url);
                Connection.Response response = Jsoup.connect(url)
                        .maxBodySize(0)
                        .timeout(60 * 1000)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36")
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .cookies(cookies)
                        .execute();
                int statusCode = response.statusCode();
                log.info("Status code: {} connection to : {}", statusCode, url);

                if (statusCode == 200) {
                    hasConnection = true;
                    document = response.parse();
                } else {
                    countBadConnection++;
                }


            } catch (Exception e) {
                countBadConnection++;
                log.warn("{}. Bad connection by link: {}", countBadConnection, url, e);
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException ex) {
                    log.warn("Error sleeping while bad connection", ex);
                }
            }
        }
        return document;

    }


    @Test
    void getProductsInitDataByCategory() {
        SupplierApp maresto = supplierAppService.getByName("maresto");
        List<CategoryOpencart> supplierCategoryOpencart = categoryOpencartService.getSupplierCategoryOpencart(maresto);
        CategoryOpencart categoryOpencart = supplierCategoryOpencart
                .stream()
                .filter(c -> c.getDescriptions().get(0).getName().equals("Пароконвектомати"))
                .findFirst()
                .orElse(null);
        categoryOpencart.setUrl("https://maresto.ua/ua/catalog/konvektsionnye_pechi/pech_konvektsionnaya/");
        List<CategoryOpencart> categoryOpencarts = Arrays.asList(categoryOpencart);
        log.info("categoryOpencart: {}", categoryOpencart);
        parserServiceMaresto.getProductsInitDataByCategory(categoryOpencarts, maresto);
    }

    @Test
    void getFullProductsData() {

        List<ProductOpencart> productOpencartList = Arrays.asList(new ProductOpencart.Builder()
                .withUrlProduct("https://maresto.ua/ua/catalog/oborudovanie_fast_fud/sosiskovarka_bartscher_a120456.html")
                .withSku("sosiskovarka_bartscher_a120456").build());
        SupplierApp maresto = supplierAppService.getByName("maresto");
        parserServiceMaresto.getFullProductsData(productOpencartList, maresto);
    }

    @Test
    void updateDescription() {
        parserServiceMaresto.updateDescription();
    }


    @Test
    void updateImages() {
//        parserServiceMaresto.updateImages();
    }
}