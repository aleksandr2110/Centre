package orlov.home.centurapp.service.parser;

import com.beust.jcommander.converters.BigDecimalConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.ImageData;
import orlov.home.centurapp.dto.OpencartDto;
import orlov.home.centurapp.dto.app.ProductAppDto;
import orlov.home.centurapp.entity.app.*;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.service.api.translate.TranslateService;
import orlov.home.centurapp.service.appservice.FileService;
import orlov.home.centurapp.service.appservice.ScraperDataUpdateService;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;
import orlov.home.centurapp.util.AppConstant;
import orlov.home.centurapp.util.OCConstant;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static javax.management.Query.attr;


@Service
@Slf4j
public class ParserServiceRP extends ParserServiceAbstract {

    private final String SUPPLIER_NAME = "rp";
    private final String SUPPLIER_URL = "http://www.rp.ua/";
    private final String DISPLAY_NAME = "115 - РП УКРАЇНА";
    private final String URL_PART_PAGE = "&p=";
    private final String URL_FILE_LINK = "http://www.rp.ua/ru/prais";


    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final TranslateService translateService;
    private final FileService fileService;

    public ParserServiceRP(AppDaoService appDaoService, OpencartDaoService opencartDaoService, ScraperDataUpdateService scraperDataUpdateService, TranslateService translateService, FileService fileService) {
        super(appDaoService, opencartDaoService, scraperDataUpdateService, translateService, fileService);
        this.appDaoService = appDaoService;
        this.opencartDaoService = opencartDaoService;
        this.translateService = translateService;
        this.fileService = fileService;
    }


    @Override
    public void doProcess() {
        try {

            OrderProcessApp orderProcessApp = new OrderProcessApp();
            Timestamp start = new Timestamp(Calendar.getInstance().getTime().getTime());

            SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);

            List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);


            List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);

            log.info("Supplier products count: {}", productsFromSite.size());

            List<ProductOpencart> fullProductsData = getFullProductsData(productsFromSite, supplierApp);


            OpencartDto opencartInfo = getOpencartInfo(fullProductsData, supplierApp);


            checkPrice(opencartInfo, supplierApp);

            opencartInfo.getNewProduct()
                    .forEach(np -> {
                        ProductDescriptionOpencart descriptionOpencart = np.getProductsDescriptionOpencart().get(0);
                        String description = descriptionOpencart.getDescription();
                        String translatedDescription = translateDescription(description);
                        descriptionOpencart.setDescription(translatedDescription);
                    });

            opencartInfo.getNewProduct()
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

    public List<ProductAppDto> getProductsFromFile() {
        List<ProductAppDto> productAppDtoList = new ArrayList<>();
        try {
            Document doc = getWebDocument(URL_FILE_LINK, new HashMap<>());
            String fileUrl = doc.select("a.at_icon").attr("href");
            fileUrl = UriUtils.encodePath(fileUrl, "UTF-8");
            HttpURLConnection in = (HttpURLConnection) new URL(fileUrl).openConnection();
            in.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36");

            Workbook workbook = WorkbookFactory.create(in.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            AtomicInteger countFileProduct = new AtomicInteger();
            Iterator<Row> rowIterator = sheet.rowIterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                Cell cellCountOffer = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String url = Objects.isNull(cellCountOffer) ? "" : cellCountOffer.getStringCellValue().trim();

                if (url.startsWith("http") && !url.contains("#")) {

                    url = url.replace("//rp", "//www.rp");
                    String lastPath = url.substring(url.lastIndexOf("/"));
                    String sku = lastPath.replaceAll("\\D", "");

                    ProductAppDto productAppDto = new ProductAppDto();
                    Cell cellTitle = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String title = Objects.isNull(cellTitle) ? "" : cellTitle.getStringCellValue();
                    productAppDto.setTitle(title);
                    productAppDto.setUrl(url);
                    productAppDto.setSku(sku);
                    Cell cellPrice = row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    int price = Objects.isNull(cellPrice) ? 0 : (int) cellPrice.getNumericCellValue();
                    productAppDto.setPrice(new BigDecimal(price).setScale(4));

                    Cell cellBalance = row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    boolean isBalance = Objects.nonNull(cellBalance);
                    if (isBalance) {
                        CellType cellType = cellBalance.getCellType();
                        if (cellType == CellType.STRING) {
                            String balance = cellBalance.getStringCellValue();
                            productAppDto.setStatus(true);
                        } else if (cellType == CellType.NUMERIC) {
                            int numberBalance = (int) cellBalance.getNumericCellValue();
                            productAppDto.setStatus(false);
                        }

                    }
                    log.info("{}. File product:{}", countFileProduct.addAndGet(1), productAppDto);
                    productAppDtoList.add(productAppDto);
                }

            }


        } catch (IOException e) {
            log.error("Exception", e);
        }

        return productAppDtoList;
    }


    @Override
    public List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp) {

        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        Document doc = getWebDocument(supplierApp.getUrl(), new HashMap<>());
        log.info("Doc: {}", doc);
        if (Objects.nonNull(doc)) {
            List<CategoryOpencart> mainCategories = doc.select("div.t3-megamenu li")
                    .stream()
                    .filter(e -> !e.attr("data-id").equals("196"))
                    .map(ec -> {
                        String attr = ec.select("a").attr("href");

                        String url = SUPPLIER_URL.concat(attr.replace("/", ""));
                        log.info("Category url: {}", url);
                        String title = ec.text();
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


            CategoryOpencart fileCategory = new CategoryOpencart.Builder()
                    .withUrl(null)
                    .withParentCategory(supplierApp.getMainSupplierCategory())
                    .withParentId(supplierApp.getMainSupplierCategory().getCategoryId())
                    .withTop(false)
                    .withStatus(false)
                    .build();
            CategoryDescriptionOpencart fileDescription = new CategoryDescriptionOpencart.Builder()
                    .withName("File category")
                    .withDescription(supplierApp.getName())
                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                    .build();
            fileCategory.getDescriptions().add(fileDescription);
            mainCategories.add(fileCategory);

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

            return siteCategoryList;

        }
        return new ArrayList<>();

    }

    @Override
    public List<ProductOpencart> getProductsInitDataByCategory(List<CategoryOpencart> categoriesWithProduct, SupplierApp supplierApp) {
        List<ProductOpencart> supplierProducts = new ArrayList<>();
        List<ProductAppDto> productsFromFile = getProductsFromFile();
        List<ProductAppDto> productsFromWeb = new ArrayList<>();

        AtomicInteger countProduct = new AtomicInteger();

        categoriesWithProduct
                .stream()
                .filter(c -> {
                    int categoryId = c.getCategoryId();
                    List<CategoryOpencart> childrenCategory = categoriesWithProduct.stream().filter(sub -> sub.getParentId() == categoryId).collect(Collectors.toList());
                    return childrenCategory.isEmpty() && Objects.nonNull(c.getUrl());
                })
                .peek(c -> {

                    String name = c.getDescriptions().get(0).getName();
                    CategoryApp categoryApp = getCategoryApp(name, supplierApp);

                    String url = c.getUrl();
                    Document doc = getWebDocument(url, new HashMap<>());

                    List<CategoryOpencart> parentsCategories = getParentsCategories(c, categoriesWithProduct);


                    if (Objects.nonNull(doc)) {

                        Elements elementsProduct = doc.select("td >  a, h5 > a");

                        elementsProduct
                                .stream()
                                .peek(ep -> {

                                    try {

                                        String href = ep.attr("href").trim();
                                        if (href.contains("#")) {
                                            ep = null;
                                        } else {
                                            String productUrl = SUPPLIER_URL.concat(href.substring(1));
                                            int ibxStartUrlId = href.lastIndexOf("/");
                                            int ibxEndUrlId = href.indexOf("-", ibxStartUrlId);
                                            if (ibxStartUrlId != -1 && ibxEndUrlId != -1) {
                                                String newHref = href.substring(0, ibxStartUrlId);
                                                String hrefId = href.substring(ibxStartUrlId, ibxEndUrlId);
                                                productUrl = SUPPLIER_URL.concat(newHref.concat(hrefId).substring(1));
                                            }

                                            String sku = productUrl.substring(productUrl.lastIndexOf("/")).replaceAll("\\D", "");

                                            ProductAppDto webProductAppDto = new ProductAppDto();
                                            webProductAppDto.setUrl(productUrl);
                                            webProductAppDto.setSku(sku);

                                            if (!productsFromWeb.contains(webProductAppDto)) {
                                                productsFromWeb.add(webProductAppDto);
                                            }

                                            if (productsFromFile.contains(webProductAppDto)) {
                                                ProductAppDto productAppDtoFromFile = productsFromFile.get(productsFromFile.indexOf(webProductAppDto));
                                                webProductAppDto.setPrice(productAppDtoFromFile.getPrice());
                                                webProductAppDto.setTitle(productAppDtoFromFile.getTitle());
                                                webProductAppDto.setStatus(productAppDtoFromFile.isStatus());
                                            }

                                            boolean status = webProductAppDto.isStatus();
                                            String model = generateModel(sku, "0000");


                                            ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                                                    .withSupplierId(supplierApp.getSupplierAppId())
                                                    .withSupplierApp(supplierApp)
                                                    .withCategoryId(categoryApp.getCategoryId())
                                                    .withCategoryApp(categoryApp)
                                                    .withUrl(productUrl)
                                                    .withTitle(webProductAppDto.getTitle())
                                                    .withSku(sku)
                                                    .build();


                                            ProductOpencart wp = new ProductOpencart.Builder()
                                                    .withProductProfileApp(productProfileApp)
                                                    .withUrlProduct(productUrl)
                                                    .withModel(model)
                                                    .withSku(sku)
                                                    .withStockStatusId(status ? 7 : 8)
                                                    .withTitle(webProductAppDto.getTitle())
                                                    .withJan(supplierApp.getName())
                                                    .withPrice(webProductAppDto.getPrice())
                                                    .withItuaOriginalPrice(webProductAppDto.getPrice())
                                                    .build();

                                            wp.setCategoriesOpencart(parentsCategories);
                                            supplierProducts.add(wp);
//                                            log.info("{}. Web product: {}, sku: {}, model: {}, jan: {}, title: {}, price: {}, stock status: {}, ", countProduct.addAndGet(1), wp.getUrlProduct(), wp.getSku(), wp.getModel(), wp.getJan(), wp.getTitle(), wp.getPrice(), wp.getStockStatusId());


                                        }
                                    } catch (Exception ex) {
                                        log.error("Bad init data.", ex);
                                    }

                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                    }

                })
                .collect(Collectors.toList());


        List<ProductAppDto> onlyFile = productsFromFile
                .stream()
                .filter(pf -> !productsFromWeb.contains(pf))
                .collect(Collectors.toList());


        CategoryOpencart fileCategory = categoriesWithProduct
                .stream()
                .filter(fv -> fv.getDescriptions().get(0).getName().equals("File category"))
                .findFirst()
                .orElse(null);

        boolean hasFileCategory = Objects.nonNull(fileCategory);
        if (hasFileCategory) {


            AtomicInteger countUniqueProductFile = new AtomicInteger();
            onlyFile.forEach(pf -> {

                List<CategoryOpencart> parentsCategories = getParentsCategories(fileCategory, categoriesWithProduct);
                CategoryApp categoryApp = getCategoryApp(fileCategory.getDescriptions().get(0).getName(), supplierApp);

                String sku = pf.getSku();
                String url = pf.getUrl();
                BigDecimal price = pf.getPrice();
                String title = pf.getTitle();
                boolean status = pf.isStatus();
                ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                        .withSupplierId(supplierApp.getSupplierAppId())
                        .withSupplierApp(supplierApp)
                        .withCategoryId(categoryApp.getCategoryId())
                        .withCategoryApp(categoryApp)
                        .withPrice(price)
                        .withUrl(url)
                        .withTitle(title)
                        .withSku(sku)
                        .build();
                String model = generateModel(pf.getSku(), "0000");

                ProductOpencart fp = new ProductOpencart.Builder()
                        .withProductProfileApp(productProfileApp)
                        .withUrlProduct(url)
                        .withModel(model)
                        .withSku(sku)
                        .withStockStatusId(status ? 7 : 8)
                        .withTitle(title)
                        .withJan(supplierApp.getName())
                        .withPrice(price)
                        .withItuaOriginalPrice(price)
                        .build();


                fp.setCategoriesOpencart(parentsCategories);
                supplierProducts.add(fp);
//                log.info("{}. File product: {}, sku: {}, model: {}, jan: {}, title: {}, price: {}, stock status: {}, ", countUniqueProductFile.addAndGet(1), fp.getUrlProduct(), fp.getSku(), fp.getModel(), fp.getJan(), fp.getTitle(), fp.getPrice(), fp.getStockStatusId());

            });

        }


        return supplierProducts;
    }


    //  TODO
    public void updateMainImage() {
        List<ProductOpencart> allProductOpencartBySupplierAppName = opencartDaoService.getAllProductOpencartBySupplierAppName(SUPPLIER_NAME);
        log.info("allProductOpencartBySupplierAppName: {}", allProductOpencartBySupplierAppName.size());
        allProductOpencartBySupplierAppName.forEach(p -> {
            String image = p.getImage();
            image = AppConstant.PART_DIR_OC_IMAGE.concat(image);
            p.setImage(image);
            opencartDaoService.updateMainProductImageOpencart(p);
        });


    }

    public void downloadImages() {
        SupplierApp supplierApp = buildSupplierApp(SUPPLIER_NAME, DISPLAY_NAME, SUPPLIER_URL);

        List<CategoryOpencart> siteCategories = getSiteCategories(supplierApp);


        List<ProductOpencart> productsFromSite = getProductsInitDataByCategory(siteCategories, supplierApp);

        log.info("Supplier products count: {}", productsFromSite.size());
        AtomicInteger count = new AtomicInteger();
        productsFromSite
                .stream()
                .peek(p -> {
                    List<ImageOpencart> productImages = new ArrayList<>();
                    String urlProduct = p.getUrlProduct();
                    Document webDocument = getWebDocument(urlProduct, new HashMap<>());
                    log.info("{}. RP product info by url: {}", count.addAndGet(1), urlProduct);


                    if (Objects.nonNull(webDocument)) {

                        try {

                            Elements infoElements = webDocument.select("section.article-content.clearfix");
                            if (!infoElements.isEmpty()) {
                                Element infoElement = infoElements.get(0);

                                Element mainImageElement = infoElement.select("img").first();
                                String srcImage = Objects.isNull(mainImageElement) ? "" : mainImageElement.attr("src").replaceAll("_s\\.", "\\.");
                                String fullUrlMainImage = SUPPLIER_URL.concat(srcImage.substring(1));
                                String mainImageName = srcImage.substring(srcImage.lastIndexOf("/") + 1);
                                String dbMainImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(mainImageName);
                                downloadImage(fullUrlMainImage, dbMainImgPath);


                                Elements tables = infoElement.select("table");
                                Element tableElement = tables.isEmpty() ? null : tables.get(0);
                                if (Objects.nonNull(tableElement)) {

                                    AtomicInteger countImage = new AtomicInteger();
                                    productImages = infoElement.select("img[src^=/images]")
                                            .stream()
                                            .skip(1)
                                            .map(ie -> {
                                                String src = ie.attr("src").replaceAll("_s\\.", "\\.");
                                                String fullUrl = SUPPLIER_URL.concat(src.substring(1));

                                                String imgName = src.substring(src.lastIndexOf("/") + 1);
                                                log.info("Sub full url : {}", fullUrl);
                                                log.info("Sub image name: {}", imgName);
                                                String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
                                                log.info("Sub db img path: {}", dbImgPath);
                                                downloadImage(fullUrl, dbImgPath);

                                                ImageOpencart imageOpencart = new ImageOpencart.Builder()
                                                        .withImage(dbImgPath)
                                                        .withSortOrder(countImage.addAndGet(1))
                                                        .build();
                                                return imageOpencart;

                                            })
                                            .collect(Collectors.toList());

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

    }


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
                    List<ImageOpencart> productImages = new ArrayList<>();
                    String urlProduct = p.getUrlProduct();
                    Document webDocument = getWebDocument(urlProduct, new HashMap<>());
                    log.info("{}. RP product info by url: {}", count.addAndGet(1), urlProduct);


                    if (Objects.nonNull(webDocument)) {

                        try {

                            String title = webDocument.select("h1.article-title").text().trim();
//                            log.info("Title from web: {}", title);
                            String manufacturer = webDocument.select("div.content-links").text().replaceAll("(Все|товары|производства)", "").trim();
//                            log.info("Manufacturer from web: {}", manufacturer);

                            String description = getDescription(webDocument);
//                            log.info("Description: {}", description);

                            log.info("Description final: {}", description);
                            Elements priceMetaElement = webDocument.select("meta[itemprop=price]");

                            String currencyStringPrice = webDocument.select("span#courseEuro").text().replaceAll(",", "\\.").trim();
                            log.info("currencyStringPrice: {}", currencyStringPrice);
                            BigDecimal currency = new BigDecimal(currencyStringPrice).setScale(4);
                            log.info("currency: {}", currency);

                            BigDecimal price = p.getPrice();
                            if (!priceMetaElement.isEmpty()) {
                                String priceString = priceMetaElement.attr("content");
                                price = new BigDecimal(priceString).setScale(4);
                            } else if (!webDocument.select("td:contains(ЦЕНА)").isEmpty()) {
                                Elements tablePriceElement = webDocument.select("td:contains(ЦЕНА)");
                                String stringPriceTable = tablePriceElement.isEmpty() ? "0" : tablePriceElement.get(0).text().replaceAll("\\D", "");
                                price = new BigDecimal(stringPriceTable).setScale(4);
                            }

                            if (!price.equals(new BigDecimal("0.0"))) {
                                price = new BigDecimal(price.multiply(currency).setScale(4).toString());
                                log.info("price: {}", price);
                            }


                            ManufacturerApp manufacturerApp = getManufacturerApp(manufacturer, supplierApp);
                            ProductProfileApp firstProfileApp = p.getProductProfileApp();
                            firstProfileApp.setManufacturerId(manufacturerApp.getManufacturerId());
                            firstProfileApp.setManufacturerApp(manufacturerApp);
                            ProductProfileApp savedProductProfile = getProductProfile(firstProfileApp, supplierApp);
//                            savedProductProfile.setPrice(price);
                            p.setProductProfileApp(savedProductProfile);

                            p.setPrice(price);
                            p.setItuaOriginalPrice(price);


                            Elements infoElements = webDocument.select("section.article-content.clearfix");
                            if (!infoElements.isEmpty()) {
                                Element infoElement = infoElements.get(0);

                                Element mainImageElement = infoElement.select("img").first();
                                String srcImage = Objects.isNull(mainImageElement) ? "" : mainImageElement.attr("src").replaceAll("_s\\.", "\\.");
//                                log.info("src main img: {}", srcImage);
                                String fullUrlMainImage = SUPPLIER_URL.concat(srcImage.substring(1));

                                String mainImageName = srcImage.substring(srcImage.lastIndexOf("/") + 1);
//                                log.info("Main full url : {}", fullUrlMainImage);
//                                log.info("Main image name: {}", mainImageName);
                                String dbMainImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(mainImageName);
//                                log.info("Main db img path: {}", dbMainImgPath);
                                downloadImage(fullUrlMainImage, dbMainImgPath);
                                p.setImage(dbMainImgPath);

                                Elements tables = infoElement.select("table");
                                Element tableElement = tables.isEmpty() ? null : tables.get(0);
                                if (Objects.nonNull(tableElement)) {
                                    List<AttributeWrapper> attributes = tableElement
                                            .select("tr")
                                            .stream()
                                            .map(row -> {
                                                Elements tdElements = row.select("td");
                                                if (tdElements.size() == 2 && !tdElements.text().trim().isEmpty()) {
                                                    String key = tdElements.get(0).text();
                                                    String value = tdElements.get(1).text();
                                                    log.info("");
                                                    AttributeWrapper attributeWrapper = new AttributeWrapper(key, value, null);
//                                                    log.info("Begin attribute: {}", attributeWrapper);
                                                    AttributeWrapper attribute = getAttribute(attributeWrapper, supplierApp, savedProductProfile);
//                                                    log.info("Finish attribute: {}", attribute);
                                                    return attribute;
                                                } else {
                                                    return null;
                                                }
                                            })
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());
                                    p.getAttributesWrapper().addAll(attributes);


                                    AtomicInteger countImage = new AtomicInteger();
                                    productImages = infoElement.select("img[src^=/images]")
                                            .stream()
                                            .skip(1)
                                            .map(ie -> {
                                                String src = ie.attr("src").replaceAll("_s\\.", "\\.");
                                                String fullUrl = SUPPLIER_URL.concat(src.substring(1));

                                                String imgName = src.substring(src.lastIndexOf("/") + 1);
                                                log.info("Sub full url : {}", fullUrl);
                                                log.info("Sub image name: {}", imgName);
                                                String dbImgPath = AppConstant.PART_DIR_OC_IMAGE.concat(DISPLAY_NAME.concat("/")).concat(imgName);
                                                log.info("Sub db img path: {}", dbImgPath);
                                                downloadImage(fullUrl, dbImgPath);


                                                ImageOpencart imageOpencart = new ImageOpencart.Builder()
                                                        .withImage(dbImgPath)
                                                        .withSortOrder(countImage.addAndGet(1))
                                                        .build();
                                                return imageOpencart;

                                            })
                                            .collect(Collectors.toList());

                                }
                            }


                            setManufacturer(p, supplierApp);
//                            setPriceWithMarkup(p);

                            ProductDescriptionOpencart productDescriptionOpencart = new ProductDescriptionOpencart.Builder()
                                    .withDescription(description)
                                    .withName(title)
                                    .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                                    .withMetaH1(title)
                                    .withMetaDescription(title.concat("  купляйте в інтернет-магазині CENTUR за Найнижчою Ціною. Швидка Доставка. Офіційна Гарантія від Виробника. Акції, Знижки, Розпродажі! ☎ 0 800 307 999"))
                                    .withMetaTitle("Купити ".concat(title).concat("  інтернет-магазин CENTUR. Прямі Поставки. Ціни від Виробника."))
                                    .withMetaKeyword("Купити ".concat(title))
                                    .build();
                            p.getProductsDescriptionOpencart().add(productDescriptionOpencart);
                            p.setImagesOpencart(productImages);

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

    public String translateDescription(String description) {
        String desc = cleanDescription(Jsoup.parse(description));
        desc = translateService.getTranslatedText(desc);
        return wrapToHtml(desc);
    }


    public String getDescription(Document doc) {
        Elements descElements = doc.select("section.article-content.clearfix > p:lt(1)");
        String description = descElements.size() == 0 ? "" : descElements.get(0).text();
        Elements infoElement = doc.select("section.article-content.clearfix");
        Elements uls = infoElement.select("ul");
        Element otherDescElement = uls.isEmpty() ? null : uls.get(0);
        if (Objects.nonNull(otherDescElement)) {
            otherDescElement = otherDescElement.parents().first();
            if (Objects.nonNull(otherDescElement)) {
                String desc = cleanDescription(Jsoup.parse(description));
                String therDesc = cleanDescription(otherDescElement);
                desc = desc.concat("\n\n").concat(therDesc);
                description = wrapToHtml(desc);
            }
        }
        log.info("Description UA text: {}", description);
        return wrapToHtml(description);
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
                        log.info("Title before translate: {}", name);
                        name = translateService.getTranslatedText(name);

                        log.info("Title after translate: {}", name);
                        descriptionOpencart.setName(name);
                        opencartDaoService.updateDescription(descriptionOpencart);
                    }
                });
    }


    @Override
    public CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category) {
        String url = category.getUrl();
        if (Objects.isNull(url)) {
            return category;
        }
        Document doc = getWebDocument(url, new HashMap<>());
        log.info("Get subcategories of category: {}", category.getDescriptions().get(0).getName());
        if (Objects.nonNull(doc)) {
            Elements subCateElements = doc.select("table.category.table.table-striped.table-bordered.table-hover.table-noheader tbody tr");

            List<CategoryOpencart> subCategories = subCateElements
                    .stream()
                    .map(el -> {
                        String attr = el.select("a").attr("href");
                        String subUrl = SUPPLIER_URL.concat(attr.substring(1));
                        log.info("subUrl: {}", subUrl);
                        String subTitle = el.text();
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
            String manufacturer = "";
            if (manufacturerId == 0) {
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
