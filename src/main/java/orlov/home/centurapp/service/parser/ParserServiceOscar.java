package orlov.home.centurapp.service.parser;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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
import orlov.home.centurapp.util.AppConstant;
import orlov.home.centurapp.util.OCConstant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserServiceOscar extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "oscar";
    private final String SUPPLIER_URL = "https://oskar.dp.ua/";
    private final String DISPLAY_NAME = "73 - ОСКАР";
    private final String URL_PART_PAGE = "/page/";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;

    private final UpdateDataService updateDataService;

    public ParserServiceOscar(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService) {
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
            log.warn("Exception parsing nowystyle", ex);
        }

    }

    public void updateAttributeValue() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);
        List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);
        getFullProductsData(productsFromSite, supplierApp);
    }

    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {
        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();

        Document doc = getWebDocument(supplierApp.getUrl(), new HashMap<>());

        if (Objects.nonNull(doc)) {

            List<CategoryOpencart> mainCategories = doc.select("ul.products > li.product-category > a")
                    .stream()
                    .map(ec -> {
                        String url = ec.attr("href");
                        String title = ec.text().trim();
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
                    .peek(c -> log.info("url category: {}", c.getUrl()))
                    //:TODO next line uncommitted only debug
                    //.findFirst().stream()
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

            AtomicInteger countCategory = new AtomicInteger();
            siteCategoryList
                    .forEach(c -> log.info("{}. Full Site category: {}, subCategorySize: {}", countCategory.addAndGet(1), c.getDescriptions().get(0).getName(), c.getCategoriesOpencart().size()));

            return siteCategoryList;

        }
        return new ArrayList<>();

    }

    public void downloadImages() {

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


                            String imgUrl = p.getUrlImage();
                            log.info("Main imgUrl: {}", imgUrl);
                            String format = imgUrl.substring(imgUrl.lastIndexOf("."));

                            String imageName = p.getModel().concat("_main").concat(format);
                            log.info("Main imageName: {}", imageName);
                            String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);
                            log.info("Main dbImgPath: {}", dbImgPath);
                            downloadImage(imgUrl, dbImgPath);
                            p.setImage(dbImgPath);

                            searchProductOpencart.setImage(dbImgPath);
                            opencartDaoService.deleteImageOpencartByProductId(searchProductOpencart.getId());
                            opencartDaoService.updateMainProductImageOpencart(searchProductOpencart);

                        }
                    } catch (Exception ex) {
                        log.warn("Exception update image.", ex);
                    }


                });

    }

    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();

        Document doc = getWebDocument(url, new HashMap<>());
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {

            Elements subCateElements = doc.select("ul.products li.product-category a");

            List<CategoryOpencart> subCategories = subCateElements
                    .stream()
                    .map(el -> {

                        String href = el.attr("href");

                        log.info("Sub href: {}", href);
                        String title = el.text().trim();
                        log.info("Sub title: {}", title);
                        CategoryOpencart subCategory = new CategoryOpencart.Builder()
                                .withUrl(href)
                                .withTop(false)
                                .withParentCategory(category)
                                .withStatus(false)
                                .build();
                        CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                                .withName(title)
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
        List<CurrencyOpencart> allCurrencyOpencart = opencartDaoService.getAllCurrencyOpencart();
        CurrencyOpencart eur = allCurrencyOpencart.stream().filter(c -> c.getCode().equals("EUR")).findFirst().orElse(null);
        log.info("EUR: {}", eur);
        BigDecimal eurValue = eur.getValue();


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

                    Document doc = getWebDocument(url, new HashMap<>());

                    List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);


                    if (Objects.nonNull(doc)) {
                        int countPage = 1;

                        boolean hasNextPage = true;
                        while (hasNextPage) {
                            try {
                                if (countPage != 1) {
                                    String newUrlPage = url.concat(URL_PART_PAGE).concat(String.valueOf(countPage));
                                    doc = getWebDocument(newUrlPage, new HashMap<>());
                                    log.info("{}. URL page: {}", countPage, newUrlPage);
                                } else {
                                    log.info("{}. URL page: {}", countPage, url);
                                }


                                Elements elementsProduct = doc.select("ul.products li.product");
                                log.info("Count product of page: {}", elementsProduct.size());


                                elementsProduct
                                        .stream()
                                        .peek(ep -> {
                                            log.info(" - - - ");
                                            String urlProduct = ep.select("a").attr("href");
                                            log.info("Product url: {}", urlProduct);

                                            String title = ep.select("*[class=woocommerce-loop-product__title]").text();
                                            log.info("Product title: {}", title);

                                            String sku = ep.attr("class");
                                            int idxProductId = sku.indexOf("post-");
                                            sku = sku.substring(idxProductId, sku.indexOf(" ", idxProductId)).replaceAll("\\D", "");
                                            log.info("Product sku: {}", sku);

                                            String model = generateModel(sku, "0000");
                                            log.info("Product model: {}", model);

                                            Elements priceElement = ep.select("span.price");
                                            String price = "0.0";
                                            if (!priceElement.isEmpty()) {

                                                Elements bdi = priceElement.select("bdi");
                                                Elements ins = priceElement.select("ins");

                                                if (ins.isEmpty()) {
                                                    price = bdi.text().trim().replaceAll("[^(\\d.)]", "");
                                                } else {
                                                    price = ins.text().trim().replaceAll("[^(\\d.)]", "");
                                                }

                                            }

                                            log.info("Product price: {}", price);
                                            BigDecimal divide = new BigDecimal(price).divide(eurValue, 0, RoundingMode.HALF_UP);
                                            BigDecimal priceNumberFree = divide.setScale(4);
                                            log.info("Product priceNumberFree: {}", priceNumberFree);

                                            String urlImage = ep.select("img").attr("src").replaceAll("-350x350", "");
                                            log.info("Product image: {}", urlImage);


                                            ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                                                    .withSupplierId(supplierApp.getSupplierAppId())
                                                    .withSupplierApp(supplierApp)
                                                    .withCategoryId(categoryApp.getCategoryId())
                                                    .withCategoryApp(categoryApp)
                                                    .withUrl(urlProduct)
                                                    .withSku(sku)
                                                    .withTitle(title)
                                                    .withPrice(priceNumberFree)
                                                    .build();

                                            ProductOpencart product = new ProductOpencart.Builder()
                                                    .withProductProfileApp(productProfileApp)
                                                    .withModel(model)
                                                    .withUrlProduct(urlProduct)
                                                    .withUrlImage(urlImage)
                                                    .withTitle(title)
                                                    .withSku(sku)
                                                    .withJan(supplierApp.getName())
                                                    .withPrice(priceNumberFree)
                                                    .withItuaOriginalPrice(priceNumberFree)
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

                                        })
                                        .collect(Collectors.toList());

                                List<Integer> paginationPageList = doc.select("ul.page-numbers li")
                                        .stream()
                                        .map(ep -> ep.text().replaceAll("\\D", ""))
                                        .filter(p -> !p.isEmpty())
                                        .map(p -> Integer.parseInt(p))
                                        .collect(Collectors.toList());

                                countPage++;
                                hasNextPage = paginationPageList.contains(countPage);


                            } catch (Exception e) {
                                log.warn("Problem iterate page", e);
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

        List<ProductOpencart> fullProducts = products
                .stream()
                .peek(p -> {
                    String urlProduct = p.getUrlProduct();

                    Document webDocument = getWebDocument(urlProduct, new HashMap<>());
                    log.info("{}. Get data from product url: {}", count.addAndGet(1), urlProduct);

                    if (Objects.nonNull(webDocument)) {
                        try {

                            String title = p.getTitle();
                            title = translateService.getTranslatedText(title);
                            Elements manuElem = webDocument.select("tr.woocommerce-product-attributes-item.woocommerce-product-attributes-item--attribute_pa_proizvoditel a[href*='https://oskar.dp.ua/proizvoditel/']");
                            String manufacturer = "";

                            if (!manuElem.isEmpty()) {
                                manufacturer = manuElem.select("a").text().trim();
                            }

                            log.info("Manu elem: {}", manufacturer);

                            ManufacturerApp manufacturerApp = getManufacturerApp(manufacturer, supplierApp);
                            ProductProfileApp firstProfileApp = p.getProductProfileApp();
                            firstProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                            firstProfileApp.setManufacturerApp(manufacturerApp);
                            ProductProfileApp savedProductProfile = getProductProfile(firstProfileApp, supplierApp);
                            p.setProductProfileApp(savedProductProfile);

                            setManufacturer(p, supplierApp);
                            setPriceWithMarkup(p);

                            String description = getDescription(webDocument);


                            ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                    .withDescription(description)
                                    .withName(title)
                                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                    .build();
                            p.getProductsDescriptionOpencart().add(productDescriptionOpencart);

                            List<String> attributeRows = getCutAttribute(webDocument);

                            List<AttributeWrapper> attributes = attributeRows
                                    .stream()
                                    .map(row -> {
                                        List<String> dataAttribute = Arrays.asList(row.split("[—–：:]"));
                                        if (dataAttribute.size() == 2) {
                                            String keyAttr = dataAttribute.get(0).trim();
                                            String valueAttr = dataAttribute.get(1).trim();
                                            if (valueAttr.endsWith(";") || valueAttr.endsWith(".") || valueAttr.endsWith(",")) {
                                                valueAttr = valueAttr.substring(0, valueAttr.length() - 1);
                                            }
                                            log.info("Key: [{}]  Value: [{}]", keyAttr, valueAttr);
                                            AttributeWrapper attributeWrapper = new AttributeWrapper(keyAttr, valueAttr, null);
                                            AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                            return attributeWrapper;
                                        }
                                        return null;
                                    })
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                            p.getAttributesWrapper().addAll(attributes);


                            String url = p.getUrlImage();

                            String format = url.substring(url.lastIndexOf("."));
                            String imageName = p.getModel().concat("_main").concat(format);
                            String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);
                            downloadImage(url, dbImgPath);

                            p.setImage(dbImgPath);

                        } catch (Exception ex) {
                            log.warn("Bad parsing product data", ex);
                        }
                    } else {
                        p.setId(-1);
                    }

                })
                .filter(p -> p.getId() != -1)
                .collect(Collectors.toList());

        return fullProducts;

    }

    public void updateDescription() {
        Map<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk-ua");

        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductOpencart> productOpencartList = opencartDaoService.getAllProductOpencartBySupplierAppName(supplierApp.getName());
        log.info("Kodaki product opencart size: {}", productOpencartList.size());
        List<ProductProfileApp> productProfilesApp = supplierApp.getProductProfilesApp();
        log.info("Kodaki product profile size: {}", productProfilesApp.size());
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
                        Document webDocument = getWebDocument(url, cookies);
                        String description = getDescription(webDocument);
                        ProductDescriptionOpencart desc = new ProductDescriptionOpencart.Builder()
                                .withProductId(id)
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .withDescription(description)
                                .build();
                        opencartDaoService.updateDescription(desc);
                        log.info("Product id: {}", id);
                    } else {
                        log.warn("Product is NULL: {}", sku);
                    }

                });
    }

    @Override
    public String cleanDescription(Element descriptionElement) {
        descriptionElement.select("br").append("\n");
        descriptionElement.select("p").append("\n\n");
        descriptionElement.select("li").append("\n");
        List<String> lines = Arrays.asList(descriptionElement.wholeText().split("\n"));
        return lines
                .stream()
                .map(String::trim)
                .collect(Collectors.joining("\n")).replaceAll("\n{3,}", "\n\n").replaceAll("\\u00a0", "").trim();
    }

    public String getDescription(Document doc) {
        String descTxtKay = "Описание";
        String attrTxtKay = "арактеристики";
        String description = "";

        Elements descElement = doc.select("div#tab-description");
        if (!descElement.isEmpty()) {
//            Elements contactElement = descElement.select("p:matches(" + AppConstant.PHONE_NUMBER_REGEX + ")");
            Elements tableElement = descElement.select("table");
            Elements linkElement = descElement.select("p:has(a)");
            Elements iframeElement = descElement.select("iframe");


            if (!tableElement.isEmpty()) {
                tableElement.remove();
            }

            if (!linkElement.isEmpty()) {
                linkElement.remove();
            }

            if (!iframeElement.isEmpty()) {
                iframeElement.remove();
            }


            String dirtyDescriptionText = cleanDescription(descElement.get(0));
            dirtyDescriptionText = dirtyDescriptionText.replaceAll("Оскар", "Centur");
            int idxDesc = dirtyDescriptionText.indexOf(descTxtKay);
            int idxAttr = dirtyDescriptionText.indexOf(attrTxtKay);


            if (idxDesc != -1) {
                description = dirtyDescriptionText.substring(idxDesc + descTxtKay.length()).trim();
                if (idxAttr != -1) {
                    int idxReplace = description.indexOf(":", idxAttr);
                    if (idxReplace != -1) {
                        String firstPart = description.substring(0, idxReplace + 1).trim();
                        String lastPart = description.substring(idxReplace + 1).trim();
                        description = firstPart.concat("\n").concat(lastPart);
                    }
                }
                Pattern pattern = Pattern.compile(AppConstant.PHONE_NUMBER_REGEX);
                Matcher matcher = pattern.matcher(description);
                if (matcher.find()) {

                    List<String> collectExtraLine = Arrays.stream(description.split("[.\n]"))
                            .filter(ps -> pattern.matcher(ps).find())
                            .collect(Collectors.toList());

                    for (String el : collectExtraLine) {

                        description = description.replace(el, "");
                    }

                }

            } else {

            }

        }

        if (!description.isEmpty()) {
            description = translateService.getTranslatedText(description);
        }

        return wrapToHtml(description);
    }

    public List<String> getCutAttribute(Document doc) {

        String attrTxtKay = "арактеристики";
        String attribute = "";

        Elements descElement = doc.select("div#tab-description");
        if (!descElement.isEmpty()) {


            Elements contactElement = descElement.select("p:matches(" + AppConstant.PHONE_NUMBER_REGEX + ")");
            Elements tableElement = descElement.select("table");
            Elements linkElement = descElement.select("p:has(a)");
            Elements iframeElement = descElement.select("iframe");

            if (!contactElement.isEmpty()) {
                contactElement.remove();
            }

            if (!tableElement.isEmpty()) {
                tableElement.remove();
            }

            if (!linkElement.isEmpty()) {
                linkElement.remove();
            }

            if (!iframeElement.isEmpty()) {
                iframeElement.remove();
            }


            String dirtyAttributeText = cleanAttribute(descElement.get(0));
            int idxAttr = dirtyAttributeText.indexOf(attrTxtKay);


            if (idxAttr != -1) {
                attribute = dirtyAttributeText.substring(idxAttr + attrTxtKay.length()).trim();
                if (attribute.contains(":")) {
                    attribute = attribute.substring(attribute.indexOf(":") + 1).trim();
                }
            }


        }
        List<String> strings = Arrays.asList(attribute.split("\n"));

        List<String> attributeRows = strings
                .stream()
                .filter(s -> Pattern.matches(".+[—–：:].+", s))
                .map(String::trim)
                .collect(Collectors.toList());

        attributeRows.forEach(r -> log.info("row: {}", r));
        return attributeRows;
    }


    public String cleanAttribute(Element descriptionElement) {
        descriptionElement.select("br").append("\n");
        descriptionElement.select("p").append("\n");
        descriptionElement.select("li").append("\n");
        List<String> lines = Arrays.asList(descriptionElement.wholeText().split("\n"));
        return lines
                .stream()
                .map(String::trim)
                .collect(Collectors.joining("\n")).replaceAll("\n{3,}", "\n\n").replaceAll("\\u00a0", "").trim();
    }

    public String wrapToHtml(String text) {
        String result = text.replaceAll("\n", "</br>");
        return "<span class=\"centurapp\" style=\"white-space: pre-wrap; font-size: 16px; \">".concat(result).concat("</span>");
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
                Elements manuElem = webDocument.select("li:contains(Виробник: )");
                String manufacturer = "";
                if (!manuElem.isEmpty()) {
                    manufacturer = manuElem.select("a").text().trim();
                }
                log.info("Manu elem: {}", manufacturer);


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


    public void updateModel() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductProfileApp> profiles = appDaoService.getAllProductProfileAppBySupplierId(supplierApp.getSupplierAppId());
        profiles
                .forEach(p -> {
                    try {
                        String url = p.getUrl();
                        Document doc = getWebDocument(url, new HashMap<>());
                        if (Objects.nonNull(doc)) {
                            String content = doc.select("meta[itemprop=model]").attr("content");
                            String model = generateModel(content, String.valueOf(p.getProductProfileId()));

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

}
