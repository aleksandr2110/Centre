package orlov.home.centurapp.service.parser;


import lombok.extern.slf4j.Slf4j;
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
public class ParserServiceUhlmash extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "uhlmash";
    private final String SUPPLIER_URL = "https://uhl-mash.com.ua/ua/";
    private final String SUPPLIER_PART_URL = "https://uhl-mash.com.ua";
    private final String DISPLAY_NAME = "99 - УХЛ-МАШ";
    private final String URL_PART_PAGE = "?PAGEN_1=";
    private final String MANUFACTURER_NAME = "УХЛ-МАШ";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;

    public ParserServiceUhlmash(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService) {
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
            log.info("count product: {}", productsFromSite.size());


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

        Document doc = getWebDocument(supplierApp.getUrl(), new HashMap<>());

        if (Objects.nonNull(doc)) {

            List<CategoryOpencart> mainCategories = doc.select("ul.accordeon > li > a")
                    .stream()
                    .map(ec -> {
                        String partUrl = ec.attr("href");
                        String url = SUPPLIER_URL.concat(partUrl).replaceAll("ua//ua", "ua");
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
                    .peek(c -> log.info("Name category: {}m url: {}", c.getDescriptions().get(0).getName(), c.getUrl()))
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
        Document doc = getWebDocument(url, new HashMap<>());
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {

            Elements subCateElements = doc.select("div.bx_catalog_tile div.one-category-info-tit a[onclick]");

            List<CategoryOpencart> subCategories = subCateElements
                    .stream()
                    .map(el -> {

                        String stringHref = el.attr("onclick").replaceAll("window\\.location.href='", "").replaceAll("';", "");
                        String href = SUPPLIER_URL.concat(stringHref).replaceAll("ua/ua//ua/", "ua/ua/");
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
                        int countPage = 1;

                        while (countPage != -1) {
                            try {
                                if (countPage != 1) {
                                    String newUrlPage = url.concat(URL_PART_PAGE).concat(String.valueOf(countPage));
                                    doc = getWebDocument(newUrlPage, new HashMap<>());
                                    log.info("Go to page: {}", newUrlPage);
                                }


                                Elements elementsProduct = doc.select("div.home-one-item");
                                log.info("elementsProduct: {}", elementsProduct.size());

                                elementsProduct
                                        .stream()
                                        .peek(ep -> {
                                            String partUrlProduct = ep.select("div.item-tit a").attr("href");
                                            String urlProduct = SUPPLIER_URL.concat(partUrlProduct).replaceAll("ua/ua//ua/", "ua/ua/");
                                            log.info("Product url: {}", urlProduct);

                                            String title = ep.select("div.item-tit a").text().trim();

                                            log.info("Product title: {}", title);

                                            String sku = partUrlProduct.substring(partUrlProduct.lastIndexOf("/") + 1).replaceAll("\\.php", "");
                                            if (sku.length() > 64){
                                                sku = sku.substring(sku.length() - 64);
                                            }
                                                log.info("Product sku: {}", sku);

                                            Elements priceElement = ep.select("div.item-pr div.eco-in span");
                                            String price = "0.0";

                                            if (!priceElement.isEmpty()) {
                                                Elements discount = priceElement.select("s.discount");
                                                if (!discount.isEmpty()) {
                                                    discount.remove();
                                                }
                                                String textPrice = priceElement.text().replaceAll("\\D", "");
                                                if (!textPrice.isEmpty()) {
                                                    price = textPrice;
                                                }
                                            }

                                            log.info("Product price: {}", price);
                                            BigDecimal priceNumberFree = new BigDecimal(price).setScale(4);
                                            log.info("Product priceNumberFree: {}", priceNumberFree);

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


                                List<Integer> paginationPages = doc.select("div.pagination li")
                                        .stream()
                                        .map(ep -> ep.text().trim().replaceAll("\\D", ""))
                                        .filter(s -> !s.isEmpty())
                                        .map(Integer::parseInt)
                                        .collect(Collectors.toList());


                                boolean contains = paginationPages.contains(countPage + 1);
                                if (contains) {
                                    countPage++;
                                } else {
                                    countPage = -1;
                                }

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

                            String name = webDocument.select("p#ga_name").text().trim();
                            log.info("name: {}", name);

                            String stringModel = webDocument.select("p#ga_artnumber").text();
                            log.info("stringModel: {}", stringModel);
                            String model = generateModel(stringModel, "0000");

                            p.setModel(model);

                            ManufacturerApp manufacturerApp = getManufacturerApp(MANUFACTURER_NAME, supplierApp);

                            ProductProfileApp firstProfileApp = p.getProductProfileApp();
                            firstProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                            firstProfileApp.setManufacturerApp(manufacturerApp);
                            ProductProfileApp savedProductProfile = getProductProfile(firstProfileApp, supplierApp);
                            log.info("Saved PP Noveen: {}", savedProductProfile);
                            p.setProductProfileApp(savedProductProfile);
                            setManufacturer(p, supplierApp);
                            setPriceWithMarkup(p);

                            String description = getDescription(webDocument);

                            ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                    .withDescription(description)
                                    .withName(name)
                                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                    .withMetaH1(name)
                                    .withMetaDescription(name.concat(AppConstant.META_DESCRIPTION_PART))
                                    .withMetaKeyword("Купити ".concat(name))
                                    .build();
                            p.getProductsDescriptionOpencart().add(productDescriptionOpencart);


                            Elements tableAttr = webDocument.select("div#det_characteristics tbody tr");
                            log.info("tableAttr: {}", tableAttr.size());
                            List<AttributeWrapper> attributes = tableAttr
                                    .stream()
                                    .map(row -> {
                                        Elements attrElementRow = row.select("td");
                                        if (attrElementRow.size() >= 2) {
                                            String keyAttr = attrElementRow.get(0).text().trim();
                                            String valueAttr = attrElementRow.get(1).text().trim();
                                            log.info("Key: {}     Value: {}", keyAttr, valueAttr);

                                            AttributeWrapper attributeWrapper = new AttributeWrapper(keyAttr, valueAttr, null);
                                            log.info("Init attribute: {}", attributeWrapper);
                                            AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                            log.info("Final attribute: {}", attribute);
                                            if (keyAttr.isEmpty() || valueAttr.isEmpty()) {
                                                return null;
                                            }
                                            return attributeWrapper;
                                        } else {
                                            return null;
                                        }

                                    })
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                            p.getAttributesWrapper().addAll(attributes);


                            Elements imagesElement = webDocument.select("div.one-min.product-item-detail-slider-controls-image");
                            log.info("imagesElement: {}", imagesElement.size());

                            if (imagesElement.size() > 0) {

                                AtomicInteger countImage = new AtomicInteger();
                                List<ImageOpencart> productImages = imagesElement
                                        .stream()
                                        .map(ie -> {
                                            String stringUrl = ie.select("img").attr("src").replaceAll("65_65_", "1920_1080_");
                                            String url = SUPPLIER_PART_URL.concat(stringUrl);

                                            String format = url.substring(url.lastIndexOf("."));
                                            String imageName = p.getModel().concat("_").concat(String.valueOf(countImage.addAndGet(1))).concat(format);
                                            String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imageName);
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


                            } else {

                                Elements mainImage = webDocument.select("div.product-item-detail-slider-image.active img");
                                if (!mainImage.isEmpty()) {
                                    String stringUrl = mainImage.attr("src");

                                    String url = SUPPLIER_PART_URL.concat(stringUrl);

                                    String format = url.substring(url.lastIndexOf("."));
                                    String imageName = p.getModel().concat("_").concat(String.valueOf(1).concat(format));
                                    String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imageName);
                                    log.info("image url: {}", url);
                                    log.info("image name: {}", imageName);
                                    log.info("dbImg path: {}", dbImgPath);
                                    downloadImage(url, dbImgPath);
                                    p.setImage(dbImgPath);
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
        Elements descElement = doc.select("div#det_description");
        Elements titleDesc = descElement.select("h2.desc-prod-tit");
        titleDesc.remove();
        String description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
        log.info("Description UA text: {}", description);
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
