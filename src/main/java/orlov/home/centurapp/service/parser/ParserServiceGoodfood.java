package orlov.home.centurapp.service.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.XmlStreamReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.ImageData;
import orlov.home.centurapp.dto.OpencartDto;
import orlov.home.centurapp.dto.api.astim.AstimCatalog;
import orlov.home.centurapp.dto.api.astim.Offer;
import orlov.home.centurapp.dto.api.astim.Shop;
import orlov.home.centurapp.dto.api.goodfood.*;
import orlov.home.centurapp.entity.app.*;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.service.api.translate.TranslateService;
import orlov.home.centurapp.service.appservice.FileService;
import orlov.home.centurapp.service.appservice.ScraperDataUpdateService;
import orlov.home.centurapp.service.appservice.UpdateDataService;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;
import orlov.home.centurapp.util.AppConstant;
import orlov.home.centurapp.util.OCConstant;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@Slf4j
public class ParserServiceGoodfood extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "goodfood";
    private final String SUPPLIER_URL = "https://1gf.com.ua/";
    private final String SUPPLIER_URL_FILE = "http://1gf.com.ua/reklama/xml/GoodFood.zip";
    private final String DISPLAY_NAME = "101 - GoodFood";


    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;

    private final UpdateDataService updateDataService;

    public ParserServiceGoodfood(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService, UpdateDataService updateDataService) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService;
        this.opencartDaoService = opencartDaoService;
        this.translateService = translateService;
        this.updateDataService = updateDataService;
    }


    public GoodfoodRootXml getXmlProductData() {
        GoodfoodRootXml result = null;
        try {
            InputStream in = new URL(SUPPLIER_URL_FILE).openStream();
            Path path = Paths.get("goodfood.zip");
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            ZipFile zipFile = new ZipFile(path.toString());
            ZipEntry xmlEntity = zipFile.getEntry("GoodFood.xml");
            log.info("xml entity: {}, size: {}", xmlEntity.getName(), xmlEntity.getSize());

            InputStream inputStream = zipFile.getInputStream(xmlEntity);
            Path pathXml = Paths.get("good.xml");
            Files.copy(inputStream, pathXml, StandardCopyOption.REPLACE_EXISTING);

            XmlMapper xml = new XmlMapper();
            xml.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            result = xml.readValue(new XmlStreamReader(pathXml), GoodfoodRootXml.class);

            GoodfoodShop shop = result.getShop();
            List<GoodfoodCategory> categories = shop.getCategories();
            List<GoodfoodOffer> offers = shop.getOffers();

            AtomicInteger countCategory = new AtomicInteger();
            AtomicInteger countOffer = new AtomicInteger();
            categories.forEach(c -> log.info("{}. car: {}", countCategory.addAndGet(1), c));
            offers.forEach(o -> log.info("{}. offer: {}", countOffer.addAndGet(1), o));

        } catch (IOException e) {
            log.error("Bad xml download", e);
        }

        return result;
    }


    @Override
    public void doProcess() {
        try {

            OrderProcessApp orderProcessApp = new OrderProcessApp();
            Timestamp start = new Timestamp(Calendar.getInstance().getTime().getTime());

            GoodfoodRootXml goodfoodRootXml = getXmlProductData();
            GoodfoodShop shop = goodfoodRootXml.getShop();
            List<GoodfoodCategory> categories = shop.getCategories();
            List<GoodfoodOffer> offers = shop.getOffers();

            SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
            supplierApp.setGoodfoodCategories(categories);
            supplierApp.setGoodfoodOffers(offers);

            List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);

            siteCategories.forEach(c -> log.info("name: {}, file id: {}", c.getDescriptions().get(0).getName(), c.getUrl()));


            List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);
            productsFromSite = productsFromSite
                    .stream()
                    .filter(ProductOpencart::isStatus)
                    .collect(Collectors.toList());


            OpencartDto opencartInfo = getOpencartInfo(productsFromSite, supplierApp);
            checkPrice(opencartInfo, supplierApp);
            List<ProductOpencart> newProduct = opencartInfo.getNewProduct();


            List<ProductOpencart> fullProductsData = getFullProductsData(newProduct, supplierApp);

            fullProductsData
                    .forEach(opencartDaoService::saveProductOpencart);

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
            log.warn("Exception parsing nowystyle", ex);
        }
    }


    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();


        List<GoodfoodCategory> goodfoodFileCategories = supplierApp.getGoodfoodCategories();

        List<GoodfoodCategory> structureGoodfoodCategories = goodfoodFileCategories
                .stream()
                .peek(gc -> {

                    goodfoodFileCategories
                            .forEach(sc -> {
                                if (gc.getId().equals(sc.getParentId())) {
                                    gc.getSubCategories().add(sc);
                                }
                            });
                })
                .filter(mc -> Objects.isNull(mc.getParentId()))
                .collect(Collectors.toList());

        structureGoodfoodCategories.forEach(c -> log.info("Structure main cat: {}", c));


        List<CategoryOpencart> mainCategories = structureGoodfoodCategories
                .stream()
                .map(fc -> {
                    CategoryOpencart categoryOpencart = new CategoryOpencart.Builder()
                            .withUrl(fc.getId())
                            .withParentCategory(supplierApp.getMainSupplierCategory())
                            .withParentId(supplierApp.getMainSupplierCategory().getCategoryId())
                            .withTop(false)
                            .withStatus(false)
                            .build();
                    CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                            .withName(fc.getText())
                            .withDescription(supplierApp.getName())
                            .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                            .build();
                    categoryOpencart.getDescriptions().add(description);
                    categoryOpencart.setSubGoodfoodCategories(fc.getSubCategories());

                    return categoryOpencart;
                })
                .collect(Collectors.toList());


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


    @Override
    public List<ProductOpencart> getProductsInitDataByCategory(List<CategoryOpencart> categoriesWithProduct, SupplierApp supplierApp) {

        List<GoodfoodOffer> offers = supplierApp.getGoodfoodOffers();

        List<ProductOpencart> resultProducts = offers
                .stream()
                .map(offer -> {

                    log.info("");
                    log.info("Offer: {}, {}", offer.getName(), offer.getCategoryId());
                    CategoryOpencart offerCategory = categoriesWithProduct
                            .stream()
                            .filter(c -> offer.getCategoryId().equals(c.getUrl()))
                            .findFirst()
                            .orElse(null);
                    String name = offerCategory.getDescriptions().get(0).getName();
                    log.info("Offer category id: {}, name: {}", offerCategory.getUrl(), name);

                    CategoryApp categoryApp = getCategoryApp(name, supplierApp);
                    List<CategoryOpencart> parentsCategories = getParentsCategories(offerCategory, categoriesWithProduct);
                    parentsCategories.forEach(pc -> log.info("Parent category: ID={}, name={}", pc.getUrl(), pc.getDescriptions().get(0).getName()));


                    String offerUrl = offer.getUrl();
                    log.info("offerUrl : {}", offerUrl);
                    String offerId = offer.getId();
                    log.info("offerId : {}", offerId);
                    String price = offer.getPrice();
                    log.info(" price: {}", price);
                    price = Objects.isNull(price) || price.isEmpty() ? "0" : price.replaceAll(",", ".");
                    log.info(" price new: {}", price);


                    String offerName = offer.getName();
                    log.info("offerName : {}", offerName);


                    String offerDescription = offer.getDescription();
                    int idxDesc = offerDescription.indexOf("Характеристики");
                    if (idxDesc != -1) {
                        offerDescription = offerDescription.substring(0, idxDesc);
                    }
                    String offerVendor = offer.getVendor();
                    log.info(" offerVendor: {}", offerVendor);
                    boolean available = offer.isAvailable();
                    List<GoodfoodParam> params = offer.getParams();
                    List<String> pictures = offer.getPicture();

                    String model = generateModel(offerId, "0000");

                    BigDecimal priceNumberFree = new BigDecimal(price).setScale(4);
                    ManufacturerApp manufacturerApp = getManufacturerApp(offerVendor, supplierApp);


                    ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                            .withSupplierId(supplierApp.getSupplierAppId())
                            .withSupplierApp(supplierApp)
                            .withCategoryId(categoryApp.getCategoryId())
                            .withCategoryApp(categoryApp)
                            .withManufacturerApp(manufacturerApp)
                            .withManufacturerId(manufacturerApp.getManufacturerId())
                            .withUrl(offerUrl)
                            .withSku(offerId)
                            .withTitle(offerName)
                            .withPrice(priceNumberFree)
                            .build();

                    ProductProfileApp savedProductProfile = getProductProfile(productProfileApp, supplierApp);
                    log.info("Product profile: {}", savedProductProfile);


                    ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                            .withDescription(offerDescription)
                            .withName(offerName)
                            .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                            .build();


                    ProductOpencart product = new ProductOpencart.Builder()
                            .withProductProfileApp(productProfileApp)
                            .withUrlImage(offerUrl)
                            .withProductsDescriptionOpencart(Arrays.asList(productDescriptionOpencart))
                            .withModel(model)
                            .withTitle(offerName)
                            .withSku(offerId)
                            .withStatus(available)
                            .withProductProfileApp(productProfileApp)
                            .withJan(supplierApp.getName())
                            .withPrice(priceNumberFree)
                            .withItuaOriginalPrice(priceNumberFree)
                            .build();

                    product.setCategoriesOpencart(parentsCategories);
                    setManufacturer(product, supplierApp);


                    List<AttributeWrapper> attributes = params
                            .stream()
                            .map(row -> {
                                String th = row.getName();
                                String td = row.getText();
                                if (Objects.isNull(th) || Objects.isNull(td) || th.isEmpty() || td.isEmpty()) {
                                    return null;
                                }
                                AttributeWrapper attributeWrapper = new AttributeWrapper(th, td, null);
                                AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
                                return attribute;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    product.getAttributesWrapper().addAll(attributes);


                    AtomicInteger countImage = new AtomicInteger();
                    pictures
                            .forEach(picture -> {
                                int count = countImage.addAndGet(1);
                                int idx = picture.lastIndexOf("/");

                                if (idx != -1) {
                                    String imgName = picture.substring(idx + 1);
                                    String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);

                                    downloadImage(picture, dbImgPath);

                                    if (count == 1) {
                                        product.setImage(dbImgPath);
                                    } else {
                                        ImageOpencart imageOpencart = new ImageOpencart.Builder()
                                                .withImage(dbImgPath)
                                                .withSortOrder(count)
                                                .build();
                                        product.getImagesOpencart().add(imageOpencart);
                                    }
                                }

                            });

                    return product;

                })
                .collect(Collectors.toList());

        return resultProducts;
    }

    public void updateAttributeValue() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);
        List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);
        List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);
        List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);
    }

    @Override
    public List<ProductOpencart> getFullProductsData(List<ProductOpencart> products, SupplierApp
            supplierApp) {
        AtomicInteger count = new AtomicInteger();
        List<ProductOpencart> fullProducts = products
                .stream()
                .peek(p -> {
                    setPriceWithMarkup(p);
                    ProductDescriptionOpencart descriptionOpencart = p.getProductsDescriptionOpencart().get(0);
                    String offerName = descriptionOpencart.getName();
                    offerName = translateService.getTranslatedText(offerName);

                    String offerDescription = descriptionOpencart.getDescription();
                    Document parse = Jsoup.parse(offerDescription);
                    parse = Jsoup.parse("<!DOCTYPE html>".concat(parse.outerHtml()));
                    String description = getDescription(parse);
                    log.info("offer clean desc: {}", description);

                    descriptionOpencart.setDescription(description);
                    descriptionOpencart.setName(offerName);
                    descriptionOpencart.setMetaH1(offerName);
                    descriptionOpencart.setMetaDescription(offerName.concat("  купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999"));
                    descriptionOpencart.setMetaTitle("Купити ".concat(offerName).concat("  інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника."));
                    descriptionOpencart.setMetaKeyword("Купити ".concat(offerName));

                })
                .filter(p -> p.getId() != -1)
                .filter(p -> !p.getSku().isEmpty())
                .collect(Collectors.toList());

        return fullProducts;
    }


    public String getDescription(Document doc) {
        Elements descElement = doc.select("body");
        String description = descElement.size() > 0 ? cleanDescription(descElement.get(0)) : "";
        String translatedText = translateService.getTranslatedText(description);
        String result = wrapToHtml(translatedText).replaceAll("[</br>\\\\]{3,}", "</br></br>");

        return result;
    }


    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {


        List<CategoryOpencart> subCategories = category.getSubGoodfoodCategories()
                .stream()
                .map(fc -> {

                    CategoryOpencart subCategory = new CategoryOpencart.Builder()
                            .withUrl(fc.getId())
                            .withTop(false)
                            .withParentCategory(category)
                            .withStatus(false)
                            .build();
                    CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                            .withName(fc.getText())
                            .withDescription(category.getDescriptions().get(0).getDescription())
                            .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                            .build();

                    subCategory.setSubGoodfoodCategories(fc.getSubCategories());
                    subCategory.getDescriptions().add(description);

                    recursiveWalkSiteCategory(subCategory);

                    return subCategory;

                })
                .collect(Collectors.toList());

        category.getCategoriesOpencart().addAll(subCategories);

        return category;
    }


    @Override
    public ProductProfileApp getProductProfile(ProductProfileApp productProfileApp, SupplierApp
            supplierApp) {
        List<ProductProfileApp> productProfilesAppDB = supplierApp.getProductProfilesApp();

        boolean contains = productProfilesAppDB.contains(productProfileApp);

        if (contains) {
            productProfileApp = productProfilesAppDB.get(productProfilesAppDB.indexOf(productProfileApp));
        } else {

            int manufacturerId = productProfileApp.getManufacturerId();

            if (manufacturerId == 0) {
                ManufacturerApp manufacturerApp = getManufacturerApp("", supplierApp);
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


    public void translateSupplierProducts() {
        SupplierApp supplierApp = new SupplierApp();
        supplierApp.setName(SUPPLIER_NAME);
        List<Integer> idSupplierProducts = opencartDaoService.getAllProductOpencartIdBySupplier(supplierApp);
        log.info("Count products: {}", idSupplierProducts.size());
        idSupplierProducts
                .stream()
                .forEach(p -> {
                    ProductOpencart productOpencart = opencartDaoService.getProductOpencartWithDescriptionById(p);
                    log.info("Product id: {}", productOpencart.getId());
                    List<ProductDescriptionOpencart> descriptionsOpencart = productOpencart.getProductsDescriptionOpencart();
                    if (!descriptionsOpencart.isEmpty()) {

                        ProductDescriptionOpencart descriptionOpencart = descriptionsOpencart.get(0);

                        String name = descriptionOpencart.getName();
                        name = translateService.getTranslatedText(name);


                        String description = descriptionOpencart.getDescription();
                        log.info("Desc: {}", description);
                        Document parse = Jsoup.parse(description);
                        parse = Jsoup.parse("<!DOCTYPE html>".concat(parse.outerHtml()));
                        description = getDescription(parse);

                        log.info("Translated desc: {}", description);
                        descriptionOpencart.setName(name);
                        descriptionOpencart.setDescription(description);
                        opencartDaoService.updateDescription(descriptionOpencart);
                    }
                });
    }

}
