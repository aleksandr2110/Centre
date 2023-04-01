package orlov.home.centurapp.service.parser;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.OpencartDto;
import orlov.home.centurapp.dto.api.astim.AstimCatalog;
import orlov.home.centurapp.dto.api.astim.Currency;
import orlov.home.centurapp.dto.api.astim.Offer;
import orlov.home.centurapp.dto.api.astim.Shop;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserServiceAstim extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "astim";
    private final String SUPPLIER_URL = "https://astim.in.ua/";
    private final String DISPLAY_NAME = "74 - АС-ТІМ";
    private final String SUPPLIER_URL_XML = "https://astim.in.ua/export/astim.xml";
    private final String SUPPLIER_URL_EXCEL = "https://astim.in.ua/datawork/downbasexls";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;

    private final UpdateDataService updateDataService;


    public ParserServiceAstim(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService) {
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


            supplierApp.setSiteCategories(siteCategories);

            List<ProductOpencart> fullProductsData = getFullProductsData(opencartInfo.getNewProduct(), supplierApp);

            checkPrice(opencartInfo, supplierApp);
            checkStockStatusId(opencartInfo, supplierApp);

            fullProductsData.forEach(opencartDaoService::saveProductOpencart);

            //:TODO update price in function checkPrice
            /*
            if (!opencartInfo.getNewProduct().isEmpty()) {
                updateDataService.updatePrice(supplierApp.getSupplierAppId());
            }*/

            updateProductSupplierOpencartBySupplierApp(supplierApp);

            Timestamp end = new Timestamp(Calendar.getInstance().getTime().getTime());
            orderProcessApp = opencartInfo.getOrderProcessApp();
            orderProcessApp.setStartProcess(start);
            orderProcessApp.setEndProcess(end);
            appDaoService.saveOrderDataApp(orderProcessApp);

        } catch (Exception ex) {
            log.warn("Exception parsing astim", ex);
        }
    }


    public void updateAttributeValue() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);
        List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);
        List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);
    }

    public AstimCatalog getOffersFromSiteAsExcel() {

        AstimCatalog astimCatalogExcel = new AstimCatalog();
        Shop shop = new Shop();
        astimCatalogExcel.setShop(shop);

        try (BufferedInputStream in =
                     new BufferedInputStream(new URL(SUPPLIER_URL_EXCEL).openStream())) {

            Workbook workbook = WorkbookFactory.create(in);
            Sheet sheet = workbook.getSheetAt(0);


            int offerCount = 0;

            for (int i = 0; offerCount != -1; i++) {
                Row row = sheet.getRow(i);

                if (i == 0) {
                    double euroRate = row.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL).getNumericCellValue();
                    log.info("Euro rate: {}", euroRate);
                    Currency currency = new Currency();
                    currency.setRate(euroRate);
                    currency.setId("EUR");
                    shop.setCurrencies(Collections.singletonList(currency));
                }


                if (i > 5) {

                    Offer offer = new Offer();

                    Cell cellCountOffer = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    offerCount = Objects.isNull(cellCountOffer) ? -1 : (int) cellCountOffer.getNumericCellValue();
//                    log.info("Offer count: {}", offerCount);

                    if (offerCount != -1) {
                        Cell cellArticle = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String article = Objects.isNull(cellArticle) ? "" : cellArticle.getStringCellValue();
//                        log.info("Article ( code/model ): {}", article);

                        offer.setCode(article);
                        Cell cellTitle = row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String title = Objects.isNull(cellTitle) ? "" : cellTitle.getStringCellValue();
//                        log.info("Title : {}", title);

                        Cell cellPriceEuro = row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        double priceEuro = Objects.isNull(cellPriceEuro) ? 0 : cellPriceEuro.getNumericCellValue();
//                        log.info("Price euro : {}", priceEuro);
                        offer.setPrice(priceEuro);

                        Cell cellProductSale = row.getCell(5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String productSale = Objects.isNull(cellProductSale) ? "" : cellProductSale.getStringCellValue();
//                        log.info("Sale: {}", productSale);
                        offer.setProductSale(productSale);

                        Cell cellProductStorehouse = row.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String productStorehouse = Objects.isNull(cellProductStorehouse) ? "" : cellProductStorehouse.getStringCellValue();
//                        log.info("Stock: {}", productStorehouse);
                        offer.setProductStorehouse(productStorehouse);

                        Cell cellProductOnWay = row.getCell(7, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String productOnWay = Objects.isNull(cellProductOnWay) ? "" : cellProductOnWay.getStringCellValue();
//                        log.info("On way: {}", productOnWay);
                        offer.setProductOnWay(productOnWay);

                        Cell cellProductUrl = row.getCell(8, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String productUrl = Objects.isNull(cellProductUrl) ? "" : cellProductUrl.getStringCellValue();
//                        log.info("Url: {}", productUrl);
                        offer.setUrl(productUrl);
                        shop.getOffers().add(offer);
                    }

//                    log.info("\n-  -  -  -  -  -\n");
                }

            }

            return astimCatalogExcel;
        } catch (IOException e) {
            log.error("Exception", e);
        }
        return null;

    }

    public AstimCatalog getOffersFromSiteAsXml() {
        try (BufferedInputStream in =
                     new BufferedInputStream(new URL(SUPPLIER_URL_XML).openStream())) {

            XmlMapper xml = new XmlMapper();
            AstimCatalog astimCatalog = xml.readValue(in, AstimCatalog.class);
            log.info("Asti date: {}", astimCatalog.getDate());
            Shop shop = astimCatalog.getShop();
            log.info("Shop name: {}", shop.getName());
            log.info("Shop company: {}", shop.getCompany());
            log.info("Shop url: {}", shop.getUrl());
            log.info("Shop currency size: {}:", shop.getCurrencies().size());
            log.info("Shop offer size: {}", shop.getOffers().size());
            return astimCatalog;
        } catch (IOException e) {
            log.error("Exception", e);
        }
        return null;
    }

    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        Document doc = getWebDocument(supplierApp.getUrl(), new HashMap<>());

        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select("div.footernew1 a")
                    .stream()
                    .map(ec -> {
                        String url = ec.attr("href");
                        String title = ec.text();
//                        log.info("Main site category title: {}, url: {}", title, url);
                        CategoryOpencart categoryOpencart = new CategoryOpencart.Builder()
                                .withUrl(SUPPLIER_URL.concat(url.replace("/", "")))
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

            CategoryOpencart categoryOpencartDefault = new CategoryOpencart.Builder()
                    .withParentCategory(supplierApp.getMainSupplierCategory())
                    .withParentId(supplierApp.getMainSupplierCategory().getCategoryId())
                    .withTop(false)
                    .withStatus(false)
                    .build();
            CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                    .withName("non")
                    .withDescription(supplierApp.getName())
                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                    .build();
            categoryOpencartDefault.getDescriptions().add(description);
            mainCategories.add(categoryOpencartDefault);

//            log.info("Main category size: {}", mainCategories.size());


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

//            siteCategoryList
//                    .forEach(c -> log.info("Full Site category: {}, subCategorySize: {}", c.getDescriptions().get(0).getName(), c.getCategoriesOpencart().size()));

            return siteCategoryList;

        }
        return new ArrayList<>();

    }

    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        if (Objects.isNull(category.getUrl())) {
            return category;
        }
        String url = category.getUrl();
        Document doc = getWebDocument(url, new HashMap<>());
        if (Objects.nonNull(doc)) {
            Elements subCateElement = doc.select("div.centercategory");
//            log.info("Main category by name '{}' has sub category: '{}'", category.getDescriptions().get(0).getName(), subCateElement.size());

            if (!subCateElement.isEmpty()) {
                List<CategoryOpencart> subCategories = subCateElement
                        .select("div.centercategory_bottomname")
                        .stream()
                        .map(el -> {
                            String subUrl = SUPPLIER_URL.concat(el.select("a").attr("href").replace("/", ""));
                            String subTitle = el.text();
//                            log.info("      Sub category: {} , {}", subTitle, subUrl);
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


    public void updateModel() {
        AstimCatalog astimCatalogXml = getOffersFromSiteAsXml();
        List<Offer> offersFromSiteAsXml = astimCatalogXml.getShop().getOffers();
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductProfileApp> profiles = appDaoService.getAllProductProfileAppBySupplierId(supplierApp.getSupplierAppId());
        offersFromSiteAsXml
                .stream()
                .forEach(of -> {
                    try {


                        String sku = of.getCode();
                        ProductProfileApp productProfileApp = new ProductProfileApp();
                        productProfileApp.setSku(of.getCode());
                        productProfileApp.setSupplierId(supplierApp.getSupplierAppId());
                        if (profiles.contains(productProfileApp)) {
                            ProductProfileApp getted = profiles.get(profiles.indexOf(productProfileApp));
                            int productProfileId = getted.getProductProfileId();

                            String model = generateModel(sku, String.valueOf(productProfileId));


                            ProductOpencart productOpencart = new ProductOpencart.Builder()
                                    .withModel(model)
                                    .withSku(of.getCode())
                                    .withJan(supplierApp.getName())
                                    .build();
                            opencartDaoService.updateModel(productOpencart);
                        }

                    } catch (Exception ex) {
                        log.warn("exception update.", ex);
                    }
                });

    }

    @Override
    public List<ProductOpencart> getProductsInitDataByCategory(List<CategoryOpencart> categoriesWithProduct, SupplierApp supplierApp) {

        AstimCatalog astimCatalogXml = getOffersFromSiteAsXml();
        List<Offer> offersFromSiteAsXml = astimCatalogXml.getShop().getOffers();
        AstimCatalog astimCatalogExcel = getOffersFromSiteAsExcel();
        List<Offer> offersFromSiteAsExcel = astimCatalogExcel.getShop().getOffers();

        double euroRate = astimCatalogExcel.getShop().getCurrencies().get(0).getRate();

        List<ProductOpencart> productsCategory = offersFromSiteAsXml
                .stream()
                .map(offer -> {

                    try {

                        int stockStatusId = 8;
                        BigDecimal price;
                        boolean contains = offersFromSiteAsExcel.contains(offer);

                        if (contains) {
                            offer.setFrom("excel");

                            Offer offerExcel = offersFromSiteAsExcel.get(offersFromSiteAsExcel.indexOf(offer));
                            price = BigDecimal.valueOf(euroRate * offerExcel.getPrice()).setScale(2, RoundingMode.UP).setScale(4);

                            if (!offerExcel.getProductSale().isEmpty()) {
                                stockStatusId = 7;
                            } else if (offerExcel.getProductSale().isEmpty() && !offerExcel.getProductOnWay().isEmpty()) {
                                stockStatusId = 6;
                            }

                        } else {
                            price = BigDecimal.valueOf(euroRate * offer.getPrice()).setScale(2, RoundingMode.UP).setScale(4);
                        }

                        String urlProduct = offer.getUrl();
                        String urlImage = offer.getPictures().size() > 0 ? offer.getPictures().get(0) : "";
                        String title = offer.getName();
                        String sku = offer.getCode();
                        String model = generateModel(sku, "0000");
                        List<String> subImagesUrl = offer.getPictures().size() > 0 ? offer.getPictures().stream().skip(1).collect(Collectors.toList()) : new ArrayList<>();
                        String vendor = offer.getVendor();
                        String param = offer.getParam();
                        String description = offer.getDescription();

                        String metaDescription = title.concat("  купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999");
                        if (metaDescription.length() > 255) {
                            metaDescription = "Купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999";
                        }

                        String metaTitle = "Купити ".concat(title).concat("  інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника.");
                        if (metaTitle.length() > 255) {
                            metaTitle = "Купити інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника.";
                        }

                        ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                .withDescription(description)
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .withName(title)
                                .withMetaH1(title)
                                .withMetaDescription(metaDescription)
                                .withMetaTitle(metaTitle)
                                .withMetaKeyword("Купити ".concat(title))
                                .build();

                        ManufacturerApp manufacturerApp;
                        if (Objects.nonNull(vendor)) {
                            manufacturerApp = getManufacturerApp(vendor, supplierApp);
                        } else {
                            manufacturerApp = getManufacturerApp("non", supplierApp);
                        }

                        ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                                .withManufacturerId(manufacturerApp.getManufacturerId())
                                .withManufacturerApp(manufacturerApp)
                                .withTitle(title)
                                .withSku(sku)
                                .withUrl(urlProduct)
                                .withSupplierApp(supplierApp)
                                .withSupplierId(supplierApp.getSupplierAppId())
                                .withPrice(price)
                                .build();

                        List<AttributeWrapper> attributeWrapper = getAttributeWrapper(param, supplierApp);

                        ProductOpencart product = new ProductOpencart.Builder()
                                .withSku(sku)
                                .withJan(supplierApp.getName())
                                .withPrice(price)
                                .withTitle(title)
                                .withItuaOriginalPrice(price)
                                .withStockStatusId(stockStatusId)
                                .withProductProfileApp(productProfileApp)
                                .withAttributesWrapper(attributeWrapper)
                                .withProductsDescriptionOpencart(Collections.singletonList(productDescriptionOpencart))
                                .withUrlProduct(urlProduct)
                                .withUrlImage(urlImage)
                                .withSubImages(subImagesUrl)
                                .withModel(model)
                                .build();

                        if (Objects.nonNull(vendor)) {
                            setManufacturer(product, supplierApp);
                        }

                        return product;
                    } catch (Exception e) {
                        log.warn("Exception get init data", e);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return productsCategory;
    }

    public String getTitle(String title) {
        char f = 'і';
        char F = 'І';
        char s = 'ї';
        char S = 'Ї';
        char t = 'є';
        char T = 'Є';
        boolean contains = title.indexOf(f) > 0
                || title.indexOf(F) > 0
                || title.indexOf(s) > 0
                || title.indexOf(S) > 0
                || title.indexOf(t) > 0
                || title.indexOf(T) > 0;
        if (contains) {
            log.info("TITLE LANGUAGE UK");
        } else {
            log.info("TITLE LANGUAGE RU");
            title = translateService.getTranslatedText(title);
        }
        return title;
    }

    public String getDescription(Document doc) {
        Elements table = doc.select("table").remove();
        String cleaned = cleanDescription(doc);
        char f = 'і';
        char F = 'І';
        char s = 'ї';
        char S = 'Ї';
        char t = 'є';
        char T = 'Є';
        boolean contains = cleaned.indexOf(f) > 0
                || cleaned.indexOf(F) > 0
                || cleaned.indexOf(s) > 0
                || cleaned.indexOf(S) > 0
                || cleaned.indexOf(t) > 0
                || cleaned.indexOf(T) > 0;
        if (contains) {
            log.info("DESC LANGUAGE UK");
        } else if (!cleaned.isEmpty()) {
            log.info("DESC LANGUAGE RU");
            cleaned = translateService.getTranslatedText(cleaned);
        }
        return wrapToHtml(cleaned);
    }

    public List<AttributeWrapper> getAttributeWrapper(String param, SupplierApp supplierApp) {
        List<AttributeWrapper> result = new ArrayList<>();

        if (param.isEmpty()) {
            return result;
        } else {
            Document doc = Jsoup.parse(param.replaceAll("&nbsp;", ""));
            if (!doc.select("table.product__desc__profile_table").isEmpty()) {
                result = doc.select("tr")
                        .stream()
                        .map(row -> {

                            String line = row.select("th").text();
                            if (line.contains(":")) {
                                String key = line.substring(0, line.indexOf(":")).replaceAll("&nbsp;", "").trim();
                                String value = line.substring(line.indexOf(":") + 1).replaceAll("&nbsp;", "").trim();
                                if (value.isEmpty()) {
                                    value = row.select("h3").text().trim();
                                }
                                AttributeWrapper attributeWrapper = new AttributeWrapper(key, value, null);
//                                AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, sav);
                                if (key.isEmpty() || value.isEmpty())
                                    attributeWrapper = null;
                                return attributeWrapper;
                            }

                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else if (!doc.select("table:not([class])").isEmpty()) {

            } else {

                String textHTML = cleanJsoupAttribute(Jsoup.parse(param));
                log.info("Parameters cleaned text: {}", textHTML);
                result = Arrays.stream(textHTML.split("\n"))
                        .map(line -> {

                            if (line.contains(":")) {
                                String key = line.substring(0, line.indexOf(":")).replaceAll("&nbsp;", " ").trim();
                                String value = line.substring(line.indexOf(":") + 1).replaceAll("&nbsp;", " ").trim();
                                if (key.length() > 50 || value.length() > 100) {
                                    key = "";
                                    value = "";
                                }
                                AttributeWrapper attributeWrapper = new AttributeWrapper(key, value, null);
//                                AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp);
                                if (key.isEmpty() || value.isEmpty())
                                    attributeWrapper = null;
                                return attributeWrapper;
                            } else if (line.contains("-")) {
                                String key = line.substring(0, line.indexOf("-")).replaceAll("&nbsp;", " ").trim();
                                String value = line.substring(line.indexOf("-") + 1).replaceAll("&nbsp;", " ").trim();
                                if (key.length() > 50 || value.length() > 100) {
                                    key = "";
                                    value = "";
                                }
                                AttributeWrapper attributeWrapper = new AttributeWrapper(key, value, null);
//                                AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp);
                                if (key.isEmpty() || value.isEmpty())
                                    attributeWrapper = null;
                                return attributeWrapper;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

            }


        }

        return result;
    }


    public String cleanJsoupAttribute(Element attributeElement) {
        attributeElement.select("br").append("\n");
        attributeElement.select("p").append("\n");
        List<String> lines = Arrays.asList(attributeElement.wholeText().split("\n"));
        return lines
                .stream()
                .map(String::trim)
                .collect(Collectors.joining("\n")).replaceAll("\n{2,}", "\n").replaceAll("&nbsp;", "").trim();
    }

    @Override
    public List<ProductOpencart> getFullProductsData(List<ProductOpencart> products, SupplierApp supplierApp) {
        AtomicInteger countProduct = new AtomicInteger();
        List<CategoryOpencart> siteCategories = supplierApp.getSiteCategories();
        List<ProductOpencart> productsReady = products
                .stream()
                .map(p -> {

                    String urlProduct = p.getUrlProduct();
                    Document doc = getWebDocument(urlProduct, new HashMap<>());

                    if (Objects.nonNull(doc)) {
                        try {
                            Elements catElems = doc.select("div[itemprop='itemListElement']");
                            String subCategoryName = catElems.get(catElems.size() - 2).text().trim();

                            CategoryOpencart subCategory = siteCategories
                                    .stream()
                                    .filter(sc -> sc.getDescriptions().get(0).getName().equals(subCategoryName))
                                    .findFirst()
                                    .orElse(null);

                            if (Objects.isNull(subCategory)) {
                                subCategory = siteCategories
                                        .stream()
                                        .filter(sc -> sc.getDescriptions().get(0).getName().equals("non"))
                                        .findFirst()
                                        .orElse(null);
                            }

                            List<CategoryOpencart> parentsCategories = getParentsCategories(subCategory, siteCategories);
                            p.setCategoriesOpencart(parentsCategories);

                            String urlImage = p.getUrlImage();
                            String imgName = urlImage.substring(urlImage.lastIndexOf("/") + 1);
                            String imgNameDb = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
                            p.setImage(imgNameDb);
                            downloadImage(urlImage, imgNameDb);

                            List<String> subImages = p.getSubImages();
                            AtomicInteger countImg = new AtomicInteger();
                            List<ImageOpencart> images = subImages
                                    .stream()
                                    .map(ui -> {

                                        String subImgName = ui.substring(urlImage.lastIndexOf("/") + 1);
                                        String subImgNameDb = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(subImgName);
                                        downloadImage(ui, subImgNameDb);
                                        return new ImageOpencart.Builder()
                                                .withImage(subImgNameDb)
                                                .withSortOrder(countImg.getAndIncrement())
                                                .build();

                                    })
                                    .collect(Collectors.toList());

                            p.setImagesOpencart(images);

                            CategoryApp categoryApp = getCategoryApp(subCategoryName, supplierApp);


                            ProductDescriptionOpencart productDescriptionOpencart = p.getProductsDescriptionOpencart().get(0);
                            String name = productDescriptionOpencart.getName();
                            String description = productDescriptionOpencart.getDescription();

                            name = getTitle(name);
                            description = getDescription(Jsoup.parse(description));

                            String metaDescription = name.concat("  купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999");
                            if (metaDescription.length() > 255) {
                                metaDescription = "Купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999";
                            }

                            String metaTitle = "Купити ".concat(name).concat("  інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника.");
                            if (metaTitle.length() > 255) {
                                metaTitle = "Купити інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника.";
                            }

                            ProductDescriptionOpencart translatedProductDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                    .withDescription(description)
                                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                    .withName(name)
                                    .withMetaH1(name)
                                    .withMetaDescription(metaDescription)
                                    .withMetaTitle(metaTitle)
                                    .withMetaKeyword("Купити ".concat(name))
                                    .build();
                            p.setProductsDescriptionOpencart(Collections.singletonList(translatedProductDescriptionOpencart));


                            ProductProfileApp productProfileApp = p.getProductProfileApp();
                            productProfileApp.setCategoryApp(categoryApp);
                            productProfileApp.setCategoryId(categoryApp.getCategoryId());
                            productProfileApp.setTitle(name);
                            ProductProfileApp savedProductProfile = getProductProfile(productProfileApp, supplierApp);
                            log.info("{}. productProfileFull: {}", countProduct.addAndGet(1), savedProductProfile);
                            p.setProductProfileApp(savedProductProfile);
                            setPriceWithMarkup(p);

                            List<AttributeWrapper> attributeWrappers = p.getAttributesWrapper()
                                    .stream()
                                    .peek(attrWrapp -> {
                                        getAttribute(attrWrapp, supplierApp, savedProductProfile);
                                    })
                                    .collect(Collectors.toList());
                            p.setAttributesWrapper(attributeWrappers);
                            log.info("\n-  -  -  -  -  -  -\n");
                        } catch (Exception ex) {
                            log.warn("Bad parsing product");
                        }
                        return p;
                    } else {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return productsReady;
    }


    @Override
    public ProductProfileApp getProductProfile(ProductProfileApp productProfileApp, SupplierApp supplierApp) {
        List<ProductProfileApp> productProfilesAppDB = supplierApp.getProductProfilesApp();

        boolean contains = productProfilesAppDB.contains(productProfileApp);

        if (contains) {
            productProfileApp = productProfilesAppDB.get(productProfilesAppDB.indexOf(productProfileApp));
        } else {
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
