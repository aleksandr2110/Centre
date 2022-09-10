package orlov.home.centurapp.service.parser;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
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
import orlov.home.centurapp.dto.api.artinhead.OptionDto;
import orlov.home.centurapp.dto.api.artinhead.OptionValuesDto;
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
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ParserServiceVivat extends ParserServiceAbstract {
    private final String SUPPLIER_NAME = "vivat";
    private final String SUPPLIER_URL = "https://hator-m.com/";
    private final String SUPPLIER_URL_CATEGORY = "";
    private final String DISPLAY_NAME = "502 - Vivat";
    private final String MANUFACTURER_NAME = "Віват";


    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;
    private final UpdateDataService updateDataService;

    public ParserServiceVivat(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService) {
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
            List<ProductOpencart> productsFromSite = getAllProducts(supplierApp);

            List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);


            OpencartDto opencartInfo = getOpencartInfo(fullProductsData, supplierApp);

            checkProductOption(opencartInfo);

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


    public ProductOpencart settingOptionsOpencart(ProductOpencart productOpencart, SupplierApp supplierApp) {

        try {

            List<OptionDto> optionEXCELList = productOpencart.getOptionDtoList();

            List<OptionOpencart> optionsOpencartList = optionEXCELList
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
                        optionOpencart.setType(o.getOptionType());
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
        return new ArrayList<>();
    }

    @Override
    public List<ProductOpencart> getProductsInitDataByCategory(List<CategoryOpencart> categoriesWithProduct, SupplierApp supplierApp) {
        return null;
    }

    @Override
    public List<ProductOpencart> getFullProductsData(List<ProductOpencart> products, SupplierApp supplierApp) {

        CategoryOpencart mainSupplierCategory = supplierApp.getMainSupplierCategory();
        log.info("Main cat: {}", mainSupplierCategory.getDescriptions().get(0).getName());
        CategoryApp categoryApp = getCategoryApp(mainSupplierCategory.getDescriptions().get(0).getName(), supplierApp);
        return products
                .stream()
                .peek(prod -> {

                    try {
                        prod.setCategoriesOpencart(new ArrayList<>(Arrays.asList(mainSupplierCategory)));

                        ManufacturerApp manufacturerApp = getManufacturerApp(MANUFACTURER_NAME, supplierApp);
                        ProductProfileApp productProfileApp = new ProductProfileApp();
                        productProfileApp.setUrl("");
                        productProfileApp.setCategoryId(categoryApp.getCategoryId());
                        productProfileApp.setSupplierId(supplierApp.getSupplierAppId());
                        productProfileApp.setSku(prod.getSku());
                        productProfileApp.setTitle(prod.getTitle());
                        productProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                        productProfileApp.setManufacturerApp(manufacturerApp);
                        productProfileApp.setPrice(prod.getPrice());
                        ProductProfileApp savedProductProfile = getProductProfile(productProfileApp, supplierApp);
                        prod.setProductProfileApp(savedProductProfile);

                        List<AttributeWrapper> attributesWrapper = prod.getAttributesWrapper();
                        List<AttributeWrapper> attributes = attributesWrapper
                                .stream()
                                .map(row -> {
                                    String key = row.getKeySite();
                                    String value = row.getValueSite();
                                    log.info("Key: {}, value: {}", key, value);
                                    AttributeWrapper attributeWrapper = new AttributeWrapper(key, value, null);
                                    return getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        prod.setAttributesWrapper(new ArrayList<>());
                        prod.getAttributesWrapper().addAll(attributes);

                        settingOptionsOpencart(prod, supplierApp);
                        setManufacturer(prod, supplierApp);

                    } catch (Exception ex) {
                        log.warn("Bad parsing product data", ex);
                    }
                })
                .collect(Collectors.toList());

    }


    public String getDescription(String text) {
        return wrapToHtml(text);
    }

    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        return null;
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

    public String generateModel(String supplierModel) {
        String substring = DISPLAY_NAME.substring(0, DISPLAY_NAME.indexOf("-")).trim();
        String result;
        result = substring.concat("-").concat(supplierModel);
        return result;
    }

    public List<ProductOpencart> getAllProducts(SupplierApp supplierApp) {
        List<ProductOpencart> fullProductList = new ArrayList<>();

        List<ProductOpencart> krislaImport = getKrislaImport(supplierApp);
        fullProductList.addAll(krislaImport);

        List<ProductOpencart> krisla = getKrisla(supplierApp);
        fullProductList.addAll(krisla);

        List<ProductOpencart> bar = getBar(supplierApp);
        fullProductList.addAll(bar);

        List<ProductOpencart> offic = getOffic(supplierApp);
        fullProductList.addAll(offic);

        List<ProductOpencart> komple = getKomple(supplierApp);
        fullProductList.addAll(komple);

        List<ProductOpencart> divan = getDivan(supplierApp);
        fullProductList.addAll(divan);

        return fullProductList;
    }


    public List<ProductOpencart> getKomple(SupplierApp supplierApp) {
        CategoryOpencart mainSupplierCategory = supplierApp.getMainSupplierCategory();
        List<CategoryOpencart> parentsCategories = getParentsCategories(mainSupplierCategory, supplierApp.getCategoryOpencartDB());
        List<ProductOpencart> products = new ArrayList<>();

        //      TODO 0075
        ProductOpencart product0075 = new ProductOpencart.Builder()
                .withModel("502-0075")
                .withTitle("Ролик гумовий")
                .withSku("0075")
                .withPrice(new BigDecimal("42"))
                .withItuaOriginalPrice(new BigDecimal("42"))
                .withJan("vivat")
                .build();
        product0075.setCategoriesOpencart(parentsCategories);

        List<OptionDto> optionDtoFileList0075 = Arrays.asList(
                new OptionDto("Найменування", "Найменування", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("Пластик", "Пластик", true, 0, 0, "", ""),
                        new OptionValuesDto("SH-WL1", "SH-WL1", false, 10, 0, "", ""),
                        new OptionValuesDto("SH-WL2", "SH-WL2", false, 10, 0, "", "")
                        )));
        product0075.setOptionDtoList(optionDtoFileList0075);
        products.add(product0075);


        //      TODO 0076
        ProductOpencart product0076 = new ProductOpencart.Builder()
                .withModel("502-0076")
                .withTitle("Газліфти")
                .withSku("0076")
                .withPrice(new BigDecimal("280"))
                .withItuaOriginalPrice(new BigDecimal("280"))
                .withJan("vivat")
                .build();
        product0076.setCategoriesOpencart(parentsCategories);

        List<OptionDto> optionDtoFileList0076 = Arrays.asList(
                new OptionDto("Найменування", "Найменування", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("50x160,stroke 60mm.class2", "50x160,stroke 60mm.class2", true, 0, 0, "", ""),
                        new OptionValuesDto("50x180,stroke 80mm.class2", "50x180,stroke 80mm.class2", false, 0, 0, "", ""),
                        new OptionValuesDto("50x230,stroke 140mm,class2", "50x230,stroke 140mm,class2", false, 0, 0, "", ""),
                        new OptionValuesDto("50x180,stroke 80mm.class2 хром", "50x180,stroke 80mm.class2 хром", false, 70, 0, "", ""),
                        new OptionValuesDto("50x230,stroke 140mm,class2 хром", "50x230,stroke 140mm,class2 хром", false, 70, 0, "", "")
                        )));
        product0076.setOptionDtoList(optionDtoFileList0076);
        products.add(product0076);

        //      TODO 0077
        ProductOpencart product0077 = new ProductOpencart.Builder()
                .withModel("502-0077")
                .withTitle("Механізми D-Tilt")
                .withSku("0077")
                .withPrice(new BigDecimal("735"))
                .withItuaOriginalPrice(new BigDecimal("735"))
                .withJan("vivat")
                .build();
        product0077.setCategoriesOpencart(parentsCategories);
        products.add(product0077);

        //      TODO 0078
        ProductOpencart product0078 = new ProductOpencart.Builder()
                .withModel("502-0078")
                .withTitle("Хрестовина хром")
                .withSku("0078")
                .withPrice(new BigDecimal("602"))
                .withItuaOriginalPrice(new BigDecimal("602"))
                .withJan("vivat")
                .build();
        product0078.setCategoriesOpencart(parentsCategories);
        products.add(product0078);


        //      TODO 0079
        ProductOpencart product0079 = new ProductOpencart.Builder()
                .withModel("502-0079")
                .withTitle("Хрестовина хром 2")
                .withSku("0079")
                .withPrice(new BigDecimal("798"))
                .withItuaOriginalPrice(new BigDecimal("798"))
                .withJan("vivat")
                .build();
        product0079.setCategoriesOpencart(parentsCategories);
        products.add(product0079);

        return products;
    }


    public List<ProductOpencart> getOffic(SupplierApp supplierApp) {
        CategoryOpencart mainSupplierCategory = supplierApp.getMainSupplierCategory();
        List<CategoryOpencart> parentsCategories = getParentsCategories(mainSupplierCategory, supplierApp.getCategoryOpencartDB());
        List<ProductOpencart> products = new ArrayList<>();

        //      TODO 0050
        ProductOpencart product0050 = new ProductOpencart.Builder()
                .withModel("502-0050")
                .withTitle("Стіл Пекін")
                .withSku("0050")
                .withPrice(new BigDecimal("1682"))
                .withItuaOriginalPrice(new BigDecimal("1682"))
                .withJan("vivat")
                .build();
        product0050.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0050 = new ProductDescriptionOpencart.Builder()
                .withName(product0050.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0050.getProductsDescriptionOpencart().add(descriptionOpencart0050);

        List<OptionDto> optionDtoFileList0050 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 273, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 36, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 328, 0, "", "")
                )));
        product0050.setOptionDtoList(optionDtoFileList0050);
        products.add(product0050);


        //      TODO 0051
        ProductOpencart product0051 = new ProductOpencart.Builder()
                .withModel("502-0051")
                .withTitle("Стіл Зеро")
                .withSku("0051")
                .withPrice(new BigDecimal("1892"))
                .withItuaOriginalPrice(new BigDecimal("1892"))
                .withJan("vivat")
                .build();
        product0051.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0051 = new ProductDescriptionOpencart.Builder()
                .withName(product0051.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0051.getProductsDescriptionOpencart().add(descriptionOpencart0051);

        List<OptionDto> optionDtoFileList0051 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 273, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 35, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 331, 0, "", "")
                )));
        product0051.setOptionDtoList(optionDtoFileList0051);
        products.add(product0051);


        //      TODO 0052
        ProductOpencart product0052 = new ProductOpencart.Builder()
                .withModel("502-0052")
                .withTitle("Стіл Бьюти")
                .withSku("0052")
                .withPrice(new BigDecimal("2770"))
                .withItuaOriginalPrice(new BigDecimal("2770"))
                .withJan("vivat")
                .build();
        product0052.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0052 = new ProductDescriptionOpencart.Builder()
                .withName(product0052.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0052.getProductsDescriptionOpencart().add(descriptionOpencart0052);

        List<OptionDto> optionDtoFileList0052 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 389, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 15, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 437, 0, "", "")
                )));
        product0052.setOptionDtoList(optionDtoFileList0052);
        products.add(product0052);


        //      TODO 0053
        ProductOpencart product0053 = new ProductOpencart.Builder()
                .withModel("502-0053")
                .withTitle("Стіл Бьюти")
                .withSku("0053")
                .withPrice(new BigDecimal("2770"))
                .withItuaOriginalPrice(new BigDecimal("2770"))
                .withJan("vivat")
                .build();
        product0053.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0053 = new ProductDescriptionOpencart.Builder()
                .withName(product0053.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0053.getProductsDescriptionOpencart().add(descriptionOpencart0053);

        List<OptionDto> optionDtoFileList0053 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 389, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 15, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 437, 0, "", "")
                )));
        product0053.setOptionDtoList(optionDtoFileList0053);
        products.add(product0053);


        //      TODO 0054
        ProductOpencart product0054 = new ProductOpencart.Builder()
                .withModel("502-0054")
                .withTitle("Стіл Бостон")
                .withSku("0054")
                .withPrice(new BigDecimal("2238"))
                .withItuaOriginalPrice(new BigDecimal("2238"))
                .withJan("vivat")
                .build();
        product0054.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0054 = new ProductDescriptionOpencart.Builder()
                .withName(product0054.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0054.getProductsDescriptionOpencart().add(descriptionOpencart0054);

        List<OptionDto> optionDtoFileList0054 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 284, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 34, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 344, 0, "", "")
                )));
        product0054.setOptionDtoList(optionDtoFileList0054);
        products.add(product0054);


        //      TODO 0055
        ProductOpencart product0055 = new ProductOpencart.Builder()
                .withModel("502-0055")
                .withTitle("Стіл Сет")
                .withSku("0055")
                .withPrice(new BigDecimal("2257"))
                .withItuaOriginalPrice(new BigDecimal("2257"))
                .withJan("vivat")
                .build();
        product0055.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0055 = new ProductDescriptionOpencart.Builder()
                .withName(product0055.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0055.getProductsDescriptionOpencart().add(descriptionOpencart0055);

        List<OptionDto> optionDtoFileList0055 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 274, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 36, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 331, 0, "", "")
                )));
        product0055.setOptionDtoList(optionDtoFileList0055);
        products.add(product0055);


        //      TODO 0056
        ProductOpencart product0056 = new ProductOpencart.Builder()
                .withModel("502-0056")
                .withTitle("Стіл Луи")
                .withSku("0056")
                .withPrice(new BigDecimal("2356"))
                .withItuaOriginalPrice(new BigDecimal("2356"))
                .withJan("vivat")
                .build();
        product0056.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0056 = new ProductDescriptionOpencart.Builder()
                .withName(product0056.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0056.getProductsDescriptionOpencart().add(descriptionOpencart0056);

        List<OptionDto> optionDtoFileList0056 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 266, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 36, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 324, 0, "", "")
                )));
        product0056.setOptionDtoList(optionDtoFileList0056);
        products.add(product0056);


        //      TODO 0057
        ProductOpencart product0057 = new ProductOpencart.Builder()
                .withModel("502-0057")
                .withTitle("Стіл Норд")
                .withSku("0057")
                .withPrice(new BigDecimal("2394"))
                .withItuaOriginalPrice(new BigDecimal("2394"))
                .withJan("vivat")
                .build();
        product0057.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0057 = new ProductDescriptionOpencart.Builder()
                .withName(product0057.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0057.getProductsDescriptionOpencart().add(descriptionOpencart0057);

        List<OptionDto> optionDtoFileList0057 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 271, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 33, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 330, 0, "", "")
                )));
        product0057.setOptionDtoList(optionDtoFileList0057);
        products.add(product0057);


        //      TODO 0058
        ProductOpencart product0058 = new ProductOpencart.Builder()
                .withModel("502-0058")
                .withTitle("Стіл Лайт")
                .withSku("0058")
                .withPrice(new BigDecimal("2045"))
                .withItuaOriginalPrice(new BigDecimal("2045"))
                .withJan("vivat")
                .build();
        product0058.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0058 = new ProductDescriptionOpencart.Builder()
                .withName(product0058.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0058.getProductsDescriptionOpencart().add(descriptionOpencart0058);

        List<OptionDto> optionDtoFileList0058 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 273, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 35, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 328, 0, "", "")
                )));
        product0058.setOptionDtoList(optionDtoFileList0058);
        products.add(product0058);


        //      TODO 0059
        ProductOpencart product0059 = new ProductOpencart.Builder()
                .withModel("502-0059")
                .withTitle("Стіл Космо")
                .withSku("0059")
                .withPrice(new BigDecimal("2662"))
                .withItuaOriginalPrice(new BigDecimal("2662"))
                .withJan("vivat")
                .build();
        product0059.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0059 = new ProductDescriptionOpencart.Builder()
                .withName(product0059.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат.+ сетка ячейка 5*5 мм"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0059.getProductsDescriptionOpencart().add(descriptionOpencart0059);

        List<OptionDto> optionDtoFileList0059 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 271, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 33, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 329, 0, "", "")
                )));
        product0059.setOptionDtoList(optionDtoFileList0059);
        products.add(product0059);


        //      TODO 0060
        ProductOpencart product0060 = new ProductOpencart.Builder()
                .withModel("502-0060")
                .withTitle("Стіл Хард")
                .withSku("0060")
                .withPrice(new BigDecimal("3472"))
                .withItuaOriginalPrice(new BigDecimal("3472"))
                .withJan("vivat")
                .build();
        product0060.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0060 = new ProductDescriptionOpencart.Builder()
                .withName(product0060.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат.+ сетка ячейка 5*5 мм"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0060.getProductsDescriptionOpencart().add(descriptionOpencart0060);

        List<OptionDto> optionDtoFileList0060 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 273, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 36, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 331, 0, "", "")
                )));
        product0060.setOptionDtoList(optionDtoFileList0060);
        products.add(product0060);


        //      TODO 0061
        ProductOpencart product0061 = new ProductOpencart.Builder()
                .withModel("502-0061")
                .withTitle("Стіл Вика")
                .withSku("0061")
                .withPrice(new BigDecimal("2928"))
                .withItuaOriginalPrice(new BigDecimal("2928"))
                .withJan("vivat")
                .build();
        product0061.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0061 = new ProductDescriptionOpencart.Builder()
                .withName(product0061.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат.+ сетка ячейка 5*5 мм"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0061.getProductsDescriptionOpencart().add(descriptionOpencart0061);

        List<OptionDto> optionDtoFileList0061 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 316, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 78, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 372, 0, "", "")
                )));
        product0061.setOptionDtoList(optionDtoFileList0061);
        products.add(product0061);


        //      TODO 0062
        ProductOpencart product0062 = new ProductOpencart.Builder()
                .withModel("502-0062")
                .withTitle("Стіл Ника")
                .withSku("0062")
                .withPrice(new BigDecimal("2804"))
                .withItuaOriginalPrice(new BigDecimal("2804"))
                .withJan("vivat")
                .build();
        product0062.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0062 = new ProductDescriptionOpencart.Builder()
                .withName(product0062.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20 ДСП-дуб сонома, металл -черный мат.+ труба 12*12"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0062.getProductsDescriptionOpencart().add(descriptionOpencart0062);

        List<OptionDto> optionDtoFileList0062 = Arrays.asList(
                new OptionDto("Габарити, товщина стільниці, мм", "Габарити, товщина стільниці, мм", 0, OCConstant.OPTION_TYPE_SELECT, Arrays.asList(
                        new OptionValuesDto("1200*600*750\\16", "1200*600*750\\16", true, 0, 0, "", ""),
                        new OptionValuesDto("1200*600*750\\32", "1200*600*750\\32", false, 236, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\16", "1350*600*750\\16", false, 36, 0, "", ""),
                        new OptionValuesDto("1350*600*750\\32", "1350*600*750\\32", false, 333, 0, "", "")
                )));
        product0062.setOptionDtoList(optionDtoFileList0062);
        products.add(product0062);

        //      TODO 0063
        ProductOpencart product0063 = new ProductOpencart.Builder()
                .withModel("502-0063")
                .withTitle("Стіл Дуэт")
                .withSku("0063")
                .withPrice(new BigDecimal("2149"))
                .withItuaOriginalPrice(new BigDecimal("2149"))
                .withJan("vivat")
                .build();
        product0063.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0063 = new ProductDescriptionOpencart.Builder()
                .withName(product0063.getTitle())
                .withDescription(getDescription("Характеристики:\n" +
                        "Труба 40*20. ДСП - дуб сонома.\n\n" +
                        "Габарити, товщина стільниці, мм:\n" +
                        "1000*500*500\\20"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0063.getProductsDescriptionOpencart().add(descriptionOpencart0063);
        products.add(product0063);


        //      TODO 0064
        ProductOpencart product0064 = new ProductOpencart.Builder()
                .withModel("502-0064")
                .withTitle("Шафа для одягу ШД-01")
                .withSku("0064")
                .withPrice(new BigDecimal("2064"))
                .withItuaOriginalPrice(new BigDecimal("2064"))
                .withJan("vivat")
                .build();
        product0064.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0064 = new ProductDescriptionOpencart.Builder()
                .withName(product0064.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "700*350*1750"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0064.getProductsDescriptionOpencart().add(descriptionOpencart0064);
        products.add(product0064);


        //      TODO 0065
        ProductOpencart product0065 = new ProductOpencart.Builder()
                .withModel("502-0065")
                .withTitle("Шафа для документів ШБ-01")
                .withSku("0065")
                .withPrice(new BigDecimal("1800"))
                .withItuaOriginalPrice(new BigDecimal("1800"))
                .withJan("vivat")
                .build();
        product0065.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0065 = new ProductDescriptionOpencart.Builder()
                .withName(product0065.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "700*350*1750"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0065.getProductsDescriptionOpencart().add(descriptionOpencart0065);
        products.add(product0065);


        //      TODO 0066
        ProductOpencart product0066 = new ProductOpencart.Builder()
                .withModel("502-0066")
                .withTitle("Пенал відкритий ПВ-01")
                .withSku("0066")
                .withPrice(new BigDecimal("1399"))
                .withItuaOriginalPrice(new BigDecimal("1399"))
                .withJan("vivat")
                .build();
        product0066.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0066 = new ProductDescriptionOpencart.Builder()
                .withName(product0066.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "700*350*1750"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0066.getProductsDescriptionOpencart().add(descriptionOpencart0066);
        products.add(product0066);


        //      TODO 0067
        ProductOpencart product0067 = new ProductOpencart.Builder()
                .withModel("502-0067")
                .withTitle("Опора Пекин")
                .withSku("0067")
                .withPrice(new BigDecimal("639"))
                .withItuaOriginalPrice(new BigDecimal("639"))
                .withJan("vivat")
                .build();
        product0067.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0067 = new ProductDescriptionOpencart.Builder()
                .withName(product0067.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "700*350*1750"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0067.getProductsDescriptionOpencart().add(descriptionOpencart0067);
        products.add(product0067);

        //      TODO 0068
        ProductOpencart product0068 = new ProductOpencart.Builder()
                .withModel("502-0068")
                .withTitle("Опора Зеро")
                .withSku("0068")
                .withPrice(new BigDecimal("733"))
                .withItuaOriginalPrice(new BigDecimal("733"))
                .withJan("vivat")
                .build();
        product0068.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0068 = new ProductDescriptionOpencart.Builder()
                .withName(product0068.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "700*350*1750"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0068.getProductsDescriptionOpencart().add(descriptionOpencart0068);
        products.add(product0068);

        //      TODO 0069
        ProductOpencart product0069 = new ProductOpencart.Builder()
                .withModel("502-0069")
                .withTitle("Опора Хард")
                .withSku("0069")
                .withPrice(new BigDecimal("996"))
                .withItuaOriginalPrice(new BigDecimal("996"))
                .withJan("vivat")
                .build();
        product0069.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0069 = new ProductDescriptionOpencart.Builder()
                .withName(product0069.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "700*350*1750"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0069.getProductsDescriptionOpencart().add(descriptionOpencart0069);
        products.add(product0069);


        //      TODO 0070
        ProductOpencart product0070 = new ProductOpencart.Builder()
                .withModel("502-0070")
                .withTitle("Тумба мобільна 1-ТМ")
                .withSku("0070")
                .withPrice(new BigDecimal("1059"))
                .withItuaOriginalPrice(new BigDecimal("1059"))
                .withJan("vivat")
                .build();
        product0070.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0070 = new ProductDescriptionOpencart.Builder()
                .withName(product0070.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "400*400*625"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0070.getProductsDescriptionOpencart().add(descriptionOpencart0070);
        products.add(product0070);


        //      TODO 0071
        ProductOpencart product0071 = new ProductOpencart.Builder()
                .withModel("502-0071")
                .withTitle("Тумба приставна 1-ТП")
                .withSku("0071")
                .withPrice(new BigDecimal("1035"))
                .withItuaOriginalPrice(new BigDecimal("1035"))
                .withJan("vivat")
                .build();
        product0071.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0071 = new ProductDescriptionOpencart.Builder()
                .withName(product0071.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "400*600*750"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0071.getProductsDescriptionOpencart().add(descriptionOpencart0071);
        products.add(product0071);

        //      TODO 0072
        ProductOpencart product0072 = new ProductOpencart.Builder()
                .withModel("502-0072")
                .withTitle("Опора тумби Пекин")
                .withSku("0072")
                .withPrice(new BigDecimal("549"))
                .withItuaOriginalPrice(new BigDecimal("549"))
                .withJan("vivat")
                .build();
        product0072.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0072 = new ProductDescriptionOpencart.Builder()
                .withName(product0072.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "400*400*100"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0072.getProductsDescriptionOpencart().add(descriptionOpencart0072);
        products.add(product0072);

        //      TODO 0073
        ProductOpencart product0073 = new ProductOpencart.Builder()
                .withModel("502-0073")
                .withTitle("Опора тумби ЗЕРО")
                .withSku("0073")
                .withPrice(new BigDecimal("652"))
                .withItuaOriginalPrice(new BigDecimal("652"))
                .withJan("vivat")
                .build();
        product0073.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0073 = new ProductDescriptionOpencart.Builder()
                .withName(product0073.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "400*400*100"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0073.getProductsDescriptionOpencart().add(descriptionOpencart0073);
        products.add(product0073);


        //      TODO 0074
        ProductOpencart product0074 = new ProductOpencart.Builder()
                .withModel("502-0074")
                .withTitle("Опора тумби Хард")
                .withSku("0074")
                .withPrice(new BigDecimal("936"))
                .withItuaOriginalPrice(new BigDecimal("936"))
                .withJan("vivat")
                .build();
        product0074.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0074 = new ProductDescriptionOpencart.Builder()
                .withName(product0074.getTitle())
                .withDescription(getDescription(
                        "Габарити, товщина стільниці, мм:\n" +
                                "400*400*100"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0074.getProductsDescriptionOpencart().add(descriptionOpencart0074);
        products.add(product0074);


        return products;
    }

    public List<ProductOpencart> getBar(SupplierApp supplierApp) {
        CategoryOpencart mainSupplierCategory = supplierApp.getMainSupplierCategory();
        List<CategoryOpencart> parentsCategories = getParentsCategories(mainSupplierCategory, supplierApp.getCategoryOpencartDB());
        List<ProductOpencart> products = new ArrayList<>();

        // TODO 0034
        ProductOpencart product0034 = new ProductOpencart.Builder()
                .withModel("502-0034")
                .withTitle("Стілець Зєтта CLBX-04")
                .withSku("0034")
                .withPrice(new BigDecimal("3055"))
                .withItuaOriginalPrice(new BigDecimal("3055"))
                .withJan("vivat")
                .build();
        product0034.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0034 = new ProductDescriptionOpencart.Builder()
                .withName(product0034.getTitle())
                .withDescription(getDescription("Стілець Зєтта (CLBX-04 чорний, 42*59*103 PU шкіра, хромовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0034.getProductsDescriptionOpencart().add(descriptionOpencart0034);
        products.add(product0034);

        // TODO 0035
        ProductOpencart product0035 = new ProductOpencart.Builder()
                .withModel("502-0035")
                .withTitle("Стілець Лада CLBX-01")
                .withSku("0035")
                .withPrice(new BigDecimal("1684"))
                .withItuaOriginalPrice(new BigDecimal("1684"))
                .withJan("vivat")
                .build();
        product0035.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0035 = new ProductDescriptionOpencart.Builder()
                .withName(product0035.getTitle())
                .withDescription(getDescription("Стілець Лада (CLBX-01, колір беж PU28, 45*52*98, KD, PU шкіра, хромовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0035.getProductsDescriptionOpencart().add(descriptionOpencart0035);
        products.add(product0035);

        // TODO 0036
        ProductOpencart product0036 = new ProductOpencart.Builder()
                .withModel("502-0036")
                .withTitle("Стілець Леді CLBX-03")
                .withSku("0036")
                .withPrice(new BigDecimal("3055"))
                .withItuaOriginalPrice(new BigDecimal("3055"))
                .withJan("vivat")
                .build();
        product0036.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0036 = new ProductDescriptionOpencart.Builder()
                .withName(product0036.getTitle())
                .withDescription(getDescription("Стілець Зєтта (CLBX-04 чорний, 42*59*103 PU шкіра, хромовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0036.getProductsDescriptionOpencart().add(descriptionOpencart0036);
        products.add(product0036);


        // TODO 0037
        ProductOpencart product0037 = new ProductOpencart.Builder()
                .withModel("502-0037")
                .withTitle("Стілець Рітц CLBX-02")
                .withSku("0037")
                .withPrice(new BigDecimal("1885"))
                .withItuaOriginalPrice(new BigDecimal("1885"))
                .withJan("vivat")
                .build();
        product0037.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0037 = new ProductDescriptionOpencart.Builder()
                .withName(product0037.getTitle())
                .withDescription(getDescription("Стілець Рітц (CLBX-02, PU X-113 молочний, 55*42*100, PU шкіра, хромовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0037.getProductsDescriptionOpencart().add(descriptionOpencart0037);
        products.add(product0037);


        // TODO 0038
        ProductOpencart product0038 = new ProductOpencart.Builder()
                .withModel("502-0038")
                .withTitle("Обідній стілець Коралл")
                .withSku("0038")
                .withPrice(new BigDecimal("715"))
                .withItuaOriginalPrice(new BigDecimal("715"))
                .withJan("vivat")
                .build();
        product0038.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0038 = new ProductDescriptionOpencart.Builder()
                .withName(product0038.getTitle())
                .withDescription(getDescription("Обідній стілець з поліпропілену чорний, сірий"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0038.getProductsDescriptionOpencart().add(descriptionOpencart0038);
        products.add(product0038);

        // TODO 0039
        ProductOpencart product0039 = new ProductOpencart.Builder()
                .withModel("502-0039")
                .withTitle("Стіл Павук TLBX-04")
                .withSku("0039")
                .withPrice(new BigDecimal("7300"))
                .withItuaOriginalPrice(new BigDecimal("7300"))
                .withJan("vivat")
                .build();
        product0039.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0039 = new ProductDescriptionOpencart.Builder()
                .withName(product0039.getTitle())
                .withDescription(getDescription("Стіл Павук (TLBX-04чорний 119/83*83*75, 12мм гартоване скло, фарбований каркас та ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0039.getProductsDescriptionOpencart().add(descriptionOpencart0039);
        products.add(product0039);

        // TODO 0040
        ProductOpencart product0040 = new ProductOpencart.Builder()
                .withModel("502-0040")
                .withTitle("Стіл Павук 2 TLBX-05")
                .withSku("0040")
                .withPrice(new BigDecimal("7410"))
                .withItuaOriginalPrice(new BigDecimal("7410"))
                .withJan("vivat")
                .build();
        product0040.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0040 = new ProductDescriptionOpencart.Builder()
                .withName(product0040.getTitle())
                .withDescription(getDescription("Стіл Павук 2 (TLBX-05 чорний 175/125*80*75, стільниця - 12мм гартоване скло, стальний фарбований каркас та стальні хромовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0040.getProductsDescriptionOpencart().add(descriptionOpencart0040);
        products.add(product0040);

        // TODO 0041
        ProductOpencart product0041 = new ProductOpencart.Builder()
                .withModel("502-0041")
                .withTitle("Стіл Лайт TLBX-02")
                .withSku("0041")
                .withPrice(new BigDecimal("3972"))
                .withItuaOriginalPrice(new BigDecimal("3972"))
                .withJan("vivat")
                .build();
        product0041.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0041 = new ProductDescriptionOpencart.Builder()
                .withName(product0041.getTitle())
                .withDescription(getDescription(" Стіл Лайт (TLBX-02 білий 100*60*74, 10мм білизна гартоване скло, фарбований каркас та ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0041.getProductsDescriptionOpencart().add(descriptionOpencart0041);
        products.add(product0041);


        // TODO 0042
        ProductOpencart product0042 = new ProductOpencart.Builder()
                .withModel("502-0042")
                .withTitle("Стіл Грація TLBX-01")
                .withSku("0042")
                .withPrice(new BigDecimal("5096"))
                .withItuaOriginalPrice(new BigDecimal("5096"))
                .withJan("vivat")
                .build();
        product0042.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0042 = new ProductDescriptionOpencart.Builder()
                .withName(product0042.getTitle())
                .withDescription(getDescription(" Стіл Грація (TLBX-01 білий (120/60)*90*74, MDF з меламін стільниця фарбовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0042.getProductsDescriptionOpencart().add(descriptionOpencart0042);
        products.add(product0042);

        // TODO 0043
        ProductOpencart product0043 = new ProductOpencart.Builder()
                .withModel("502-0043")
                .withTitle("Стіл Ніка TLBX-03")
                .withSku("0043")
                .withPrice(new BigDecimal("3029"))
                .withItuaOriginalPrice(new BigDecimal("3029"))
                .withJan("vivat")
                .build();
        product0043.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0043 = new ProductDescriptionOpencart.Builder()
                .withName(product0043.getTitle())
                .withDescription(getDescription("Стіл Ніка (TLBX-03 білий, 120*80*75, 8мм гартоване скло, фарбовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0043.getProductsDescriptionOpencart().add(descriptionOpencart0043);
        products.add(product0043);


        // TODO 0045
        ProductOpencart product0045 = new ProductOpencart.Builder()
                .withModel("502-0045")
                .withTitle("Стіл Фокус TLBX-06")
                .withSku("0045")
                .withPrice(new BigDecimal("2080"))
                .withItuaOriginalPrice(new BigDecimal("2080"))
                .withJan("vivat")
                .build();
        product0045.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0045 = new ProductDescriptionOpencart.Builder()
                .withName(product0045.getTitle())
                .withDescription(getDescription("Стіл Фокус (TLBX-06 чорний, 100*60*74, стільниця - 10мм гартоване фарбоване скло, нижнє гартоване скло  з фарбованим матовим покриттям 5мм,  ніжки - нержавіюча сталь)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0045.getProductsDescriptionOpencart().add(descriptionOpencart0045);
        products.add(product0045);

        // TODO 0046
        ProductOpencart product0046 = new ProductOpencart.Builder()
                .withModel("502-0046")
                .withTitle("Стіл Плато TLBX-07")
                .withSku("0046")
                .withPrice(new BigDecimal("6422"))
                .withItuaOriginalPrice(new BigDecimal("6422"))
                .withJan("vivat")
                .build();
        product0046.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0046 = new ProductDescriptionOpencart.Builder()
                .withName(product0046.getTitle())
                .withDescription(getDescription("Стіл Плато (TLBX-07 кремовий 155/100*80*77, стільниця - 10мм гартоване скло, стальний фарбований каркас та стальні хромовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0046.getProductsDescriptionOpencart().add(descriptionOpencart0046);
        products.add(product0046);

        // TODO 0047
        ProductOpencart product0047 = new ProductOpencart.Builder()
                .withModel("502-0047")
                .withTitle("Стіл Фемілі Р TLBX-08P")
                .withSku("0047")
                .withPrice(new BigDecimal("5096"))
                .withItuaOriginalPrice(new BigDecimal("5096"))
                .withJan("vivat")
                .build();
        product0047.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0047 = new ProductDescriptionOpencart.Builder()
                .withName(product0047.getTitle())
                .withDescription(getDescription("Стіл Фемілі Р (TLBX-08P чорний з візерунком, 170/110*75*77, стільниця - 12мм гартоване скло, стальний фарбований каркас та стальні хромовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0047.getProductsDescriptionOpencart().add(descriptionOpencart0047);
        products.add(product0047);


        // TODO 0048
        ProductOpencart product0048 = new ProductOpencart.Builder()
                .withModel("502-0048")
                .withTitle("Стіл Фемілі СБ TLBX-08SW")
                .withSku("0048")
                .withPrice(new BigDecimal("5720"))
                .withItuaOriginalPrice(new BigDecimal("5720"))
                .withJan("vivat")
                .build();
        product0048.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0048 = new ProductDescriptionOpencart.Builder()
                .withName(product0048.getTitle())
                .withDescription(getDescription("Стіл Фемілі СБ (TLBX-08SW супер-білий 170/110*75*77, стільниця - 12мм гартоване скло, стальний фарбований каркас та стальні хромовані ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0048.getProductsDescriptionOpencart().add(descriptionOpencart0048);
        products.add(product0048);


        // TODO 0049
        ProductOpencart product0049 = new ProductOpencart.Builder()
                .withModel("502-0049")
                .withTitle("Стіл Фемілі 2 TLBX-09")
                .withSku("0049")
                .withPrice(new BigDecimal("7400"))
                .withItuaOriginalPrice(new BigDecimal("7400"))
                .withJan("vivat")
                .build();
        product0049.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0049 = new ProductDescriptionOpencart.Builder()
                .withName(product0049.getTitle())
                .withDescription(getDescription("Стіл Фемілі 2 (TLBX-09 сірий 200/120*83*77, стільниця - 10мм гартоване скло, стальний фарбований каркас та стальні крашені ніжки)"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0049.getProductsDescriptionOpencart().add(descriptionOpencart0049);
        products.add(product0049);


        return products;
    }


    public List<ProductOpencart> getDivan(SupplierApp supplierApp) {
        CategoryOpencart mainSupplierCategory = supplierApp.getMainSupplierCategory();
        List<CategoryOpencart> parentsCategories = getParentsCategories(mainSupplierCategory, supplierApp.getCategoryOpencartDB());
        List<ProductOpencart> products = new ArrayList<>();

        ProductOpencart product0080 = new ProductOpencart.Builder()
                .withModel("502-0080")
                .withTitle("Диван Сідней Двойной")
                .withSku("0080")
                .withPrice(new BigDecimal("5400"))
                .withItuaOriginalPrice(new BigDecimal("5400"))
                .withJan("vivat")
                .build();
        product0080.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0080 = new ProductDescriptionOpencart.Builder()
                .withName(product0080.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "165*90*90"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0080.getProductsDescriptionOpencart().add(descriptionOpencart0080);

        List<OptionDto> optionDtoFileList0080 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 100, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 260, 0, "", ""))));
        product0080.setOptionDtoList(optionDtoFileList0080);
        products.add(product0080);


        ProductOpencart product0081 = new ProductOpencart.Builder()
                .withModel("502-0081")
                .withTitle("Диван Сідней Одинарний")
                .withSku("0081")
                .withPrice(new BigDecimal("3300"))
                .withItuaOriginalPrice(new BigDecimal("3300"))
                .withJan("vivat")
                .build();
        product0081.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0081 = new ProductDescriptionOpencart.Builder()
                .withName(product0081.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "90*84*90"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0081.getProductsDescriptionOpencart().add(descriptionOpencart0081);

        List<OptionDto> optionDtoFileList0081 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 80, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 150, 0, "", ""))));
        product0081.setOptionDtoList(optionDtoFileList0081);
        products.add(product0081);


        ProductOpencart product0082 = new ProductOpencart.Builder()
                .withModel("502-0082")
                .withTitle("Диван Сідней Кутовий")
                .withSku("0082")
                .withPrice(new BigDecimal("3520"))
                .withItuaOriginalPrice(new BigDecimal("3520"))
                .withJan("vivat")
                .build();
        product0082.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0082 = new ProductDescriptionOpencart.Builder()
                .withName(product0082.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "90*84*90"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0082.getProductsDescriptionOpencart().add(descriptionOpencart0082);

        List<OptionDto> optionDtoFileList0082 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 70, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 390, 0, "", ""))));
        product0082.setOptionDtoList(optionDtoFileList0082);
        products.add(product0082);


        ProductOpencart product0083 = new ProductOpencart.Builder()
                .withModel("502-0083")
                .withTitle("Диван Сідней Підлокітники")
                .withSku("0083")
                .withPrice(new BigDecimal("930"))
                .withItuaOriginalPrice(new BigDecimal("930"))
                .withJan("vivat")
                .build();
        product0083.setCategoriesOpencart(parentsCategories);


        List<OptionDto> optionDtoFileList0083 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 20, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 40, 0, "", ""))));
        product0083.setOptionDtoList(optionDtoFileList0083);
        products.add(product0083);


        ProductOpencart product0084 = new ProductOpencart.Builder()
                .withModel("502-0084")
                .withTitle("Диван Сідней Підлокітники")
                .withSku("0084")
                .withPrice(new BigDecimal("12000"))
                .withItuaOriginalPrice(new BigDecimal("12000"))
                .withJan("vivat")
                .build();
        product0084.setCategoriesOpencart(parentsCategories);


        List<OptionDto> optionDtoFileList0084 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 100, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 999, 0, "", ""))));
        product0084.setOptionDtoList(optionDtoFileList0084);
        products.add(product0084);



        ProductOpencart product0085 = new ProductOpencart.Builder()
                .withModel("502-0085")
                .withTitle("Диван Сідней КОМБІ одинарний")
                .withSku("0085")
                .withPrice(new BigDecimal("3400"))
                .withItuaOriginalPrice(new BigDecimal("3400"))
                .withJan("vivat")
                .build();
        product0085.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0085 = new ProductDescriptionOpencart.Builder()
                .withName(product0085.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "90*84*90"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0085.getProductsDescriptionOpencart().add(descriptionOpencart0085);

        List<OptionDto> optionDtoFileList0085 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 50, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 100, 0, "", ""))));
        product0085.setOptionDtoList(optionDtoFileList0085);
        products.add(product0085);

        ProductOpencart product0086 = new ProductOpencart.Builder()
                .withModel("502-0086")
                .withTitle("Диван Сідней КОМБІ Подвійний")
                .withSku("0086")
                .withPrice(new BigDecimal("5550"))
                .withItuaOriginalPrice(new BigDecimal("5550"))
                .withJan("vivat")
                .build();
        product0086.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0086 = new ProductDescriptionOpencart.Builder()
                .withName(product0086.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "165*90*90"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0086.getProductsDescriptionOpencart().add(descriptionOpencart0086);

        List<OptionDto> optionDtoFileList0086 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 50, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 250, 0, "", ""))));
        product0086.setOptionDtoList(optionDtoFileList0086);
        products.add(product0086);




        ProductOpencart product0087 = new ProductOpencart.Builder()
                .withModel("502-0087")
                .withTitle("Диван Сідней КОМБІ Кутовий")
                .withSku("0087")
                .withPrice(new BigDecimal("3600"))
                .withItuaOriginalPrice(new BigDecimal("3600"))
                .withJan("vivat")
                .build();
        product0087.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0087 = new ProductDescriptionOpencart.Builder()
                .withName(product0087.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "90/90*125*90"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0087.getProductsDescriptionOpencart().add(descriptionOpencart0087);

        List<OptionDto> optionDtoFileList0087 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 50, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 399, 0, "", ""))));
        product0087.setOptionDtoList(optionDtoFileList0087);
        products.add(product0087);




        ProductOpencart product0088 = new ProductOpencart.Builder()
                .withModel("502-0088")
                .withTitle("Диван ЧИКАГО одинарний")
                .withSku("0088")
                .withPrice(new BigDecimal("2299"))
                .withItuaOriginalPrice(new BigDecimal("2299"))
                .withJan("vivat")
                .build();
        product0088.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0088 = new ProductDescriptionOpencart.Builder()
                .withName(product0088.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "74*77*66"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0088.getProductsDescriptionOpencart().add(descriptionOpencart0088);

        List<OptionDto> optionDtoFileList0088 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 11, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 101, 0, "", ""))));
        product0088.setOptionDtoList(optionDtoFileList0088);
        products.add(product0088);


        ProductOpencart product0089 = new ProductOpencart.Builder()
                .withModel("502-0089")
                .withTitle("Диван ЧИКАГО Подвійний")
                .withSku("0089")
                .withPrice(new BigDecimal("4200"))
                .withItuaOriginalPrice(new BigDecimal("4200"))
                .withJan("vivat")
                .build();
        product0089.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0089 = new ProductDescriptionOpencart.Builder()
                .withName(product0089.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "148*77*66"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0089.getProductsDescriptionOpencart().add(descriptionOpencart0089);

        List<OptionDto> optionDtoFileList0089 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 50, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 150, 0, "", ""))));
        product0089.setOptionDtoList(optionDtoFileList0089);
        products.add(product0089);





        ProductOpencart product0090 = new ProductOpencart.Builder()
                .withModel("502-0090")
                .withTitle("Диван ЧИКАГО Кутовий")
                .withSku("0090")
                .withPrice(new BigDecimal("3250"))
                .withItuaOriginalPrice(new BigDecimal("3250"))
                .withJan("vivat")
                .build();
        product0090.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0090 = new ProductDescriptionOpencart.Builder()
                .withName(product0090.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "95*66"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0090.getProductsDescriptionOpencart().add(descriptionOpencart0090);

        List<OptionDto> optionDtoFileList0090 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 30, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 100, 0, "", ""))));
        product0090.setOptionDtoList(optionDtoFileList0090);
        products.add(product0090);





        ProductOpencart product0091 = new ProductOpencart.Builder()
                .withModel("502-0091")
                .withTitle("Диван КУПЕР одинарний")
                .withSku("0091")
                .withPrice(new BigDecimal("1885"))
                .withItuaOriginalPrice(new BigDecimal("1885"))
                .withJan("vivat")
                .build();
        product0091.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0091 = new ProductDescriptionOpencart.Builder()
                .withName(product0091.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "Д73*Ш62*В78"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0091.getProductsDescriptionOpencart().add(descriptionOpencart0091);

        List<OptionDto> optionDtoFileList0091 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 15, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 210, 0, "", ""))));
        product0091.setOptionDtoList(optionDtoFileList0091);
        products.add(product0091);





        ProductOpencart product0092 = new ProductOpencart.Builder()
                .withModel("502-0092")
                .withTitle("Диван КУПЕР Подвійний")
                .withSku("0092")
                .withPrice(new BigDecimal("2850"))
                .withItuaOriginalPrice(new BigDecimal("2850"))
                .withJan("vivat")
                .build();
        product0092.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0092 = new ProductDescriptionOpencart.Builder()
                .withName(product0092.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "Д132*Ш62*В78"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0092.getProductsDescriptionOpencart().add(descriptionOpencart0092);

        List<OptionDto> optionDtoFileList0092 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 20, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 290, 0, "", ""))));
        product0092.setOptionDtoList(optionDtoFileList0092);
        products.add(product0092);


        ProductOpencart product0093 = new ProductOpencart.Builder()
                .withModel("502-0093")
                .withTitle("Диван МАЙАМІ")
                .withSku("0093")
                .withPrice(new BigDecimal("7800"))
                .withItuaOriginalPrice(new BigDecimal("7800"))
                .withJan("vivat")
                .build();
        product0093.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0093 = new ProductDescriptionOpencart.Builder()
                .withName(product0093.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "1020*800*178"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0093.getProductsDescriptionOpencart().add(descriptionOpencart0093);

        List<OptionDto> optionDtoFileList0093 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 100, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 200, 0, "", ""))));
        product0093.setOptionDtoList(optionDtoFileList0093);
        products.add(product0093);


        ProductOpencart product0094 = new ProductOpencart.Builder()
                .withModel("502-0094")
                .withTitle("Диван Олімпік")
                .withSku("0094")
                .withPrice(new BigDecimal("7500"))
                .withItuaOriginalPrice(new BigDecimal("7500"))
                .withJan("vivat")
                .build();
        product0094.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0094 = new ProductDescriptionOpencart.Builder()
                .withName(product0094.getTitle())
                .withDescription(getDescription("Габарити:\n" +
                        "Ш200*В620*Г800 посадочное место 150*620"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0094.getProductsDescriptionOpencart().add(descriptionOpencart0094);

        List<OptionDto> optionDtoFileList0094 = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 120, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 350, 0, "", ""))));
        product0094.setOptionDtoList(optionDtoFileList0094);
        products.add(product0094);


        return products;
    }

    public List<ProductOpencart> getKrisla(SupplierApp supplierApp) {
        CategoryOpencart mainSupplierCategory = supplierApp.getMainSupplierCategory();
        List<CategoryOpencart> parentsCategories = getParentsCategories(mainSupplierCategory, supplierApp.getCategoryOpencartDB());
        List<ProductOpencart> products = new ArrayList<>();


        ProductOpencart product0020 = new ProductOpencart.Builder()
                .withModel("502-0020")
                .withTitle("Крісло АТЛАНТИК")
                .withSku("0020")
                .withPrice(new BigDecimal("2400"))
                .withItuaOriginalPrice(new BigDecimal("2400"))
                .withJan("vivat")
                .build();
        product0020.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0020 = new ProductDescriptionOpencart.Builder()
                .withName(product0020.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n\n" +
                        "Розміри упаковки, об'єм, вага:\n" +
                        "86х67х40, 0,21 м3, 18,00 кг.\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0020.getProductsDescriptionOpencart().add(descriptionOpencart0020);

        List<AttributeWrapper> attributeWrappers0020 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null),
                new AttributeWrapper("Розмір коробки", "86х67х40", null));
        product0020.setAttributesWrapper(attributeWrappers0020);

        List<OptionDto> optionDtoFileList = Arrays.asList(
                new OptionDto("ШКІРЗАМ", "ШКІРЗАМ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 30, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 145, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 50, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 800, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 200, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 950, 0, "", ""))));
        product0020.setOptionDtoList(optionDtoFileList);

        products.add(product0020);


        ProductOpencart product0021 = new ProductOpencart.Builder()
                .withModel("502-0021")
                .withTitle("Крісло АТЛАС/АТЛЕТИК Тканина")
                .withSku("0021")
                .withPrice(new BigDecimal("2110"))
                .withItuaOriginalPrice(new BigDecimal("2110"))
                .withJan("vivat")
                .build();
        product0021.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0021 = new ProductDescriptionOpencart.Builder()
                .withName(product0021.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n\n" +
                        "Розміри упаковки, об'єм, вага:\n" +
                        "86х67х40, 0,21 м3, 18,00 кг.\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0021.getProductsDescriptionOpencart().add(descriptionOpencart0021);

        List<AttributeWrapper> attributeWrappers0021 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null),
                new AttributeWrapper("Розмір коробки", "86х67х40", null));
        product0021.setAttributesWrapper(attributeWrappers0021);

        List<OptionDto> optionDtoFileList0021 = Arrays.asList(
                new OptionDto("Тканина", "Тканина", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("Фінт (шенніл)", "Фінт (шенніл)", true, 0, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 80, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 830, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 230, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 980, 0, "", ""))));
        product0021.setOptionDtoList(optionDtoFileList0021);
        products.add(product0021);

        ProductOpencart product0022 = new ProductOpencart.Builder()
                .withModel("502-0022")
                .withTitle("Крісло АТЛАС/АТЛЕТИК Шкірзам")
                .withSku("0022")
                .withPrice(new BigDecimal("2140"))
                .withItuaOriginalPrice(new BigDecimal("2140"))
                .withJan("vivat")
                .build();
        product0022.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0022 = new ProductDescriptionOpencart.Builder()
                .withName(product0022.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n\n" +
                        "Розміри упаковки, об'єм, вага:\n" +
                        "86х67х40, 0,21 м3, 18,00 кг.\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0022.getProductsDescriptionOpencart().add(descriptionOpencart0022);

        List<AttributeWrapper> attributeWrappers0022 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null),
                new AttributeWrapper("Розмір коробки", "86х67х40", null));
        product0022.setAttributesWrapper(attributeWrappers0022);

        List<OptionDto> optionDtoFileList0022 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 20, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 150, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 0, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 720, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 120, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 870, 0, "", ""))));
        product0022.setOptionDtoList(optionDtoFileList0022);
        products.add(product0022);


        ProductOpencart product0023 = new ProductOpencart.Builder()
                .withModel("502-0023")
                .withTitle("Крісло CHESS")
                .withSku("0023")
                .withPrice(new BigDecimal("2400"))
                .withItuaOriginalPrice(new BigDecimal("2400"))
                .withJan("vivat")
                .build();
        product0023.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0023 = new ProductDescriptionOpencart.Builder()
                .withName(product0023.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n\n" +
                        "Розміри упаковки, об'єм, вага:\n" +
                        "86х67х40, 0,21 м3, 18,00 кг.\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0023.getProductsDescriptionOpencart().add(descriptionOpencart0023);

        List<AttributeWrapper> attributeWrappers0023 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null),
                new AttributeWrapper("Розмір коробки", "86х67х40", null));
        product0023.setAttributesWrapper(attributeWrappers0023);

        List<OptionDto> optionDtoFileList0023 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 30, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 145, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 50, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 800, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 200, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 950, 0, "", ""))));
        product0023.setOptionDtoList(optionDtoFileList0023);
        products.add(product0023);


        ProductOpencart product0024 = new ProductOpencart.Builder()
                .withModel("502-0024")
                .withTitle("Крісло BRIGHT")
                .withSku("0024")
                .withPrice(new BigDecimal("2400"))
                .withItuaOriginalPrice(new BigDecimal("2400"))
                .withJan("vivat")
                .build();
        product0024.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0024 = new ProductDescriptionOpencart.Builder()
                .withName(product0024.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n\n" +
                        "Розміри упаковки, об'єм, вага:\n" +
                        "86х67х40, 0,21 м3, 18,00 кг.\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0024.getProductsDescriptionOpencart().add(descriptionOpencart0024);

        List<AttributeWrapper> attributeWrappers0024 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null),
                new AttributeWrapper("Розмір коробки", "86х67х40", null));
        product0024.setAttributesWrapper(attributeWrappers0024);

        List<OptionDto> optionDtoFileList0024 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 30, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 145, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 50, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 800, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 200, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 950, 0, "", ""))));
        product0024.setOptionDtoList(optionDtoFileList0024);
        products.add(product0024);


        ProductOpencart product0025 = new ProductOpencart.Builder()
                .withModel("502-0025")
                .withTitle("Крісло ГЕРКУЛЕС")
                .withSku("0025")
                .withPrice(new BigDecimal("2540"))
                .withItuaOriginalPrice(new BigDecimal("2540"))
                .withJan("vivat")
                .build();
        product0025.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0025 = new ProductDescriptionOpencart.Builder()
                .withName(product0025.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0025.getProductsDescriptionOpencart().add(descriptionOpencart0025);

        List<AttributeWrapper> attributeWrappers0025 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null));
        product0025.setAttributesWrapper(attributeWrappers0025);

        List<OptionDto> optionDtoFileList0025 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 10, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 140, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 0, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 750, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 150, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 900, 0, "", ""))));
        product0025.setOptionDtoList(optionDtoFileList0025);
        products.add(product0025);


        ProductOpencart product0026 = new ProductOpencart.Builder()
                .withModel("502-0026")
                .withTitle("Крісло ПАССАТ")
                .withSku("0026")
                .withPrice(new BigDecimal("2400"))
                .withItuaOriginalPrice(new BigDecimal("2400"))
                .withJan("vivat")
                .build();
        product0026.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0026 = new ProductDescriptionOpencart.Builder()
                .withName(product0026.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n\n" +
                        "Розміри упаковки, об'єм, вага:\n" +
                        "86х67х40, 0,21 м3, 18,00 кг.\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0026.getProductsDescriptionOpencart().add(descriptionOpencart0026);

        List<AttributeWrapper> attributeWrappers0026 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null),
                new AttributeWrapper("Розмір коробки", "86х67х40", null));
        product0026.setAttributesWrapper(attributeWrappers0026);

        List<OptionDto> optionDtoFileList0026 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 30, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 145, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 50, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 800, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 200, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 950, 0, "", ""))));
        product0026.setOptionDtoList(optionDtoFileList0026);
        products.add(product0026);


        ProductOpencart product0027 = new ProductOpencart.Builder()
                .withModel("502-0027")
                .withTitle("Крісло СКАЙ Шкірзам")
                .withSku("0027")
                .withPrice(new BigDecimal("2750"))
                .withItuaOriginalPrice(new BigDecimal("2750"))
                .withJan("vivat")
                .build();
        product0027.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0027 = new ProductDescriptionOpencart.Builder()
                .withName(product0027.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0027.getProductsDescriptionOpencart().add(descriptionOpencart0027);

        List<AttributeWrapper> attributeWrappers0027 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null));
        product0027.setAttributesWrapper(attributeWrappers0027);

        List<OptionDto> optionDtoFileList0027 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 20, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 145, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 0, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 750, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 150, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 900, 0, "", ""))));
        product0027.setOptionDtoList(optionDtoFileList0027);
        products.add(product0027);


        ProductOpencart product0028 = new ProductOpencart.Builder()
                .withModel("502-0028")
                .withTitle("Крісло СКАЙ Тканина")
                .withSku("0028")
                .withPrice(new BigDecimal("2840"))
                .withItuaOriginalPrice(new BigDecimal("2840"))
                .withJan("vivat")
                .build();
        product0027.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0028 = new ProductDescriptionOpencart.Builder()
                .withName(product0028.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0028.getProductsDescriptionOpencart().add(descriptionOpencart0028);

        List<AttributeWrapper> attributeWrappers0028 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null));
        product0028.setAttributesWrapper(attributeWrappers0028);

        List<OptionDto> optionDtoFileList0028 = Arrays.asList(
                new OptionDto("Тканина", "Тканина", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("Фінт (шенніл)", "Фінт (шенніл)", true, 0, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 0, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 750, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 150, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 900, 0, "", ""))));
        product0028.setOptionDtoList(optionDtoFileList0028);
        products.add(product0028);


        ProductOpencart product0029 = new ProductOpencart.Builder()
                .withModel("502-0029")
                .withTitle("Крісло Скай комбі")
                .withSku("0029")
                .withPrice(new BigDecimal("2800"))
                .withItuaOriginalPrice(new BigDecimal("2800"))
                .withJan("vivat")
                .build();
        product0029.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0029 = new ProductDescriptionOpencart.Builder()
                .withName(product0029.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0029.getProductsDescriptionOpencart().add(descriptionOpencart0029);

        List<AttributeWrapper> attributeWrappers0029 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null));
        product0029.setAttributesWrapper(attributeWrappers0029);

        List<OptionDto> optionDtoFileList0029 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 20, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 210, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 0, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 750, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 150, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 900, 0, "", ""))));
        product0029.setOptionDtoList(optionDtoFileList0029);
        products.add(product0029);

        //      TODO cont 30
        ProductOpencart product0030 = new ProductOpencart.Builder()
                .withModel("502-0030")
                .withTitle("Крісло БОРА")
                .withSku("0030")
                .withPrice(new BigDecimal("2800"))
                .withItuaOriginalPrice(new BigDecimal("2800"))
                .withJan("vivat")
                .build();
        product0030.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0030 = new ProductDescriptionOpencart.Builder()
                .withName(product0030.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0030.getProductsDescriptionOpencart().add(descriptionOpencart0030);

        List<AttributeWrapper> attributeWrappers0030 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null));
        product0030.setAttributesWrapper(attributeWrappers0030);

        List<OptionDto> optionDtoFileList0030 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 20, 0, "", ""),
                        new OptionValuesDto("МАДРАС", "МАДРАС", false, 210, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 0, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 750, 0, "", ""))),

                new OptionDto("АНІФІКС", "АНІФІКС", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 150, 0, "", ""),
                        new OptionValuesDto("ХРОМ", "ХРОМ", false, 900, 0, "", ""))));
        product0030.setOptionDtoList(optionDtoFileList0030);
        products.add(product0030);


        //      TODO cont 31
        ProductOpencart product0031 = new ProductOpencart.Builder()
                .withModel("502-0031")
                .withTitle("Крісло ДЖЕТА")
                .withSku("0031")
                .withPrice(new BigDecimal("1920"))
                .withItuaOriginalPrice(new BigDecimal("1920"))
                .withJan("vivat")
                .build();
        product0031.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0031 = new ProductDescriptionOpencart.Builder()
                .withName(product0031.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53\n\n" +
                        "Розміри упаковки, об'єм, вага:\n" +
                        "86х67х40, 0,21 м3, 18,00 кг.\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0031.getProductsDescriptionOpencart().add(descriptionOpencart0031);

        List<AttributeWrapper> attributeWrappers0031 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null),
                new AttributeWrapper("Розмір коробки", "86х67х40", null));
        product0031.setAttributesWrapper(attributeWrappers0031);

        List<OptionDto> optionDtoFileList0031 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 20, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 140, 0, "", ""))),

                new OptionDto("ПІАСТРА", "ПІАСТРА", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 70, 0, "", ""))));
        product0031.setOptionDtoList(optionDtoFileList0031);
        products.add(product0031);


        //      TODO cont 32
        ProductOpencart product0032 = new ProductOpencart.Builder()
                .withModel("502-0032")
                .withTitle("Крісло ПРАЙМ")
                .withSku("0032")
                .withPrice(new BigDecimal("1970"))
                .withItuaOriginalPrice(new BigDecimal("1970"))
                .withJan("vivat")
                .build();
        product0032.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0032 = new ProductDescriptionOpencart.Builder()
                .withName(product0032.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0032.getProductsDescriptionOpencart().add(descriptionOpencart0032);

        List<AttributeWrapper> attributeWrappers0032 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null));
        product0032.setAttributesWrapper(attributeWrappers0032);

        List<OptionDto> optionDtoFileList0032 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 20, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 70, 0, "", ""))),

                new OptionDto("ПІАСТРА", "ПІАСТРА", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 0, 0, "", ""))));
        product0032.setOptionDtoList(optionDtoFileList0032);
        products.add(product0032);


        //      TODO cont 33
        ProductOpencart product0033 = new ProductOpencart.Builder()
                .withModel("502-0033")
                .withTitle("Крісло ПРАЙМ air (сітка)")
                .withSku("0033")
                .withPrice(new BigDecimal("2090"))
                .withItuaOriginalPrice(new BigDecimal("2090"))
                .withJan("vivat")
                .build();
        product0033.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0033 = new ProductDescriptionOpencart.Builder()
                .withName(product0033.getTitle())
                .withDescription(getDescription("Габарити виробу:\n" +
                        "Висота стільця - 119-125\n" +
                        "Ширина стільця - 51\n" +
                        "Основа стільця - Ø64\n" +
                        "Висота сидіння - 47-53"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0033.getProductsDescriptionOpencart().add(descriptionOpencart0033);

        List<AttributeWrapper> attributeWrappers0033 = Arrays.asList(
                new AttributeWrapper("Розміри (ШхГхВ) см", "51x64x119-125", null));
        product0033.setAttributesWrapper(attributeWrappers0033);

        List<OptionDto> optionDtoFileList0033 = Arrays.asList(
                new OptionDto("Шкірзам", "Шкірзам", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ФЛАЙ", "ФЛАЙ", true, 0, 0, "", ""),
                        new OptionValuesDto("ZEUS DELUXE", "ZEUS DELUXE", false, 140, 0, "", ""))),

                new OptionDto("ТІЛТ", "ТІЛТ", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("За замовчуванням", "За замовчуванням", true, 0, 0, "", ""),
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", false, 20, 0, "", ""))),

                new OptionDto("ПІАСТРА", "ПІАСТРА", 0, OCConstant.OPTION_TYPE_RADIO, Arrays.asList(
                        new OptionValuesDto("ПЛАСТИК", "ПЛАСТИК", true, 0, 0, "", ""))));
        product0033.setOptionDtoList(optionDtoFileList0033);
        products.add(product0033);

        return products;
    }


    public List<ProductOpencart> getKrislaImport(SupplierApp supplierApp) {
        CategoryOpencart mainSupplierCategory = supplierApp.getMainSupplierCategory();
        List<CategoryOpencart> parentsCategories = getParentsCategories(mainSupplierCategory, supplierApp.getCategoryOpencartDB());
        List<ProductOpencart> products = new ArrayList<>();

        ProductOpencart product0001 = new ProductOpencart.Builder()
                .withModel("502-0001")
                .withTitle("Крісло Класик")
                .withSku("0001")
                .withPrice(new BigDecimal("6214"))
                .withItuaOriginalPrice(new BigDecimal("6214"))
                .withJan("vivat")
                .build();
        product0001.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0001 = new ProductDescriptionOpencart.Builder()
                .withName(product0001.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Хром. Додаткові подушки на спинці та сидінні.\n" +
                        "МЕХАНІЗМ\n" +
                        "D-Tilt\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0001.getProductsDescriptionOpencart().add(descriptionOpencart0001);

        List<AttributeWrapper> attributeWrappers0001 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU", null),
                new AttributeWrapper("Підлокітники", "Хром+PU накладки", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "320мм, хром", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "57х64х109-114", null),
                new AttributeWrapper("Розмір коробки", "90х57х59 см (2 шт)", null));
        product0001.setAttributesWrapper(attributeWrappers0001);

        products.add(product0001);


        ProductOpencart product0002 = new ProductOpencart.Builder()
                .withModel("502-0002")
                .withTitle("Крісло Смарт")
                .withSku("0002")
                .withPrice(new BigDecimal("6214"))
                .withItuaOriginalPrice(new BigDecimal("6214"))
                .withJan("vivat")
                .build();
        product0002.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0002 = new ProductDescriptionOpencart.Builder()
                .withName(product0002.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Хром. Додаткові подушки на спинці та сидінні.\n" +
                        "МЕХАНІЗМ\n" +
                        "D-Tilt\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0002.getProductsDescriptionOpencart().add(descriptionOpencart0002);

        List<AttributeWrapper> attributeWrappers0002 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU", null),
                new AttributeWrapper("Підлокітники", "Хром+PU накладки", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "320мм, хром", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "57х64х109-114", null),
                new AttributeWrapper("Розмір коробки", "90х57х59 см (2 шт)", null));
        product0002.setAttributesWrapper(attributeWrappers0002);

        products.add(product0002);


        ProductOpencart product0003 = new ProductOpencart.Builder()
                .withModel("502-0003")
                .withTitle("Крісло Аеро")
                .withSku("0003")
                .withPrice(new BigDecimal("5285"))
                .withItuaOriginalPrice(new BigDecimal("5285"))
                .withJan("vivat")
                .build();
        product0003.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0003 = new ProductDescriptionOpencart.Builder()
                .withName(product0003.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Хром.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0003.getProductsDescriptionOpencart().add(descriptionOpencart0003);

        List<AttributeWrapper> attributeWrappers0003 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "сітка", null),
                new AttributeWrapper("Підлокітники", "Хром+PU накладки", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "320мм, хром", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "57х64х109-114", null),
                new AttributeWrapper("Розмір коробки", "90х57х59 см (2 шт)", null));
        product0003.setAttributesWrapper(attributeWrappers0003);

        products.add(product0003);


        ProductOpencart product0004 = new ProductOpencart.Builder()
                .withModel("502-0004")
                .withTitle("Крісло Стартап")
                .withSku("0004")
                .withPrice(new BigDecimal("5434"))
                .withItuaOriginalPrice(new BigDecimal("5434"))
                .withJan("vivat")
                .build();
        product0004.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0004 = new ProductDescriptionOpencart.Builder()
                .withName(product0004.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Хром.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt\n"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0004.getProductsDescriptionOpencart().add(descriptionOpencart0004);

        List<AttributeWrapper> attributeWrappers0004 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU", null),
                new AttributeWrapper("Підлокітники", "Хром+PU накладки", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "320мм, хром", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "57х64х109-114", null),
                new AttributeWrapper("Розмір коробки", "90х57х59 см (2 шт)", null));
        product0004.setAttributesWrapper(attributeWrappers0004);

        products.add(product0004);


        ProductOpencart product0005 = new ProductOpencart.Builder()
                .withModel("502-0005")
                .withTitle("Крісло Стартап CF ОТ5003А")
                .withSku("0005")
                .withPrice(new BigDecimal("4862"))
                .withItuaOriginalPrice(new BigDecimal("4862"))
                .withJan("vivat")
                .build();
        product0005.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0005 = new ProductDescriptionOpencart.Builder()
                .withName(product0005.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Хром."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0005.getProductsDescriptionOpencart().add(descriptionOpencart0005);

        List<AttributeWrapper> attributeWrappers0005 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU", null),
                new AttributeWrapper("Підлокітники", "Хром+PU накладки", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "Гол. 580 Ши. 560. Висота 920", null),
                new AttributeWrapper("Розмір коробки", "79х42х49", null));
        product0005.setAttributesWrapper(attributeWrappers0005);

        products.add(product0005);


        ProductOpencart product0006 = new ProductOpencart.Builder()
                .withModel("502-0006")
                .withTitle("Крісло Елегія")
                .withSku("0006")
                .withPrice(new BigDecimal("5948"))
                .withItuaOriginalPrice(new BigDecimal("5948"))
                .withJan("vivat")
                .build();
        product0006.setCategoriesOpencart(parentsCategories);

        ProductDescriptionOpencart descriptionOpencart0006 = new ProductDescriptionOpencart.Builder()
                .withName(product0006.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Пластикові підлокітники із м'якими накладками. Хромована хрестовина із гумовими вставками."))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0006.getProductsDescriptionOpencart().add(descriptionOpencart0006);

        List<AttributeWrapper> attributeWrappers0006 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU", null),
                new AttributeWrapper("Підлокітники", "metal+PU sponge", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "100mm chrome gaslift", null),
                new AttributeWrapper("База", "350мм, nylon", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "67х71х114-124", null),
                new AttributeWrapper("Розмір коробки", "79х30х65 см (1 шт)", null));
        product0006.setAttributesWrapper(attributeWrappers0006);

        products.add(product0006);


        ProductOpencart product0007 = new ProductOpencart.Builder()
                .withModel("502-0007")
                .withTitle("Крісло Капітал")
                .withSku("0007")
                .withPrice(new BigDecimal("6227"))
                .withItuaOriginalPrice(new BigDecimal("6227"))
                .withJan("vivat")
                .build();
        product0007.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0007 = new ProductDescriptionOpencart.Builder()
                .withName(product0007.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Пластикові підлокітники із м'якими накладками. Хромована хрестовина із гумовими вставками.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0007.getProductsDescriptionOpencart().add(descriptionOpencart0007);
        List<AttributeWrapper> attributeWrappers0007 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU", null),
                new AttributeWrapper("Підлокітники", "PP+PU sponge", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "350мм, хром", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "60x72x109-117", null),
                new AttributeWrapper("Розмір коробки", "75x35x65 см (1 шт)", null));
        product0007.setAttributesWrapper(attributeWrappers0007);
        products.add(product0007);


        ProductOpencart product0008 = new ProductOpencart.Builder()
                .withModel("502-0008")
                .withTitle("Крісло Успіх")
                .withSku("0008")
                .withPrice(new BigDecimal("4147"))
                .withItuaOriginalPrice(new BigDecimal("4147"))
                .withJan("vivat")
                .build();
        product0008.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0008 = new ProductDescriptionOpencart.Builder()
                .withName(product0008.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Пластикові підлокітники. Хромована хрестовина.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0008.getProductsDescriptionOpencart().add(descriptionOpencart0008);
        List<AttributeWrapper> attributeWrappers0008 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU", null),
                new AttributeWrapper("Підлокітники", "PP", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "350мм, хром", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "60x68x107-115", null),
                new AttributeWrapper("Розмір коробки", "75x35x65 см (1 шт)", null));
        product0008.setAttributesWrapper(attributeWrappers0008);
        products.add(product0008);


        ProductOpencart product0009 = new ProductOpencart.Builder()
                .withModel("502-0009")
                .withTitle("Крісло СОФІЯ OT-8127")
                .withSku("0009")
                .withPrice(new BigDecimal("6169"))
                .withItuaOriginalPrice(new BigDecimal("6169"))
                .withJan("vivat")
                .build();
        product0009.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0009 = new ProductDescriptionOpencart.Builder()
                .withName(product0009.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Пластикові підлокітники із м'якими накладками. Хромована хрестовина.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0009.getProductsDescriptionOpencart().add(descriptionOpencart0009);
        List<AttributeWrapper> attributeWrappers0009 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU", null),
                new AttributeWrapper("Підлокітники", "PP+PU sponge", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "300мм, хром", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "60x57x92-102", null),
                new AttributeWrapper("Розмір коробки", "58x22x55 см (1 шт)", null));
        product0009.setAttributesWrapper(attributeWrappers0009);
        products.add(product0009);


        ProductOpencart product0010 = new ProductOpencart.Builder()
                .withModel("502-0010")
                .withTitle("Крісло R - 55")
                .withSku("0010")
                .withPrice(new BigDecimal("7631"))
                .withItuaOriginalPrice(new BigDecimal("7631"))
                .withJan("vivat")
                .build();
        product0010.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0010 = new ProductDescriptionOpencart.Builder()
                .withName(product0010.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Дуже зручний підголовник зі знімною подушечкою. Поперековий упор. Оригінальна ергономічна форма спинки та підлокітників. Розкладається до 180 градусів.\n" +
                        "МЕХАНІЗМ\n" +
                        "D-Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0010.getProductsDescriptionOpencart().add(descriptionOpencart0010);
        List<AttributeWrapper> attributeWrappers0010 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "mesh (сітка)", null),
                new AttributeWrapper("Підлокітники", "PP", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "300мм, хром", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) см", "60x57x92-102", null),
                new AttributeWrapper("Розмір коробки", "58x22x55 см (1 шт)", null));
        product0010.setAttributesWrapper(attributeWrappers0010);
        products.add(product0010);


        ProductOpencart product0011 = new ProductOpencart.Builder()
                .withModel("502-0011")
                .withTitle("Крісло R - OT-R90")
                .withSku("0011")
                .withPrice(new BigDecimal("5382"))
                .withItuaOriginalPrice(new BigDecimal("5382"))
                .withJan("vivat")
                .build();
        product0011.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0011 = new ProductDescriptionOpencart.Builder()
                .withName(product0011.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Дуже зручний підголовник зі знімною подушечкою. Поперековий упор. Оригінальна ергономічна форма спинки та підлокітників. Розкладається до 180 градусів.\n" +
                        "МЕХАНІЗМ\n" +
                        "D-Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0011.getProductsDescriptionOpencart().add(descriptionOpencart0011);
        List<AttributeWrapper> attributeWrappers0011 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "mesh (сітка)", null),
                new AttributeWrapper("Підлокітники", "PP", null),
                new AttributeWrapper("Механізм", "Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "300мм, хром", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розмір коробки", " 87х71х42 см (1 шт)", null));
        product0011.setAttributesWrapper(attributeWrappers0011);
        products.add(product0011);


        ProductOpencart product0012 = new ProductOpencart.Builder()
                .withModel("502-0012")
                .withTitle("Крісло OT-R09")
                .withSku("0012")
                .withPrice(new BigDecimal("8697"))
                .withItuaOriginalPrice(new BigDecimal("8697"))
                .withJan("vivat")
                .build();
        product0012.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0012 = new ProductDescriptionOpencart.Builder()
                .withName(product0012.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Дуже зручний підголовник зі знімною подушечкою для фіксації шиї. Оригінальна ергономічна форма спинки та підлокітників. Розкладається до 180 градусів.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0012.getProductsDescriptionOpencart().add(descriptionOpencart0012);
        List<AttributeWrapper> attributeWrappers0012 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU BEIGE+BLACK", null),
                new AttributeWrapper("Підлокітники", "PVC 3D", null),
                new AttributeWrapper("Механізм", "butterfly", null),
                new AttributeWrapper("Газліфт", "BIFMA", null),
                new AttributeWrapper("База", "350мм, метал", null),
                new AttributeWrapper("Ролики", "nylon caster", null),
                new AttributeWrapper("Розмір коробки", "Розмір коробки: 84х70х34 см (1 шт)", null));
        product0012.setAttributesWrapper(attributeWrappers0012);
        products.add(product0012);


        ProductOpencart product0013 = new ProductOpencart.Builder()
                .withModel("502-0013")
                .withTitle("Крісло OT-R113")
                .withSku("0013")
                .withPrice(new BigDecimal("5122"))
                .withItuaOriginalPrice(new BigDecimal("5122"))
                .withJan("vivat")
                .build();
        product0013.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0013 = new ProductDescriptionOpencart.Builder()
                .withName(product0013.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Дуже зручний підголовник зі знімною подушечкою. Поперековий упор. Оригінальна ергономічна форма спинки та підлокітників. Розкладається до 180 градусів.\n" +
                        "МЕХАНІЗМ\n" +
                        "D-Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0013.getProductsDescriptionOpencart().add(descriptionOpencart0013);
        List<AttributeWrapper> attributeWrappers0013 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU/ подушка велюр", null),
                new AttributeWrapper("Підлокітники", "PVC 2D", null),
                new AttributeWrapper("Механізм", "D-Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "350мм, nylon", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розмір коробки", "84х65х34.5 см (1 шт)+", null));
        product0013.setAttributesWrapper(attributeWrappers0013);
        products.add(product0013);


        ProductOpencart product0014 = new ProductOpencart.Builder()
                .withModel("502-0014")
                .withTitle("Крісло OT-R98")
                .withSku("0014")
                .withPrice(new BigDecimal("7514"))
                .withItuaOriginalPrice(new BigDecimal("7514"))
                .withJan("vivat")
                .build();
        product0014.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0014 = new ProductDescriptionOpencart.Builder()
                .withName(product0014.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Дуже зручний підголівник. Поперековий упор. Оригінальна ергономічна форма спинки та підлокітників.\n" +
                        "МЕХАНІЗМ\n" +
                        "D-Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0014.getProductsDescriptionOpencart().add(descriptionOpencart0014);
        List<AttributeWrapper> attributeWrappers0014 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU", null),
                new AttributeWrapper("Підлокітники", "PVC 2D", null),
                new AttributeWrapper("Механізм", "D-Tilt", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "350мм, nylon", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) див", "72х72х125-133", null),
                new AttributeWrapper("Розмір коробки", "90х70х31 см (1 шт)", null));
        product0014.setAttributesWrapper(attributeWrappers0014);
        products.add(product0014);


        ProductOpencart product0015 = new ProductOpencart.Builder()
                .withModel("502-0015")
                .withTitle("Крісло OT-B23 RGB BLACK+WHITE")
                .withSku("0015")
                .withPrice(new BigDecimal("8502"))
                .withItuaOriginalPrice(new BigDecimal("8502"))
                .withJan("vivat")
                .build();
        product0015.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0015 = new ProductDescriptionOpencart.Builder()
                .withName(product0015.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Дуже зручний підголовник зі знімною подушечкою. Поперековий упор. Оригінальна ергономічна форма спинки та підлокітників. Розкладається до 180 градусів.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0015.getProductsDescriptionOpencart().add(descriptionOpencart0015);
        List<AttributeWrapper> attributeWrappers0015 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU світиться", null),
                new AttributeWrapper("Підлокітники", "PVC 2D", null),
                new AttributeWrapper("Механізм", "Tilt , регулює спинку в будь-якому положенні", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "350мм, nylon", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) див", "72х72х125-133", null),
                new AttributeWrapper("Розмір коробки", "83х65х33 см (1 шт)", null));
        product0015.setAttributesWrapper(attributeWrappers0015);
        products.add(product0015);


        ProductOpencart product0016 = new ProductOpencart.Builder()
                .withModel("502-0016")
                .withTitle("Крісло OT-B23 BLACK+Yellow")
                .withSku("0016")
                .withPrice(new BigDecimal("6897"))
                .withItuaOriginalPrice(new BigDecimal("6897"))
                .withJan("vivat")
                .build();
        product0016.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0016 = new ProductDescriptionOpencart.Builder()
                .withName(product0016.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Дуже зручний підголовник зі знімною подушечкою. Поперековий упор. Оригінальна ергономічна форма спинки та підлокітників. Розкладається до 180 градусів.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0016.getProductsDescriptionOpencart().add(descriptionOpencart0016);
        List<AttributeWrapper> attributeWrappers0016 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU Чорно/білий", null),
                new AttributeWrapper("Підлокітники", "PVC 2D", null),
                new AttributeWrapper("Механізм", "Tilt , регулює спинку в будь-якому положенні", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "350мм, nylon", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) див", "72х72х125-133", null),
                new AttributeWrapper("Розмір коробки", "83х65х33 см (1 шт)", null));
        product0016.setAttributesWrapper(attributeWrappers0016);
        products.add(product0016);


        ProductOpencart product0017 = new ProductOpencart.Builder()
                .withModel("502-0017")
                .withTitle("Крісло OT-B23")
                .withSku("0017")
                .withPrice(new BigDecimal("6897"))
                .withItuaOriginalPrice(new BigDecimal("6897"))
                .withJan("vivat")
                .build();
        product0017.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0017 = new ProductDescriptionOpencart.Builder()
                .withName(product0017.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Дуже зручний підголовник зі знімною подушечкою. Поперековий упор. Оригінальна ергономічна форма спинки та підлокітників. Розкладається до 180 градусів.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0017.getProductsDescriptionOpencart().add(descriptionOpencart0017);
        List<AttributeWrapper> attributeWrappers0017 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU Чорно/білий", null),
                new AttributeWrapper("Підлокітники", "PVC 2D", null),
                new AttributeWrapper("Механізм", "Tilt , регулює спинку в будь-якому положенні", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "350мм, nylon", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) див", "72х72х125-133", null),
                new AttributeWrapper("Розмір коробки", "83х65х33 см (1 шт)", null));
        product0017.setAttributesWrapper(attributeWrappers0017);
        products.add(product0017);


        ProductOpencart product0018 = new ProductOpencart.Builder()
                .withModel("502-0018")
                .withTitle("Крісло R-99")
                .withSku("0018")
                .withPrice(new BigDecimal("5967"))
                .withItuaOriginalPrice(new BigDecimal("5967"))
                .withJan("vivat")
                .build();
        product0018.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0018 = new ProductDescriptionOpencart.Builder()
                .withName(product0018.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Дуже зручний підголовник зі знімною подушечкою. Поперековий упор. Оригінальна ергономічна форма спинки та підлокітників. Розкладається до 180 градусів.\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0018.getProductsDescriptionOpencart().add(descriptionOpencart0018);
        List<AttributeWrapper> attributeWrappers0018 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU Чорно/білий", null),
                new AttributeWrapper("Підлокітники", "PVC 2D", null),
                new AttributeWrapper("Механізм", "Tilt , регулює спинку в будь-якому положенні", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "350мм, nylon", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) див", "72х72х125-133", null),
                new AttributeWrapper("Розмір коробки", "81х28х52.5 см (1 шт)   ", null));
        product0018.setAttributesWrapper(attributeWrappers0018);
        products.add(product0018);


        ProductOpencart product0019 = new ProductOpencart.Builder()
                .withModel("502-0019")
                .withTitle("Крісло OT-2219")
                .withSku("0019")
                .withPrice(new BigDecimal("3367"))
                .withItuaOriginalPrice(new BigDecimal("3367"))
                .withJan("vivat")
                .build();
        product0019.setCategoriesOpencart(parentsCategories);
        ProductDescriptionOpencart descriptionOpencart0019 = new ProductDescriptionOpencart.Builder()
                .withName(product0019.getTitle())
                .withDescription(getDescription("КОМПЛЕКТАЦІЯ\n" +
                        "Сітка + Хром\n" +
                        "МЕХАНІЗМ\n" +
                        "Tilt"))
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        product0019.getProductsDescriptionOpencart().add(descriptionOpencart0019);
        List<AttributeWrapper> attributeWrappers0019 = Arrays.asList(
                new AttributeWrapper("Матеріал оббивки", "PU Чорно/білий", null),
                new AttributeWrapper("Підлокітники", "PVC 2D", null),
                new AttributeWrapper("Механізм", "Tilt , регулює спинку в будь-якому положенні", null),
                new AttributeWrapper("Газліфт", "class2", null),
                new AttributeWrapper("База", "350мм, nylon", null),
                new AttributeWrapper("Ролики", "nylon", null),
                new AttributeWrapper("Розміри (ШхГхВ) див", "72х72х125-133", null),
                new AttributeWrapper("Розмір коробки", "83х65х33 см (1 шт)", null));
        product0019.setAttributesWrapper(attributeWrappers0019);
        products.add(product0019);

        return products;
    }

}
