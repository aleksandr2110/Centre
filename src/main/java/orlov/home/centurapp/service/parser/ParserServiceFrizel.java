package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
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
import orlov.home.centurapp.service.appservice.ImageService;
import orlov.home.centurapp.service.appservice.ScraperDataUpdateService;
import orlov.home.centurapp.service.appservice.UpdateDataService;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;
import orlov.home.centurapp.util.AppConstant;
import orlov.home.centurapp.util.OCConstant;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ParserServiceFrizel extends ParserServiceAbstract {
    private final String SUPPLIER_NAME = "frizel";
    private final String SUPPLIER_URL = "https://frizel.com.ua/";
    private final String DISPLAY_NAME = "19 - FRIZEL";
    private final String URL_PART_PAGE = "?page=";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;
    private final UpdateDataService updateDataService;
    private final ImageService imageService;

    public ParserServiceFrizel(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService, ImageService imageService) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService;
        this.opencartDaoService = opencartDaoService;
        this.translateService = translateService;
        this.fileService = fileService;
        this.updateDataService = updateDataService;
        this.imageService = imageService;
    }


    @Override
    public void doProcess() {
        try {

            OrderProcessApp orderProcessApp = new OrderProcessApp();
            Timestamp start = new Timestamp(Calendar.getInstance().getTime().getTime());

            SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);


            List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);


            List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);

            log.info("Count products: {}", productsFromSite.size());

            List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);

            OpencartDto opencartInfo = getOpencartInfo(fullProductsData, supplierApp);

            checkPrice(opencartInfo, supplierApp);
            checkProductOption(opencartInfo);

            checkStockStatusId(opencartInfo, supplierApp);

            List<ProductOpencart> newProduct = opencartInfo.getNewProduct();


            translateService.webTranslate(newProduct);

            newProduct.forEach(opencartDaoService::saveProductOpencart);
            //:TODO update price in function checkPrice
            if(!newProduct.isEmpty()) {
                updateDataService.updatePrice(supplierApp.getSupplierAppId());
            }

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

    public void translateProducts() {
        List<ProductOpencart> products = opencartDaoService.getAllProductOpencartBySupplierAppName(SUPPLIER_NAME);

        products = products
                .stream()
                .map(p -> opencartDaoService.getProductOpencartWithDescriptionById(p.getId()))
                .collect(Collectors.toList());

        translateService.webTranslate(products);

        products
                .forEach(p -> opencartDaoService.updateDescription(p.getProductsDescriptionOpencart().get(0)));

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

    public ProductOpencart settingOptionsOpencart(Document doc, ProductOpencart productOpencart, SupplierApp supplierApp) {

        try {

            List<OptionDto> optionDtoList = setOptions(doc, productOpencart);
            log.info("ACTUAL PRODUCT DATA = SKU: {}, PRICE: {}, PP: {}", productOpencart.getSku(), productOpencart.getPrice(), productOpencart.getProductProfileApp().getPrice());


            List<OptionOpencart> optionsOpencartList = optionDtoList
                    .stream()
                    .map(o -> {

                        AtomicInteger countSort = new AtomicInteger();
                        List<OptionValueOpencart> optionValues = o.getValues()
                                .stream()
                                .map(v -> {
                                    int sort = v.isDefault() ? 0 : countSort.addAndGet(1);
                                    OptionValueDescriptionOpencart valueDescription = new OptionValueDescriptionOpencart();
                                    valueDescription.setName(v.getValue());

                                    ProductOptionValueOpencart productOptionValueOpencart = new ProductOptionValueOpencart();
                                    productOptionValueOpencart.setPrice(new BigDecimal(v.getPrice()));

                                    OptionValueOpencart optionValue = new OptionValueOpencart();
                                    optionValue.setSortOrder(sort);
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
                        optionOpencart.setType(o.getOptionType());
                        optionOpencart.setValues(optionValues);


                        checkPersistOptionOpencart(optionOpencart, supplierApp);

                        ProductOptionOpencart productOptionOpencart = new ProductOptionOpencart();
                        productOptionOpencart.setOptionId(optionOpencart.getOptionId());
                        productOptionOpencart.setRequired(o.getRequired() == 1);
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

    public List<OptionDto> setOptions(Document doc, ProductOpencart productOpencart) {

        Elements optionElements = doc.select("div.options_group");
        log.info("Options elements size: {}", optionElements.size());


        List<OptionDto> optionDtoList = optionElements
                .stream()
                .map(oe -> {


                    OptionDto optionDto = new OptionDto();
                    AtomicInteger countOption = new AtomicInteger();

                    String optionTitleCode = oe.select("label.control-label").text().trim();
                    log.info("Option title/code: {}", optionTitleCode);
                    String aClass = oe.attr("class");
                    log.info("Option class: {}", aClass);

                    int required = aClass.contains("required") ? 1 : 0;

                    optionDto.setName(optionTitleCode);
                    optionDto.setNameCode(optionTitleCode);
                    optionDto.setRequired(required);

                    Elements optionsElement = oe.select("div[id^='input-option'] > div");

                    optionsElement
                            .forEach(row -> {


                                Elements inputElement = row.select("input");
                                String typeOption = inputElement.attr("type");
                                optionDto.setOptionType(typeOption);

                                log.info("Option type: {}", typeOption);
                                if (!typeOption.equals("text")) {
                                    int dataPrice = (int) Double.parseDouble(inputElement.attr("data-price"));
                                    log.info("Option data price: {}", dataPrice);

                                    OptionValuesDto optionValue = new OptionValuesDto();
                                    optionValue.setPrice(dataPrice);
                                    Elements imgOptionElement = row.select("img");
                                    if (!imgOptionElement.isEmpty()) {

                                        String optionImageUrl = imgOptionElement.attr("src");
                                        log.info("Option image url: {}", optionImageUrl);
                                        String optionValueName = imgOptionElement.attr("title");
                                        log.info("Option value  name: {}", optionValueName);
                                        String imageName = optionImageUrl.substring(optionImageUrl.lastIndexOf("/") + 1);
                                        log.info("Option image  name: {}", imageName);
                                        String dbImage = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);
                                        log.info("Option image DB name: {}", dbImage);

                                        optionValue.setDbpathImage(dbImage);
                                        optionValue.setValue(optionValueName);
                                        optionValue.setValueCode(optionValueName);
                                        fileService.downloadImg(optionImageUrl, dbImage);

                                    } else {
                                        String optionValueName = row.select("span.im_option").text().trim();
                                        log.info("Option value  name: {}", optionValueName);
                                        optionValue.setValue(optionValueName);
                                        optionValue.setValueCode(optionValueName);
                                    }


                                    if (countOption.get() == 0 || required == 1) {
                                        optionValue.setDefault(true);
                                    }

                                    optionDto.getValues().add(optionValue);
                                    countOption.addAndGet(1);
                                }

                            });
                    return optionDto;
                })
                .filter(odto -> !odto.getValues().isEmpty())
                .collect(Collectors.toList());


        return optionDtoList;

    }


    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();

        Document doc = getWebDocument(SUPPLIER_URL, new HashMap<>());

        AtomicInteger countMainCategory = new AtomicInteger();
        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select("div#tab_catwall1 a.linkchild")
                    .stream()
                    .map(ec -> {

                        String url = ec.attr("href").replaceAll("http:", "https:");
                        String title = ec.text().trim();
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
                    //:TODO next line uncommitted only debug
                    //.findFirst().stream()
                    .collect(Collectors.toList());

            log.info("Main category size: {}", mainCategories.size());


//            List<CategoryOpencart> siteCategoryStructure = mainCategories
//                    .stream()
//                    .map(mc -> {
//                        log.info("MAIN CATEGORY: {}", mc.getDescriptions().get(0).getName());
//                        CategoryOpencart categoryOpencart = recursiveWalkSiteCategory(mc);
//                        return categoryOpencart;
//                    })
//                    .collect(Collectors.toList());

            List<CategoryOpencart> siteCategoryList = mainCategories
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

                        int countPage = 0;
                        boolean hasNextPage = true;

                        while (hasNextPage) {

                            try {
                                countPage++;
                                String newUrlPage = url.concat(URL_PART_PAGE).concat(String.valueOf(countPage));
                                log.info("URL: {}", newUrlPage);

                                doc = getWebDocument(newUrlPage, new HashMap<>());

                                Elements elementsProduct = doc.select("div#product_block div.product-thumb");
                                log.info("Count product: {} on page: {}", elementsProduct.size(), countPage);

                                elementsProduct
                                        .stream()
                                        .map(ep -> {

                                            log.info("");
                                            String urlProduct = ep.select("*.name_pricelist a").attr("href");
                                            log.info("Product url: {}", urlProduct);

                                            String title = ep.select("*.name_pricelist").text().replaceAll("\\.\\.\\.", "").trim();
                                            log.info("Product title: {}", title);

                                            String sku = ep.select("button.button_img_quick").attr("onclick").replaceAll("\\D", "").trim();
                                            log.info("Product sku: {}", sku);

                                            String model = generateModel(sku, "0000");
                                            log.info("Product model: {}", model);

                                            Elements priceElement = ep.select("*.price_pricelist");

                                            String textPrice = "0";
                                            if (!priceElement.isEmpty()) {
                                                textPrice = priceElement.text();
                                                int idxUAN = textPrice.indexOf("₴");

                                                if (idxUAN != -1) {
                                                    textPrice = textPrice.substring(0, idxUAN).replaceAll("\\D", "");

                                                }
                                            }
                                            BigDecimal price = new BigDecimal(textPrice);
                                            log.info("Product price: {}", price);


                                            ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                                                    .withUrl(urlProduct)
                                                    .withSku(sku)
                                                    .withTitle(title)
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
                                                    .withTitle(title)
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

                            } catch (Exception e) {
                                log.warn("Problem iterate page", e);
                            } finally {
                                int finalCountPage = countPage + 1;
                                Elements select = doc.select("ul.pagination li");
                                log.info("SELECT: {}", select.text());
                                hasNextPage = !select.stream()
                                        .map(n -> {
                                            String pageNumberString = n.text().replaceAll("\\D", "");
                                            if (pageNumberString.isEmpty())
                                                return 0;
                                            else
                                                return Integer.parseInt(pageNumberString);
                                        })
                                        .filter(n -> n == finalCountPage)
                                        .collect(Collectors.toList()).isEmpty();
                            }


                        }


                    }
                });

        return allProductInitList;
    }

    @Override
    public List<ProductOpencart> getFullProductsData(List<ProductOpencart> products, SupplierApp supplierApp) {


        AtomicInteger count = new AtomicInteger();

        List<ProductOpencart> fullProducts = products
                .stream()
                .peek(prod -> {

                    String urlProduct = prod.getUrlProduct();
                    log.info("{}. Get data from product url: {}", count.addAndGet(1), urlProduct);
                    Document doc = getWebDocument(urlProduct, new HashMap<>());


                    try {

                        String title = prod.getTitle();
                        Elements elementDescription = doc.select("div.description_pr");
                        String description = "";
                        if (!elementDescription.isEmpty()) {
                            description = getDescription(elementDescription.get(0));
                        }

                        String dataModel = doc.select("span[itemprop=model]").text();
                        String model = generateModel(dataModel, prod.getSku());
                        prod.setModel(model);
                        ProductDescriptionOpencart descriptionOpencart = new ProductDescriptionOpencart.Builder()
                                .withName(title)
                                .withDescription(description)
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .build();
                        prod.getProductsDescriptionOpencart().add(descriptionOpencart);

                        Elements elementManufacturer = doc.select("a[itemprop=manufacturer]");
                        String manufacturerName = "non";
                        if (!elementManufacturer.isEmpty()) {
                            manufacturerName = elementManufacturer.text().trim();
                        }
                        log.info("Manufacturer: {}", manufacturerName);
                        ManufacturerApp manufacturerApp = getManufacturerApp(manufacturerName, supplierApp);
                        ProductProfileApp productProfileApp = prod.getProductProfileApp();
                        productProfileApp.setTitle(title);
                        productProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                        productProfileApp.setManufacturerApp(manufacturerApp);
                        productProfileApp.setPrice(prod.getPrice());
                        ProductProfileApp savedProductProfile = getProductProfile(productProfileApp, supplierApp);
                        prod.setProductProfileApp(savedProductProfile);

                        log.info("Product price for profile: {}", prod.getPrice());


                        Elements attributeElement = doc.select("div#tab-specification tbody tr");


                        List<AttributeWrapper> attributeWrappers = attributeElement
                                .stream()
                                .map(row -> {

                                    Elements td = row.select("td");
                                    if (td.size() == 2) {
                                        String key = td.get(0).text().trim();
                                        String value = td.get(1).text().trim();
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
                        prod.getAttributesWrapper().addAll(attributeWrappers);


                        Elements optionsForm = doc.select("div.options_group");
                        if (!optionsForm.isEmpty()) {
                            settingOptionsOpencart(doc, prod, supplierApp);
                        }


                        setManufacturer(prod, supplierApp);
                        setPriceWithMarkup(prod);


                        Elements imageElements = doc.select("div.prmain img.main-image");
                        log.info("Images count: {}", imageElements.size());

                        AtomicInteger countImg = new AtomicInteger();
                        List<ImageOpencart> productImages = imageElements
                                .stream()
                                .map(i -> {
                                    String src = i.attr("src").replaceAll("400x350", "700x700");
                                    log.info("img src: {}", src);

                                    if (countImg.get() == 0) {
                                        log.info("Image url: {}", src);
                                        String imgName = prod.getSku().concat("_" + countImg.addAndGet(1)).concat(".gif");
                                        log.info("Image name: {}", imgName);
                                        String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
                                        log.info("Image DB name: {}", dbImgPath);
                                        downloadImage(src, dbImgPath);
                                        prod.setImage(dbImgPath);
                                        return null;
                                    } else {
                                        log.info("Image url: {}", src);
                                        String imgName = prod.getSku().concat("_" + countImg.addAndGet(1)).concat(".gif");
                                        log.info("Image name: {}", imgName);
                                        String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
                                        log.info("Image DB name: {}", dbImgPath);
                                        downloadImage(src, dbImgPath);
                                        return new ImageOpencart.Builder()
                                                .withImage(dbImgPath)
                                                .withSortOrder(countImg.get())
                                                .build();
                                    }

                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());


                        log.info("Images: {}", productImages);
                        prod.setImagesOpencart(productImages);

                        log.info("Product: [sku={}] price: [{}]", prod.getSku(), prod.getPrice());

                    } catch (Exception ex) {
                        log.warn("Bad parsing product data", ex);
                    }

                })
                .filter(p -> p.getId() != -1)
                .filter(p -> !p.getSku().isEmpty())
                .collect(Collectors.toList());


        return fullProducts;
    }

    public String translateDescription(String desc) {
        Document parse = Jsoup.parse(desc);
        String description = cleanDescription(parse);
        String translatedText = translateService.getTranslatedText(description);
        return wrapToHtml(translatedText);
    }

    public String getDescription(Element descElement) {
        String description = cleanDescription(descElement);
        log.info("Desc: {}", description);
        return wrapToHtml(description);
    }


    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {

        Element doc = category.getCategoryElement();
        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> subCategories = doc.select("ul.sub_list > li > a")
                    .stream()
                    .map(el -> {
                        String subUrl = SUPPLIER_URL.concat(el.attr("href"));
                        String subTitle = el.text();
                        log.info("    Sub category: {}, url: {}", subTitle, subUrl);
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
                ManufacturerApp manufacturerApp = getManufacturerApp(productProfileApp.getManufacturerApp().getSupplierTitle(), supplierApp);
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
