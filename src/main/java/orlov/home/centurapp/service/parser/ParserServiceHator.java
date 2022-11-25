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
import orlov.home.centurapp.dto.api.hator.HatorOptionDto;
import orlov.home.centurapp.dto.api.hator.HatorProductDto;
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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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


            List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);

            OpencartDto opencartInfo = getOpencartInfo(fullProductsData, supplierApp);

            checkPrice(opencartInfo, supplierApp);
            checkProductOption(opencartInfo);

            checkStockStatusId(opencartInfo, supplierApp);

            List<ProductOpencart> newProduct = opencartInfo.getNewProduct();

            newProduct.forEach(opencartDaoService::saveProductOpencart);
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
        opencartInfo.getAvailableProducts()
                .forEach(p -> {

                    List<ProductOptionOpencart> productOptionsOpencartDB = opencartDaoService.getProductOptionsById(p.getId());
                    log.info("Check product option [ SKU: {} ]", p.getSku());

                    ProductProfileApp productProfileApp = p.getProductProfileApp();
                    List<OptionApp> optionsAppDB = productProfileApp.getOptions();

                    p.getOptionsOpencart()
                            .forEach(o -> {

                                ProductOptionOpencart productOptionOpencartWEB = o.getProductOptionOpencart();
                                productOptionOpencartWEB.setProductId(p.getId());
                                productOptionOpencartWEB.setOptionId(o.getOptionId());
                                log.info("WEB OPTION: {}", productOptionOpencartWEB);

                                ProductOptionOpencart productOptionOpencartDB = productOptionsOpencartDB
                                        .stream()
                                        .filter(poo -> productOptionOpencartWEB.getOptionId() == poo.getOptionId())
                                        .findFirst()
                                        .orElse(null);
                                log.info("DATABASE OPTION : {}", productOptionOpencartDB);

                                if (Objects.isNull(productOptionOpencartDB)) {
                                    ProductOptionOpencart savedProductOption = opencartDaoService.saveProductOptionOpencart(productOptionOpencartWEB);
                                    productOptionsOpencartDB.add(savedProductOption);
                                    o.getValues()
                                            .forEach(v -> {
                                                ProductOptionValueOpencart newProductOptionValueOpencart = v.getProductOptionValueOpencart();
                                                newProductOptionValueOpencart.setProductOptionId(savedProductOption.getProductOptionId());
                                                newProductOptionValueOpencart.setProductId(p.getId());
                                                newProductOptionValueOpencart.setOptionId(v.getOptionId());
                                                newProductOptionValueOpencart.setOptionValueId(v.getOptionValueId());
                                                ProductOptionValueOpencart savedOptionValueOpencart = opencartDaoService.saveProductOptionValueOpencart(newProductOptionValueOpencart);
                                                log.info("ALL NEW OPTION: {}", savedOptionValueOpencart);
                                                OptionApp optionApp = new OptionApp();
                                                optionApp.setProductProfileId(productProfileApp.getProductProfileId());
                                                optionApp.setValueId(savedOptionValueOpencart.getProductOptionValueId());
                                                optionApp.setOptionValue("");
                                                optionApp.setOptionPrice(savedOptionValueOpencart.getPrice());
                                                OptionApp saveOptionApp = appDaoService.saveOptionApp(optionApp);

                                                log.info("ALL NEW OPTION APP: {}", saveOptionApp);
                                            });
                                } else {
                                    o.getValues()
                                            .forEach(v -> {
                                                ProductOptionValueOpencart newProductOptionValueOpencart = v.getProductOptionValueOpencart();
                                                newProductOptionValueOpencart.setProductOptionId(productOptionOpencartDB.getProductOptionId());
                                                newProductOptionValueOpencart.setProductId(p.getId());
                                                newProductOptionValueOpencart.setOptionValueId(v.getOptionValueId());

                                                ProductOptionValueOpencart productOptionValueOpencartDB = productOptionOpencartDB.getOptionValues()
                                                        .stream()
                                                        .filter(spov -> spov.getOptionValueId() == v.getOptionValueId())
                                                        .findFirst()
                                                        .orElse(null);

                                                log.info("DATABASE OPTION VALUE: {}", productOptionValueOpencartDB);

                                                if (Objects.isNull(productOptionValueOpencartDB)) {
                                                    ProductOptionValueOpencart savedOptionValueOpencart = opencartDaoService.saveProductOptionValueOpencart(newProductOptionValueOpencart);
                                                    log.info("NEW OPTION: {}", savedOptionValueOpencart);
                                                    OptionApp optionApp = new OptionApp();
                                                    optionApp.setProductProfileId(productProfileApp.getProductProfileId());
                                                    optionApp.setValueId(savedOptionValueOpencart.getProductOptionValueId());
                                                    optionApp.setOptionValue("");
                                                    optionApp.setOptionPrice(savedOptionValueOpencart.getPrice());
                                                    OptionApp saveOptionApp = appDaoService.saveOptionApp(optionApp);
                                                    log.info("NEW OPTION APP: {}", saveOptionApp);
                                                } else {

                                                    OptionApp searchOptionApp = optionsAppDB
                                                            .stream()
                                                            .filter(oapp -> oapp.getValueId() == productOptionValueOpencartDB.getProductOptionValueId())
                                                            .findFirst()
                                                            .orElse(null);
                                                    log.info("SALE DATABASE OPTIONS APP [ price: {} ]", searchOptionApp);
                                                    if (Objects.nonNull(searchOptionApp)) {
                                                        BigDecimal newPrice = newProductOptionValueOpencart.getPrice().setScale(4);
                                                        BigDecimal oldPrice = searchOptionApp.getOptionPrice();
                                                        boolean equalsPrice = newPrice.equals(oldPrice);
                                                        log.info("equalsPrice: {} | {} | {}", equalsPrice, newPrice, oldPrice);
                                                        if (!equalsPrice) {
                                                            productOptionValueOpencartDB.setPrice(newPrice);
                                                            opencartDaoService.updateProductOptionValueOpencart(productOptionValueOpencartDB);
                                                            searchOptionApp.setOptionPrice(newPrice);
                                                            appDaoService.updateOptionApp(searchOptionApp);
                                                        }
                                                    }
                                                    optionsAppDB.remove(searchOptionApp);
                                                    productOptionOpencartDB.getOptionValues().remove(productOptionValueOpencartDB);
                                                }
                                            });


                                }
                            });


                    optionsAppDB.stream().forEach(opp -> {
                        log.info("OLD Option app: {}", opp);
                        appDaoService.deleteOptionValue(opp.getOptionId());
                    });

                    productOptionsOpencartDB.stream().forEach(pood -> {
                        pood.getOptionValues().stream().forEach(vv -> {
                            log.info("OLD value: {}", vv);
                            opencartDaoService.deleteProductOptionValue(vv.getProductId(), vv.getOptionId(), vv.getOptionValueId());
                        });
                    });

                    List<ProductOptionOpencart> options = opencartDaoService.getProductOptionsById(p.getId());
                    options
                            .stream().forEach(oldoo -> {
                                List<ProductOptionValueOpencart> oldOptions = oldoo.getOptionValues()
                                        .stream()
                                        .filter(ooo -> ooo.getProductOptionValueId() != 0)
                                        .collect(Collectors.toList());
                                if (oldOptions.size() == 0) {
                                    log.info("OLD option: {}", oldoo);
                                    opencartDaoService.deleteProductOption(oldoo.getProductId(), oldoo.getOptionId());
                                }
                            });


                });


    }

    public ProductOpencart settingOptionsOpencart(WebDriver driver, ProductOpencart productOpencart, SupplierApp supplierApp) {

        try {

            waitScripts(driver);
            List<OptionDto> optionDtoWithMargin = setOptions(driver, productOpencart);
            log.info("ACTUAL PRODUCT DATA = SKU: {}, PRICE: {}, PP: {}", productOpencart.getSku(), productOpencart.getPrice(), productOpencart.getProductProfileApp().getPrice());
            List<OptionValuesDto> optionValueDtoWithMargin = new ArrayList<>();
            optionDtoWithMargin.stream().forEach(o -> optionValueDtoWithMargin.addAll(o.getValues()));

            List<OptionDto> optionsDtoList = driver.findElements(By.xpath("//div[@id='fm-product-options-box']//div[@class='form-group']"))
                    .stream()
                    .map(eop -> {
                        moveToElement(eop, driver, 500);
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

                                        String optionImageName = optionImageUrl.substring(optionImageUrl.lastIndexOf("/") + 1);

                                        optionImageName = URLDecoder.decode(optionImageName);

                                        String optionImgDB = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(optionImageName);

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


                                    if (optionValueDtoWithMargin.contains(optionValuesDto)) {
                                        OptionValuesDto valueWithmargin = optionValueDtoWithMargin.get(optionValueDtoWithMargin.indexOf(optionValuesDto));
                                        optionValuesDto.setMargin(valueWithmargin.getMargin());
                                    }


                                    return optionValuesDto;
                                })
                                .collect(Collectors.toList());


                        optionDto.setValues(optionValues);
                        return optionDto;
                    })
                    .collect(Collectors.toList());


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

    public List<OptionDto> setOptions(WebDriver driver, ProductOpencart productOpencart) {
        String pageSource = driver.getPageSource();
        int idxStart = pageSource.indexOf("\"ro\":");
        int idxEnd = pageSource.indexOf("\"options_ids\":");
        log.info("IDX s: {} | e: {}", idxStart, idxEnd);
        String jsAllDat = pageSource.substring(idxStart, idxEnd);

        List<String> hatorDataStringList = new ArrayList<>();
        int idxBegin = 0;
        int idxFinish = 0;
        boolean hasNext = true;

        do {
            idxBegin = jsAllDat.indexOf("{\"relatedoptions_id\"", idxFinish);
            idxFinish = jsAllDat.indexOf("\"specials\":", idxBegin);
            log.info("idx 1: [{}] 2: [{}]", idxBegin, idxFinish);
            if (idxBegin == -1 || idxFinish == -1) {
                hasNext = false;
            } else {
                String substring = jsAllDat.substring(idxBegin, idxFinish);
                hatorDataStringList.add(substring);
                log.info("ONCE result: {}", substring);
            }

        } while (hasNext);


        List<OptionDto> optionDtoList = new ArrayList<>();

        String subStringId = "\"relatedoptions_id\":\"";
        String subStringPrice = "\"price\":\"";
        String subStringOption = "\"options\":{";
        String subStringOptionEnd = "\"discounts\"";
        List<HatorProductDto> hatorProductDtoList = hatorDataStringList
                .stream()
                .map(hs -> {
                    log.info("NEW PRODUCT");
                    int idxId = hs.indexOf(subStringId) + subStringId.length();
                    int idxPoint = hs.indexOf(",", idxId) - 1;
                    String subId = hs.substring(idxId, idxPoint);
                    log.info("subId: {}", subId);

                    int idxPrice = hs.indexOf(subStringPrice) + subStringPrice.length();
                    idxPoint = hs.indexOf(",", idxPrice) - 1;
                    String subPrice = hs.substring(idxPrice, idxPoint);
                    log.info("subPrice: {}", subPrice);

                    int idxOption = hs.indexOf(subStringOption) + subStringOption.length();
                    idxPoint = hs.indexOf(subStringOptionEnd, idxOption);
                    String subOption = hs.substring(idxOption, idxPoint);
                    log.info("subOption: {}", subOption);

                    List<String> stringOptions = Arrays.asList(subOption.split(","));
                    List<OptionValuesDto> optionValuesDtoList = new ArrayList<>();
                    List<HatorOptionDto> hatorOptionDtoList = stringOptions
                            .stream()
                            .map(so -> {
                                String kay = so.substring(0, so.indexOf(":")).replaceAll("\\D", "");
                                String value = so.substring(so.indexOf(":")).replaceAll("\\D", "");
                                log.info("Option K: [{}], V: [{}]", kay, value);


                                OptionDto optionDto = new OptionDto();
                                optionDto.setNameCode(kay);
                                OptionValuesDto optionValuesDto = new OptionValuesDto();
                                optionValuesDto.setValueCode(value);
                                optionValuesDtoList.add(optionValuesDto);
                                boolean hasOption = optionDtoList.contains(optionDto);
                                if (hasOption) {
                                    OptionDto optionDtoFromList = optionDtoList.get(optionDtoList.indexOf(optionDto));
                                    boolean hasValue = optionDtoFromList.getValues().contains(optionValuesDto);
                                    if (!hasValue) {
                                        optionDtoFromList.getValues().add(optionValuesDto);
                                    }
                                } else {
                                    optionDto.getValues().add(optionValuesDto);
                                    optionDtoList.add(optionDto);
                                }


                                return new HatorOptionDto(kay, value);
                            })
                            .collect(Collectors.toList());
                    return new HatorProductDto(Integer.parseInt(subId), (int) Double.parseDouble(subPrice), hatorOptionDtoList, optionValuesDtoList);
                })
                .collect(Collectors.toList());

        List<HatorProductDto> sortedProductDto = hatorProductDtoList
                .stream()
                .sorted(Comparator.comparingInt(HatorProductDto::getPrice))
                .collect(Collectors.toList());


        HatorProductDto defaultHatorProductDto = sortedProductDto.get(0);


        defaultHatorProductDto.getHatorOptionDtoList()
                .forEach(ho -> {
                    optionDtoList
                            .forEach(o -> {
                                boolean isDef = o.getNameCode().equals(ho.getOptionCode());
                                if (isDef) {
                                    o.getValues()
                                            .forEach(v -> {
                                                boolean isDefVal = v.getValueCode().equals(ho.getValueCode());
                                                if (isDefVal) {
                                                    v.setDefault(true);
                                                }
                                            });
                                }
                            });
                });

        log.info("Hator DTO DEFAULT: {}", defaultHatorProductDto);
        List<OptionValuesDto> defaultOptionValues = optionDtoList
                .stream()
                .map(od -> od.getValues().stream().filter(OptionValuesDto::isDefault).findFirst().orElse(null))
                .collect(Collectors.toList());
        log.info("DEF VALUES: {}", defaultOptionValues);


        int priceDef = defaultHatorProductDto.getPrice();

        BigDecimal bigDecimalPrice = new BigDecimal(priceDef).setScale(4);
        productOpencart.setPrice(bigDecimalPrice);
        productOpencart.setItuaOriginalPrice(bigDecimalPrice);


        optionDtoList
                .forEach(od -> {
                    OptionValuesDto optionValuesDtoDefault = od.getValues().stream().filter(OptionValuesDto::isDefault).findFirst().orElse(null);

                    od.getValues().stream()
                            .filter(ovd -> !ovd.isDefault())
                            .forEach(ovd -> {

                                List<OptionValuesDto> optionCheckedList = defaultOptionValues
                                        .stream()
                                        .filter(dov -> !dov.equals(optionValuesDtoDefault))
                                        .collect(Collectors.toList());

                                optionCheckedList.add(ovd);

                                log.info("checked list: {}", optionCheckedList);

                                HatorProductDto equeldHatorProduct = hatorProductDtoList
                                        .stream()
                                        .filter(h -> {
                                            List<OptionValuesDto> collect = h.getOptionValuesDtoList()
                                                    .stream()
                                                    .filter(optionCheckedList::contains)
                                                    .collect(Collectors.toList());
                                            log.info("Contains colect option: {}", collect.size());
                                            return collect.size() == optionCheckedList.size();
                                        })
                                        .findFirst()
                                        .orElse(null);

                                if (Objects.nonNull(equeldHatorProduct)) {
                                    int priceHatorDto = equeldHatorProduct.getPrice();
                                    int actualPrice = priceHatorDto - priceDef;
                                    ovd.setMargin(actualPrice);
                                }
                                log.info("equeldHatorProduct: {}", equeldHatorProduct);

                            });
                });


        log.info("Options DTO: \n{}", optionDtoList);
        return optionDtoList;
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
                                    elementsProduct
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

                                                imageName = sku.concat("-").concat(imageName);
                                                log.info("Product image name: {}", imageName);
                                                String imageDBName = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);
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

                                                if (!allProductInitList.contains(product)) {
                                                    allProductInitList.add(product);
                                                }

                                                return product;
                                            })
                                            .collect(Collectors.toList());

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
                                .pause(Duration.ofSeconds(1))
                                .perform();
                        waitTranslate(driver);


                        String title = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//h1[@class='fm-main-title fm-page-title']"))).getText();
                        log.info("title: {}", title);
                        WebElement descriptionElement = webDriverWait
                                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='fm-product-description-cont']")));
                        moveToElement(descriptionElement, driver, 200);
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

                        log.info("Product price for profile: {}", prod.getPrice());

                        List<WebElement> attributeElements = driver.findElements(By.xpath("//div[@id='fm-product-attributes-top']"));
                        if (!attributeElements.isEmpty()) {
                            moveToElement(attributeElements.get(0), driver, 200);
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
                                            imgName = prod.getSku().concat("-").concat(imgName);
                                            log.info("Image name: {}", imgName);
                                            String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
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

                            log.info("Product: [sku={}] price: [{}]", prod.getSku(), prod.getPrice());
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

    public void dataDuplicateProduct() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductProfileApp> productProfilesApp = supplierApp.getProductProfilesApp();

        List<ProductProfileApp> unicList = productProfilesApp
                .stream()
                .distinct()
                .collect(Collectors.toList());

        List<ProductProfileApp> collect = productProfilesApp
                .stream()
                .filter(pp -> {
                    int productProfileId = pp.getProductProfileId();
                    ProductProfileApp prodileResult = unicList
                            .stream()
                            .filter(upp -> productProfileId == upp.getProductProfileId())
                            .findFirst()
                            .orElse(null);
                    return Objects.isNull(prodileResult);
                })
                .collect(Collectors.toList());

        collect.forEach(c -> {
            int productProfileId = c.getProductProfileId();

            appDaoService.deleteProfileAttributeByProfileId(productProfileId);
            appDaoService.deleteOptionByProfileId(productProfileId);
            appDaoService.deleteProfileById(productProfileId);
        });

    }


    public void changeFirstSecondImage() {

        List<ProductOpencart> products = opencartDaoService.getAllProductOpencartBySupplierAppName(SUPPLIER_NAME);
        log.info("Product hator count: {}", products.size());
        products = products
                .stream()
                .map(p -> opencartDaoService.getProductOpencartWithImageById(p.getId()))
                .collect(Collectors.toList());


        products
                .forEach(p -> {
                    String sku = p.getSku();
                    log.info("Sku: {}", sku);
                    String image = p.getImage();
                    log.info("Main image: {}", image);
                    List<ImageOpencart> imagesOpencart = p.getImagesOpencart();
                    log.info("Sub images size: {}", imagesOpencart.size());
                    if (!imagesOpencart.isEmpty()) {
                        List<ImageOpencart> sortedImages = imagesOpencart
                                .stream()
                                .sorted(Comparator.comparingInt(ImageOpencart::getSortOrder))
                                .collect(Collectors.toList());

                        ImageOpencart secondImage = sortedImages.get(0);

                        p.setImage(secondImage.getImage());
                        secondImage.setImage(image);

                        opencartDaoService.updateMainProductImageOpencart(p);

                        opencartDaoService.deleteImageByImageId(secondImage.getProductImageId());
                        opencartDaoService.saveImageOpencart(secondImage);

                    }
                });


    }

    public void updateImages() {

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

        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductOpencart> productOpencartList = opencartDaoService.getAllProductOpencartBySupplierAppName(supplierApp.getName());

        List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);
        List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);

        productsFromSite
                .stream()
                .forEach(p -> {

                    try {
                        String url = p.getUrlProduct();
                        String sku = p.getSku();

                        ProductOpencart searchProductOpencart = productOpencartList
                                .stream()
                                .filter(pdb -> pdb.getSku().equals(sku))
                                .findFirst()
                                .orElse(null);

                        if (Objects.nonNull(searchProductOpencart)) {
                            log.info("URL: {}", url);
                            log.info("SKU: {}", sku);


                            driver.get(url);
                            String mainImageName = searchProductOpencart.getImage().replaceAll("catalog/app/", "");
                            waitScripts(driver);
                            TimeUnit.SECONDS.sleep(2);

                            List<WebElement> imageElements = new WebDriverWait(driver, 10, 500)
                                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//div[@id='image-additional']//a[@data-href and contains(@data-href, '1000x1000')]")));
                            log.info("Images count: {}", imageElements.size());


                            List<String> stringUIrlImageList = new ArrayList<>();
                            if (imageElements.size() > 0) {
                                AtomicInteger countImg = new AtomicInteger();
                                List<ImageOpencart> productImages = imageElements
                                        .stream()
                                        .map(i -> {
                                            try {


                                                String fullUrl = i.getAttribute("data-href");
                                                if (!stringUIrlImageList.contains(fullUrl) && !fullUrl.contains(mainImageName)) {
                                                    stringUIrlImageList.add(fullUrl);
                                                    int imageCount = countImg.addAndGet(1);
                                                    log.info("Image url: {}", fullUrl);
                                                    String imgName = fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
                                                    imgName = searchProductOpencart.getSku().concat("-").concat(imgName);
                                                    log.info("Image name: {}", imgName);
                                                    String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
                                                    log.info("Image DB name: {}", dbImgPath);
                                                    if (!searchProductOpencart.getImage().equals(dbImgPath)) {
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
                                            } catch (Exception ex) {
                                                log.warn("Exception update image inner.", ex);
                                                return null;
                                            }
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());

                                opencartDaoService.deleteImageOpencartByProductId(searchProductOpencart.getId());
                                log.info("Images: {}", productImages);
                                productImages
                                        .forEach(i -> {
                                            i.setProductId(searchProductOpencart.getId());
                                            opencartDaoService.saveImageOpencart(i);
                                        });


                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Exception update image.", ex);
                    }


                });


        driver.close();
        driver.quit();
    }

    public void moveToElement(WebElement elementForMove, WebDriver driver, int pose) {
        new Actions(driver)
                .moveToElement(elementForMove)
                .pause(Duration.ofMillis(pose))
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
        new WebDriverWait(driver, 30, 500).until((ExpectedCondition<Boolean>) webDriver -> {
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
