package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
public class ParserServiceArmEco extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "arm-eko";
    private final String SUPPLIER_URL = "https://arm-eko.com.ua/";
    private final String DISPLAY_NAME = "2 - АРМ-ЕКО";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;
    private final UpdateDataService updateDataService;
    private final ImageService imageService;

    public ParserServiceArmEco(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, AppDaoService appDaoService1, OpencartDaoService opencartDaoService1, TranslateService translateService1, FileService fileService1, UpdateDataService updateDataService, ImageService imageService) {
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

            OrderProcessApp orderProcessApp;
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

        CategoryOpencart categoryOpencart = new CategoryOpencart.Builder()
                .withUrl("https://arm-eko.com.ua/oborudovanie-sobstvennogo-proizvodstva-ua/")
                .withParentCategory(supplierApp.getMainSupplierCategory())
                .withParentId(supplierApp.getMainSupplierCategory().getCategoryId())
                .withTop(false)
                .withStatus(false)
                .build();
        CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                .withName("ОБЛАДНАННЯ ВЛАСНОГО ВИРОБНИЦТВА")
                .withDescription(supplierApp.getName())
                .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                .build();
        categoryOpencart.getDescriptions().add(description);
        List<CategoryOpencart> mainCategories = Arrays.asList(categoryOpencart);

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

    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {

        String url = category.getUrl();
        Document doc = getWebDocument(url, new HashMap<>());
        if (Objects.nonNull(doc)) {

            List<CategoryOpencart> subCategories = doc
                    .select("#content > div:nth-child(2) > div:nth-child(2) > div:nth-child(1) > div")
                    .stream()
                    .map(el -> {
                        String subUrl = el.select("div > a:nth-child(1)").attr("href");
                        String subTitle = el.select("div > a:nth-child(1) > img").attr("alt");
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

                    Element nextPage = null;
                    Elements pagination;
                    do {
                        Document doc = getWebDocument(url, new HashMap<>());
                        try {
                            if (Objects.nonNull(doc)) {

                                Elements elementsProduct = doc
                                        .select(".products-collection > div > .product-layout.product-grid");
                                log.info("Count product: {} on page: {}", elementsProduct.size(), url);
                                elementsProduct
                                        .forEach(ep -> {
                                            String textPrice = ep.select(".price").text()
                                                    .replace(" грн","");
                                            if (!StringUtils.isNumeric(textPrice)) {
                                                return;
                                            }
                                            BigDecimal price = new BigDecimal("0");
                                            if (!textPrice.isEmpty()) {
                                                price = new BigDecimal(textPrice);
                                            }
                                            log.info("Product price: {}", price);

                                            String urlProduct = ep.select(".product-meta a")
                                                    .attr("href");
                                            log.info("Product url: {}", urlProduct);
                                            String title = ep.select(".product-meta a").text();
                                            log.info("Product title: {}", title);
                                            String sku = getSKUOnProductPage(urlProduct);
                                            log.info("Product sku: {}", sku);
                                            String model = generateModel(sku, "0000");
                                            log.info("Product model: {}", model);

                                            String urlImage = ep.select(" img").attr("src")
                                                    .replace("-258x258", "")
                                                    .replace("cache/", "");
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
                                                    .withQuantity(100)
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
                                        });

                            }
                        } catch (Exception e) {
                            log.warn("Problem iterate page", e);
                        }
                        pagination = doc.select(".pagination > li");
                        if (!pagination.isEmpty()) {
                            nextPage = pagination.get(pagination.size() - 2);
                            url = nextPage.select("a").attr("href");
                        }
                    } while (!pagination.isEmpty() && nextPage.text().equals(">"));
                });

        return allProductInitList;
    }

    private String getSKUOnProductPage(String url) {
        return getWebDocument(url, new HashMap<>())
                .select(".sku").text();
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
                        Elements elementDescription = doc.select("#tab-description");
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
                        prod.setStockStatusId(getStockStatus(doc));

                        Elements elementManufacturer = doc
                                .select(".brand");
                        String manufacturerName = "non";
                        if (!elementManufacturer.isEmpty()) {
                            manufacturerName = elementManufacturer.text().replace("Виробник: ","");
                        }
                        log.info("Manufacturer: {}", manufacturerName);
                        ManufacturerApp manufacturerApp = getManufacturerApp(manufacturerName, supplierApp);
                        ProductProfileApp productProfileApp = prod.getProductProfileApp();
                        productProfileApp.setTitle(title);
                        productProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                        productProfileApp.setManufacturerApp(manufacturerApp);
                        productProfileApp.setPrice(prod.getPrice());
                        prod.setQuantity(100); // set status
                        ProductProfileApp savedProductProfile = getProductProfile(productProfileApp, supplierApp);
                        prod.setProductProfileApp(savedProductProfile);

                        log.info("Product price for profile: {}", prod.getPrice());

                        Elements attributeElement = doc.select(".table > tbody > tr");

                        List<AttributeWrapper> attributeWrappers = attributeElement
                                .stream()
                                .map(row -> {

                                    Elements td = row.select("td");
                                    if (td.size() == 2) {
                                        String key = td.get(0).text().trim();
                                        String value = td.get(1).text().trim();
                                        log.info("Key: {}, value: {}", key, value);
                                        AttributeWrapper attributeWrapper = new AttributeWrapper(key, value, null);
                                        AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                        return attribute;
                                    } else {
                                        return null;
                                    }

                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        prod.getAttributesWrapper().addAll(attributeWrappers);

                        setManufacturer(prod, supplierApp);
                        setPriceWithMarkup(prod);


                        Elements imageElements = doc.select(".img-full img");
                        log.info("Images count: {}", imageElements.size());

                        AtomicInteger countImg = new AtomicInteger();
                        List<ImageOpencart> productImages = imageElements
                                .stream()
                                .map(i -> {
                                    String src = i.attr("src");
                                    log.info("img src: {}", src);

                                    if (countImg.get() == 0) {
                                        log.info("Image url: {}", src);
                                        String imgName = prod.getSku().concat("_" + countImg.addAndGet(1) + src.substring(src.lastIndexOf("/") + 1));
                                        log.info("Image name: {}", imgName);
                                        String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
                                        log.info("Image DB name: {}", dbImgPath);
                                        downloadImage(src, dbImgPath);
                                        prod.setImage(dbImgPath);
                                        return null;
                                    } else {
                                        log.info("Image url: {}", src);
                                        String imgName = prod.getSku()
                                                .concat("_" + countImg.addAndGet(1)
                                                        + src.substring(src.lastIndexOf("/") + 1));
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
