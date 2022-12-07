package orlov.home.centurapp.service.parser;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ParserServiceAnshar extends ParserServiceAbstract {
    private final String SUPPLIER_NAME = "anshar";
    private final String SUPPLIER_URL = "https://www.anshar.com.ua/uk";
    private final String SUPPLIER_URL_DEFAULT = "https://www.anshar.com.ua";
    private final String SUPPLIER_ALL_CATALOG = "https://www.anshar.com.ua/uk/catalog/all";
    private final String DISPLAY_NAME = "1 - ГЕЛІКА";
    private final String MANUFACTURER_NAME = "non";
    private final String URL_PART_PAGE = "?page=";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;
    private final UpdateDataService updateDataService;
    private final ImageService imageService;

    public ParserServiceAnshar(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService, ImageService imageService) {
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
                                    productOptionValueOpencart.setPrice(new BigDecimal(v.getMargin()));

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

    public List<OptionDto> setOptions(Document doc, ProductOpencart productOpencart) {

        String multiImageOption = "Комбінація кольорів";

        Elements optionMainElement = doc.select("div.field_inner_node_process.field_inner_node_process_field_products_color.field_inner_node_process_field_products_color_products");

        Elements optionElements = optionMainElement.select("div.entity.entity-field-collection-item.field-collection-item-field-products-color");
        log.info("Options elements size: {}", optionElements.size());

        List<OptionDto> setColorOptionList = new ArrayList<>();

        List<OptionDto> optionDtoList = optionElements
                .stream()
                .map(oe -> {


                    OptionDto optionDto = new OptionDto();
                    AtomicInteger countOption = new AtomicInteger();

                    String optionTitleCode = oe.select("div.field.field-name-field-products-color-f1.field-type-text.field-label-hidden").text().replaceAll(":", "");
                    log.info("Option title/code: {}", optionTitleCode);

                    boolean isSetColor = optionTitleCode.contains(multiImageOption);


                    optionDto.setName(optionTitleCode);
                    optionDto.setNameCode(optionTitleCode);


                    Elements hexRows = oe.select("div.field.field-name-field-products-color-f2.field-type-jquery-colorpicker.field-label-hidden");

                    hexRows
                            .forEach(hr -> {
                                Elements hexValues = hr.select("div[id^=jquery_colorpicker_color_display]");
                                hexValues
                                        .forEach(hexValue -> {
                                            String hexValueClass = hexValue.attr("class");
                                            int idxHex = hexValueClass.lastIndexOf("_");
                                            if (idxHex != -1) {


                                                String hexImageText = hexValueClass.substring(idxHex + 1);
                                                String optionImage = imageService.createOptionImage("#" + hexImageText);

                                                OptionValuesDto optionValue = new OptionValuesDto();
                                                optionValue.setDbpathImage(optionImage);
                                                optionValue.setValue(hexImageText);
                                                optionValue.setValueCode(hexValueClass);
                                                if (countOption.get() == 0) {
                                                    optionValue.setDefault(true);
                                                }
                                                optionDto.getValues().add(optionValue);
//                                                log.info("    Option values ( HEX color) : {}", optionValue);
                                                countOption.addAndGet(1);
                                            }
                                        });
                            });


                    Elements imageRows = oe.select("div.field.field-name-field-products-color-f3.field-type-image.field-label-hidden");

                    imageRows
                            .forEach(ir -> {
                                Elements imageValues = ir.select("img");

                                imageValues
                                        .forEach(imageValue -> {
                                            String src = imageValue.attr("src");
                                            int idxStartTitle = src.lastIndexOf("/") + 1;
                                            int idxEndTitle = src.indexOf("?");


                                            if (idxStartTitle != -1 && idxEndTitle != -1) {
                                                String imageName = src.substring(idxStartTitle, idxEndTitle);
                                                String valueTitleCode = imageName.substring(0, imageName.indexOf("."));
                                                String dbImage = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);

                                                OptionValuesDto optionValue = new OptionValuesDto();
                                                optionValue.setDbpathImage(dbImage);
                                                optionValue.setValue(valueTitleCode);
                                                optionValue.setValueCode(valueTitleCode);
                                                if (countOption.get() == 0) {
                                                    optionValue.setDefault(true);
                                                }

                                                downloadImage(src, dbImage);
                                                optionDto.getValues().add(optionValue);

//                                                log.info("    Option values ( IMG name) : {}", optionValue);
                                                countOption.addAndGet(1);
                                            }

                                        });
                            });


                    if (isSetColor) {
                        setColorOptionList.add(optionDto);
                        return null;
                    } else {
                        return optionDto;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        OptionDto setColorOption = new OptionDto();
        setColorOption.setNameCode(multiImageOption);
        setColorOption.setName(multiImageOption);

        AtomicInteger countSetColot = new AtomicInteger();

        setColorOptionList
                .forEach(sco -> {

                    OptionValuesDto setColorOptionValuesDto = new OptionValuesDto();
                    boolean isDefault = countSetColot.addAndGet(1) == 1;
                    String name = sco.getName().concat(" - ").concat(productOpencart.getSku());
                    setColorOptionValuesDto.setValue(name);
                    setColorOptionValuesDto.setDefault(isDefault);

                    String optionValueCode = sco.getValues()
                            .stream()
                            .map(OptionValuesDto::getValueCode)
                            .collect(Collectors.joining("-"));

                    List<String> imageList = sco.getValues()
                            .stream()
                            .map(o -> {
                                String dbpathImage = o.getDbpathImage();
                                String imageName = dbpathImage.substring(dbpathImage.lastIndexOf("/") + 1);
                                return imageName;
                            })
                            .collect(Collectors.toList());

                    String optionMultiImage = imageService.createOptionMultiImage(imageList);


                    setColorOptionValuesDto.setValueCode(optionValueCode);


                    setColorOptionValuesDto.setDbpathImage(optionMultiImage);
                    setColorOption.getValues().add(setColorOptionValuesDto);

                });


        optionDtoList.add(setColorOption);


        return optionDtoList;

    }


    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        Document doc = getWebDocument(SUPPLIER_ALL_CATALOG, new HashMap<>());


        AtomicInteger countMainCategory = new AtomicInteger();
        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select("div#sidebar-first ul.top_list > li.top_li")
                    .stream()
                    .map(li -> {

                        Elements ec = li.select(" > a");
                        String url = SUPPLIER_URL_DEFAULT.concat(ec.select("a").first().attr("href"));
                        String title = ec.select("a").first().text().trim();
                        log.info("{}. Main site category title: {}, url: {}", countMainCategory.addAndGet(1), title, url);
                        CategoryOpencart categoryOpencart = new CategoryOpencart.Builder()
                                .withUrl(url)
                                .withParentCategory(supplierApp.getMainSupplierCategory())
                                .withParentId(supplierApp.getMainSupplierCategory().getCategoryId())
                                .withTop(false)
                                .withStatus(false)
                                .build();
                        categoryOpencart.setCategoryElement(li);
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
                    log.info("url: {}", url);
                    if (Objects.nonNull(doc)) {
                        boolean goNext = true;
                        int countPage = 0;

                        while (goNext) {
                            try {

                                if (countPage != 0) {
                                    String newUrlPage = url.concat(URL_PART_PAGE).concat(String.valueOf(countPage));
                                    doc = getWebDocument(newUrlPage, new HashMap<>());
                                    log.info("url: {}", newUrlPage);
                                }

                                if (Objects.nonNull(doc)) {

                                    Elements elementsProduct = doc.select("div.view-content div.row_inner");
                                    log.info("Count product: {} on page: {}", elementsProduct.size(), countPage);

                                    //  TODO continue ...
                                    elementsProduct
                                            .stream()
                                            .map(ep -> {
                                                log.info("");
                                                String urlProduct = SUPPLIER_URL_DEFAULT.concat(ep.select("div.views-field-field-node-img a").attr("href"));
                                                log.info("Product url: {}", urlProduct);
                                                String title = ep.select("div.views-field-title-field a").text();
                                                log.info("Product title: {}", title);

                                                String sku = ep.select("div.views-field-field-products-art div.field-content").text().replaceAll("Код товара:", "").trim();
                                                log.info("Product sku: {}", sku);

                                                String model = generateModel(sku, "0000");
                                                log.info("Product model: {}", model);

                                                String textPrice = ep.select("div.views-field-field-products-price div.val").text().replaceAll("\\D", "");
                                                BigDecimal price = new BigDecimal("0");
                                                if (!textPrice.isEmpty()) {
                                                    price = new BigDecimal(textPrice);
                                                }
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

                                }
                            } catch (Exception e) {
                                log.warn("Problem iterate page", e);
                            } finally {
                                countPage++;
                                Elements ePages = doc.select("ul.pager li");
                                List<Integer> pages = ePages
                                        .stream()
                                        .map(e -> e.text().replaceAll("\\D", ""))
                                        .filter(s -> !s.isEmpty())
                                        .map(Integer::parseInt)
                                        .collect(Collectors.toList());
                                goNext = pages.contains(countPage + 1);
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
                        Elements elementDescription = doc.select("div.body_wrap div.field-items");
                        String description = "";
                        if (!elementDescription.isEmpty()) {
                            description = getDescription(elementDescription.get(0));
                        }

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


                        //  TODO attr
//                        List<WebElement> attributeElements = driver.findElements(By.xpath("//div[@id='fm-product-attributes-top']"));
//                        if (!attributeElements.isEmpty()) {
//                            moveToElement(attributeElements.get(0), driver, 200);
//                            List<AttributeWrapper> attributes = attributeElements.get(0).findElements(By.xpath(".//div[contains(@class, 'fm-product-attributtes-item')]"))
//                                    .stream()
//                                    .map(row -> {
//                                        List<WebElement> attrData = row.findElements(By.xpath(".//span"));
//                                        if (attrData.size() == 2) {
//                                            String key = attrData.get(0).getText().trim();
//                                            String value = attrData.get(1).getText().trim();
//                                            log.info("Key: {}, value: {}", key, value);
//                                            AttributeWrapper attributeWrapper = new AttributeWrapper(key, value, null);
//                                            AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
//                                            return attribute;
//                                        } else {
//                                            return null;
//                                        }
//                                    })
//                                    .filter(Objects::nonNull)
//                                    .collect(Collectors.toList());
//                            prod.getAttributesWrapper().addAll(attributes);
//                        }


                        Elements optionsForm = doc.select("div.field_inner_node_process_field_products_color_products");
                        if (!optionsForm.isEmpty()) {
                            settingOptionsOpencart(doc, prod, supplierApp);
                        }


                        setManufacturer(prod, supplierApp);
                        setPriceWithMarkup(prod);

                        Elements imageElements = doc.select("a.photoswipe > img");
                        log.info("Images count: {}", imageElements.size());
                        String partPathImage = " https://www.anshar.com.ua/sites/default/files/";
                        String finishImage = "/public/";

                        if (imageElements.size() > 0) {
                            AtomicInteger countImg = new AtomicInteger();
                            List<ImageOpencart> productImages = imageElements
                                    .stream()
                                    .map(i -> {
                                        String src = i.attr("src");
                                        src = partPathImage.concat(src.substring(src.indexOf(finishImage) + finishImage.length(), src.indexOf("?")));
                                        log.info("src: {}", src);

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


    public void waitScripts(WebDriver driver) {
        new WebDriverWait(driver, 30, 500).until((ExpectedCondition<Boolean>) webDriver -> {
            boolean isAjaxDone = ((JavascriptExecutor) driver).executeScript("return jQuery.active == 0").equals(true);
            return isAjaxDone;
        });
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
                        String subUrl = SUPPLIER_URL_DEFAULT.concat(el.attr("href"));
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
