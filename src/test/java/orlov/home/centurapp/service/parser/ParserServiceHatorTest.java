package orlov.home.centurapp.service.parser;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
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
//        for (int i = 0; i < 10; i++) {
            parserServiceHator.doProcess();
//        }
    }

    @Test
    void changeFirstSecondImage(){
        parserServiceHator.changeFirstSecondImage();
    }

    @Test
    void updateImages(){
        parserServiceHator.updateImages();
    }

    @Test
    void dataDuplicateProduct() {
        parserServiceHator.dataDuplicateProduct();
    }

    @Test
    void setOptions() {
        WebDriverManager.chromedriver().setup();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920x1080");

        WebDriver driver = new ChromeDriver(options);

        Dimension dm = new Dimension(1552, 849);
        driver.manage().window().setSize(dm);
        driver.get("https://hator-m.com/stol-detskyi-polukruhlyi-reh-22v27");
        parserServiceHator.waitScripts(driver);
        parserServiceHator.setOptions(driver, new ProductOpencart());
        driver.close();
        driver.quit();
    }

    @Test
    public void getPage(){
        String url = "https://hator-m.com/stol-detskyi-polukruhlyi-reh-22v27";
        Document webDocument = parserServiceHator.getWebDocument(url, new HashMap<>());
        log.info("Page source use Jsoup: {}\n", webDocument.html());
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