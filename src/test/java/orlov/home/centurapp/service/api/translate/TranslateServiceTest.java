package orlov.home.centurapp.service.api.translate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.swing.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.HandlerInterceptor;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class TranslateServiceTest {

    @Autowired
    private TranslateService translateService;

    @Autowired
    private OpencartDaoService opencartDaoService;

    @Test
    void getTranslatedText() {
        String text = "Наша компания предлагает Вам широкий выбор облицовки фронтальной части печи серия Inox frontal Pax, производства Morello Forni, Италия.Выполнена облицовка для печи из нержавеющей стали. Все модели включают в себя полку и аналоговый термометр.\n" +
                "\n" +
                "Модельный ряд облицовки доступный к покупке у нас: Inox frontal Pax90,Inox frontal Pax100, Inox frontal Pax110, Inox frontal Pax120, Inox frontal Pax130, Inox frontal Pax140.";
        log.info("Text: \n{}", text);
        String translatedText = translateService.getTranslatedText(text);
        log.info("Translated text: {}", translatedText);
    }

    @Test
    void webTranslate() {
        String supplierName = "frizel";
        List<ProductOpencart> products = opencartDaoService.getAllProductOpencartBySupplierAppName(supplierName);

        products = products
                .stream()
                .map(p -> opencartDaoService.getProductOpencartWithDescriptionById(p.getId()))
                .collect(Collectors.toList());

        log.info("Supplier: [{}], product count: [{}]", supplierName, products.size());
        translateService.webTranslate(products.stream().limit(10).collect(Collectors.toList()));
    }

    @Test
    void buildWebDriver() {
        WebDriver webDriver = translateService.buildWebDriver();
        webDriver.get("https://bot.sannysoft.com/");

        WebDriverWait driverWait = new WebDriverWait(webDriver, 10, 200);
        List<WebElement> rows = driverWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//tr")));

        rows.forEach(r -> {
            List<WebElement> rowData = r.findElements(By.xpath(".//td"));
            if (rowData.size() == 2) {
                WebElement keyElement = rowData.get(0);
                WebElement valueElement = rowData.get(1);

                log.info("KAY: [{}], VALUE: [{}]", keyElement.getText(), valueElement.getText());
            }
        });
    }


}