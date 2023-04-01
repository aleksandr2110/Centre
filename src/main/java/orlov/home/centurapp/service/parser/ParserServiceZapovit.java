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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserServiceZapovit extends ParserServiceAbstract {

    private static final String SUPPLIER_PART_URL = "https://zavet.kiev.ua/";
    private static final String SUPPLIER_NAME = "zapovit";
    private static final String SUPPLIER_URL = "https://zavet.kiev.ua/ua/";
    private static final String DISPLAY_NAME = "149 - ЗАПОВІТ";
    private static final String MANUFACTURER_NAME = "ZAPOVIT";//fixme: clarify


    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;
    private final UpdateDataService updateDataService;
    private final ImageService imageService;

    public ParserServiceZapovit(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService, ImageService imageService) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService;
        this.opencartDaoService = opencartDaoService;
        this.translateService = translateService;
        this.fileService = fileService;
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
            log.warn("Exception parsing zapovit", ex);
        }
    }

    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {
        //todo: check in other parsers
        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        Document doc = getWebDocument(supplierApp.getUrl(), new HashMap<>());

        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select("ul#nav li[class='nav__list-item']>a").stream()
                    .map(element -> {
                        String pathToCategory = element.attr("href").replace("ua/", "");
                        String categoryUrl = SUPPLIER_URL.concat(pathToCategory);
                        String title = element.text().trim();
                        CategoryOpencart categoryOpencart = new CategoryOpencart.Builder()
                                .withUrl(categoryUrl)
                                .withParentCategory(supplierApp.getMainSupplierCategory())
                                .withParentId(supplierApp.getMainSupplierCategory().getCategoryId())
                                .withTop(false)//what means
                                .withStatus(false)//what means
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
                    //:TODO next line uncommitted only debug
                    //.findFirst().stream()
                    .collect(Collectors.toList());
            log.info("Main categories size: {}", mainCategories.size());

            List<CategoryOpencart> siteCategoryStructure = mainCategories
                    .stream()
                    .map(this::recursiveWalkSiteCategory)
                    .collect(Collectors.toList());

            //subCategories. Final structure with parents
            List<CategoryOpencart> siteCategoryList = siteCategoryStructure
                    .stream()
                    .map(sc -> recursiveCollectListCategory(sc, supplierCategoryOpencartDB))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            siteCategoryList.add(supplierApp.getMainSupplierCategory());
            siteCategoryList.add(supplierApp.getGlobalSupplierCategory());

            return siteCategoryList;
        }


        return null;
    }

    @Override//complete
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();
        Document doc = getWebDocument(url, new HashMap<>());
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {
            Elements subCategoryElements = doc.select("div.catalogue__inner div.categories__item a");
            if (Objects.nonNull(subCategoryElements) && !subCategoryElements.isEmpty()) {
                List<CategoryOpencart> subCategories = subCategoryElements
                        .stream()
                        .map(element -> {
                            final String path = element.attr("href").replace("ua/", "");
                            final String fullUriToCategory = SUPPLIER_URL.concat(path);
                            final String title = element.text().trim();
                            log.info("Sub href: {}", fullUriToCategory);
                            log.info("Sub title: {}", title);

                            CategoryOpencart subCategory = new CategoryOpencart.Builder()
                                    .withUrl(fullUriToCategory)
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
        }
        return category;
    }

    @Override//ready to testing
    public List<ProductOpencart> getProductsInitDataByCategory(List<CategoryOpencart> categoriesWithProduct, SupplierApp supplierApp) {
        List<ProductOpencart> productsCategory = new ArrayList<>();

        for (CategoryOpencart c : categoriesWithProduct) {
            int categoryId = c.getCategoryId();
            List<CategoryOpencart> childrenCategory = categoriesWithProduct.stream().filter(sub -> sub.getParentId() == categoryId).collect(Collectors.toList());
            if (childrenCategory.isEmpty()) {
                String name = c.getDescriptions().get(0).getName();
                CategoryApp categoryApp = getCategoryApp(name, supplierApp);

                String url = c.getUrl();
                log.info("Get info by url: {}", url);
                Document doc = getWebDocument(url, new HashMap<>());
                log.info("Go to page: {}", url);

                List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);
                log.info("parentsCategories size: {}", parentsCategories.size());

                if (Objects.nonNull(doc)) {
                    Elements elementsProduct = doc.select("div#mainContainer ul>li");
                    log.info("elementsProduct: {}", elementsProduct.size());


                    elementsProduct
                            .stream()
                            .peek(ep -> {
                                String partUrlProduct = ep.select("div.products-list__caption>a").attr("href");
                                String urlProduct = SUPPLIER_URL.concat(partUrlProduct).replaceAll("/ua/ua/", "/ua/");
                                log.info("Product url: {}", urlProduct);

                                String title = ep.select("div.products-list__caption>a>span").text().trim();
                                log.info("Product title: {}", title);

                                String sku = getSKUFromHref(partUrlProduct);
                                log.info("Product sku: {}", sku);

                                Elements priceElement = ep.select("div.products-list__caption>span");
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

                                //if product exist in two different categories
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
                }
            }
        }
        return productsCategory;
    }

    @Override
    public List<ProductOpencart> getFullProductsData(List<ProductOpencart> products, SupplierApp supplierApp) {
        final String titleCss = "div#product div[class$=title] h1.catalogue__product-name";
        final String codeCss = "div#product span[class$=code]>span";
        AtomicInteger count = new AtomicInteger();
        return products
                .stream()
                .peek(p -> {
                    final String urlProduct = p.getUrlProduct();
                    Document webDocument = getWebDocument(urlProduct, new HashMap<>());
                    log.info("{}. Get data from product url: {}", count.addAndGet(1), urlProduct);

                    if (Objects.nonNull(webDocument)) {
                        try {
                            final String name = webDocument.select(titleCss).text().trim();
                            log.info("name: {}", name);

                            final String stringModel = webDocument.select(codeCss).text();
                            log.info("stringModel: {}", stringModel);
                            //fixme: need explanation about model
                            final String model = generateModel(stringModel, "0000");
                            p.setModel(model);
                            final ManufacturerApp manufacturerApp = getManufacturerApp(MANUFACTURER_NAME, supplierApp);
                            final ProductProfileApp firstProfileApp = p.getProductProfileApp();
                            firstProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                            firstProfileApp.setManufacturerApp(manufacturerApp);
                            final ProductProfileApp savedProductProfile = getProductProfile(firstProfileApp, supplierApp);
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

                            Elements tableAttr = webDocument.select("main.content div[class*=tab-content-attribute] div[class$=inner] tbody>tr");
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

                            //small images
//                            Elements imagesElement = webDocument.select("div.row.product-page div[class$=img] div[class*=preview-slider] div.slick-track");
                            Elements imagesElement = webDocument.select("a[data-fancybox='gallery']");
                            log.info("imagesElement: {}", imagesElement.size());

                            if (!imagesElement.isEmpty()) {

                                AtomicInteger countImage = new AtomicInteger();
                                List<ImageOpencart> productImages = imagesElement
                                        .stream()
                                        .map(ie -> {
                                            //fixme: need to test
                                            String stringUrl = ie.attr("href");
                                            String url = SUPPLIER_PART_URL.concat(stringUrl);

                                            String format = url.substring(url.lastIndexOf("."));
                                            String imageName = p.getModel().concat("_").concat(String.valueOf(countImage.addAndGet(1)))
                                                    .concat(format);
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
                            } else {
                                Elements mainImage = webDocument.select("div.row.product-page div[class$=img] div[class*=img-slider] div[class$=active] img");
                                if (!mainImage.isEmpty()) {
                                    String url = mainImage.attr("src");
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
    }

    @Override//Manufacture check
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

    private String getSKUFromHref(String s) {
        final String regex = "ua/(.+).html";
        final int maxLength = 64;
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);
        while (m.find()) {
            String result = m.group(1);
            if (result.length() > maxLength) {
                result = result.substring(result.length() - maxLength);
            }
            return result;
        }
        throw new RuntimeException(
                String.format("Pattern do not found.  String: %s, regex: %s", s, regex));
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

    public String getDescription(Document doc) {
        final String descriptionCSS = "div.product-info.js-product-info div[class$=container] >div div.editor";
        Elements descElement = doc.select(descriptionCSS);
        String description = !descElement.isEmpty() ? cleanDescription(descElement.get(0)) : "";
        if (description.isEmpty()) {
            log.warn("Description is empty");
        } else {
            log.info("Description UA text: {}", description);
        }
        return wrapToHtml(description);
    }

}
