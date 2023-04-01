package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
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
public class ParserServiceKidigo extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "kidigo";
    private final String SUPPLIER_URL = "http://kidigo.com.ua/ua";
    private final String SUPPLIER_URL_IMAGE = "http://kidigo.com.ua";
    private final String DISPLAY_NAME = "42 - KIDIGO";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;
    private final UpdateDataService updateDataService;
    private final ImageService imageService;

    public ParserServiceKidigo(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, AppDaoService appDaoService1, OpencartDaoService opencartDaoService1, TranslateService translateService1, FileService fileService1, UpdateDataService updateDataService, ImageService imageService) {
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

            OrderProcessApp orderProcessApp = new OrderProcessApp();
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
            //:TODO update price in function checkPrice
            if(!newProduct.isEmpty()) {
                updateDataService.updatePrice(supplierApp.getSupplierAppId());
            }

            updateProductSupplierOpencartBySupplierApp(supplierApp);

            Timestamp end = new Timestamp(Calendar.getInstance().getTime().getTime());
            orderProcessApp = opencartInfo.getOrderProcessApp();
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
            List<CategoryOpencart> mainCategories = doc.select("nav.tizers > ul > li a")
                    .stream()
                    .map(ec -> {

                        String url = SUPPLIER_URL + ec.attr("href");
                        String title = ec.text();
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
                    //:TODO next line uncommitted only debug
                    //.findFirst().stream()
                    .collect(Collectors.toList());

            log.info("Main category size: {}", mainCategories.size());

            List<CategoryOpencart> siteCategoryStructure = mainCategories
                    .stream()
                    .map(mc -> {
                        log.info("MAIN CATEGORY: {}", mc.getDescriptions().get(0).getName());
                        return recursiveWalkSiteCategory(mc);
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
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {

        String url = category.getUrl();
        Document doc = getWebDocument(url, new HashMap<>());
        if (Objects.nonNull(doc)) {

            List<CategoryOpencart> subCategories = doc.select("div.main_objectblock > .objectblock a")
                    .stream()
                    .map(el -> {
                        String subUrl = SUPPLIER_URL + el.attr("href");
                        String subTitle = el.select("img").attr("alt");
                        log.info("    Sub category: {}", subTitle);
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
                    try {
                        if (Objects.nonNull(doc)) {

                            Elements elementsProduct = doc.select("div.main_objectblocktwo");
                            log.info("Count product: {} on page: {}", elementsProduct.size(), url);
                            elementsProduct
                                    .stream()
                                    .map(ep -> {
                                        log.info("");
                                        String urlProduct = SUPPLIER_URL + ep.select(".objectblocktwo a").attr("href");
                                        log.info("Product url: {}", urlProduct);
                                        String title = ep.select(".objectblocktwo a > img").attr("alt");
                                        log.info("Product title: {}", title);
                                        String sku = getSKUOnProductPage(urlProduct);
                                        log.info("Product sku: {}", sku);
                                        String model = generateModel(sku, "0000");
                                        log.info("Product model: {}", model);
                                        String textPrice = ep.select(".price > span").text();
                                        BigDecimal price = new BigDecimal("0");
                                        if (!textPrice.isEmpty()) {
                                            price = new BigDecimal(textPrice);
                                        }
                                        log.info("Product price: {}", price);
                                        String urlImage = SUPPLIER_URL_IMAGE + ep.select(".objectblocktwo a > img").attr("src")
                                                .replace("_thumb", "");
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
                    }
                });

        return allProductInitList;
    }

    private String getSKUOnProductPage(String url) {
        return getWebDocument(url, new HashMap<>())
                .select(".item-row > span:nth-child(2)").text();
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
                        Elements elementDescription = doc.select("#description");
                        String description = "";
                        if (!elementDescription.isEmpty()) {
                            description = wrapToHtml(cleanDescription(elementDescription.first()));
                        }

                        ProductDescriptionOpencart descriptionOpencart = new ProductDescriptionOpencart.Builder()
                                .withName(title)
                                .withDescription(description)
                                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                .build();
                        prod.getProductsDescriptionOpencart().add(descriptionOpencart);
                        prod.setQuantity(toCenturStatus(doc.select(".in_stock").text()));

                        ManufacturerApp manufacturerApp = getManufacturerApp("KIDIGO", supplierApp);
                        ProductProfileApp productProfileApp = prod.getProductProfileApp();
                        productProfileApp.setTitle(title);
                        productProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                        productProfileApp.setManufacturerApp(manufacturerApp);
                        productProfileApp.setPrice(prod.getPrice());
                        ProductProfileApp savedProductProfile = getProductProfile(productProfileApp, supplierApp);
                        prod.setProductProfileApp(savedProductProfile);

                        log.info("Product price for profile: {}", prod.getPrice());

                        setManufacturer(prod, supplierApp);
                        setPriceWithMarkup(prod);


                        Elements imageElements = doc.select(".object-photos a");
                        if (imageElements.size()==0){
                            imageElements= doc.select(".photos-schema a");
                        }
                        log.info("Images count: {}", imageElements.size());

                        AtomicInteger countImg = new AtomicInteger();
                        List<ImageOpencart> productImages = imageElements
                                .stream()
                                .map(i -> {
                                    String src = SUPPLIER_URL_IMAGE + i.attr("href");
                                    log.info("img src: {}", src);

                                    if (countImg.get() == 0) {
                                        log.info("Image url: {}", src);
                                        String imgName = prod.getSku().concat("_" + countImg.addAndGet(1))
                                                .concat(src.substring(src.lastIndexOf("/") + 1));
                                        log.info("Image name: {}", imgName);
                                        String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
                                        log.info("Image DB name: {}", dbImgPath);
                                        downloadImage(src, dbImgPath);
                                        prod.setImage(dbImgPath);
                                        return null;
                                    } else {
                                        log.info("Image url: {}", src);
                                        String imgName = prod.getSku().concat("_" + countImg.addAndGet(1))
                                                .concat(src.substring(src.lastIndexOf("/") + 1));;
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
