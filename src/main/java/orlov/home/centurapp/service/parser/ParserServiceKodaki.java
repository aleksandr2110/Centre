package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.omg.CORBA.Object;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.OpencartDto;
import orlov.home.centurapp.entity.app.*;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.service.api.translate.TranslateService;
import orlov.home.centurapp.service.appservice.FileService;
import orlov.home.centurapp.service.appservice.ScraperDataUpdateService;
import orlov.home.centurapp.service.daoservice.app.*;
import orlov.home.centurapp.service.daoservice.opencart.*;
import orlov.home.centurapp.util.AppConstant;
import orlov.home.centurapp.util.OCConstant;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserServiceKodaki extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "kodaki";
    private final String SUPPLIER_URL = "https://kodaki.ua/";
    private final String DISPLAY_NAME = "8 - КОДАКИ";
    private final String URL_PART_PAGE = "?page=";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;

    public ParserServiceKodaki(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService;
        this.opencartDaoService = opencartDaoService;
        this.translateService = translateService;
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
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk-ua");
        Document doc = getWebDocument(supplierApp.getUrl(), cookies);

        if (Objects.nonNull(doc)) {

            List<CategoryOpencart> mainCategories = doc.select("ul.nav > li > a")
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
                    .filter(c -> c.getUrl().startsWith("https"))
                    .peek(c -> log.info("url category: {}", c.getUrl()))
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

    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk-ua");
        Document doc = getWebDocument(url, cookies);
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {

            Elements subCateElements = doc.select("div.row.list-category li a");

            List<CategoryOpencart> subCategories = subCateElements
                    .stream()
                    .map(el -> {

                        String href = el.attr("href");

                        log.info("Sub href: {}", href);
                        String title = el.select("span").text().trim();
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
                    HashMap<String, String> cookies = new HashMap<>();
                    cookies.put("language", "uk-ua");
                    Document doc = getWebDocument(url, cookies);

                    List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);
                    log.info("parentsCategories size: {}", parentsCategories.size());

                    if (Objects.nonNull(doc)) {

                        int numberPagesCategory = 1;
                        Elements perPageElement = doc.select("div#content div.col-sm-6.text-right");
                        log.info("perPageElement: {}", perPageElement.size());

                        if (!perPageElement.isEmpty()) {
                            String textPerPage = perPageElement.text();
                            log.info("textPerPage: {}", textPerPage);
                            String textPerPageCount = textPerPage.substring(textPerPage.indexOf("("), textPerPage.indexOf(")")).replaceAll("\\D", "");
                            numberPagesCategory = Integer.parseInt(textPerPageCount);
                        }

                        log.info("numberPagesCategory: {}", numberPagesCategory);

                        int countPage = 1;

                        while (countPage <= numberPagesCategory) {
                            try {
                                if (countPage != 1) {
                                    String newUrlPage = url.concat(URL_PART_PAGE).concat(String.valueOf(countPage));

                                    doc = getWebDocument(newUrlPage, cookies);
                                }

                                Elements elementsProduct = doc.select("div.list-product div.product-layout");
                                log.info("elementsProduct: {}", elementsProduct.size());

                                elementsProduct
                                        .stream()
                                        .peek(ep -> {
                                            String urlProduct = ep.select("div.caption span.h4 a").attr("href");
                                            log.info("Product url: {}", urlProduct);

                                            String title = ep.select("div.caption span.h4").text();
                                            log.info("Product title: {}", title);
                                            boolean contains = urlProduct.contains("product_id=");
                                            String sku = "";
                                            if (contains) {
                                                sku = urlProduct.substring(urlProduct.lastIndexOf("=") + 1);
                                            } else {
                                                sku = urlProduct.substring(urlProduct.lastIndexOf("/") + 1);

                                            }
                                            if (sku.length() > 64) {
                                                sku = sku.substring(sku.length() - 64);
                                            }
                                            log.info("Product sku: {}", sku);

                                            Elements priceElement = ep.select("p.price");
                                            String price = "0.0";
                                            if (!priceElement.isEmpty() && !priceElement.text().isEmpty()) {
                                                price = priceElement.text().replaceAll("[^\\d.]", "");
                                            }

                                            log.info("Product price: {}", price);
                                            BigDecimal priceNumberFree = new BigDecimal(price).setScale(4);
                                            log.info("Product priceNumberFree: {}", priceNumberFree);

                                            String urlImage = ep.select("img.img-responsive.imagess").attr("src").replaceAll("228x228", "500x500");

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


                            } catch (Exception e) {
                                log.warn("Problem iterate page", e);
                            } finally {
                                countPage++;
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
                    HashMap<String, String> cookies = new HashMap<>();
                    cookies.put("language", "uk-ua");
                    Document webDocument = getWebDocument(urlProduct, cookies);
                    log.info("{}. Get data from product url: {}", count.addAndGet(1), urlProduct);

                    if (Objects.nonNull(webDocument)) {
                        try {
                            Elements mainElement = webDocument.select("div#content div.product");
                            String name = mainElement.select("div.col-sm-9 h1").text().trim();
                            log.info("name: {}", name);

                            Elements manuElem = webDocument.select("li:contains(Виробник: )");
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
                            log.info("Saved PP kpdaki: {}", savedProductProfile);
                            p.setProductProfileApp(savedProductProfile);


                            String content = webDocument.select("meta[itemprop=model]").attr("content");
                            String model = generateModel(content, String.valueOf(savedProductProfile.getProductProfileId()));
                            p.setModel(model);
                            setManufacturer(p, supplierApp);
                            setPriceWithMarkup(p);


                            String description = getDescription(webDocument);


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


                            Elements tableAttr = webDocument.select("div.texdan p");
                            log.info("tableAttr: {}", tableAttr.size());
                            List<AttributeWrapper> attributes = tableAttr
                                    .stream()
                                    .map(row -> {
                                        String keyAttr = row.select("span").text().trim().replaceAll(":", "");
                                        String valueAttr = row.text().replaceAll(keyAttr.concat(":"), "").trim();
                                        log.info("Key: {}     Value: {}", keyAttr, valueAttr);

                                        AttributeWrapper attributeWrapper = new AttributeWrapper(keyAttr, valueAttr, null);
                                        log.info("Init attribute: {}", attributeWrapper);
                                        AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                        log.info("Final attribute: {}", attribute);

                                        return attributeWrapper;
                                    })
                                    .collect(Collectors.toList());
                            p.getAttributesWrapper().addAll(attributes);


                            Elements imagesElement = webDocument.select("a.cloud-zoom-gallery");
                            log.info("imagesElement: {}", imagesElement.size());

                            if (imagesElement.size() > 0) {

                                AtomicInteger countImage = new AtomicInteger();
                                List<String> urlImageList = new ArrayList<>();
                                List<ImageOpencart> productImages = imagesElement
                                        .stream()
                                        .map(ie -> {


                                            String url = ie.attr("href");

                                            boolean containsImage = urlImageList.contains(url);
                                            if (containsImage) {
                                                return null;
                                            } else {
                                                urlImageList.add(url);
                                            }

                                            String format = url.substring(url.lastIndexOf("."));
                                            String imageName = p.getSku().concat("_").concat(String.valueOf(countImage.addAndGet(1))).concat(format);
                                            String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);
                                            log.info("image url: {}", url);
                                            log.info("image name: {}", imageName);
                                            log.info("dbImg path: {}", dbImgPath);
                                            downloadImage(url, dbImgPath);

                                            if (countImage.get() == 1) {
                                                p.setImage(dbImgPath);
                                                return null;
                                            }

                                            ImageOpencart imageOpencart = new ImageOpencart.Builder()
                                                    .withImage(dbImgPath)
                                                    .withSortOrder(countImage.get())
                                                    .build();
                                            return imageOpencart;
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());

                                p.getImagesOpencart().addAll(productImages);


                            }

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

    public String getDescription(Document doc) {

        Elements descElement = doc.select("div#tab-description");
        String description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
        log.info("Description UA text: {}", description);

        if (description.isEmpty()) {
            String url = doc.baseUri();
            Map<String, String> cookies = new HashMap<>();
            cookies.put("language", "ru-ru");

            Document ruDoc = getWebDocument(url, cookies);
            descElement = ruDoc.select("div#tab-description");
            description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
            log.info("Description RU text: {}", description);

            if (description.length() > 0) {
                description = translateService.getTranslatedText(description);
                log.info("Translated RU-UK text: '{}'", description);
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
                HashMap<String, String> cookies = new HashMap<>();
                cookies.put("language", "uk-ua");
                Document webDocument = getWebDocument(productProfileApp.getUrl(), cookies);
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
