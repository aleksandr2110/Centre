package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
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
import orlov.home.centurapp.util.AppConstant;
import orlov.home.centurapp.util.OCConstant;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserServiceIndigowood extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "indigowood";
    private final String SUPPLIER_URL = "https://indigowood.com.ua/ua/";
    private final String DISPLAY_NAME = "501 - INDIGOWOOD";
    private final String MANUFACTURER_NAME = "INDIGOWOOD";
    private final String URL_PART_PAGE = "?page=2";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;

    public ParserServiceIndigowood(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService) {
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


    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        Document doc = getWebDocument(supplierApp.getUrl(), new HashMap<>());

        if (Objects.nonNull(doc)) {

            List<CategoryOpencart> mainCategories = doc.select("div.box-content div.nav_title a.title")
                    .stream()
                    .map(ec -> {

                        String url = ec.attr("href");
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
            Elements subCateElements = doc.select("elem#none");

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
                        Elements perPageElement = doc.select("div.results");
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
                                    doc = getWebDocument(newUrlPage, new HashMap<>());
                                }

                                Elements elementsProduct = doc.select("div.product-thumb");
                                log.info("elementsProduct: {}", elementsProduct.size());


                                elementsProduct
                                        .stream()
                                        .peek(ep -> {
                                            String urlProduct = ep.select("div.image a").attr("href");
                                            log.info("Product url: {}", urlProduct);


                                            String title = ep.select("div.small_detail div.name").text().trim();
                                            log.info("Product title: {}", title);

                                            boolean contains = urlProduct.contains("product_id=");

                                            String sku = "";
                                            if (contains) {
                                                sku = urlProduct.substring(urlProduct.lastIndexOf("=") + 1).replaceAll("[^\\d\\w]", "_");
                                            } else {
                                                sku = urlProduct.substring(urlProduct.lastIndexOf("/") + 1, urlProduct.indexOf("?path=")).replaceAll("[^\\d\\w]", "_");

                                            }

                                            if (sku.length() > 64) {
                                                sku = sku.substring(sku.length() - 64);
                                            }

                                            log.info("Product sku: {}", sku);
                                            String price = "0.0";
                                            Elements priceElement = ep.select("p.price");
                                            Elements newPriceElement = priceElement.select("span.price-new");
                                            if (newPriceElement.isEmpty()) {
                                                if (!priceElement.isEmpty() && !priceElement.text().isEmpty()) {
                                                    price = priceElement.text().replaceAll("\\D", "");
                                                }
                                            } else {
                                                price = newPriceElement.text().replaceAll("\\D", "");
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

                                            String urlImage = ep.select("div.image a img").attr("src");
//                                            log.info("Product url image: {}", urlImage);
//                                            String format = urlImage.substring(urlImage.lastIndexOf("."));
//                                            String imageName = sku.concat(format);
//                                            String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imageName);
//                                            log.info("imageName: {}", imageName);
//                                            log.info("dbImgPath: {}", dbImgPath);
//                                            fileService.downloadImg(urlImage, dbImgPath);

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

                            String name = p.getTitle();
                            log.info("name: {}", name);

                            ManufacturerApp manufacturerApp = getManufacturerApp(MANUFACTURER_NAME, supplierApp);


                            ProductProfileApp firstProfileApp = p.getProductProfileApp();
                            firstProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                            firstProfileApp.setManufacturerApp(manufacturerApp);
                            ProductProfileApp savedProductProfile = getProductProfile(firstProfileApp, supplierApp);

                            String supplierModel = webDocument.select("ul.list-unstyled.description").text();
                            supplierModel = supplierModel.replaceAll("Артикул:", "").replaceAll("\\*", "").trim();
                            String model = generateModel(supplierModel, String.valueOf(savedProductProfile.getProductProfileId()));

                            p.setProductProfileApp(savedProductProfile);
                            p.setModel(model);
                            setManufacturer(p, supplierApp);
                            setPriceWithMarkup(p);

                            webDocument.select("ul.list-unstyled.description").text().replaceAll("Артикул:", "").replaceAll("\\*", "");
                            String description = getDescription(webDocument);


                            String metaDesc = name.concat(AppConstant.META_DESCRIPTION_PART);
                            String metaTitle = "Купити ".concat(name).concat(AppConstant.META_TITLE_PART);
                            String metaKeyWord = "Купити ".concat(name);
                            ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                    .withDescription(description)
                                    .withName(name)
                                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                    .withMetaH1(name)
                                    .withMetaDescription(metaDesc.length() > 255 ? "" : metaDesc)
                                    .withMetaTitle(metaTitle.length() > 255 ? "" : metaTitle)
                                    .withMetaKeyword(metaKeyWord.length() > 255 ? "" : metaKeyWord)
                                    .build();
                            p.getProductsDescriptionOpencart().add(productDescriptionOpencart);


                            Elements tableAttr = webDocument.select("div#tab-specification table tbody tr");
                            log.info("tableAttr: {}", tableAttr.size());
                            List<AttributeWrapper> attributes = tableAttr
                                    .stream()
                                    .map(row -> {
                                        String keyAttr = row.select("td").get(0).text().trim();
                                        String valueAttr = row.select("td").get(1).text().trim();
                                        log.info("Key: {}     Value: {}", keyAttr, valueAttr);
                                        AttributeWrapper attributeWrapper = new AttributeWrapper(keyAttr, valueAttr, null);
                                        log.info("init attribute: {}", attributeWrapper);
                                        AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                        log.info("final attribute: {}", attribute);
                                        return attributeWrapper;
                                    })
                                    .collect(Collectors.toList());
                            p.getAttributesWrapper().addAll(attributes);


                            Elements imagesElement = webDocument.select("ul#boss-image-additional a");
                            log.info("imagesElement: {}", imagesElement.size());

                            if (imagesElement.size() > 0) {

                                AtomicInteger countImage = new AtomicInteger();
                                List<ImageOpencart> productImages = imagesElement
                                        .stream()
                                        .map(ie -> {


                                            String url = ie.attr("href");

                                            if (url.contains("800x600")) {
                                                return null;
                                            }

                                            String format = url.substring(url.lastIndexOf("."));
                                            String imageName = p.getSku().concat("_").concat(String.valueOf(countImage.addAndGet(1))).concat(format);
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

    public String getDescription(Document doc) {
        Elements descElement = doc.select("div#tab-description");
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

    public void updateModel() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<ProductProfileApp> profiles = appDaoService.getAllProductProfileAppBySupplierId(supplierApp.getSupplierAppId());
        profiles
                .forEach(p -> {
                    try {
                        String url = p.getUrl();
                        Document doc = getWebDocument(url, new HashMap<>());
                        if (Objects.nonNull(doc)) {

                            String supplierModel = doc.select("ul.list-unstyled.description").text();
                            supplierModel = supplierModel.replaceAll("Артикул:", "").replaceAll("\\*", "").trim();
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
