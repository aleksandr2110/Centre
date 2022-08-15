package orlov.home.centurapp.service.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.ImageData;
import orlov.home.centurapp.dto.OpencartDto;
import orlov.home.centurapp.dto.api.sector.CharacteristicSector;
import orlov.home.centurapp.dto.api.sector.EditionSector;
import orlov.home.centurapp.dto.api.sector.ProductSector;
import orlov.home.centurapp.dto.api.sector.ProductSectorWrapper;
import orlov.home.centurapp.entity.app.*;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.service.api.translate.TranslateService;
import orlov.home.centurapp.service.appservice.FileService;
import orlov.home.centurapp.service.appservice.ScraperDataUpdateService;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;
import orlov.home.centurapp.util.AppConstant;
import orlov.home.centurapp.util.OCConstant;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserServiceSector extends ParserServiceAbstract {


    private final String SUPPLIER_NAME = "sector";
    private final String SUPPLIER_URL = "https://sector.org.ua/";
    private final String SUPPLIER_CATALOG_URL = "https://sector.org.ua/catalog";
    private final String DISPLAY_NAME = "71 - СЕКТОР";
    private final String MANUFACTURER_NAME = "non";
    private final String URL_API = "https://store.tildacdn.com/api/getproductslist/?storepartuid=";


    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;


    public ParserServiceSector(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, ObjectMapper objectMapper) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService;
        this.opencartDaoService = opencartDaoService;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doProcess() {
        try {


            OrderProcessApp orderProcessApp = new OrderProcessApp();
            Timestamp start = new Timestamp(Calendar.getInstance().getTime().getTime());

            SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
            List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);


            List<ProductOpencart> productInitData = getProductsInitDataByCategory(siteCategories, supplierApp);

            OpencartDto opencartInfo = getOpencartInfo(productInitData, supplierApp);
            checkPrice(opencartInfo, supplierApp);
            List<ProductOpencart> newProduct = opencartInfo.getNewProduct();

            newProduct
                    .forEach(opencartDaoService::saveProductOpencart);

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


    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        Document doc = getWebDocument(SUPPLIER_CATALOG_URL, new HashMap<>());

        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select("a.tn-atom, a.t686__link")
                    .stream()
                    .filter(l -> !l.attr("href").contains("https://www."))
                    .map(ec -> {
                        String url = ec.attr("href");
                        if (!url.contains("https://sector.org.ua/")) {
                            url = SUPPLIER_URL.concat(url.replace("/", ""));
                        }
                        return url;
                    })
                    .distinct()
                    .map(url -> {

                        if (!url.contains("https://sector.org.ua/")) {
                            url = SUPPLIER_URL.concat(url.replace("/", ""));
                        }

                        Document catDoc = getWebDocument(url, new HashMap<>());
                        String title = catDoc.select("a.t-menu__link-item.t758__link-item_active").text().trim();

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
        int supplierAppId = supplierApp.getSupplierAppId();
        List<ProductOpencart> productResult = new ArrayList<>();
        try {
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
                        log.info("Get info by url: {}", url);

                        Document doc = getWebDocument(url, new HashMap<>());
                        Elements scripts = doc.getElementsByTag("script");

                        String subStringForStorepart = "storepart:";
                        String storepart = scripts
                                .stream()
                                .map(Element::outerHtml)
                                .filter(t -> t.contains(subStringForStorepart))
                                .findFirst()
                                .orElse("! non");

                        List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);

                        if (!storepart.equals("! non")) {

                            try {

                                int startIdx = storepart.indexOf(subStringForStorepart);
                                int endIdx = storepart.indexOf(",", startIdx + subStringForStorepart.length());
                                storepart = storepart.substring(startIdx + subStringForStorepart.length(), endIdx).replaceAll("\\D", "");
                                log.info("final result storepart: {}", storepart);


                                String apiUrl = URL_API.concat(storepart);
                                List<ProductSector> productsByApi = getProductsByApiUrl(apiUrl);
                                List<ProductOpencart> productOpencartList = productsByApi
                                        .stream()
                                        .distinct()
                                        .map(papi -> {
                                            String urlProduct = papi.getUrl();
                                            String title = papi.getTitle();
                                            String sku = papi.getExternalid();
                                            String brand = papi.getBrand();
                                            brand = Objects.nonNull(brand) ? brand : MANUFACTURER_NAME;
                                            String description = papi.getText();

                                            EditionSector edition = papi.getEditionSectorList().get(0);
                                            String price = edition.getPrice();
                                            price = Objects.nonNull(price) ? price.replaceAll(" ", "") : "0";
                                            String mainImgUrl = edition.getImg();
                                            BigDecimal priceNumberFree = new BigDecimal(price).setScale(4);

                                            log.info("url: {}", urlProduct);
                                            log.info("title: {}", title);
                                            log.info("sku: {}", sku);
                                            log.info("brand: {}", brand);
                                            log.info("price: {}", priceNumberFree);
                                            log.info("description: {}", description);
                                            String supplierModel = papi.getSku();

                                            ManufacturerApp manufacturerApp = getManufacturerApp(brand, supplierApp);

                                            ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                                                    .withSupplierId(supplierApp.getSupplierAppId())
                                                    .withSupplierApp(supplierApp)
                                                    .withCategoryId(categoryApp.getCategoryId())
                                                    .withCategoryApp(categoryApp)
                                                    .withUrl(urlProduct)
                                                    .withSku(sku)
                                                    .withTitle(title)
                                                    .withPrice(priceNumberFree)
                                                    .withManufacturerId(manufacturerApp.getManufacturerId())
                                                    .withManufacturerApp(manufacturerApp)
                                                    .build();
                                            ProductProfileApp savedProductProfileApp = getProductProfile(productProfileApp, supplierApp);


                                            String model = generateModel(supplierModel, String.valueOf(savedProductProfileApp.getProductProfileId()));
                                            log.info("Product profile: {}", savedProductProfileApp);

                                            Document parse = Jsoup.parse(description);
                                            parse = Jsoup.parse("<!DOCTYPE html>".concat(parse.outerHtml()));
                                            log.info("parse: {}", parse);
                                            description = getDescription(parse);
                                            log.info("Desc after clean: {}", description);

                                            ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                                    .withDescription(description)
                                                    .withName(title)
                                                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                                    .withMetaH1(title)
//                                                    .withMetaDescription(title.concat("  купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999"))
                                                    .withMetaTitle("Купити ".concat(title).concat("  інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника."))
                                                    .withMetaKeyword("Купити ".concat(title))
                                                    .build();

                                            ProductOpencart product = new ProductOpencart.Builder()
                                                    .withProductProfileApp(savedProductProfileApp)
                                                    .withSku(sku)
                                                    .withModel(model)
                                                    .withJan(supplierApp.getName())
                                                    .withPrice(priceNumberFree)
                                                    .withItuaOriginalPrice(priceNumberFree)
                                                    .withProductsDescriptionOpencart(Collections.singletonList(productDescriptionOpencart))
                                                    .build();


                                            product.setCategoriesOpencart(parentsCategories);


                                            setManufacturer(product, supplierApp);

                                            List<CharacteristicSector> characteristics = papi.getCharacteristicSectorList();
                                            List<AttributeWrapper> attributes = characteristics
                                                    .stream()
                                                    .map(charac -> {
                                                        AttributeWrapper attributeWrapper = new AttributeWrapper(charac.getTitle(), charac.getValue(), null);
                                                        return getAttribute(attributeWrapper, supplierApp, savedProductProfileApp);
                                                    })
                                                    .collect(Collectors.toList());
                                            product.setAttributesWrapper(attributes);

                                            AtomicInteger countImage = new AtomicInteger();
                                            List<ImageOpencart> productImages = papi.getImagesUrlList()
                                                    .stream()
                                                    .map(i -> {
                                                        String imgName = sku.concat("_").concat(String.valueOf(countImage.addAndGet(1))).concat(i.substring(i.lastIndexOf(".")));
                                                        String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imgName);
                                                        downloadImage(i, dbImgPath);

                                                        if (mainImgUrl.equals(i)) {
                                                            product.setImage(dbImgPath);
                                                            return null;
                                                        } else {
                                                            return new ImageOpencart.Builder()
                                                                    .withImage(dbImgPath)
                                                                    .withSortOrder(countImage.get())
                                                                    .build();
                                                        }

                                                    })
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.toList());

                                            product.setImagesOpencart(productImages);
                                            return product;
                                        })
                                        .collect(Collectors.toList());

                                productResult.addAll(productOpencartList);

                            } catch (Exception e) {
                                log.warn("Exception", e);
                            }
                        }

                    })
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.warn("Exception", ex);
        }

        return productResult.stream().distinct().collect(Collectors.toList());
    }

    public List<ProductSector> getProductsByApiUrl(String apiUrl) {
        List<ProductSector> productSectorList = new ArrayList<>();
        try {
            Connection.Response response = Jsoup.connect(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 AppleWebKit/537.36 (KHTML," +
                            " like Gecko) Chrome/45.0.2454.4 Safari/537.36")
                    .method(Connection.Method.GET)
                    .maxBodySize(1_000_000 * 30) // 30 mb ~
                    .timeout(0) // infinite timeout
                    .execute();

            String body = response.body();
            ProductSectorWrapper productSectorWrapper = objectMapper.readValue(body, ProductSectorWrapper.class);
            log.info("Body: {}", body);

            if (Objects.nonNull(productSectorWrapper)) {
                productSectorList = productSectorWrapper.getProductSectorList()
                        .stream()
                        .peek(p -> {
                            String gallery = p.getGallery();
                            p.setImagesUrlList(getImagesUrlFromGallery(gallery));
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception ex) {
            log.warn("Exception sector api", ex);
        }

        return productSectorList;
    }

    public List<String> getImagesUrlFromGallery(String gallery) {
        List<String> imagesUrl = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{(.*?)}");
        Matcher matcher = pattern.matcher(gallery);
        while (matcher.find()) {
            String row = matcher.group();
            if (row.contains(".jpg\"}")) {
                int startIdx = row.indexOf("http");
                int endIdx = row.indexOf("\"}");
                String img = row.substring(startIdx, endIdx).replaceAll("\\\\", "");
                imagesUrl.add(img);
            }
        }
        return imagesUrl;
    }

    //    TODO test attribute value update
    public void updateAttributeValue() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);
        List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);
        List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);
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

                            String name = webDocument.select("span[data-ui-id=page-title-wrapper]").text();

                            String description = getDescription(webDocument);
                            log.info("Description final: {}", description);
                            ManufacturerApp manufacturerApp = getManufacturerApp(MANUFACTURER_NAME, supplierApp);
                            ProductProfileApp firstProfileApp = p.getProductProfileApp();
                            firstProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                            firstProfileApp.setManufacturerApp(manufacturerApp);
                            ProductProfileApp savedProductProfile = getProductProfile(firstProfileApp, supplierApp);
                            p.setProductProfileApp(savedProductProfile);

                            setManufacturer(p, supplierApp);
                            setPriceWithMarkup(p);

                            ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                    .withDescription(description)
                                    .withName(name)
                                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                    .withMetaH1(name)
                                    .withMetaDescription(name.concat("  купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999"))
                                    .withMetaTitle("Купити ".concat(name).concat("  інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника."))
                                    .withMetaKeyword("Купити ".concat(name))
                                    .build();
                            p.getProductsDescriptionOpencart().add(productDescriptionOpencart);


                            Elements tableAttr = webDocument.select("table#product-attribute-specs-table-left tr");

                            List<AttributeWrapper> attributes = tableAttr
                                    .stream()
                                    .map(row -> {
                                        String th = row.select("th").text();
                                        String td = row.select("td").text();
                                        log.info("");
                                        AttributeWrapper attributeWrapper = new AttributeWrapper(th, td, null);
                                        log.info("Begin attribute: {}", attributeWrapper);
                                        AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                        log.info("Finish attribute: {}", attribute);
                                        return attribute;
                                    })
                                    .collect(Collectors.toList());
                            p.getAttributesWrapper().addAll(attributes);


                            Elements imagesElement = webDocument.select("script:containsData(mage/gallery/gallery)");
                            if (imagesElement.size() > 0) {
                                String scriptGallery = imagesElement.toString();
                                String beginPart = "\"data\":[";
                                int begin = scriptGallery.indexOf(beginPart) + beginPart.length() - 1;
                                int finish = scriptGallery.indexOf("]", begin) + 1;
                                String jsonGallery = scriptGallery.substring(begin, finish);
                                ObjectMapper objectMapper = new ObjectMapper();

                                try {
                                    List<ImageData> imageData = Arrays.asList(objectMapper.readValue(jsonGallery, ImageData[].class));

                                    List<ImageOpencart> productImages = imageData
                                            .stream()
                                            .map(i -> {
                                                String fullUrl = i.getFull();

                                                String sku = p.getSku().replaceAll("\\W", "_");

                                                String imgName = sku.concat("_").concat(String.valueOf(i.getPosition())).concat(fullUrl.substring(fullUrl.lastIndexOf(".")));

                                                String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imgName);
                                                downloadImage(fullUrl, dbImgPath);

                                                if (i.getIsMain()) {
                                                    p.setImage(dbImgPath);
                                                    return null;
                                                }

                                                ImageOpencart imageOpencart = new ImageOpencart.Builder()
                                                        .withImage(dbImgPath)
                                                        .withSortOrder(i.getPosition())
                                                        .build();
                                                return imageOpencart;
                                            })
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());

                                    p.getImagesOpencart().addAll(productImages);


                                } catch (Exception e) {
                                    log.warn("Problem get images", e);
                                }

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
        String description = cleanDescription(doc);
        return wrapToHtml(description);
    }

    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();
        Document doc = getWebDocument(url, new HashMap<>());
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {
            Element subCateElement = doc.select("not sub category").parents().first();
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

    public void updateModel() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);
        List<ProductOpencart> profiles = getProductsInitDataByCategory(siteCategories, supplierApp);
        profiles
                .forEach(opencartDaoService::updateModel);
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


}
