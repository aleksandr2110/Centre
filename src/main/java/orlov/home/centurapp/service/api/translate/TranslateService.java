package orlov.home.centurapp.service.api.translate;


import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.html5.Location;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class TranslateService {
    private final String KEY_API = "AIzaSyCfFgVYcIVu3j_RnZk8VtTtV1IaOulHyBY";
    private final String WEB_URL = "https://www.m-translate.com.ua/";
    private final String TRANSLATE_FROM = "ru";
    private final String TRANSLATE_TO = "uk";
    private final String SCRIPT_FROM = "document.getElementById('from_span').innerHTML='" + TRANSLATE_FROM + "'";
    private final String SCRIPT_TO = "document.getElementById('to_span').innerHTML='" + TRANSLATE_TO + "'";


    public String getTranslatedText(String text) {
//            TODO off translate
//        try {
//            Translate translate = TranslateOptions.newBuilder().setApiKey(KEY_API).build().getService();
//            Translation translation = translate.translate(text, Translate.TranslateOption.sourceLanguage("ru"), Translate.TranslateOption.targetLanguage("uk"), Translate.TranslateOption.format("text"));
//            text = translation.getTranslatedText();
//            log.info("Translated text: {}", text);
//        } catch (Exception e) {
//            log.warn("Exception google translate", e);
//        }
        return text;
    }


    public void webTranslate(List<ProductOpencart> productToTranslateList) {
        AtomicInteger count = new AtomicInteger();
        WebDriver driver = buildWebDriver();
        try {
            WebDriverWait driverWait = new WebDriverWait(driver, 10, 200);

            driver.get(WEB_URL);

//            TODO set languages
            setTranslateLang(driver);



            productToTranslateList
                    .forEach(ptt -> {
                        try {


                            log.info("Translate [{}] product", count.addAndGet(1));
                            ProductDescriptionOpencart description = ptt.getProductsDescriptionOpencart().get(0);

                            String name = description.getName();
                            log.info("Name before translate: {}", name);
                            String descriptionText = description.getDescription();
                            descriptionText = cleanDescription(Jsoup.parse(descriptionText));
                            log.info("Description before translate: {}", descriptionText);


                            WebElement textareaFrom = driverWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//textarea[@id='text']")));
                            textareaFrom.clear();
                            textareaFrom.sendKeys(name);

                            WebElement translateButton = driverWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@id='go_btn']")));
                            translateButton.click();

                            String translatedName = "";

                            WebElement textareaTo = driverWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//textarea[@id='text_out']")));

                            do {
                                translatedName = textareaTo.getAttribute("value");
                            } while (translatedName.isEmpty());

                            log.info("Translated name: {}", translatedName);
                            description.setName(translatedName);

                            textareaFrom = driverWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//textarea[@id='text']")));
                            textareaFrom.clear();
                            textareaFrom.sendKeys(descriptionText);

                            translateButton = driverWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@id='go_btn']")));
                            translateButton.click();

                            String translatedDescription = "";

                            textareaTo = driverWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//textarea[@id='text_out']")));

                            do {
                                translatedDescription = textareaTo.getAttribute("value");
                            } while (translatedDescription.isEmpty());

                            String wrapDesc = wrapToHtml(translatedDescription);

                            log.info("Translated description: {}", translatedDescription);
                            description.setDescription(wrapDesc);


                        } catch (Exception ex) {
                            log.warn("Exception web translate of product with [ID={}].", ptt.getId(), ex);
                        }
                    });


        } catch (Exception ex) {
            log.warn("Exception WebDriver.", ex);
        } finally {
            try {
                if (Objects.nonNull(driver)) {
                    driver.quit();
                }
            } catch (Exception exwq) {
                log.warn("Exceptrion close web driver.", exwq);
            }
        }
    }

    public void setTranslateLang(WebDriver driver) {

        WebDriverWait webDriverWait = new WebDriverWait(driver, 10, 200);

        JavascriptExecutor j = (JavascriptExecutor) driver;
        j.executeScript("document.getElementById('from_pop').style.display='block';");
        j.executeScript("document.getElementById('to_pop').style.display='block';");


        WebElement elementLangRu = webDriverWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@id='from_pop']//div[@data-code='ru']")));
        new Actions(driver)
                .moveToElement(elementLangRu)
                .pause(Duration.ofSeconds(1))
                .click()
                .perform();

        WebElement elementLangUk = webDriverWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@id='to_pop']//div[@data-code='uk']")));
        new Actions(driver)
                .moveToElement(elementLangUk)
                .pause(Duration.ofSeconds(1))
                .click()
                .perform();

    }


    public String wrapToHtml(String text) {
        String result = text.replaceAll("\n", "</br>");
        return "<span class=\"centurapp\" style=\"white-space: pre-wrap; font-size: 16px; \">".concat(result).concat("</span>");
    }

    public String cleanDescription(Element descriptionElement) {
        descriptionElement.select("br").append("\n");
        descriptionElement.select("p").append("\n\n");
        List<String> lines = Arrays.asList(descriptionElement.wholeText().split("\n"));
        return lines
                .stream()
                .map(String::trim)
                .collect(Collectors.joining("\n")).replaceAll("\n{3,}", "\n\n").replaceAll("\\u00a0", "").trim();
    }


    public WebDriver buildWebDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36");
        options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.5249.119 Safari/537.36");
        options.setHeadless(true);
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("--window-size=1920x1080");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        ChromeDriver driver = new ChromeDriver(options);

        driver.manage().window().maximize();

        return driver;
    }


}
