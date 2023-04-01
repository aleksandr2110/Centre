package orlov.home.centurapp.service.parser;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.OpencartDto;
import orlov.home.centurapp.entity.app.*;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.service.api.translate.TranslateService;
import orlov.home.centurapp.service.appservice.FileService;
import orlov.home.centurapp.service.appservice.ScraperDataUpdateService;
import orlov.home.centurapp.service.appservice.UpdateDataService;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;
import orlov.home.centurapp.service.daoservice.validator.HttpsUrlValidator;
import orlov.home.centurapp.util.AppConstant;
import orlov.home.centurapp.util.OCConstant;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserServiceTfb2b extends ParserServiceAbstract {
    private final String SUPPLIER_NAME = "tfb2b";
    private final String SUPPLIER_URL = "https://tfb2b.com.ua/uk/";
    private final String SUPPLIER_PART_IMAGE_URL = "https://tfb2b.com.ua";
    private final String SUPPLIER_URL_LOGIN = "https://tfb2b.com.ua/uk/login";
    private final String DISPLAY_NAME = "62 - ТЕХНОФУД";
    private final String URL_PART_PAGE = "&page=";
    private final String LOGIN = "centur1@ukr.net";
    private final String PASSWORD = "MRI0JKRC";
    private WebDriver driver;

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final UpdateDataService updateDataService;

    public ParserServiceTfb2b(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService;
        this.opencartDaoService = opencartDaoService;
        this.translateService = translateService;
        this.updateDataService = updateDataService;
    }


    @Override
    public void doProcess() {
        try {
            OrderProcessApp orderProcessApp = new OrderProcessApp();
            Timestamp start = new Timestamp(Calendar.getInstance().getTime().getTime());

            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.setHeadless(true);
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920x1080");
            //Added by Unable to establish websocket connection
            // to http://localhost:37582/devtools/browser/
            //https://groups.google.com/g/chromedriver-users/c/xL5-13_qGaA?pli=1
            options.addArguments("--remote-allow-origins=*");

            driver = new ChromeDriver(options);
            Dimension dm = new Dimension(1552, 849);
            driver.manage().window().setSize(dm);

            login();
            SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);

            List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);

            List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);

            OpencartDto opencartInfo = getOpencartInfo(productsFromSite, supplierApp);
            checkPrice(opencartInfo, supplierApp);

            List<ProductOpencart> fullProductsData = getFullProductsData(opencartInfo.getNewProduct(), supplierApp);


            fullProductsData
                    .forEach(opencartDaoService::saveProductOpencart);
            //:TODO update price in function checkPrice
            /*
            if(!opencartInfo.getNewProduct().isEmpty()) {
                updateDataService.updatePrice(supplierApp.getSupplierAppId());
            }*/
            updateProductSupplierOpencartBySupplierApp(supplierApp);

            Timestamp end = new Timestamp(Calendar.getInstance().getTime().getTime());
            orderProcessApp = opencartInfo.getOrderProcessApp();
            orderProcessApp.setStartProcess(start);
            orderProcessApp.setEndProcess(end);
            appDaoService.saveOrderDataApp(orderProcessApp);
        } catch (Exception ex) {
            log.warn("Exception parsing tfb2b", ex);
        } finally {
            try {
                driver.close();
                driver.quit();
            } catch (Exception ex) {
                log.warn("Exception close driver.", ex);
            }
        }
    }

    public void login() {
        driver.get(SUPPLIER_URL_LOGIN);
        WebElement formElement = new WebDriverWait(driver, 10, 500)
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//form")));
        formElement.findElement(By.xpath("//input[@autocomplete='login']")).sendKeys(LOGIN);
        formElement.findElement(By.xpath("//input[@autocomplete='password']")).sendKeys(PASSWORD);
        new WebDriverWait(driver, 10, 500)
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(text(), 'Увійти')]")))
                .click();
    }

    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {
        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();

        WebElement categoryElement = new WebDriverWait(driver, 10, 500)
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//nav")));
        List<WebElement> categoryElementsList = categoryElement
                .findElements(By.xpath("./div[starts-with(@class, 'jss') or not(@class='MuiCollapse-root')]"));

        AtomicInteger countCat = new AtomicInteger();
        List<CategoryOpencart> siteCategoryList = categoryElementsList
                .stream()
                .map(ce -> {
                    String href = ce.findElement(By.xpath(".//a")).getAttribute("href");
                    String title = ce.findElement(By.xpath(".//a")).getText();
                    if (!title.isEmpty()) {
                        CategoryOpencart categoryOpencart = new CategoryOpencart.Builder()
                                .withUrl(href)
                                .withParentCategory(supplierApp.getMainSupplierCategory())
                                .withParentId(supplierApp.getMainSupplierCategory().getCategoryId())
                                .withTop(false)
                                .withStatus(false)
                                .build();
                        CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                                .withName(title)
                                .withDescription(supplierApp.getName())
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .build();
                        categoryOpencart.getDescriptions().add(description);
                        return categoryOpencart;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                //:TODO next line uncommitted only debug
                //.findFirst().stream()
                .collect(Collectors.toList());


        siteCategoryList = siteCategoryList
                .stream()
                .map(sc -> recursiveCollectListCategory(sc, supplierCategoryOpencartDB))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        siteCategoryList.add(supplierApp.getMainSupplierCategory());
        siteCategoryList.add(supplierApp.getGlobalSupplierCategory());

        AtomicInteger countCategory = new AtomicInteger();
        siteCategoryList
                .forEach(c -> log.info("{}. Full Site category name: {}, parent id: {}, paren obj: {} subCategorySize: {}", countCategory.addAndGet(1), c.getDescriptions().get(0).getName(), c.getParentId(), Objects.isNull(c.getParentCategory()), c.getCategoriesOpencart().size()));

        return siteCategoryList;


    }

    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();
        Document doc = getWebDocument(url, new HashMap<>());
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {
            Elements subCateElements = doc.select("div.search-result__aside-item.search-result__aside-item_toggler:has(*:contains(Підкатегорії))");

            List<CategoryOpencart> subCategories = subCateElements.select("a")
                    .stream()
                    .map(el -> {

                        String subUrl = SUPPLIER_URL.concat(el.attr("href").substring(4));

                        log.info("Sub Url: {}", subUrl);
                        String subTitle = el.text().trim();
                        log.info("Main title: {} sub title: {}", category.getDescriptions().get(0).getName(), subTitle);
                        CategoryOpencart subCategory = new CategoryOpencart.Builder()
                                .withUrl(subUrl)
                                .withTop(false)
                                .withParentCategory(category)
                                .withStatus(false)
                                .build();
                        CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                                .withName(subTitle)
                                .withDescription(category.getDescriptions().get(0).getDescription())
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .build();
                        subCategory.getDescriptions().add(description);

                        recursiveWalkSiteCategory(subCategory);

                        return subCategory;

                    })
                    .collect(Collectors.toList());
            category.getCategoriesOpencart().addAll(subCategories);

        }
        return category;
    }

    @Override
    public List<ProductOpencart> getProductsInitDataByCategory(List<CategoryOpencart> categoriesWithProduct, SupplierApp supplierApp) {

        List<ProductOpencart> productsCategory = new ArrayList<>();

        categoriesWithProduct
                .stream()

                .filter(c -> {
                    int categoryId = c.getCategoryId();
                    List<CategoryOpencart> childrenCategory = categoriesWithProduct.stream().filter(sub -> sub.getParentId() == categoryId).collect(Collectors.toList());
                    return childrenCategory.isEmpty();
                })
                .peek(c -> {

                    String name = c.getDescriptions().get(0).getName();
                    CategoryApp categoryApp = getCategoryApp(name, supplierApp);

                    String url = c.getUrl();
                    log.info("Get info by url: {}", url);


                    int countPage = 1;
                    int lasPage = 1;

                    List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);


                    while (countPage <= lasPage) {
                        try {
                            if (countPage == 1) {
                                driver.get(url);
                                try {


                                List<WebElement> paginationElements = new WebDriverWait(driver, 10, 500)
                                        .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//ul[@class='MuiPagination-ul']//li")));
                                lasPage = paginationElements
                                        .stream()
                                        .map(l -> l.getText().replaceAll("\\D", ""))
                                        .filter(l -> !l.isEmpty())
                                        .map(Integer::parseInt)
                                        .max(Integer::compareTo)
                                        .orElse(1);
                                } catch (Exception ex){
                                    log.info("Has not next page.");
                                }
                            } else {
                                String newUrl = url.concat(URL_PART_PAGE.concat(String.valueOf(countPage)));
                                driver.get(newUrl);
                            }

                            List<WebElement> catalogElements = new WebDriverWait(driver, 10, 500)
                                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//ul[starts-with(@class, 'MuiGridList-root')]")));


                            if (!catalogElements.isEmpty())
                                scrollProductElements(driver, catalogElements.get(0));

                            List<WebElement> productElements = new WebDriverWait(driver, 10, 500)
                                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//div[@class='MuiGridListTile-tile']")));
                            new WebDriverWait(driver, 10, 500)
                                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//div[contains(@class, 'MuiGrid-container')]/div")));
                            new WebDriverWait(driver, 10, 500)
                                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a")));

                            productElements
                                    .stream()
                                    .peek(ep -> {
                                        log.info("");

                                        try {


                                            new WebDriverWait(driver, 10, 5)
                                                    .until(ExpectedConditions.presenceOfNestedElementLocatedBy(ep, By.xpath(".//div[contains(@class, 'MuiGrid-container')]/div")));
//
                                            List<WebElement> infoDivElements = ep.findElements(By.xpath(".//div[contains(@class, 'MuiGrid-container')]/div"));
                                            WebElement imageElement = infoDivElements.get(0);
                                            String urlProduct = new WebDriverWait(driver, 10, 500)
                                                    .until(ExpectedConditions.presenceOfNestedElementLocatedBy(ep, By.xpath(".//a"))).getAttribute("href");
                                            log.info("Product url: {}", urlProduct);
                                            String imageDB = "";
                                            String urlImage = "";
                                            try {


                                                WebElement mainImageElement = new WebDriverWait(driver, 10, 500)
                                                        .until(ExpectedConditions.presenceOfNestedElementLocatedBy(imageElement, By.xpath(".//div[contains(@style, 'background-image')]")));

                                                if (Objects.nonNull(mainImageElement)) {
                                                    String style = mainImageElement.getAttribute("style");
                                                    urlImage = SUPPLIER_PART_IMAGE_URL.concat(style.substring(style.indexOf("url(\"") + 5, style.indexOf("\");")));
                                                    log.info("urlImage {}", urlImage);
                                                    String imageName = urlImage.substring(urlImage.lastIndexOf("/") + 1);
                                                    log.info("imageName {}",imageName);
                                                    imageDB = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);
                                                    log.info("imageDB {}", imageDB);
                                                    downloadImage(urlImage, imageDB);
                                                    log.info("Image url: {}", urlImage);
                                                }

                                            } catch (Exception ex) {
                                                log.warn("Bad image", ex);
                                            }

                                            WebElement descElement = infoDivElements.get(1);
                                            String title = descElement.findElement(By.xpath(".//p")).getText();
                                            log.info("Title: {}", title);
                                            String brand = "";
                                            List<WebElement> brandElements = descElement.findElements(By.xpath(".//*[contains(text(), 'Бренд')]"));
                                            if (!brandElements.isEmpty()) {
                                                brand = brandElements.get(0).findElement(By.xpath("./following-sibling::span")).getText();
                                                log.info("Brand: {}", brand);
                                            }
                                            String article = "";
                                            List<WebElement> modelElements = descElement.findElements(By.xpath(".//*[contains(text(), 'Артикул')]"));
                                            if (!modelElements.isEmpty()) {
                                                article = modelElements.get(0).findElement(By.xpath("./following-sibling::span")).getText();
                                                log.info("Article: {}", article);
                                            }

                                            String sku = urlProduct.substring(urlProduct.lastIndexOf("/") + 1);
                                            log.info("sku: {}", sku);

                                            String model = generateModel(article, sku);
                                            log.info("model: {}", model);


                                            WebElement priceInfoElement = infoDivElements.get(2);
                                            BigDecimal price = new BigDecimal("0").setScale(4);
                                            List<WebElement> rrcElements = priceInfoElement.findElements(By.xpath(".//p[contains(text(), 'РРЦ')]"));
                                            if (!rrcElements.isEmpty()) {
                                                WebElement rrcElement = rrcElements.get(0).findElement(By.xpath("./.."));
                                                List<WebElement> priceInfoElementElements = rrcElement.findElements(By.xpath(".//span[contains(text(), 'грн')]"));
                                                if (!priceInfoElementElements.isEmpty()) {
                                                    WebElement curElement = priceInfoElementElements.get(0);
                                                    WebElement element = curElement.findElement(By.xpath("./.."));
                                                    String priceText = element.getText().replaceAll("[^,\\d]", "");

                                                    priceText = priceText.replaceAll(",", ".");
                                                    price = new BigDecimal(priceText).setScale(4);
                                                }
                                            }

                                            log.info("Price: {}", price);
                                            ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                                                    .withSupplierId(supplierApp.getSupplierAppId())
                                                    .withSupplierApp(supplierApp)
                                                    .withCategoryId(categoryApp.getCategoryId())
                                                    .withCategoryApp(categoryApp)
                                                    .withUrl(urlProduct)
                                                    .withSku(sku)
                                                    .withTitle(title)
                                                    .withPrice(price)
                                                    .build();


                                            ProductOpencart product = new ProductOpencart.Builder()
                                                    .withProductProfileApp(productProfileApp)
                                                    .withManufacturerName(brand)
                                                    .withUrlProduct(urlProduct)
                                                    .withUrlImage(urlImage)
                                                    .withImage(imageDB)
                                                    .withTitle(title)
                                                    .withModel(model)
                                                    .withSku(sku)
                                                    .withJan(supplierApp.getName())
                                                    .withPrice(price)
                                                    .withItuaOriginalPrice(price)
                                                    .build();

                                            product.setCategoriesOpencart(parentsCategories);


                                            ProductOpencart prodFromList = productsCategory
                                                    .stream()
                                                    .filter(ps -> ps.getSku().equals(product.getSku()))
                                                    .findFirst()
                                                    .orElse(null);

                                            if (Objects.nonNull(prodFromList)) {

                                                List<CategoryOpencart> categoriesOpencart = prodFromList.getCategoriesOpencart();
                                                List<CategoryOpencart> newCategoriesOpencart = product.getCategoriesOpencart();
                                                newCategoriesOpencart
                                                        .forEach(nc -> {
                                                            if (!categoriesOpencart.contains(nc)) {
                                                                categoriesOpencart.add(nc);
                                                            }
                                                        });

                                            } else {
                                                productsCategory.add(product);
                                            }

                                        } catch (Exception ex) {
                                            log.warn("Exception parsing product", ex);
                                        }

                                    })
                                    .collect(Collectors.toList());


                        } catch (Exception e) {
                            log.warn("Problem iterate page", e);
                        } finally {
                            countPage++;
                        }

                    }

                })
                .collect(Collectors.toList());

        return productsCategory;

    }

    public void scrollProductElements(WebDriver driver, WebElement element) {

        Dimension size = element.getSize();
        int height = size.getHeight();

        new Actions(driver)
                .moveToElement(element)
                .pause(Duration.ofMillis(100))
                .scrollByAmount(0, height)
                .perform();

    }

    //    TODO test attribute value update
    public void updateAttributeValue() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);
        List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);
        List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);
        fullProductsData.stream().forEach( fp -> {
            fp.getProductsDescriptionOpencart().stream().forEach( item -> {
                //:TODO
                    }

            );
        });
    }

    @Override
    public List<ProductOpencart> getFullProductsData(List<ProductOpencart> products, SupplierApp supplierApp) {

        AtomicInteger count = new AtomicInteger();
        List<ProductOpencart> fullProducts = products
                .stream()
                .peek(p -> {
                    String urlProduct = p.getUrlProduct();
                    log.info("{}. Get data from product url: {}", count.addAndGet(1), urlProduct);

                    try {
                        driver.get(urlProduct);
                        scrollPage(driver);
                        new WebDriverWait(driver, 10, 500)
                                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//span")));

                        String name = p.getTitle();
                        log.info("name: {}", name);
                        String description = "";
                        List<AttributeWrapper> attributes = new ArrayList<>();
                        try {
                            List<WebElement> descriptionElements = new WebDriverWait(driver, 10, 500)
                                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//span[contains(text(), 'Опис')]")));
                            log.info("descriptionElements: {}", descriptionElements.size());
                            if (!descriptionElements.isEmpty()) {
                                WebElement descriptionButtonElement = descriptionElements.get(0).findElement(By.xpath("./.."));
                                JavascriptExecutor js = (JavascriptExecutor) driver;
                                js.executeScript("arguments[0].click();", descriptionButtonElement);
                                new WebDriverWait(driver, 10, 500)
                                        .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//*")));
                                WebElement descriptionTextElement = new WebDriverWait(driver, 10, 500)
                                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'undefined') and contains(@class, 'MuiTabs-root')]")));
                                WebElement descElement = new WebDriverWait(driver, 10, 5)
                                        .until(ExpectedConditions.presenceOfNestedElementLocatedBy(descriptionTextElement, By.xpath("./following-sibling::div[contains(@style, 'padding')]")));
                                String textDesc = descElement.getText();
                                description = wrapToHtml(textDesc);
                                log.info("Desc: {}", description);

                            }
                        } catch (Exception exDesc) {
                            log.warn("Description is missing.");
                        }

                        String manufacturer = p.getManufacturerName();

                        ManufacturerApp manufacturerApp = getManufacturerApp(manufacturer, supplierApp);

                        ProductProfileApp firstProfileApp = p.getProductProfileApp();
                        firstProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                        firstProfileApp.setManufacturerApp(manufacturerApp);
                        ProductProfileApp savedProductProfile = getProductProfile(firstProfileApp, supplierApp);

                        p.setProductProfileApp(savedProductProfile);

                        try {
                            List<WebElement> attributeElements = new WebDriverWait(driver, 10, 500)
                                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//span[contains(text(), 'Характеристики')]")));

                            if (!attributeElements.isEmpty()) {
                                WebElement attributeButtonElements = attributeElements.get(0).findElement(By.xpath("./.."));
                                JavascriptExecutor js = (JavascriptExecutor) driver;
                                js.executeScript("arguments[0].click();", attributeButtonElements);

                                new WebDriverWait(driver, 10, 500)
                                        .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//*")));

                                WebElement attrElement = new WebDriverWait(driver, 10, 5)
                                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table[@aria-label='simple table']")));

                                log.info("attrElement: {}", attrElement.getText());

                                attributes = attrElement.findElements(By.xpath(".//tr"))
                                        .stream()
                                        .map(row -> {
                                            List<WebElement> tsElements = row.findElements(By.xpath(".//td"));
                                            if (tsElements.size() == 2) {
                                                String keyAttr = tsElements.get(0).getText();
                                                String valueAttr = tsElements.get(1).getText();
                                                log.info("Key: {}     Value: {}", keyAttr, valueAttr);
                                                AttributeWrapper attributeWrapper = new AttributeWrapper(keyAttr, valueAttr, null);
                                                AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp,savedProductProfile);
                                                return attribute;
                                            } else {
                                                return null;
                                            }
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());

                            }

                        } catch (Exception exAttr) {
                            log.warn("Attribute is missing.");
                        }

                        ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                .withDescription(description)
                                .withName(name)
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .withMetaH1(name)
                                .withMetaDescription(name.concat(AppConstant.META_DESCRIPTION_PART))
                                .withMetaTitle("Купити ".concat(name).concat(AppConstant.META_TITLE_PART))
                                .withMetaKeyword("Купити ".concat(name))
                                .build();
                        p.getProductsDescriptionOpencart().add(productDescriptionOpencart);



                        setManufacturer(p, supplierApp);
                        setPriceWithMarkup(p);
                        p.getAttributesWrapper().addAll(attributes);


                        try {

                            WebElement hTitleElement = driver.findElement(By.xpath("//h6[contains(@class, 'MuiTypography-subtitle1')]"));

                            WebElement mainGalleryElement = hTitleElement.findElement(By.xpath("./../../.."));
                            AtomicInteger countImage = new AtomicInteger();
                            List<ImageOpencart> productImages = mainGalleryElement.findElements(By.xpath(".//div[contains(@style, 'background-image')]"))
                                    .stream()
                                    .map(ie -> {
                                        String srcImageText = ie.getAttribute("style");
                                        String srcImage = srcImageText.substring(srcImageText.indexOf("url(\"") + 5, srcImageText.indexOf("\");"));
                                        String urlImage = SUPPLIER_PART_IMAGE_URL.concat(srcImage);
                                        log.info("srcImage: {}", srcImage);
                                        log.info("urlImage: {}", urlImage);

                                        String imageName = urlImage.substring(urlImage.lastIndexOf("/") + 1);
                                        String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);
                                        log.info("imageName: {}", imageName);
                                        log.info("dbImgPath: {}", dbImgPath);
                                        downloadImage(urlImage, dbImgPath);

                                        if (!p.getImage().equals(dbImgPath)) {
                                            ImageOpencart imageOpencart = new ImageOpencart.Builder()
                                                    .withImage(dbImgPath)
                                                    .withSortOrder(countImage.get())
                                                    .build();
                                            return imageOpencart;
                                        } else {
                                            return null;
                                        }

                                    })
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                            p.getImagesOpencart().addAll(productImages);

                        } catch (Exception exImg) {
                            log.warn("images missing.");
                        }

                    } catch (Exception ex) {
                        log.warn("Bad parsing product data", ex);
                    }

                })
                .filter(p -> p.getId() != -1)
                .filter(p -> !p.getSku().isEmpty())
                .collect(Collectors.toList());

        return fullProducts;

    }

    public void scrollPage(WebDriver driver) {

        Dimension body = driver.findElement(By.tagName("body")).getSize();
        int height = body.getHeight();
        log.info("Height: {}", height);
        int count = 0;
        int limit = 700;
        while (count < height) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollBy(0, " + limit + ")", "");
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
            }
            count += limit;
        }

    }


    public void updateDescription() {

        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductOpencart> productOpencartList = opencartDaoService.getAllProductOpencartBySupplierAppName(supplierApp.getName());
        log.info("Maresto product opencart size: {}", productOpencartList.size());
        List<ProductProfileApp> productProfilesApp = supplierApp.getProductProfilesApp();
        log.info("Maresto product profile size: {}", productProfilesApp.size());
        String name = supplierApp.getName();
        log.info("Supplier name: {}", name);

        productProfilesApp
                .forEach(pp -> {

                    String url = pp.getUrl();
                    String sku = pp.getSku();
                    ProductOpencart productOpencart = productOpencartList
                            .stream()
                            .filter(p -> p.getSku().equals(sku))
                            .findFirst()
                            .orElse(null);

                    if (Objects.nonNull(productOpencart)) {
                        int id = productOpencart.getId();

                        Document webDocument = getWebDocument(url, new HashMap<>());
                        String description = getDescription(webDocument);
                        log.info("Product description: {}", description);
                        ProductDescriptionOpencart desc = new ProductDescriptionOpencart.Builder()
                                .withProductId(id)
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .withDescription(description)
                                .build();
                        opencartDaoService.updateDescription(desc);
                        log.info("Product id: {}", id);
                    }

                });
    }

    public String getDescription(Document doc) {
        Elements descElement = doc.select("div[data-value=description]");
        String description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
        log.info("Description UA text: {}", description);

        if (description.isEmpty()) {
            String url = doc.baseUri();
            url = url.replaceAll("ua/ua/", "ua/");
            Document ruDoc = getWebDocument(url, new HashMap<>());
            descElement = ruDoc.select("div[data-value=description]");
            description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
            log.info("Description RU text: {}", description);


            if (!description.isEmpty()) {
                description = translateService.getTranslatedText(description);
            }
        }
        return wrapToHtml(description);
    }


    @Override
    public ProductProfileApp getProductProfile(ProductProfileApp productProfileApp, SupplierApp supplierApp) {
        List<ProductProfileApp> productProfilesAppDB = supplierApp.getProductProfilesApp();

        boolean contains = productProfilesAppDB.contains(productProfileApp);

        if (contains) {
            productProfileApp = productProfilesAppDB.get(productProfilesAppDB.indexOf(productProfileApp));
        } else {

            int manufacturerId = productProfileApp.getManufacturerId();

            if (manufacturerId == 0) {
                Document webDocument = getWebDocument(productProfileApp.getUrl(), new HashMap<>());
                Elements manuElem = webDocument.select("div.two-columns__subsection.two-columns__subsection_specs ul.product-spec li:contains(Виробник)");
                String manufacturer = "non";
                log.info("manuElem size: {}", manuElem.size());
                if (!manuElem.isEmpty()) {
                    manufacturer = manuElem.get(0).select("span.product-spec__value").text();
                }
                log.info("manufacturer: {}", manufacturer);
                ManufacturerApp manufacturerApp = getManufacturerApp(manufacturer, supplierApp);
                productProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                productProfileApp.setManufacturerApp(manufacturerApp);
            }

            productProfileApp = appDaoService.saveProductProfileApp(productProfileApp);

        }

        return productProfileApp;
    }

    public String generateModel(String supplierModel, String other) {
        String substring = DISPLAY_NAME.substring(0, DISPLAY_NAME.indexOf("-")).trim();
        String result;
        if (supplierModel.isEmpty()) {
            result = substring.concat("--").concat(other);
        } else {
            result = substring.concat("-").concat(supplierModel);
        }
        return result;
    }

    public void updateNewModel() {
        opencartDaoService.getAllProductOpencartBySupplierAppName(SUPPLIER_NAME)
                .forEach(p -> {
                    String model = p.getModel();
                    String newModel = model.replaceAll(" ", "");
                    p.setModel(newModel);
                    opencartDaoService.updateModel(p);
                });
    }

    public Document getWebDocument(String url) {
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


    public void updateModel() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductProfileApp> profiles = appDaoService.getAllProductProfileAppBySupplierId(supplierApp.getSupplierAppId());
        profiles
                .forEach(p -> {
                    try {
                        String url = p.getUrl();
                        Document doc = getWebDocument(url);
                        if (Objects.nonNull(doc)) {
                            String supplierModel = doc.select("p.product-card__code").text().replaceAll("Код:", "").trim();
                            String model = generateModel(supplierModel, String.valueOf(p.getProductProfileId()));
                            ProductOpencart productOpencart = new ProductOpencart.Builder()
                                    .withModel(model)
                                    .withSku(p.getSku())
                                    .withJan(supplierApp.getName())
                                    .build();
                            opencartDaoService.updateModel(productOpencart);
                        }
                    } catch (Exception ex) {
                        log.warn("exception update.", ex);
                    }

                });
    }

    public void waitScripts(WebDriver driver) {
        new WebDriverWait(driver, 10, 500).until((ExpectedCondition<Boolean>) webDriver -> {
            boolean isAjaxDone = ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
            return isAjaxDone;
        });
    }

}
