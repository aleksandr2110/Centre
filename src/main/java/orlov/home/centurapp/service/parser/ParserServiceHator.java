package orlov.home.centurapp.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.opencart.ProductDescriptionOpencartDao;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.OpencartDto;
import orlov.home.centurapp.dto.api.artinhead.OptionDto;
import orlov.home.centurapp.dto.api.artinhead.OptionValuesDto;
import orlov.home.centurapp.dto.api.artinhead.ProductArtinhead;
import orlov.home.centurapp.dto.app.ProductHatorDto;
import orlov.home.centurapp.entity.app.*;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.service.api.translate.TranslateService;
import orlov.home.centurapp.service.appservice.FileService;
import orlov.home.centurapp.service.appservice.ScraperDataUpdateService;
import orlov.home.centurapp.service.appservice.UpdateDataService;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;
import orlov.home.centurapp.util.AppConstant;
import orlov.home.centurapp.util.OCConstant;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@Slf4j
public class ParserServiceHator extends ParserServiceAbstract {
    private final String SUPPLIER_NAME = "hator";
    private final String SUPPLIER_URL = "https://hator-m.com/";
    private final String SUPPLIER_URL_CATEGORY = "";
    private final String DISPLAY_NAME = "179 - ХАТОР-М";
    private final String MANUFACTURER_NAME = "ХАТОР-М";
    private final String URL_PART_PAGE = "?page=";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;
    private final UpdateDataService updateDataService;

    public ParserServiceHator(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService;
        this.opencartDaoService = opencartDaoService;
        this.translateService = translateService;
        this.fileService = fileService;
        this.updateDataService = updateDataService;
    }


    @Override
    public void doProcess() {
        try {

            OrderProcessApp orderProcessApp = new OrderProcessApp();
            Timestamp start = new Timestamp(Calendar.getInstance().getTime().getTime());

            SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);

            List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);


            List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);
            log.info("Hator product size: {}", productsFromSite.size());
            List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);

            OpencartDto opencartInfo = getOpencartInfo(fullProductsData, supplierApp);

            checkPrice(opencartInfo, supplierApp);
            checkProductOption(opencartInfo);
            checkStockStatusId(opencartInfo, supplierApp);

            List<ProductOpencart> newProduct = opencartInfo.getNewProduct();

            newProduct.forEach(np -> {
                opencartDaoService.saveProductOpencart(np);
            });

            updateDataService.updatePrice(supplierApp.getSupplierAppId());


            updateProductSupplierOpencartBySupplierApp(supplierApp);

            Timestamp end = new Timestamp(Calendar.getInstance().getTime().getTime());
            orderProcessApp = opencartInfo.getOrderProcessApp();
            orderProcessApp.setStartProcess(start);
            orderProcessApp.setEndProcess(end);
            appDaoService.saveOrderDataApp(orderProcessApp);

        } catch (Exception ex) {
            log.warn("Exception parsing nowystyle", ex);
        }
    }

    //    TODO test attribute value update
    public void updateAttributeValue() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);
        List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);
        List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);
    }

    public void updateModel() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductProfileApp> profiles = appDaoService.getAllProductProfileAppBySupplierId(supplierApp.getSupplierAppId());
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk-ua");
        profiles
                .forEach(p -> {
                    try {


                        String url = p.getUrl();
                        Document doc = getWebDocument(url, cookies);
                        if (Objects.nonNull(doc)) {
                            String supplierModel = doc.select("p.product-articul").text().replaceAll("Артикул:", "").trim();
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

    public void checkProductOption(OpencartDto opencartInfo) {
        log.info("Check product option");
        opencartInfo.getAvailableProducts()
                .forEach(p -> {
                    List<ProductOptionOpencart> options = opencartDaoService.getProductOptionsById(p.getId());
                    p.setProductOptionsOpencart(options);
                    log.info("Check product option: {}", p);
                    ProductProfileApp productProfileApp = p.getProductProfileApp();
                    List<OptionApp> optionsApp = productProfileApp.getOptions();
                    log.info("Options app list: {}", optionsApp);
                    log.info("Product profile: {}", productProfileApp);

                    p.getOptionsOpencart()
                            .forEach(o -> {
                                ProductOptionOpencart newProductOptionOpencart = o.getProductOptionOpencart();
                                newProductOptionOpencart.setProductId(p.getId());
                                newProductOptionOpencart.setOptionId(o.getOptionId());
                                newProductOptionOpencart.setProductId(p.getId());
                                log.info("New product option: {}", newProductOptionOpencart);

                                ProductOptionOpencart searchProductOptionOpencart = p.getProductOptionsOpencart()
                                        .stream()
                                        .filter(po -> newProductOptionOpencart.getOptionId() == po.getOptionId())
                                        .findFirst()
                                        .orElse(null);
                                log.info("Search product option: {}", searchProductOptionOpencart);

                                if (Objects.isNull(searchProductOptionOpencart)) {
                                    log.info("searchProductOptionOpencart IS NULL and save all option");
                                    ProductOptionOpencart savedProductOption = opencartDaoService.saveProductOptionOpencart(newProductOptionOpencart);
                                    o.getValues()
                                            .forEach(v -> {
                                                ProductOptionValueOpencart newProductOptionValueOpencart = v.getProductOptionValueOpencart();
                                                newProductOptionValueOpencart.setProductOptionId(savedProductOption.getProductOptionId());
                                                newProductOptionValueOpencart.setProductId(p.getId());
                                                newProductOptionValueOpencart.setOptionId(v.getOptionId());
                                                newProductOptionValueOpencart.setOptionValueId(v.getOptionValueId());
                                                ProductOptionValueOpencart savedOptionValueOpencart = opencartDaoService.saveProductOptionValueOpencart(newProductOptionValueOpencart);
                                                log.info("savedOptionValueOpencart: {}", savedOptionValueOpencart);
                                                OptionApp optionApp = new OptionApp();
                                                optionApp.setProductProfileId(productProfileApp.getProductProfileId());
                                                optionApp.setValueId(savedOptionValueOpencart.getProductOptionValueId());
                                                optionApp.setOptionValue("");
                                                optionApp.setOptionPrice(savedOptionValueOpencart.getPrice());
                                                OptionApp saveOptionApp = appDaoService.saveOptionApp(optionApp);
                                                optionsApp.add(saveOptionApp);
                                                log.info("saveOptionApp: {}", saveOptionApp);
                                            });
                                } else {
                                    o.getValues()
                                            .forEach(v -> {
                                                ProductOptionValueOpencart newProductOptionValueOpencart = v.getProductOptionValueOpencart();
                                                newProductOptionValueOpencart.setProductOptionId(searchProductOptionOpencart.getProductOptionId());
                                                newProductOptionValueOpencart.setProductId(p.getId());
                                                newProductOptionValueOpencart.setOptionValueId(v.getOptionValueId());
                                                log.info("New Product option value: {}", newProductOptionValueOpencart);

                                                ProductOptionValueOpencart searchProductOptionValueOpencart = searchProductOptionOpencart.getOptionValues()
                                                        .stream()
                                                        .filter(spov -> {
                                                            log.info("spov == v : {}", spov.getOptionValueId() == v.getOptionValueId());
                                                            return spov.getOptionValueId() == v.getOptionValueId();
                                                        })
                                                        .findFirst()
                                                        .orElse(null);

                                                if (Objects.nonNull(searchProductOptionValueOpencart)) {
                                                    OptionApp searchOptionApp = optionsApp
                                                            .stream()
                                                            .filter(oapp -> oapp.getValueId() == searchProductOptionValueOpencart.getProductOptionValueId())
                                                            .findFirst()
                                                            .orElse(null);
                                                    log.info("Search option app for price: {}", searchOptionApp);
                                                    if (Objects.nonNull(searchOptionApp)) {
                                                        boolean equalsPrice = searchProductOptionValueOpencart.getPrice().equals(searchOptionApp.getOptionPrice());
                                                        if (!equalsPrice) {
                                                            searchProductOptionValueOpencart.setPrice(newProductOptionValueOpencart.getPrice());
                                                            opencartDaoService.updateProductOptionValueOpencart(searchProductOptionValueOpencart);
                                                            searchOptionApp.setOptionPrice(searchProductOptionValueOpencart.getPrice());
                                                            appDaoService.updateOptionApp(searchOptionApp);
                                                        }
                                                    }
                                                } else {
                                                    ProductOptionValueOpencart savedOptionValueOpencart = opencartDaoService.saveProductOptionValueOpencart(newProductOptionValueOpencart);
                                                    OptionApp optionApp = new OptionApp();
                                                    optionApp.setProductProfileId(productProfileApp.getProductProfileId());
                                                    optionApp.setValueId(savedOptionValueOpencart.getProductOptionValueId());
                                                    optionApp.setOptionValue("");
                                                    optionApp.setOptionPrice(savedOptionValueOpencart.getPrice());
                                                    OptionApp saveOptionApp = appDaoService.saveOptionApp(optionApp);
                                                    optionsApp.add(saveOptionApp);
                                                    log.info("Saved product option value: {}", savedOptionValueOpencart);
                                                }
                                            });
                                }
                            });
                });
    }

    public ProductOpencart settingOptionsOpencart(WebDriver driver, ProductOpencart productOpencart, SupplierApp supplierApp) {

        try {

            List<OptionDto> optionsDtoList = driver.findElements(By.xpath("//div[@id='fm-product-options-box']//div[@class='form-group']"))
                    .stream()
                    .map(eop -> {
                        moveToElement(eop, driver);
                        OptionDto optionDto = new OptionDto();
                        String optionTitle = eop.findElement(By.xpath(".//label[@class='fm-control-label']")).getText().replaceAll("\\*", "").trim();
                        log.info("Option title: {}", optionTitle);
                        optionDto.setName(optionTitle);
                        WebElement optionElement = eop.findElement(By.xpath(".//div[starts-with(@id, 'input-option')]"));
                        String optionsCode = optionElement.getAttribute("id").replaceAll("\\D", "");
                        log.info("Options code: {}", optionsCode);
                        optionDto.setNameCode(optionsCode);

                        List<OptionValuesDto> optionValues = optionElement.findElements(By.xpath(".//label"))
                                .stream()
                                .map(ol -> {
                                    OptionValuesDto optionValuesDto = new OptionValuesDto();
                                    List<WebElement> imagesOption = ol.findElements(By.xpath(".//img"));
                                    List<WebElement> fontsOption = ol.findElements(By.xpath(".//font"));
                                    String optionValueTitle = "";
                                    if (!imagesOption.isEmpty()) {
                                        WebElement imageElement = imagesOption.get(0);
                                        optionValueTitle = imageElement.getAttribute("alt");
                                        String optionImageUrl = imageElement.getAttribute("src");
                                        optionImageUrl = new String(optionImageUrl.getBytes(), StandardCharsets.UTF_8);
                                        String optionImageName = optionImageUrl.substring(optionImageUrl.lastIndexOf("/") + 1);
                                        String optionImgDB = AppConstant.PART_DIR_OC_IMAGE.concat(optionImageName);
                                        log.info("Option image url: {}", optionImageUrl);
                                        log.info("Option image name: {}", optionImageName);
                                        log.info("Option image db name: {}", optionImgDB);
                                        optionValuesDto.setDbpathImage(optionImgDB);
                                        downloadImage(optionImageUrl, optionImgDB);
                                    } else if (!fontsOption.isEmpty()) {
                                        optionValueTitle = fontsOption.get(0).getText().trim();
                                    } else {
                                        optionValueTitle = ol.getAttribute("data-original-title");
                                    }
                                    optionValuesDto.setValue(optionValueTitle);
                                    log.info("Option value title: {}", optionValueTitle);

                                    String optionValueCode = ol.findElement(By.xpath(".//input")).getAttribute("value");
                                    optionValuesDto.setValueCode(optionValueCode);
                                    log.info("Option value code: {}", optionValueCode);


                                    boolean selected = ol.getAttribute("class").contains(" selected");
                                    if (!selected) {
                                        new Actions(driver)
                                                .moveToElement(ol)
                                                .pause(Duration.ofMillis(100))
                                                .click()
                                                .perform();
                                        waitScripts(driver);
                                    }

                                    String textPrice = new WebDriverWait(driver, 10, 500)
                                            .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[@class='fm-module-price-new']")))
                                            .getText().replaceAll("\\D", "");

                                    int price = 0;
                                    if (!textPrice.isEmpty()) {
                                        price = Integer.parseInt(textPrice);
                                    }
                                    optionValuesDto.setPrice(price);
                                    return optionValuesDto;
                                })
                                .collect(Collectors.toList());

                        List<OptionValuesDto> sortedOptionValues = optionValues
                                .stream()
                                .sorted(Comparator.comparingInt(OptionValuesDto::getPrice))
                                .collect(Collectors.toList());

                        OptionValuesDto defaultOption = sortedOptionValues.get(0);
                        defaultOption.setDefault(true);

                        int minOptionPrice = defaultOption.getPrice();
                        optionValues = sortedOptionValues
                                .stream()
                                .peek(val -> {
                                    int price = val.getPrice();
                                    int margin = price - minOptionPrice;
                                    val.setMargin(margin);
                                })
                                .collect(Collectors.toList());

                        optionDto.setValues(optionValues);
                        return optionDto;
                    })
                    .collect(Collectors.toList());


            optionsDtoList
                    .forEach(optionDto -> {
                        OptionValuesDto optionValuesDto = optionDto.getValues()
                                .stream()
                                .filter(OptionValuesDto::isDefault).findFirst().orElse(null);
                        if (Objects.nonNull(optionValuesDto)) {
                            WebElement defaultOption = new WebDriverWait(driver, 10, 500)
                                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@id='input-option" + optionDto.getNameCode() + "']//input[@value='" + optionValuesDto.getValueCode() + "']")));
                            new Actions(driver)
                                    .moveToElement(defaultOption)
                                    .pause(Duration.ofMillis(100))
                                    .click()
                                    .perform();
                            waitScripts(driver);
                        }
                    });

            String textProductPrice = new WebDriverWait(driver, 10, 500)
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[@class='fm-module-price-new']")))
                    .getText().replaceAll("\\D", "");

            if (!textProductPrice.isEmpty()) {
                BigDecimal price = new BigDecimal(textProductPrice);
                productOpencart.setPrice(price);
                productOpencart.setItuaOriginalPrice(price);
            }


            List<OptionOpencart> optionsOpencartList = optionsDtoList
                    .stream()
                    .map(o -> {

                        List<OptionValueOpencart> optionValues = o.getValues()
                                .stream()
                                .map(v -> {
                                    OptionValueDescriptionOpencart valueDescription = new OptionValueDescriptionOpencart();
                                    valueDescription.setName(v.getValue());

                                    ProductOptionValueOpencart productOptionValueOpencart = new ProductOptionValueOpencart();
                                    productOptionValueOpencart.setPrice(new BigDecimal(v.getMargin()));

                                    OptionValueOpencart optionValue = new OptionValueOpencart();
                                    optionValue.setImage(v.getDbpathImage());
                                    optionValue.setDescriptionValue(Collections.singletonList(valueDescription));
                                    optionValue.setProductOptionValueOpencart(productOptionValueOpencart);

                                    return optionValue;
                                })
                                .collect(Collectors.toList());

                        OptionDescriptionOpencart description = new OptionDescriptionOpencart();
                        description.setName(o.getName());

                        OptionOpencart optionOpencart = new OptionOpencart();
                        optionOpencart.setDescriptions(Collections.singletonList(description));
                        optionOpencart.setValues(optionValues);


                        checkPersistOptionOpencart(optionOpencart, supplierApp);

                        ProductOptionOpencart productOptionOpencart = new ProductOptionOpencart();
                        productOptionOpencart.setOptionId(optionOpencart.getOptionId());
                        optionOpencart.setProductOptionOpencart(productOptionOpencart);

                        optionOpencart
                                .getValues()
                                .stream()
                                .forEach(v -> {
                                    ProductOptionValueOpencart pov = v.getProductOptionValueOpencart();
                                    pov.setOptionId(v.getOptionId());
                                    pov.setOptionValueId(v.getOptionValueId());
                                });

                        return optionOpencart;
                    })
                    .collect(Collectors.toList());

            productOpencart.setOptionsOpencart(optionsOpencartList);

        } catch (Exception e) {
            log.warn("Exception setting options.", e);
        }

        return productOpencart;
    }


    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        Document doc = getWebDocument(SUPPLIER_URL, new HashMap<>());

        AtomicInteger countMainCategory = new AtomicInteger();
        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select("ul#oct-menu-ul > li.oct-menu-li")
                    .stream()
                    .map(ec -> {


                        String url = ec.select("a").first().attr("href");
                        String title = ec.select("a").first().text().trim();
                        log.info("{}. Main site category title: {}, url: {}", countMainCategory.addAndGet(1), title, url);
                        CategoryOpencart categoryOpencart = new CategoryOpencart.Builder()
                                .withUrl(url)
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

                    })
                    .collect(Collectors.toList());

            log.info("Main category size: {}", mainCategories.size());

            List<CategoryOpencart> siteCategoryStructure = mainCategories
                    .stream()
                    .map(mc -> {
                        log.info("MAIN CATEGORY: {}", mc.getDescriptions().get(0).getName());
                        CategoryOpencart categoryOpencart = recursiveWalkSiteCategory(mc);
                        return categoryOpencart;
                    })
                    .collect(Collectors.toList());

            List<CategoryOpencart> siteCategoryList = siteCategoryStructure
                    .stream()
                    .map(sc -> recursiveCollectListCategory(sc, supplierCategoryOpencartDB))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            siteCategoryList.add(supplierApp.getMainSupplierCategory());
            siteCategoryList.add(supplierApp.getGlobalSupplierCategory());

            siteCategoryList
                    .forEach(c -> log.info("Full Site category: {}, subCategorySize: {}", c.getDescriptions().get(0).getName(), c.getCategoriesOpencart().size()));

            return siteCategoryList;

        }
        return new ArrayList<>();

    }

    @Override
    public List<ProductOpencart> getProductsInitDataByCategory(List<CategoryOpencart> categoriesWithProduct, SupplierApp supplierApp) {

        List<ProductOpencart> allProductInitList = new ArrayList<>();

        categoriesWithProduct
                .stream()
                .filter(c -> {
                    int categoryId = c.getCategoryId();
                    List<CategoryOpencart> childrenCategory = categoriesWithProduct.stream().filter(sub -> sub.getParentId() == categoryId).collect(Collectors.toList());
                    return childrenCategory.isEmpty();
                })
                .forEach(c -> {

                    String categoryName = c.getDescriptions().get(0).getName();
                    CategoryApp categoryApp = getCategoryApp(categoryName, supplierApp);
                    String url = c.getUrl();

                    List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);
                    Document doc = getWebDocument(url, new HashMap<>());

                    if (Objects.nonNull(doc)) {
                        boolean goNext = true;
                        int countPage = 1;

                        while (goNext) {
                            try {

                                if (countPage != 1) {
                                    String newUrlPage = url.concat(URL_PART_PAGE).concat(String.valueOf(countPage));
                                    doc = getWebDocument(newUrlPage, new HashMap<>());
                                }

                                if (Objects.nonNull(doc)) {

                                    Elements elementsProduct = doc.select("div.product-layout");
                                    log.info("Count product: {} on page: {}", elementsProduct.size(), countPage);
                                    List<ProductOpencart> productFromPage = elementsProduct
                                            .stream()
                                            .map(ep -> {
                                                log.info("");
                                                String urlProduct = ep.select("div.fm-category-product-caption > div.fm-module-title > a").attr("href");
                                                log.info("Product url: {}", urlProduct);
                                                String title = ep.select("div.fm-category-product-caption > div.fm-module-title > a").text();
                                                log.info("Product title: {}", title);
                                                String sku = ep.select("div.fm-category-product-model").text().replaceAll("Код товара:", "").trim();
                                                log.info("Product sku: {}", sku);
                                                String model = generateModel(sku, "0000");
                                                log.info("Product model: {}", model);
                                                String textPrice = ep.select("span.fm-module-price-new").text().replaceAll("\\D", "");
                                                BigDecimal price = new BigDecimal("0");
                                                if (!textPrice.isEmpty()) {
                                                    price = new BigDecimal(textPrice);
                                                }
                                                log.info("Product price: {}", price);
                                                String urlImage = ep.select("img[data-srcset]").get(0).attr("src").replaceAll("228x228", "1000x1000");
                                                log.info("Product url image: {}", urlImage);
                                                String imageName = urlImage.substring(urlImage.lastIndexOf("/") + 1);
                                                log.info("Product image name: {}", imageName);
                                                String imageDBName = AppConstant.PART_DIR_OC_IMAGE.concat(imageName);
                                                log.info("Product image db name: {}", imageDBName);
                                                downloadImage(urlImage, imageDBName);

                                                ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                                                        .withUrl(urlProduct)
                                                        .withSku(sku)
                                                        .withPrice(price)
                                                        .withSupplierId(supplierApp.getSupplierAppId())
                                                        .withSupplierApp(supplierApp)
                                                        .withCategoryId(categoryApp.getCategoryId())
                                                        .withCategoryApp(categoryApp)
                                                        .build();

                                                ProductOpencart product = new ProductOpencart.Builder()
                                                        .withUrlProduct(urlProduct)
                                                        .withModel(model)
                                                        .withSku(sku)
                                                        .withImage(imageDBName)
                                                        .withPrice(price)
                                                        .withItuaOriginalPrice(price)
                                                        .withJan(supplierApp.getName())
                                                        .withProductProfileApp(productProfileApp)
                                                        .build();
                                                product.setCategoriesOpencart(parentsCategories);
                                                return product;
                                            })
                                            .collect(Collectors.toList());
                                    allProductInitList.addAll(productFromPage);
                                }
                            } catch (Exception e) {
                                log.warn("Problem iterate page", e);
                            } finally {
                                countPage++;
                                Elements ePages = doc.select("ul.pagination li");
                                List<Integer> pages = ePages
                                        .stream()
                                        .map(e -> e.text().replaceAll("\\D", ""))
                                        .filter(s -> !s.isEmpty())
                                        .map(Integer::parseInt)
                                        .collect(Collectors.toList());
                                goNext = pages.contains(countPage);
                            }

                        }

                    }
                });

        return allProductInitList;
    }

    @Override
    public List<ProductOpencart> getFullProductsData(List<ProductOpencart> products, SupplierApp supplierApp) {

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

        AtomicInteger count = new AtomicInteger();

        List<ProductOpencart> fullProducts = products
                .stream()
                .peek(prod -> {


                    String urlProduct = prod.getUrlProduct();
                    log.info("{}. Get data from product url: {}", count.addAndGet(1), urlProduct);

                    try {
                        driver.get(urlProduct);
                        WebDriverWait webDriverWait = new WebDriverWait(driver, 10, 500);
                        WebElement ukLanguageElement = webDriverWait
                                .until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@title='Ukrainian']")));
                        new Actions(driver)
                                .moveToElement(ukLanguageElement)
                                .pause(Duration.ofSeconds(1))
                                .click()
                                .perform();
                        waitTranslate(driver);


                        String title = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//h1[@class='fm-main-title fm-page-title']"))).getText();
                        log.info("title: {}", title);
                        WebElement descriptionElement = webDriverWait
                                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='fm-product-description-cont']")));
                        moveToElement(descriptionElement, driver);
                        String description = descriptionElement.getText();
                        description = getDescription(description);

                        ProductDescriptionOpencart descriptionOpencart = new ProductDescriptionOpencart.Builder()
                                .withName(title)
                                .withDescription(description)
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .build();
                        prod.getProductsDescriptionOpencart().add(descriptionOpencart);



                        ManufacturerApp manufacturerApp = getManufacturerApp(MANUFACTURER_NAME, supplierApp);
                        ProductProfileApp productProfileApp = prod.getProductProfileApp();
                        productProfileApp.setTitle(title);
                        productProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                        productProfileApp.setManufacturerApp(manufacturerApp);
                        productProfileApp.setPrice(prod.getPrice());
                        ProductProfileApp savedProductProfile = getProductProfile(productProfileApp, supplierApp);
                        prod.setProductProfileApp(savedProductProfile);

                        List<WebElement> attributeElements = driver.findElements(By.xpath("//div[@id='fm-product-attributes-top']"));
                        if (!attributeElements.isEmpty()) {
                            moveToElement(attributeElements.get(0), driver);
                            List<AttributeWrapper> attributes = attributeElements.get(0).findElements(By.xpath(".//div[contains(@class, 'fm-product-attributtes-item')]"))
                                    .stream()
                                    .map(row -> {
                                        List<WebElement> attrData = row.findElements(By.xpath(".//span"));
                                        if (attrData.size() == 2) {
                                            String key = attrData.get(0).getText().trim();
                                            String value = attrData.get(1).getText().trim();
                                            log.info("Key: {}, value: {}", key, value);
                                            AttributeWrapper attributeWrapper = new AttributeWrapper(key, value, null);
                                            AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                            return attribute;
                                        } else {
                                            return null;
                                        }
                                    })
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                            prod.getAttributesWrapper().addAll(attributes);
                        }


                        List<WebElement> optionsForm = driver.findElements(By.xpath("//div[@class='form-group']"));
                        if (!optionsForm.isEmpty()) {
                            settingOptionsOpencart(driver, prod, supplierApp);
                        }




                        setManufacturer(prod, supplierApp);
                        setPriceWithMarkup(prod);
                        List<WebElement> imageElements = driver.findElements(By.xpath("//div[@id='image-additional']//a[@data-href and contains(@data-href, '1000x1000')]"));
                        log.info("Images count: {}", imageElements.size());

                        List<String> stringUIrlImageList = new ArrayList<>();
                        if (imageElements.size() > 0) {
                            AtomicInteger countImg = new AtomicInteger();
                            List<ImageOpencart> productImages = imageElements
                                    .stream()
                                    .map(i -> {
                                        String fullUrl = i.getAttribute("data-href");
                                        if (!stringUIrlImageList.contains(fullUrl)) {
                                            stringUIrlImageList.add(fullUrl);
                                            int imageCount = countImg.addAndGet(1);
                                            log.info("Image url: {}", fullUrl);
                                            String imgName = fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
                                            log.info("Image name: {}", imgName);
                                            String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imgName);
                                            log.info("Image DB name: {}", dbImgPath);
                                            if (!prod.getImage().equals(dbImgPath)) {
                                                downloadImage(fullUrl, dbImgPath);
                                                return new ImageOpencart.Builder()
                                                        .withImage(dbImgPath)
                                                        .withSortOrder(imageCount)
                                                        .build();
                                            } else {
                                                return null;
                                            }
                                        } else {
                                            return null;
                                        }
                                    })
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());


                            log.info("Images: {}", productImages);

                            prod.setImagesOpencart(productImages);
                        }

                    } catch (Exception ex) {
                        log.warn("Bad parsing product data", ex);
                    }

                })
                .filter(p -> p.getId() != -1)
                .filter(p -> !p.getSku().isEmpty())
                .collect(Collectors.toList());

        try {
            driver.close();
            driver.quit();
        } catch (Exception e) {
            log.warn("Exception close -> quite dricer.", e);
        }

        return fullProducts;
    }

    public void moveToElement(WebElement elementForMove, WebDriver driver) {
        new Actions(driver)
                .moveToElement(elementForMove)
                .pause(Duration.ofMillis(100))
                .perform();
        waitScripts(driver);
    }

    public void waitTranslate(WebDriver driver) {
        new WebDriverWait(driver, 10, 500).until((ExpectedCondition<Boolean>) webDriver -> {
            boolean isAjaxDone = ((JavascriptExecutor) driver).executeScript("return jQuery.active == 0").equals(true);
            return isAjaxDone;
        });
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
        new WebDriverWait(driver, 10, 500).until((ExpectedCondition<Boolean>) webDriver -> {
            boolean isAjaxDone = ((JavascriptExecutor) driver).executeScript("return jQuery.active == 0").equals(true);
            return isAjaxDone;
        });

    }

    public void waitScripts(WebDriver driver) {
        new WebDriverWait(driver, 10, 500).until((ExpectedCondition<Boolean>) webDriver -> {
            boolean isAjaxDone = ((JavascriptExecutor) driver).executeScript("return jQuery.active == 0").equals(true);
            return isAjaxDone;
        });
    }


    public String getDescription(String text) {
        return wrapToHtml(text);
    }


    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();
        Document doc = getWebDocument(url, new HashMap<>());
        if (Objects.nonNull(doc)) {


            List<CategoryOpencart> subCategories = doc.select("a.subcat-item")
                    .stream()
                    .map(el -> {
                        String subUrl = el.attr("href");
                        String subTitle = el.text();
                        log.info("    Sub category: {}", subTitle);
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
    public ProductProfileApp getProductProfile(ProductProfileApp productProfileApp, SupplierApp supplierApp) {
        List<ProductProfileApp> productProfilesAppDB = supplierApp.getProductProfilesApp();

        boolean contains = productProfilesAppDB.contains(productProfileApp);

        if (contains) {
            productProfileApp = productProfilesAppDB.get(productProfilesAppDB.indexOf(productProfileApp));
        } else {

            int manufacturerId = productProfileApp.getManufacturerId();

            if (manufacturerId == 0) {
                ManufacturerApp manufacturerApp = getManufacturerApp(MANUFACTURER_NAME, supplierApp);
                productProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                productProfileApp.setManufacturerApp(manufacturerApp);
            }

            productProfileApp = appDaoService.saveProductProfileApp(productProfileApp);

        }

        return productProfileApp;
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

}
