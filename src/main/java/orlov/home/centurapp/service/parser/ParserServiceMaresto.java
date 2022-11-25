package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
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
import orlov.home.centurapp.service.daoservice.validator.HttpsUrlValidator;
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
public class ParserServiceMaresto extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "maresto";
    private final String SUPPLIER_URL = "https://maresto.ua/ua/";
    private final String DISPLAY_NAME = "3 - МАРЕСТО";
    private final String URL_PART_PAGE = "?PAGEN_6=";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;

    public ParserServiceMaresto(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, FileService fileService1) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService;
        this.opencartDaoService = opencartDaoService;
        this.translateService = translateService;

        this.fileService = fileService1;
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

    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {
        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        String su = supplierApp.getUrl();
        log.info("su: {}", su);
        Document doc = getWebDocument(su);
        log.info("Get");
        if (Objects.nonNull(doc)) {

            List<CategoryOpencart> mainCategories = doc.select("div.accordionLeft")
                    .get(0)
                    .select("a.accordion__btn")
                    .stream()
                    .map(ec -> {

                        String url = SUPPLIER_URL.concat(ec.attr("href").substring(1)).replaceAll("\\.ua/ua/ua/", "\\.ua/ua/");
                        String title = ec.text().trim();
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
            supplierApp.getMainSupplierCategory().getCategoriesOpencart().addAll(mainCategories);

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
                    .forEach(c -> log.info("{}. Full Site category name: {}, parent id: {}, paren obj: {} subCategorySize: {}", countCategory.addAndGet(1), c.getDescriptions().get(0).getName(), c.getParentId(), Objects.isNull(c.getParentCategory()), c.getCategoriesOpencart().size()));

            return siteCategoryList;

        }
        return new ArrayList<>();

    }

    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();
        Document doc = getWebDocument(url, new HashMap<>());
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {
            Elements subCateElements = doc.select("div.search-result__aside-item.search-result__aside-item_toggler:has(*:contains(Підкатегорії))");

            List<CategoryOpencart> subCategories = subCateElements.select("a")
                    .stream()
                    .map(el -> {

                        String subUrl = SUPPLIER_URL.concat(el.attr("href").substring(4));

                        log.info("Sub Url: {}", subUrl);
                        String subTitle = el.text().trim();
                        log.info("Main title: {} sub title: {}", category.getDescriptions().get(0).getName(), subTitle);
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
                    Document doc = getWebDocument(url, new HashMap<>());

                    List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);
                    log.info("parentsCategories size: {}", parentsCategories.size());

                    if (Objects.nonNull(doc)) {
                        int numberPagesCategory = 1;
                        Elements perPageElement = doc.select("div.pagination a");

                        List<Integer> perPagesList = perPageElement
                                .stream()
                                .filter(e -> e.text().trim().matches("\\d+"))
                                .map(e -> Integer.parseInt(e.text().trim()))
                                .collect(Collectors.toList());

                        if (!perPagesList.isEmpty()) {
                            numberPagesCategory = perPagesList
                                    .stream()
                                    .max(Integer::compare)
                                    .orElse(1);
                        }

                        int countPage = 1;

                        while (countPage <= numberPagesCategory) {
                            try {
                                if (countPage != 1) {
                                    String newUrlPage = url.concat(URL_PART_PAGE).concat(String.valueOf(countPage));
                                    doc = getWebDocument(newUrlPage, new HashMap<>());
                                }

                                Elements elementsProduct = doc.select("div.sectionwrap div.product");

                                log.info("elementsProduct: {}", elementsProduct.size());

                                elementsProduct
                                        .stream()
                                        .peek(ep -> {
                                            String urlProduct = SUPPLIER_URL.concat(ep.select("div.product__name a").attr("href").substring(4));
                                            log.info("Product url: {}", urlProduct);

                                            String title = ep.select("div.product__name a").text().trim();
                                            log.info("Product title: {}", title);
                                            String sku = urlProduct.substring(urlProduct.lastIndexOf("/") + 1, urlProduct.lastIndexOf("."));
                                            if (sku.length() > 64) {
                                                sku = sku.substring(sku.length() - 64);
                                            }

                                            String price = ep.select("div.product__price-current").text();

                                            try {
                                                price = price.substring(0, price.indexOf("грн")).replaceAll("\\D", "");
                                            } catch (Exception ex) {
                                                price = "0.0";
                                            }

                                            BigDecimal priceNumberFree = new BigDecimal(price).setScale(4);
                                            log.info("price: {}", priceNumberFree);
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

                                            String urlImage = SUPPLIER_URL.replace("/ua", "").concat(ep.select("div.product__img img").attr("src").substring(1)).replace("resize_cache/", "").replace("200_200_1/", "");

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

                            String name = webDocument.select("h1.h2").text();
                            log.info("name: {}", name);


                            String description = getDescription(webDocument);
                            log.info("Description final: {}", description);

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


                            Elements manuElem = webDocument.select("div.two-columns__subsection.two-columns__subsection_specs ul.product-spec li:contains(Виробник)");
                            String manufacturer = "non";
                            log.info("manuElem size: {}", manuElem.size());
                            if (!manuElem.isEmpty()) {
                                manufacturer = manuElem.get(0).select("span.product-spec__value").text();
                            }
                            log.info("manufacturer: {}", manufacturer);

                            ManufacturerApp manufacturerApp = getManufacturerApp(manufacturer, supplierApp);

                            ProductProfileApp firstProfileApp = p.getProductProfileApp();
                            firstProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                            firstProfileApp.setManufacturerApp(manufacturerApp);
                            ProductProfileApp savedProductProfile = getProductProfile(firstProfileApp, supplierApp);

                            String supplierModel = webDocument.select("p.product-card__code").text().replaceAll("Код:", "").trim();
                            String model = generateModel(supplierModel, String.valueOf(savedProductProfile.getProductProfileId()));
                            p.setModel(model);
                            p.setProductProfileApp(savedProductProfile);

                            setManufacturer(p, supplierApp);
                            setPriceWithMarkup(p);

                            Elements tableAttr = webDocument.select("div.two-columns__subsection.two-columns__subsection_specs ul.product-spec li");

                            List<AttributeWrapper> attributes = tableAttr
                                    .stream()
                                    .map(row -> {
                                        String keyAttr = row.select("span.product-spec__caption").text();
                                        String valueAttr = row.select("span.product-spec__value").text();
//                                        log.info("Key: {}     Value: {}", keyAttr, valueAttr);
                                        AttributeWrapper attributeWrapper = new AttributeWrapper(keyAttr, valueAttr, null);
                                        log.info("init attr: {}", attributeWrapper);
                                        AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                        log.info("final attr: {}", attribute);
                                        return attribute;
                                    })
                                    .collect(Collectors.toList());
                            p.getAttributesWrapper().addAll(attributes);


                            Elements imagesElement = webDocument.select("div.card-slider__navs img.card-slider__thumb");

                            if (imagesElement.size() > 0) {
                                AtomicInteger countImage = new AtomicInteger();
                                List<ImageOpencart> productImages = imagesElement
                                        .stream()
                                        .map(ie -> {
                                            String srcImage = ie.attr("src");
                                            String url = SUPPLIER_URL.replace("/ua", "").concat(srcImage.substring(1));
                                            String format = url.substring(url.lastIndexOf("."));
                                            String imageName = p.getSku().concat("_").concat(String.valueOf(countImage.addAndGet(1))).concat(format);
                                            String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);

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
                .filter(p -> !p.getSku().isEmpty())
                .collect(Collectors.toList());

        return fullProducts;

    }


    public void updateDescription() {

        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductOpencart> productOpencartList = opencartDaoService.getAllProductOpencartBySupplierAppName(supplierApp.getName());
        log.info("Maresto product opencart size: {}", productOpencartList.size());
        List<ProductProfileApp> productProfilesApp = supplierApp.getProductProfilesApp();
        log.info("Maresto product profile size: {}", productProfilesApp.size());
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

                        Document webDocument = getWebDocument(url, new HashMap<>());
                        String description = getDescription(webDocument);
                        ProductDescriptionOpencart desc = new ProductDescriptionOpencart.Builder()
                                .withProductId(id)
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .withDescription(description)
                                .build();
                        opencartDaoService.updateDescription(desc);
                        log.info("Product id: {}", id);
                    }

                });
    }

    public String getDescription(Document doc) {
        Elements descElement = doc.select("div[data-value=description]");
        String description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
        log.info("Description UA text: {}", description);

        if (description.isEmpty()) {
            String url = doc.baseUri();
            url = url.replaceAll("ua/ua/", "ua/");
            Document ruDoc = getWebDocument(url, new HashMap<>());
            descElement = ruDoc.select("div[data-value=description]");
            description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
            log.info("Description RU text: {}", description);


            if (!description.isEmpty()) {
                description = translateService.getTranslatedText(description);
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
                Document webDocument = getWebDocument(productProfileApp.getUrl(), new HashMap<>());
                Elements manuElem = webDocument.select("div.two-columns__subsection.two-columns__subsection_specs ul.product-spec li:contains(Виробник)");
                String manufacturer = "non";
                log.info("manuElem size: {}", manuElem.size());
                if (!manuElem.isEmpty()) {
                    manufacturer = manuElem.get(0).select("span.product-spec__value").text();
                }
                log.info("manufacturer: {}", manufacturer);
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

    public Document getWebDocument(String url) {
        Document document = null;
        int countBadConnection = 0;
        boolean hasConnection = false;

        while (!hasConnection && countBadConnection < AppConstant.BAD_CONNECTION_LIMIT) {
            try {
                HttpsUrlValidator.retrieveResponseFromServer(url);
                Connection.Response response = Jsoup.connect(url)
                        .maxBodySize(0)
                        .timeout(60 * 1000)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36")
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .execute();
                int statusCode = response.statusCode();
                log.info("Status code: {} connection to : {}", statusCode, url);

                if (statusCode == 200) {
                    hasConnection = true;
                    document = response.parse();
                } else {
                    countBadConnection++;
                }


            } catch (Exception e) {
                countBadConnection++;
                log.warn("{}. Bad connection by link: {}", countBadConnection, url, e);
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException ex) {
                    log.warn("Error sleeping while bad connection", ex);
                }
            }
        }
        return document;

    }


    public void updateModel() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductProfileApp> profiles = appDaoService.getAllProductProfileAppBySupplierId(supplierApp.getSupplierAppId());
        profiles
                .forEach(p -> {
                    try {
                        String url = p.getUrl();
                        Document doc = getWebDocument(url);
                        if (Objects.nonNull(doc)) {
                            String supplierModel = doc.select("p.product-card__code").text().replaceAll("Код:", "").trim();
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


}
