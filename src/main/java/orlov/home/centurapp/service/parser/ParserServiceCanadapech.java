package orlov.home.centurapp.service.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.ImageData;
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
public class ParserServiceCanadapech extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "canadapech";
    private final String SUPPLIER_URL = "https://canada-pechi.com.ua/uk/";
    private final String DISPLAY_NAME = "183 - Canada";
    private final String MANUFACTURER_NAME = "CANAD";

    private final String URL_PART_PAGE = "?page=";

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;

    public ParserServiceCanadapech(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService) {
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
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk");
        Document doc = getWebDocument(supplierApp.getUrl(), cookies);

        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select("div.catalog.box > ul > li > a")
                    .stream()
                    .map(ec -> {

                        String url = ec.attr("href");
                        String title = ec.text();
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
        List<ProductOpencart> productsCategory = new ArrayList<>();
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk");
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

                    Document doc = getWebDocument(url, cookies);
                    log.info("Get info by url: {}", url);
                    List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);
                    log.info("parentsCategories size: {}", parentsCategories.size());


                    if (Objects.nonNull(doc)) {

                        int countPage = 1;
                        boolean hasNextPage = true;
                        log.info("Count page: {}", countPage);
                        while (hasNextPage) {
                            try {
                                if (countPage != 1) {
                                    String newUrlPage = url.concat(URL_PART_PAGE).concat(String.valueOf(countPage));
                                    doc = getWebDocument(newUrlPage, cookies);
                                    log.info("Get info by url: {}", newUrlPage);
                                }

                                if (Objects.nonNull(doc)) {

                                    Elements elementsProduct = doc.select("div.product-layout");
                                    log.info("Product count: {}", elementsProduct.size());
                                    elementsProduct
                                            .stream()
                                            .peek(ep -> {
                                                Elements titleElement = ep.select("div.bi-title");
                                                String urlProduct = titleElement.select("a").attr("href");

                                                String title = titleElement.text();

                                                String urlImage = ep.select("img").attr("data-src").replaceAll("350x350", "800x600");
                                                log.info("urlImage: {}", urlImage);

                                                String sku = "";
                                                if (urlProduct.contains("product_id=")) {
                                                    sku = urlProduct.substring(urlProduct.indexOf("product_id=") + "product_id=".length());
                                                } else {
                                                    sku = urlProduct.replace(supplierApp.getUrl(), "").replaceAll("/", "");
                                                }

                                                if (sku.length() > 64) {
                                                    sku = sku.substring(sku.length() - 64);
                                                }

                                                String dbImgPath = "";

                                                if (!urlImage.isEmpty()) {
                                                    String imgName = sku.concat("_").concat(String.valueOf(0)).concat(".jpg");
                                                    dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imgName);

                                                    log.info("imgName: {}", imgName);
                                                    log.info("dbImgPath: {}", dbImgPath);
                                                    downloadImage(urlImage, dbImgPath);
                                                }


                                                String price = "0";
                                                Elements curPrice = ep.select("span.current-price");
                                                Elements newPrice = ep.select("span.price-new");
                                                if (!curPrice.isEmpty()) {
                                                    price = curPrice.text().replaceAll(" ", "").replace("грн", "");
                                                } else if (!newPrice.isEmpty()) {
                                                    price = newPrice.text().replaceAll(" ", "").replace("грн", "");
                                                }
                                                BigDecimal priceNumberFree = new BigDecimal(price).setScale(4);

                                                log.info("Product url: {}", urlProduct);
                                                log.info("Product title: {}", title);
                                                log.info("Product sku: {}", sku);
                                                log.info("Product price: {}", priceNumberFree);


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

                                                log.info("Product profile: {}", productProfileApp);

                                                ProductOpencart product = new ProductOpencart.Builder()
                                                        .withProductProfileApp(productProfileApp)
                                                        .withUrlProduct(urlProduct)
                                                        .withUrlImage(urlImage)
                                                        .withTitle(title)
                                                        .withSku(sku)
                                                        .withImage(dbImgPath)
                                                        .withJan(supplierApp.getName())
                                                        .withPrice(priceNumberFree)
                                                        .withItuaOriginalPrice(priceNumberFree)
                                                        .build();

                                                product.setCategoriesOpencart(parentsCategories);


                                                ProductOpencart prodFromList = productsCategory
                                                        .stream()
                                                        .filter(ps -> ps.getUrlProduct().equals(product.getUrlProduct()))
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

                                    List<String> namberPageList = doc.select("ul.pagination li")
                                            .stream()
                                            .map(el -> el.text().replaceAll("\\D", ""))
                                            .filter(sn -> !sn.isEmpty())
                                            .collect(Collectors.toList());
                                    log.info("Number page list: {}", namberPageList);
                                    countPage++;
                                    if (namberPageList.isEmpty() || !namberPageList.contains(String.valueOf(countPage))) {
                                        log.info("Has not next page");
                                        hasNextPage = false;
                                    }


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
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk");
        List<ProductOpencart> fullProducts = products
                .stream()
                .peek(p -> {
                    String urlProduct = p.getUrlProduct();
                    Document webDocument = getWebDocument(urlProduct, cookies);
                    log.info("{}. Get data from product url: {}", count.addAndGet(1), urlProduct);

                    if (Objects.nonNull(webDocument)) {

                        try {

                            String name = p.getTitle();

                            Elements articleelement = webDocument.select("span:contains(Артикул: )");
                            String mod = "";
                            if (!articleelement.isEmpty()) {
                                mod = articleelement.text().replaceAll("Артикул: ", "");
                            } else {
                                mod = "-" + webDocument.select("input[name=product_id]").attr("value");
                            }


                            String model = generateModel(mod, "0000");

                            log.info("MOD: {}", mod);

                            p.setModel(model);
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
//                                    .withMetaDescription(name.concat("  купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999"))
//                                    .withMetaTitle("Купити ".concat(name).concat("  інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника."))
//                                    .withMetaKeyword("Купити ".concat(name))
                                    .build();
                            p.getProductsDescriptionOpencart().add(productDescriptionOpencart);


                            Elements tableAttr = webDocument.select("ul.attribute > li");
                            String attributeSeparator = " - ";
                            List<AttributeWrapper> attributes = tableAttr
                                    .stream()
                                    .map(li -> {
                                        String row = li.text();
                                        String th = row.substring(0, row.indexOf(attributeSeparator));
                                        String td = row.substring(row.indexOf(attributeSeparator) + attributeSeparator.length());
                                        log.info("");
                                        AttributeWrapper attributeWrapper = new AttributeWrapper(th, td, null);
                                        log.info("Begin attribute: {}", attributeWrapper);
                                        AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                        log.info("Finish attribute: {}", attribute);
                                        return attribute;
                                    })
                                    .collect(Collectors.toList());
                            p.getAttributesWrapper().addAll(attributes);

                            AtomicInteger countImage = new AtomicInteger();
                            Elements imagesElement = webDocument.select("ul.image_carousel a[itemscope]");
                            if (imagesElement.size() > 1) {

                                String mainImageName = p.getUrlImage();
                                log.info("mainImageName: {}", mainImageName);

                                try {

                                    List<ImageOpencart> productImages = imagesElement
                                            .stream()
                                            .map(i -> {
                                                Elements img = i.select("img");
                                                String urlImage = img.attr("src").replaceAll("74x74", "800x600");

                                                if (mainImageName.substring(0, mainImageName.indexOf(".jpg")).equals(urlImage.substring(0, urlImage.indexOf(".jpg")))) {
                                                    log.info("MAIN PAGE UTL!!!");
                                                    return null;
                                                }
                                                String sku = p.getSku();

                                                String imgName = sku.concat("_").concat(String.valueOf(countImage.addAndGet(1))).concat(".jpg");
                                                String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(imgName);

                                                log.info("urlImage: {}", urlImage);
                                                log.info("imgName: {}", imgName);
                                                log.info("dbImgPath: {}", dbImgPath);

                                                downloadImage(urlImage, dbImgPath);

                                                ImageOpencart imageOpencart = new ImageOpencart.Builder()
                                                        .withImage(dbImgPath)
                                                        .withSortOrder(countImage.get())
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
        Elements descElement = doc.select("div.tab-content");
        String description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
        int idxWorn = description.indexOf("Увага!");
        if (idxWorn != -1) {
            description = description.substring(0, idxWorn);
        }

        int idxContact = description.indexOf("Зв'яжіться з нами");

        if (idxContact != -1) {
            description = description.substring(0, idxContact);
        }


        log.info("Description UA text: {}", description);
        return wrapToHtml(description);
    }


    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();
        HashMap<String, String> cookies = new HashMap<>();
        cookies.put("language", "uk");
        Document doc = getWebDocument(url, cookies);
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {
            Elements subCateElements = doc.select("div.sv_brder");

            List<CategoryOpencart> subCategories = subCateElements
                    .stream()
                    .map(el -> {
                        String subUrl = el.select("a").attr("href");
                        log.info("subUrl: {}", subUrl);
                        String subTitle = el.select("a").text();
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
                            String supplierModel = doc.select("div.product-info-stock-sku").text().replaceAll("Код:", "").trim();
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
