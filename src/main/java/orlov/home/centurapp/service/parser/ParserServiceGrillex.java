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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserServiceGrillex extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "grillex";
    private final String SUPPLIER_URL = "https://grillex.com.ua/";
    private final String DISPLAY_NAME = "98 - Grillex";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;
    private final UpdateDataService updateDataService;
    private final ImageService imageService;

    public ParserServiceGrillex(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, AppDaoService appDaoService1, OpencartDaoService opencartDaoService1, TranslateService translateService1, FileService fileService1, UpdateDataService updateDataService, ImageService imageService) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService1;
        this.opencartDaoService = opencartDaoService1;
        this.translateService = translateService1;
        this.fileService = fileService1;
        this.updateDataService = updateDataService;
        this.imageService = imageService;
    }

    @Override
    public void doProcess() {
        try {

            Timestamp start = new Timestamp(Calendar.getInstance().getTime().getTime());

            SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);

            List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);

            List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);

            log.info("Count products: {}", productsFromSite.size());

            List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);

            OpencartDto opencartInfo = getOpencartInfo(fullProductsData, supplierApp);

            checkPrice(opencartInfo, supplierApp);

            checkStockStatusId(opencartInfo, supplierApp);

            List<ProductOpencart> newProduct = opencartInfo.getNewProduct();

            newProduct.forEach(opencartDaoService::saveProductOpencart);
            updateDataService.updatePrice(supplierApp.getSupplierAppId());

            updateProductSupplierOpencartBySupplierApp(supplierApp);

            Timestamp end = new Timestamp(Calendar.getInstance().getTime().getTime());
            OrderProcessApp orderProcessApp = opencartInfo.getOrderProcessApp();
            orderProcessApp.setStartProcess(start);
            orderProcessApp.setEndProcess(end);
            appDaoService.saveOrderDataApp(orderProcessApp);

        } catch (Exception ex) {
            log.warn("Exception parsing nowystyle", ex);  // FIXME: 24.12.2022 Save exception description
        }
    }

    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        Document doc = getWebDocument(SUPPLIER_URL, new HashMap<>());

        AtomicInteger countMainCategory = new AtomicInteger();
        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select(".main-categories-list > li >div > div > a")
                    .stream()
                    .map(ec -> {

                        String url = ec.attr("href");
                        String title = ec.select("img").attr("alt");
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
                    .collect(Collectors.toList());

            log.info("Main category size: {}", mainCategories.size());


            return mainCategories
                    .stream()
                    .map(sc -> recursiveCollectListCategory(sc, supplierCategoryOpencartDB))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {

        return null;
    }

    @Override
    public List<ProductOpencart> getProductsInitDataByCategory(List<CategoryOpencart> categoriesWithProduct, SupplierApp supplierApp) {

        List<ProductOpencart> allProductInitList = new ArrayList<>();

        categoriesWithProduct
                .forEach(c -> {

                    String categoryName = c.getDescriptions().get(0).getName();
                    CategoryApp categoryApp = getCategoryApp(categoryName, supplierApp);
                    String url = c.getUrl();

                    Elements nextPage;
                    do {
                        Document doc = getWebDocument(url, new HashMap<>());
                        try {
                            if (Objects.nonNull(doc)) {

                                Elements elementsProduct = doc.select(".cat-item-wrapper");

                                if (elementsProduct.isEmpty()) {
                                    elementsProduct = doc.select(".item-wrapper");
                                    getProductsInitDataByCategoryFirst(allProductInitList, supplierApp, categoryApp,
                                            elementsProduct, c);
                                } else {
                                    getProductsInitDataByCategorySecond(allProductInitList, supplierApp, categoryApp,
                                            elementsProduct, c);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Problem iterate page", e);
                        }
                        nextPage = doc.select(".nextpostslink");
                        url = nextPage.attr("href");
                    } while (nextPage.size() != 0);
                });

        return allProductInitList;
    }

    private void getProductsInitDataByCategoryFirst(List<ProductOpencart> allProductInitList, SupplierApp supplierApp,
                                                    CategoryApp categoryApp, Elements elementsProduct, CategoryOpencart c) {
        elementsProduct
                .forEach(ep -> {
                    String urlProduct = ep.select(".item-img > a").attr("href");
                    log.info("Product url: {}", urlProduct);
                    String title = ep.select(".item-img > a").attr("title");
                    log.info("Product title: {}", title);
                    String sku = ep.select(".cat-item-code").text().replace("код","").trim();
                    log.info("Product sku: {}", sku);
                    String model = generateModel(sku, "0000");
                    log.info("Product model: {}", model);
                    String textPrice = ep.select(".price").text().replace("грн.","")
                            .replaceAll(" ","");
                    BigDecimal price = new BigDecimal("0");
                    if (!textPrice.isEmpty()) {
                        price = new BigDecimal(textPrice);
                    }
                    log.info("Product price: {}", price);
                    String urlImage = ep.select(".item-img img").attr("src");
                    log.info("Product url image: {}", urlImage);
                    String imageName = urlImage.substring(urlImage.lastIndexOf("/") + 1);

                    imageName = sku.concat("-").concat(imageName);
                    log.info("Product image name: {}", imageName);
                    String imageDBName = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);
                    log.info("Product image db name: {}", imageDBName);
                    downloadImage(urlImage, imageDBName);

                    ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                            .withTitle(title)
                            .withUrl(urlProduct)
                            .withSku(sku)
                            .withPrice(price)
                            .withSupplierId(supplierApp.getSupplierAppId())
                            .withSupplierApp(supplierApp)
                            .withCategoryId(categoryApp.getCategoryId())
                            .withCategoryApp(categoryApp)
                            .build();

                    ProductOpencart product = new ProductOpencart.Builder()
                            .withTitle(title)
                            .withUrlProduct(urlProduct)
                            .withModel(model)
                            .withSku(sku)
                            .withImage(imageDBName)
                            .withPrice(price)
                            .withItuaOriginalPrice(price)
                            .withJan(supplierApp.getName())
                            .withProductProfileApp(productProfileApp)
                            .build();
                    product.setCategoriesOpencart(Arrays.asList(c));

                    if (!allProductInitList.contains(product)) {
                        allProductInitList.add(product);
                    }
                });
    }

    private void getProductsInitDataByCategorySecond(List<ProductOpencart> allProductInitList, SupplierApp supplierApp,
                                                     CategoryApp categoryApp, Elements elementsProduct, CategoryOpencart c) {
        elementsProduct
                .forEach(ep -> {
                    String urlProduct = ep.select(".cat-item-title > a").attr("href");
                    log.info("Product url: {}", urlProduct);
                    String title = ep.select(".cat-item-title > a").text();
                    log.info("Product title: {}", title);
                    String sku = ep.select(".cat-item-code > span").text();
                    log.info("Product sku: {}", sku);
                    String model = generateModel(sku, "0000");
                    log.info("Product model: {}", model);
                    String textPrice = ep.select(".cat-item-price span").get(0).text()
                            .replace(" ", "");
                    BigDecimal price = new BigDecimal("0");
                    if (!textPrice.isEmpty()) {
                        price = new BigDecimal(textPrice);
                    }
                    log.info("Product price: {}", price);
                    String urlImage = ep.select(".img img").attr("src");
                    log.info("Product url image: {}", urlImage);
                    String imageName = urlImage.substring(urlImage.lastIndexOf("/") + 1);

                    imageName = sku.concat("-").concat(imageName);
                    log.info("Product image name: {}", imageName);
                    String imageDBName = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imageName);
                    log.info("Product image db name: {}", imageDBName);
                    downloadImage(urlImage, imageDBName);

                    ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                            .withTitle(title)
                            .withUrl(urlProduct)
                            .withSku(sku)
                            .withPrice(price)
                            .withSupplierId(supplierApp.getSupplierAppId())
                            .withSupplierApp(supplierApp)
                            .withCategoryId(categoryApp.getCategoryId())
                            .withCategoryApp(categoryApp)
                            .build();

                    ProductOpencart product = new ProductOpencart.Builder()
                            .withTitle(title)
                            .withUrlProduct(urlProduct)
                            .withModel(model)
                            .withSku(sku)
                            .withImage(imageDBName)
                            .withPrice(price)
                            .withItuaOriginalPrice(price)
                            .withJan(supplierApp.getName())
                            .withProductProfileApp(productProfileApp)
                            .build();
                    product.setCategoriesOpencart(Arrays.asList(c));

                    if (!allProductInitList.contains(product)) {
                        allProductInitList.add(product);
                    }
                });
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
                        Elements elementDescription = doc.select(".description-block div.text");
                        String description = "";
                        if (!elementDescription.isEmpty()) {
                            doc.select(".all-tech-features-list").text();
                            description = wrapToHtml(cleanDescription(elementDescription.first()),
                                    doc.select(".all-tech-features-list > li > ul > li > ul > li"));
                            description = description.replaceAll("\uD83D\uDE09","");
                        }else {
                            System.out.println("");
                        }

                        ProductDescriptionOpencart descriptionOpencart = new ProductDescriptionOpencart.Builder()
                                .withName(title)
                                .withDescription(description)
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .build();
                        prod.getProductsDescriptionOpencart().add(descriptionOpencart);
                        prod.setStockStatusId(getStockStatus(doc));

                        String manufacturerName = "non";
                        ManufacturerApp manufacturerApp = getManufacturerApp(manufacturerName, supplierApp);
                        ProductProfileApp productProfileApp = prod.getProductProfileApp();
                        productProfileApp.setTitle(title);
                        productProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                        productProfileApp.setManufacturerApp(manufacturerApp);
                        productProfileApp.setPrice(prod.getPrice());
                        ProductProfileApp savedProductProfile = getProductProfile(productProfileApp, supplierApp);
                        prod.setProductProfileApp(savedProductProfile);
                        Elements status = doc.select(".stock-status-item-card > div");
                        if(status.isEmpty()){
                            status = doc.select("div.in-stock");
                        }
                        prod.setQuantity(toCenturStatus(status.text()));  //set product status !!!(((

                        log.info("Product price for profile: {}", prod.getPrice());

                        setManufacturer(prod, supplierApp);
                        setPriceWithMarkup(prod);


                        Elements imageElements = doc.select(".gallery-photos-slider a");
                        log.info("Images count: {}", imageElements.size());
                        if (imageElements.isEmpty()){
                            imageElements = doc.select(".gallery-item a");
                        }

                        AtomicInteger countImg = new AtomicInteger();
                        List<ImageOpencart> productImages = imageElements
                                .stream()
                                .map(i -> {
                                    String src = i.attr("href");
                                    log.info("img src: {}", src);

                                    if (countImg.get() == 0) {
                                        log.info("Image url: {}", src);
                                        String imgName = prod.getSku().concat("_" + countImg.addAndGet(1)).concat(".jpg");
                                        log.info("Image name: {}", imgName);
                                        String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
                                        log.info("Image DB name: {}", dbImgPath);
                                        downloadImage(src, dbImgPath);
                                        prod.setImage(dbImgPath);
                                        return null;
                                    } else {
                                        log.info("Image url: {}", src);
                                        String imgName = prod.getSku().concat("_" + countImg.addAndGet(1)).concat(".jpg");
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

    private int toCenturStatus(String text) {
        switch (text){
            case "Наявність уточнюйте":
                return 2;
            case "В наявності":
                return 100;
            case "Під замовлення":
                return 33;
            default:
                return 33;
        }
    }

    private int getStockStatus(Document doc) {
        return doc.select(".alert-alt").text().equals("Є в наявності") ? 7 : 5;
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

    private String generateModel(String supplierModel, String other) {
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
