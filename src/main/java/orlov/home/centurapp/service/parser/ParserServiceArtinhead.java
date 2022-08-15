package orlov.home.centurapp.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.OpencartDto;
import orlov.home.centurapp.dto.api.artinhead.OptionDto;
import orlov.home.centurapp.dto.api.artinhead.OptionValuesDto;
import orlov.home.centurapp.dto.api.artinhead.ProductArtinhead;
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
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserServiceArtinhead extends ParserServiceAbstract {
    private final String SUPPLIER_NAME = "artinhead";
    private final String SUPPLIER_URL = "https://artinhead.com/";
    private final String SUPPLIER_URL_CATEGORY = "https://artinhead.com/product-category/all/";
    private final String DISPLAY_NAME = "20 - ART IN HEAD";
    private final String MANUFACTURER_NAME = "ART IN HEAD";
    private final String URL_PART_PAGE = "page/";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;
    private final UpdateDataService updateDataService;

    public ParserServiceArtinhead(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService) {
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
        List<ProductProfileApp> productProfilesApp = supplierApp.getProductProfilesApp();
        productProfilesApp
                .forEach(p -> {

                    String urlProduct = p.getUrl();
                    log.info("URL: {}", urlProduct);

                    try {
                        Document webDocument = getWebDocument(urlProduct, new HashMap<>());
                        Elements tableAttr = webDocument.select("tr.woocommerce-product-attributes-item");

                        List<AttributeWrapper> attributes = tableAttr
                                .stream()
                                .map(row -> {
                                    String th = row.select("td.product-attributes-item__label").text();
                                    String td = row.select("td.product-attributes-item__value").text();
                                    AttributeWrapper attributeWrapper = new AttributeWrapper(th, td, null);
                                    log.info("Before attribute wrapper: {}", attributeWrapper);
                                    AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, p);
                                    log.info("After attribute wrapper: {}", attributeWrapper);

                                    return attribute;

                                })
                                .collect(Collectors.toList());

                    } catch (Exception ex) {
                        log.warn("Page not found. {}", 1);
                    }


                });
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

    public ProductOpencart settingOptionsOpencart(Document doc, ProductOpencart productOpencart, SupplierApp supplierApp) {

        Element formDataElement = doc.select("form[data-product_variations]").get(0);
        String jsonString = formDataElement.attr("data-product_variations");
        ObjectMapper mapper = new ObjectMapper();
        log.info("JSON OPTION: {}", jsonString);

        try {

            List<ProductArtinhead> productsFromJson = mapper.readValue(jsonString, new TypeReference<List<ProductArtinhead>>() {
            });

            Integer defaultPrice;

            List<ProductArtinhead> sortedProducts = productsFromJson
                    .stream()
                    .sorted(Comparator.comparingInt(ProductArtinhead::getDisplayPrice))
                    .collect(Collectors.toList());

            ProductArtinhead productArtinheadDefault = sortedProducts.get(0);
            productArtinheadDefault.setDefault(true);
            defaultPrice = productArtinheadDefault.getDisplayPrice();
            productOpencart.setPrice(new BigDecimal(defaultPrice).setScale(4));
            productOpencart.setItuaOriginalPrice(new BigDecimal(defaultPrice).setScale(4));

            List<OptionDto> optionsDtoList = new ArrayList<>();
            sortedProducts.forEach(p -> {

                boolean isDefault = p.equals(productArtinheadDefault);

                p.getAttributes()
                        .entrySet()
                        .forEach(e -> {
                            String nameCode = e.getKey();
                            String valueCode = e.getValue();

                            OptionDto optionDto = new OptionDto();
                            optionDto.setNameCode(nameCode);
                            OptionValuesDto optionValuesDto = new OptionValuesDto();
                            optionValuesDto.setDefault(isDefault);
                            optionValuesDto.setValueCode(valueCode);

                            boolean containsName = optionsDtoList.contains(optionDto);
                            if (containsName) {
                                OptionDto optionInList = optionsDtoList.get(optionsDtoList.indexOf(optionDto));
                                List<OptionValuesDto> values = optionInList.getValues();
                                boolean containsValue = values.contains(optionValuesDto);
                                if (!containsValue) {
                                    optionInList.getValues().add(optionValuesDto);
                                }
                            } else {
                                optionDto.getValues().add(optionValuesDto);
                                optionsDtoList.add(optionDto);
                            }
                        });
            });


            optionsDtoList
                    .forEach(o -> {
                        o.getValues()
                                .stream()
                                .filter(v -> !v.isDefault())
                                .forEach(v -> {

                                    Map<String, String> data = new HashMap<>();
                                    data.put(o.getNameCode(), v.getValueCode());

                                    optionsDtoList
                                            .stream()
                                            .filter(so -> !so.equals(o))
                                            .peek(so -> {
                                                OptionValuesDto sv = so.getValues()
                                                        .stream()
                                                        .filter(OptionValuesDto::isDefault)
                                                        .findFirst()
                                                        .get();
                                                data.put(so.getNameCode(), sv.getValueCode());
                                            })
                                            .collect(Collectors.toList());
                                    log.info("Data: {}", data);

                                    productsFromJson
                                            .forEach(p -> {
                                                boolean hasOptions = p.getAttributes().equals(data);
                                                if (hasOptions) {
                                                    int marginOption = p.getDisplayPrice() - defaultPrice;
                                                    v.setMargin(marginOption);
                                                }
                                            });
                                });
                    });

            optionsDtoList
                    .stream()
                    .peek(o -> {
                        String nameCode = o.getNameCode();
                        log.info("");
                        String name = doc.select("ul[data-attribute_name=" + nameCode + "]").attr("aria-label");
                        o.setName(name);
                        log.info("Option name: {}", name);
                        o.getValues()
                                .stream()
                                .peek(v -> {
                                    String valueCode = v.getValueCode();
                                    if (valueCode.isEmpty() && o.getValues().size() == 1) {
                                        valueCode = doc.select("ul[data-attribute_name=" + nameCode + "] li").attr("data-value");
                                    }
                                    log.info("Option valueCode: {}", valueCode);
                                    Elements liValue = doc.select("li[data-value=" + valueCode + "]");
                                    String value = doc.select("option[value=" + valueCode + "]").text().trim();

                                    if (value.isEmpty()) {
                                        value = valueCode;
                                    }

                                    v.setValue(value);

                                    Elements imageElement = liValue.select("img.variable-item-image");
                                    if (!imageElement.isEmpty()) {
                                        String url = imageElement.attr("data-src");
                                        log.info("img: {}", url);
                                        v.setImgUrl(url);
                                        String imgName = "option_".concat(url.substring(url.lastIndexOf("/") + 1));
                                        log.info("Image option name: {}", imgName);
                                        String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imgName);
                                        log.info("dbImgPath option : {}", dbImgPath);
                                        downloadImage(url, dbImgPath);
                                        v.setDbpathImage(dbImgPath);
                                    }
                                })
                                .collect(Collectors.toList());
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

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return productOpencart;
    }


    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        Document doc = getWebDocument(SUPPLIER_URL_CATEGORY, new HashMap<>());

        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select("p.thumb__title a")
                    .stream()
                    .map(ec -> {


                        String url = ec.attr("href");
                        String title = ec.text();
                        log.info("Main site category title: {}, url: {}", title, url);
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
                    .map(this::recursiveWalkSiteCategory)
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
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk-ua");
        List<ProductOpencart> productsCategory = new ArrayList<>();

        categoriesWithProduct
                .stream()
                .filter(c -> {
                    int categoryId = c.getCategoryId();
                    List<CategoryOpencart> childrenCategory = categoriesWithProduct.stream().filter(sub -> sub.getParentId() == categoryId).collect(Collectors.toList());
                    return childrenCategory.isEmpty();
                })
                .peek(c -> {

                    String categoryName = c.getDescriptions().get(0).getName();
                    CategoryApp categoryApp = getCategoryApp(categoryName, supplierApp);

                    String url = c.getUrl();
                    Document doc = getWebDocument(url, cookies);

                    List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);

                    if (Objects.nonNull(doc)) {
                        boolean goNext = true;
                        int countPage = 1;
                        while (goNext) {
                            try {
                                if (countPage != 1) {
                                    String newUrlPage = url.concat(URL_PART_PAGE).concat(String.valueOf(countPage));
                                    doc = getWebDocument(newUrlPage, cookies);
                                }

                                if (Objects.nonNull(doc)) {

                                    Elements elementsProduct = doc.select("div.item.product-item");

                                    elementsProduct
                                            .stream()
                                            .peek(ep -> {
                                                String urlProduct = ep.select("h6.product-title a").attr("href");
                                                String title = ep.select("h6.product-title a").text().trim();
                                                String sku = ep.select("a[data-product_id]").attr("data-product_id");
                                                String supplierModel = ep.select("a[data-product_sku]").attr("data-product_sku");
                                                String model = generateModel(supplierModel, sku);

                                                Elements priceElements = ep.select("p.product-price");
                                                Elements ins = priceElements.select("ins");
                                                String stringSatus = ep.select("div.product-condition__status").text().trim();
                                                int stockStatusId = stringSatus.equals("Під замовлення") ? 8 : 7;

                                                String price = "0";

                                                if (!priceElements.isEmpty() && !ins.isEmpty()) {
                                                    price = priceElements.select("ins").text().replaceAll("\\D", "");
                                                } else if (!priceElements.isEmpty() && priceElements.select("span.woocommerce-Price-amount").size() == 2) {
                                                    price = priceElements.select("span.woocommerce-Price-amount")
                                                            .stream()
                                                            .map(span -> Double.parseDouble(span.text().replaceAll("\\D", "")))
                                                            .max(Double::compare)
                                                            .map(String::valueOf)
                                                            .get();
                                                } else if (!priceElements.isEmpty()) {
                                                    price = priceElements.select("span.woocommerce-Price-amount").text().replaceAll("\\D", "");
                                                }

                                                BigDecimal priceNumberFree = new BigDecimal(price).setScale(4);

                                                log.info("Product url: {}", urlProduct);
                                                log.info("Product title: {}", title);
                                                log.info("Product sku: {}", sku);
                                                log.info("Product price: {}", price);

                                                ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                                                        .withUrl(urlProduct)
                                                        .withSku(sku)
                                                        .withTitle(title)
                                                        .withSupplierId(supplierApp.getSupplierAppId())
                                                        .withSupplierApp(supplierApp)
                                                        .withCategoryId(categoryApp.getCategoryId())
                                                        .withCategoryApp(categoryApp)
                                                        .withPrice(priceNumberFree)
                                                        .build();

                                                log.info("Product profile: {}", productProfileApp);


                                                ProductOpencart product = new ProductOpencart.Builder()
                                                        .withProductProfileApp(productProfileApp)
                                                        .withModel(model)
                                                        .withUrlProduct(urlProduct)
                                                        .withTitle(title)
                                                        .withStockStatusId(stockStatusId)
                                                        .withSku(sku)
                                                        .withJan(supplierApp.getName())
                                                        .withPrice(priceNumberFree)
                                                        .withItuaOriginalPrice(priceNumberFree)
                                                        .build();


                                                product.setCategoriesOpencart(parentsCategories);


                                                ProductOpencart prodFromList = productsCategory
                                                        .stream()
                                                        .filter(ps -> ps.getUrlProduct().equals(product.getUrlProduct()))
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
                                                log.info("");
                                            })
                                            .collect(Collectors.toList());

                                }
                            } catch (Exception e) {
                                log.warn("Problem iterate page", e);
                            } finally {
                                countPage++;
                                Elements ePages = doc.select("ul.pagination-classic li");
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
                })
                .collect(Collectors.toList());

        return productsCategory;
    }


    @Override
    public List<ProductOpencart> getFullProductsData(List<ProductOpencart> products, SupplierApp supplierApp) {
        AtomicInteger count = new AtomicInteger();
        String partModel = supplierApp.getSupplierAppId() + "-";
        List<ProductOpencart> fullProducts = products
                .stream()
                .peek(p -> {
                    String urlProduct = p.getUrlProduct();
                    Document webDocument = getWebDocument(urlProduct, new HashMap<>());
                    log.info("{}. Get data from product url: {}", count.addAndGet(1), urlProduct);

                    if (Objects.nonNull(webDocument)) {

                        try {
                            String title = p.getTitle();

                            String description = getDescription(webDocument);
                            log.info("Description final: {}", description);


                            Elements optionsForm = webDocument.select("form[data-product_variations]");
                            if (!optionsForm.isEmpty()) {
                                settingOptionsOpencart(webDocument, p, supplierApp);
                            }


                            ManufacturerApp manufacturerApp = getManufacturerApp(MANUFACTURER_NAME, supplierApp);
                            ProductProfileApp firstProfileApp = p.getProductProfileApp();
                            firstProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                            firstProfileApp.setManufacturerApp(manufacturerApp);
                            firstProfileApp.setPrice(p.getPrice());
                            ProductProfileApp savedProductProfile = getProductProfile(firstProfileApp, supplierApp);
                            p.setProductProfileApp(savedProductProfile);

                            setManufacturer(p, supplierApp);

                            ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                    .withDescription(description)
                                    .withName(title)
                                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                    .withMetaH1(title)
//                                    .withMetaDescription(title.concat("  купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999"))
                                    .withMetaTitle("Купити ".concat(title).concat("  інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника."))
                                    .withMetaKeyword("Купити ".concat(title))
                                    .build();
                            p.getProductsDescriptionOpencart().add(productDescriptionOpencart);


                            Elements tableAttr = webDocument.select("tr.woocommerce-product-attributes-item");

                            List<AttributeWrapper> attributes = tableAttr
                                    .stream()
                                    .map(row -> {
                                        String th = row.select("td.product-attributes-item__label").text();
                                        String td = row.select("td.product-attributes-item__value").text();
                                        AttributeWrapper attributeWrapper = new AttributeWrapper(th, td, null);
                                        AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                        return attribute;
                                    })
                                    .collect(Collectors.toList());
                            p.getAttributesWrapper().addAll(attributes);


                            Elements imagesElement = webDocument.select("a.img-thumbnail-variant-2");
                            if (imagesElement.size() > 0) {
                                AtomicInteger countImg = new AtomicInteger();
                                List<ImageOpencart> productImages = imagesElement
                                        .stream()
                                        .map(i -> {

                                            int imageCount = countImg.addAndGet(1);
                                            String fullUrl = i.attr("href");

                                            String imgName = p.getSku().concat("_").concat(String.valueOf(imageCount)).concat(fullUrl.substring(fullUrl.lastIndexOf(".")));

                                            String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imgName);
                                            downloadImage(fullUrl, dbImgPath);
                                            log.info("Image name: {}, image dbPath: {}", imgName, dbImgPath);
                                            if (countImg.get() == 1) {
                                                p.setImage(dbImgPath);
                                                return null;
                                            } else {

                                                return new ImageOpencart.Builder()
                                                        .withImage(dbImgPath)
                                                        .withSortOrder(imageCount)
                                                        .build();
                                            }

                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());


                                p.setImagesOpencart(productImages);
                            }


                        } catch (Exception ex) {
                            log.warn("Bad parsing product data", ex);
                        }
                    } else {
                        p.setId(-1);
                    }

                })
                .filter(p -> p.getId() != -1)
                .filter(p -> !p.getSku().isEmpty())
                .collect(Collectors.toList());

        return fullProducts;
    }


    public String getDescription(Document doc) {
        Elements descElement = doc.select("div.product-information div.accordion__content");
        String description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
        log.info("Description text: {}", description);
        return wrapToHtml(description);
    }


    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();
        Document doc = getWebDocument(url, new HashMap<>());
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {
            Element subCateElement = doc.select("has not sub category").parents().first();
            if (Objects.nonNull(subCateElement)) {
                log.info("sub category: {}", subCateElement.text());
                List<CategoryOpencart> subCategories = subCateElement.select("li.item")
                        .stream()
                        .map(el -> {
                            String subUrl = el.select("a").attr("href");
                            log.info("subUrl: {}", subUrl);
                            String subTitle = el.select("a span.item-label").text();
                            log.info("main title: {} sub title: {}", category.getDescriptions().get(0).getName(), subTitle);
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
