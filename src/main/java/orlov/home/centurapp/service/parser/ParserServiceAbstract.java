package orlov.home.centurapp.service.parser;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@Slf4j
@AllArgsConstructor
public abstract class ParserServiceAbstract implements ParserService {

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;
    private final ScraperDataUpdateService scraperDataUpdateService;
    private final TranslateService translateService;
    private final FileService fileService;


    @Override
    public AttributeOpencart getAttributeOpencartByNameIfHas(List<AttributeOpencart> attributes, String name) {
        AttributeDescriptionOpencart attributeDescriptionOpencart = attributes
                .stream()
                .flatMap(attr -> attr.getDescriptions().stream())
                .filter(desc -> desc.getName().trim().equals(name.trim()))
                .findFirst()
                .orElse(null);
        if (Objects.nonNull(attributeDescriptionOpencart)) {
            AttributeOpencart attributeOpencart = attributes
                    .stream()
                    .filter(attr -> attr.getAttributeId() == attributeDescriptionOpencart.getAttributeId())
                    .findFirst()
                    .orElse(null);
            return attributeOpencart;
        }
        return null;
    }

    @Override
    public int getLastSortedAttribute(List<AttributeOpencart> attributesOpencart) {
        int sortOrder = attributesOpencart
                .stream()
                .mapToInt(AttributeOpencart::getSortOrder)
                .max()
                .orElse(0);
        return sortOrder;
    }

    @Override
    public Document getWebDocument(String url, Map<String, String> cookies) {
        Document document = null;
        int countBadConnection = 0;
        boolean hasConnection = false;

        while (!hasConnection && countBadConnection < AppConstant.BAD_CONNECTION_LIMIT) {
            try {
                Connection.Response response = Jsoup.connect(url)
                        .maxBodySize(0)
                        .timeout(60 * 1000)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36")
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .cookies(cookies)
                        .execute();
                int statusCode = response.statusCode();

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

    @Override
    public ProductOpencart setManufacturer(ProductOpencart product, SupplierApp supplierApp) {
        List<ManufacturerOpencart> manufacturerOpencartDB = supplierApp.getManufacturerOpencartDB();

        ProductProfileApp productProfileApp = product.getProductProfileApp();
        String manufactureName = productProfileApp.getManufacturerApp().getSupplierTitle();
        log.info("manufacturerName: {}", manufactureName);

        ManufacturerApp manufacturerApp = getManufacturerApp(manufactureName, supplierApp);

        log.info("manufacturerApp: {}", manufacturerApp);
        String opencartTitle = manufacturerApp.getOpencartTitle();

        if (!opencartTitle.isEmpty()) {
            ManufacturerOpencart manufacturerOpencart = manufacturerOpencartDB
                    .stream()
                    .filter(m -> m.getName().trim().equals(opencartTitle.trim()))
                    .findFirst()
                    .orElse(null);
            log.info("!opencartTitle.isEmpty(). manufacturerOpencart: {}", manufacturerOpencart);
            if (Objects.nonNull(manufacturerOpencart)) {
                int manufacturerId = manufacturerOpencart.getManufacturerId();
                product.setManufacturerId(manufacturerId);
            } else {
                log.warn("Manufacturer with name: {} not found", opencartTitle);
            }
        } else {
            ManufacturerOpencart manufacturerOpencart = manufacturerOpencartDB
                    .stream()
                    .filter(m -> m.getName().trim().equalsIgnoreCase(manufacturerApp.getSupplierTitle().trim()))
                    .findFirst()
                    .orElse(null);
            log.info("Manu by name supplier: {}", manufacturerOpencart);
            if (Objects.nonNull(manufacturerOpencart)) {
                manufacturerApp.setOpencartTitle(manufacturerOpencart.getName());
                appDaoService.updateManufacturerApp(manufacturerApp);
                int manufacturerId = manufacturerOpencart.getManufacturerId();
                product.setManufacturerId(manufacturerId);
            }
        }

        return product;

    }

    @Override
    public ManufacturerApp getManufacturerApp(String manufacturerName, SupplierApp supplierApp) {
        List<ManufacturerApp> manufacturersAppDB = supplierApp.getManufacturersAppDB();

        ManufacturerApp manufacturerApp = manufacturersAppDB
                .stream()
                .filter(m -> m.getSupplierTitle().equals(manufacturerName))
                .findFirst()
                .orElse(null);
        log.info("Get manufacturerApp by name manufacturerName: {}", manufacturerName);
        if (Objects.isNull(manufacturerApp)) {
            manufacturerApp = new ManufacturerApp.Builder()
                    .withSupplierId(supplierApp.getSupplierAppId())
                    .withSupplierTitle(manufacturerName)
                    .withMarkup(0)
                    .build();
            appDaoService.saveManufacturerApp(manufacturerApp);
            manufacturersAppDB.add(manufacturerApp);
        }
        return manufacturerApp;
    }


    @Override
    public AttributeWrapper getAttribute(AttributeWrapper a, SupplierApp supplierApp, ProductProfileApp productProfileApp) {
        List<AttributeApp> attributesAppDB = supplierApp.getAttributesAppDB();
        List<AttributeOpencart> attributesOpencartDB = supplierApp.getAttributesOpencartDB();
        AttributeGroupOpencart defaultAttributeGroup = supplierApp.getDefaultGlobalAttributeGroup();

        AttributeApp attributeAppByName = getAttributeAppByNameIfHas(attributesAppDB, a.getKeySite());

        if (Objects.isNull(attributeAppByName)) {
            AttributeApp newAttributeApp = new AttributeApp.Builder()
                    .withSupplierId(supplierApp.getSupplierAppId())
                    .withSupplierTitle(a.getKeySite())
                    .build();
            attributeAppByName = appDaoService.saveAttributeApp(newAttributeApp);
            attributesAppDB.add(attributeAppByName);
        }

        ProductAttributeApp productAttributeById = appDaoService.getProductAttributeById(productProfileApp.getProductProfileId(), attributeAppByName.getAttributeId());

        if (Objects.isNull(productAttributeById)) {
            ProductAttributeApp newProductAttributeApp = new ProductAttributeApp();
            newProductAttributeApp.setProductProfileAppId(productProfileApp.getProductProfileId());
            newProductAttributeApp.setAttributeAppId(attributeAppByName.getAttributeId());
            newProductAttributeApp.setAttributeValue(a.getValueSite());
            appDaoService.saveProductAttributeApp(newProductAttributeApp);
        }

        String opencartTitle = attributeAppByName.getOpencartTitle();

        if (opencartTitle.isEmpty()) {

            AttributeOpencart attrByNameIfHas = getAttributeOpencartByNameIfHas(attributesOpencartDB, a.getKeySite());
            if (Objects.isNull(attrByNameIfHas)) {
                int lastSortedAttribute = getLastSortedAttribute(attributesOpencartDB);
                AttributeOpencart newAttributeOpencart = new AttributeOpencart.Builder()
                        .withAttributeGroupId(defaultAttributeGroup.getAttributeGroupId())
                        .withSortOrder(++lastSortedAttribute)
                        .build();
                AttributeOpencart attributeOpencart = opencartDaoService.saveAttributeOpencart(newAttributeOpencart);
                AttributeDescriptionOpencart attributeDescriptionOpencart = new AttributeDescriptionOpencart.Builder()
                        .withName(a.getKeySite())
                        .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                        .withAttributeId(attributeOpencart.getAttributeId())
                        .build();
                opencartDaoService.saveAttributeDescriptionOpencart(attributeDescriptionOpencart);
                attributeOpencart.getDescriptions().add(attributeDescriptionOpencart);
                a.setAttributeOpencart(attributeOpencart);
                attributesOpencartDB.add(attributeOpencart);
            } else {
                a.setAttributeOpencart(attrByNameIfHas);
            }

        } else {

            AttributeOpencart attrByNameIfHas = getAttributeOpencartByNameIfHas(attributesOpencartDB, opencartTitle);

            if (Objects.isNull(attrByNameIfHas)) {

                attrByNameIfHas = getAttributeOpencartByNameIfHas(attributesOpencartDB, a.getKeySite());

                if (Objects.isNull(attrByNameIfHas)) {
                    int lastSortedAttribute = getLastSortedAttribute(attributesOpencartDB);
                    AttributeOpencart newAttributeOpencart = new AttributeOpencart.Builder()
                            .withAttributeGroupId(defaultAttributeGroup.getAttributeGroupId())
                            .withSortOrder(++lastSortedAttribute)
                            .build();
                    AttributeOpencart attributeOpencart = opencartDaoService.saveAttributeOpencart(newAttributeOpencart);
                    AttributeDescriptionOpencart attributeDescriptionOpencart = new AttributeDescriptionOpencart.Builder()
                            .withName(a.getKeySite())
                            .withLanguageId(OCConstant.UA_LANGUAGE_ID)
                            .withAttributeId(attributeOpencart.getAttributeId())
                            .build();
                    opencartDaoService.saveAttributeDescriptionOpencart(attributeDescriptionOpencart);
                    attributeOpencart.getDescriptions().add(attributeDescriptionOpencart);
                    a.setAttributeOpencart(attributeOpencart);
                    attributesOpencartDB.add(attributeOpencart);
                } else {
                    a.setAttributeOpencart(attrByNameIfHas);
                }

            } else {
                a.setAttributeOpencart(attrByNameIfHas);
            }
        }

        String newAttributeValue = changeAttributeValue(attributeAppByName, a.getValueSite());
        a.setValueSite(newAttributeValue);

        return a;
    }

    public String changeAttributeValue(AttributeApp attributeApp, String value) {

        String replacementFrom = attributeApp.getReplacementFrom();
        String replacementTo = attributeApp.getReplacementTo();
        String mathSign = attributeApp.getMathSign();
        int mathNumber = attributeApp.getMathNumber();

        if (!replacementFrom.isEmpty()) {
            value = value.replaceAll(replacementFrom, replacementTo);
        }

        if (mathNumber > 0 && !mathSign.isEmpty()) {

            Pattern pattern = Pattern.compile("\\d+,*\\.*\\d+|\\d+");
            Matcher matcher = pattern.matcher(value);

            double result = 0;

            if (matcher.find()) {
                String foundNumberText = matcher.group();
                double foundDouble = Double.parseDouble(foundNumberText.replaceAll(",", "."));
                if (mathSign.equals("+")) {
                    result = foundDouble + mathNumber;
                } else if (mathSign.equals("-")) {
                    result = foundDouble - mathNumber;
                } else if (mathSign.equals("/")) {
                    result = foundDouble / mathNumber;
                } else if (mathSign.equals("*")) {
                    result = foundDouble * mathNumber;
                }
                DecimalFormat df = new DecimalFormat("#.##");
                String format = df.format(result);
                value = value.replaceAll(foundNumberText, format);

            }

        }

        return value;
    }


    @Override
    public void downloadImage(String url, String imageName) {
        fileService.downloadImg(url, imageName);
    }

    @Override
    public CategoryApp getCategoryApp(String name, SupplierApp supplierApp) {

        List<CategoryApp> categoryAppDB = supplierApp.getCategoryAppDB();

        CategoryApp categoryApp = categoryAppDB
                .stream()
                .filter(c -> c.getSupplierTitle().equals(name))
                .findFirst()
                .orElse(null);

        if (Objects.isNull(categoryApp)) {
            categoryApp = appDaoService.saveCategoryApp(new CategoryApp.Builder()
                    .withSupplierId(supplierApp.getSupplierAppId())
                    .withSupplierTitle(name)
                    .build());
            categoryAppDB.add(categoryApp);
        }

        return categoryApp;

    }

    @Override
    public AttributeApp getAttributeAppByNameIfHas(List<AttributeApp> attributesAppDB, String name) {
        return attributesAppDB
                .stream()
                .filter(a -> a.getSupplierTitle().equals(name))
                .findFirst()
                .orElse(null);
    }


    @Override
    public List<CategoryOpencart> getParentsCategories(CategoryOpencart lowerCategory, List<CategoryOpencart> siteCategory) {

        List<CategoryOpencart> parentsCategories = new ArrayList<>();
        parentsCategories.add(lowerCategory);
        int parentId = lowerCategory.getParentId();

        while (parentId != 0) {
            for (CategoryOpencart cat : siteCategory) {
                if (cat.getCategoryId() == parentId) {
                    parentsCategories.add(cat);
                    parentId = cat.getParentId();
                }
            }
        }

        return parentsCategories;
    }

    @Override
    public CategoryOpencart findCategoryFromDBListByName(CategoryOpencart newCategory, List<CategoryOpencart> categoriesOpencartDB) {

        CategoryDescriptionOpencart newDescription = newCategory
                .getDescriptions()
                .stream()
                .filter(cd -> cd.getLanguageId() == OCConstant.UA_LANGUAGE_ID)
                .findFirst()
                .orElse(null);

        CategoryOpencart categoryOpencart = null;
        if (Objects.nonNull(newDescription)) {
            categoryOpencart = categoriesOpencartDB
                    .stream()
                    .filter(c -> {
                        List<CategoryDescriptionOpencart> descriptions = c.getDescriptions();
                        CategoryDescriptionOpencart foundDesc = descriptions
                                .stream()
                                .filter(dc -> dc.getName().equals(newDescription.getName()) && c.getParentId() == newCategory.getParentId())
                                .findFirst()
                                .orElse(null);
                        return Objects.nonNull(foundDesc);
                    })
                    .findFirst()
                    .orElse(null);

            if (Objects.isNull(categoryOpencart)) {
                log.info("New Category opencart: {}", newDescription.getName());
                categoryOpencart = opencartDaoService.saveCategoryOpencart(newCategory);
                categoriesOpencartDB.add(categoryOpencart);
            } else {
                categoryOpencart.setCategoriesOpencart(newCategory.getCategoriesOpencart());
                categoryOpencart.setParentCategory(newCategory.getParentCategory());
                categoryOpencart.setUrl(newCategory.getUrl());
            }

        }

        return categoryOpencart;
    }


    @Override
    public void checkPrice(OpencartDto opencartInfo, SupplierApp supplierApp) {
        List<ProductOpencart> productsOpencartDB = opencartInfo.getProductsOpencartDB();
        List<ProductOpencart> availableProducts = opencartInfo.getAvailableProducts();

        List<ProductApp> newPriceProductAppList = new ArrayList<>();
        List<ProductOpencart> newPriceProducts = productsOpencartDB
                .stream()
                .filter(availableProducts::contains)
                .filter(p -> {

                    ProductOpencart availableProduct = availableProducts.get(availableProducts.indexOf(p));
                    ProductProfileApp newProductProfile = availableProduct.getProductProfileApp();
                    ProductProfileApp productProfileDB = getProductProfile(availableProduct.getProductProfileApp(), supplierApp);
                    BigDecimal priceNEW = availableProduct.getPrice();
                    BigDecimal priceNOW = productProfileDB.getPrice();
                    log.info("");
                    boolean notEqualsPrice = !priceNEW.equals(priceNOW);
                    log.info("Product SKU: {} with price: {} new: {} old: {}", p.getSku(), notEqualsPrice, priceNEW, priceNOW);

                    if (notEqualsPrice) {
                        log.info("UPDATED profile: \nNEW {}, \nOLD{}", priceNEW, priceNOW);
                        productProfileDB.setPrice(priceNEW);
                        availableProduct.setProductProfileApp(productProfileDB);
                        setPriceWithMarkup(availableProduct);

                        appDaoService.updateProductProfileApp(productProfileDB);
                        newPriceProductAppList.add(new ProductApp.Builder()
                                .withName(availableProduct.getSku())
                                .withUrl(Objects.isNull(availableProduct.getUrlProduct()) ? "" : availableProduct.getUrlProduct())
                                .withStatus("price")
                                .withNewPrice(priceNEW)
                                .withOldPrice(priceNOW)
                                .build());

                        p.setPrice(availableProduct.getPrice());
                        p.setItuaOriginalPrice(availableProduct.getPrice());
                    }
                    return notEqualsPrice;
                })
                .collect(Collectors.toList());


        newPriceProducts
                .forEach(opencartDaoService::updatePriceProductOpencart);
        log.info("New price products count: {}", newPriceProducts.size());
        newPriceProducts.forEach(p -> log.debug("new price product: {}", p));
        opencartInfo.getOrderProcessApp().setNewPriceProduct(newPriceProductAppList);
    }

    public void checkStockStatusId(OpencartDto opencartInfo, SupplierApp supplierApp) {
        List<ProductOpencart> productsOpencartDB = opencartInfo.getProductsOpencartDB();
        List<ProductOpencart> availableProducts = opencartInfo.getAvailableProducts();

        availableProducts
                .stream()
                .peek(ap -> {
                    ProductOpencart productOpencart = productsOpencartDB.get(productsOpencartDB.indexOf(ap));
                    if (productOpencart.getStockStatusId() != ap.getStockStatusId()) {
                        productOpencart.setStockStatusId(ap.getStockStatusId());
                        opencartDaoService.updateStockStatus(productOpencart);
                        log.info("Update stock status id product with id: {}", productOpencart.getId());
                    }
                })
                .collect(Collectors.toList());

    }

    @Override
    public CategoryOpencart getGlobalCategory() {

        CategoryOpencart categoryOpencart = opencartDaoService.getCategoryOpencartByNameAndDescription(AppConstant.GLOBAL_SUPPLIER_CATEGORY_NAME, AppConstant.GLOBAL_SUPPLIER_CATEGORY_KEY);

        if (Objects.isNull(categoryOpencart)) {
            CategoryOpencart newCategoryOpencart = new CategoryOpencart.Builder()
                    .withColumn(1)
                    .withTop(true)
                    .withStatus(false)
                    .build();
            CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                    .withName(AppConstant.GLOBAL_SUPPLIER_CATEGORY_NAME)
                    .withDescription(AppConstant.GLOBAL_SUPPLIER_CATEGORY_KEY)
                    .build();
            newCategoryOpencart.getDescriptions().add(description);
            categoryOpencart = opencartDaoService.saveCategoryOpencart(newCategoryOpencart);
        }

        log.info("Global supplier category: {}", categoryOpencart);
        return categoryOpencart;
    }


    @Override
    public CategoryOpencart findMainSupplierCategory(SupplierApp supplierApp) {
        List<CategoryOpencart> supplierCategoryOpencartDB = supplierApp.getCategoryOpencartDB();
        CategoryOpencart globalSupplierCategory = supplierApp.getGlobalSupplierCategory();
        CategoryOpencart mainSupplierCategory = supplierCategoryOpencartDB
                .stream()
                .filter(c -> {
                    List<CategoryDescriptionOpencart> descriptions = c.getDescriptions();
                    CategoryDescriptionOpencart foundDesc = descriptions
                            .stream()
                            .filter(dc -> dc.getName().equals(supplierApp.getDisplayName()) && c.getParentId() == globalSupplierCategory.getCategoryId())
                            .findFirst()
                            .orElse(null);
                    return Objects.nonNull(foundDesc);
                })
                .findFirst()
                .orElse(null);
        log.info("Has main category: {}", Objects.nonNull(mainSupplierCategory));

        if (Objects.isNull(mainSupplierCategory)) {
            CategoryOpencart newCategoryOpencart = new CategoryOpencart.Builder()
                    .withTop(false)
                    .withStatus(false)
                    .withParentId(supplierApp.getGlobalSupplierCategory().getCategoryId())
                    .build();
            CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                    .withName(supplierApp.getDisplayName())
                    .withDescription(supplierApp.getName())
                    .build();
            newCategoryOpencart.getDescriptions().add(description);
            mainSupplierCategory = opencartDaoService.saveCategoryOpencart(newCategoryOpencart);
            supplierCategoryOpencartDB.add(mainSupplierCategory);
        }
        return mainSupplierCategory;
    }


    @Override
    public SupplierApp buildSupplierApp(String supplierName, String displayName, String supplierUrl) {
        SupplierApp supplierApp = appDaoService.getSupplierAppByName(supplierName);
        log.info("Has supplier app with name: {} ", Objects.isNull(supplierApp));
        if (Objects.isNull(supplierApp)) {
            log.info("Save new supplier app with name:{}", supplierName);
            SupplierApp newSupplierApp = new SupplierApp.Builder()
                    .withName(supplierName)
                    .withDisplayName(displayName)
                    .withUrl(supplierUrl)
                    .build();

            supplierApp = appDaoService.saveSupplierApp(newSupplierApp);
        }

        CategoryOpencart globalSupplierCategory = getGlobalCategory();
        supplierApp.setGlobalSupplierCategory(globalSupplierCategory);
        List<CategoryOpencart> categoryOpencartDB = opencartDaoService.getAllSupplierCategoryOpencart(supplierApp);
        categoryOpencartDB.add(globalSupplierCategory);
        log.info("Supplier category opencart from database: {}", categoryOpencartDB.size());
        supplierApp.setCategoryOpencartDB(categoryOpencartDB);
        List<CategoryApp> categoryAppDB = appDaoService.getAllCategoryAppBySupplierAppId(supplierApp.getSupplierAppId());
        log.info("Supplier category app from database: {}", categoryAppDB.size());
        supplierApp.setCategoryAppDB(categoryAppDB);
        CategoryOpencart mainSupplierCategory = findMainSupplierCategory(supplierApp);
        supplierApp.setMainSupplierCategory(mainSupplierCategory);
        log.info("Main supplier category: {}, {}", mainSupplierCategory.getCategoryId(), mainSupplierCategory.getDescriptions().get(0).getName());
        AttributeGroupOpencart defaultAttributeGroup = opencartDaoService.getDefaultGlobalAttributeOpencartGroupByName(OCConstant.DEFAULT_ATTR_GROUP_NAME);
        supplierApp.setDefaultGlobalAttributeGroup(defaultAttributeGroup);
        log.info("GLOBAL ATTRIBUTE GROUP: {}", defaultAttributeGroup);
        List<AttributeOpencart> attributesOpencartDB = opencartDaoService.getAllAttributeOpencartWithDesc();
        supplierApp.setAttributesOpencartDB(attributesOpencartDB);
        List<AttributeApp> attributesAppDB = appDaoService.getAllAttributeAppBySupplierAppId(supplierApp.getSupplierAppId());
        supplierApp.setAttributesAppDB(attributesAppDB);
        List<ManufacturerOpencart> manufacturersOpencartDB = opencartDaoService.getAllManufacturerOpencart();
        supplierApp.setManufacturerOpencartDB(manufacturersOpencartDB);
        List<ManufacturerApp> manufacturersAppDB = appDaoService.getAllManufacturerAppBySupplierId(supplierApp.getSupplierAppId());
        supplierApp.setManufacturersAppDB(manufacturersAppDB);
        List<ProductProfileApp> productProfilesAppDB = appDaoService.getAllProductProfileAppBySupplierId(supplierApp.getSupplierAppId());
        supplierApp.setProductProfilesApp(productProfilesAppDB);
        List<OptionOpencart> allOptionOpencartFullData = opencartDaoService.getAllOptionOpencartFullData();
        supplierApp.setOptionOpencartList(allOptionOpencartFullData);
        return supplierApp;
    }


    public OptionOpencart checkPersistOptionOpencart(OptionOpencart o, SupplierApp supplierApp) {
        List<OptionOpencart> optionsDatabase = supplierApp.getOptionOpencartList();

        String optionName = o.getDescriptions().get(0).getName();
        OptionOpencart searchOptionOpencart = optionsDatabase
                .stream()
                .filter(odb -> {
                    List<OptionDescriptionOpencart> descriptionsDatabase = odb.getDescriptions();
                    OptionDescriptionOpencart searchDescription = descriptionsDatabase
                            .stream()
                            .filter(d -> d.getLanguageId() == OCConstant.UA_LANGUAGE_ID && d.getName().equals(optionName))
                            .findFirst()
                            .orElse(null);
                    return Objects.nonNull(searchDescription);
                })
                .findFirst()
                .orElse(null);

        log.info("Option {} is null {}", optionName, Objects.isNull(searchOptionOpencart));
        if (Objects.isNull(searchOptionOpencart)) {

            OptionOpencart savedOption = opencartDaoService.saveOption(o);

            savedOption
                    .getDescriptions()
                    .stream()
                    .forEach(desc -> {
                        desc.setOptionId(savedOption.getOptionId());
                        opencartDaoService.saveOptionDescription(desc);
                    });

            savedOption
                    .getValues()
                    .stream()
                    .forEach(v -> {
                        v.setOptionId(savedOption.getOptionId());
                        OptionValueOpencart savedOptionValueOpencart = opencartDaoService.saveOptionValueOpencart(v);

                        savedOptionValueOpencart.getDescriptionValue()
                                .stream()
                                .forEach(desc -> {
                                    desc.setOptionValueId(savedOptionValueOpencart.getOptionValueId());
                                    desc.setOptionId(savedOptionValueOpencart.getOptionId());
                                    opencartDaoService.saveOptionValueDescription(desc);
                                });
                    });

            optionsDatabase.add(savedOption);

        } else {
            o.setOptionId(searchOptionOpencart.getOptionId());
            o.getDescriptions().forEach(d -> d.setOptionId(o.getOptionId()));

            o.getValues()
                    .stream()
                    .peek(v -> {
                        String valueName = v.getDescriptionValue().get(0).getName();

                        OptionValueOpencart searchValueOpencart = searchOptionOpencart.getValues()
                                .stream()
                                .filter(vdb -> {
                                    OptionValueDescriptionOpencart searchValueDescription = vdb.getDescriptionValue()
                                            .stream()
                                            .filter(d -> d.getLanguageId() == OCConstant.UA_LANGUAGE_ID && d.getName().equals(valueName))
                                            .findFirst()
                                            .orElse(null);

                                    return Objects.nonNull(searchValueDescription);
                                })
                                .findFirst()
                                .orElse(null);

                        log.info("Option {} value {} is null {}", optionName, valueName, Objects.isNull(searchValueOpencart));
                        if (Objects.isNull(searchValueOpencart)) {
                            v.setOptionId(searchOptionOpencart.getOptionId());
                            OptionValueOpencart savedOptionValueOpencart = opencartDaoService.saveOptionValueOpencart(v);
                            savedOptionValueOpencart
                                    .getDescriptionValue()
                                    .stream()
                                    .forEach(desc -> {
                                        desc.setOptionValueId(savedOptionValueOpencart.getOptionValueId());
                                        desc.setOptionId(savedOptionValueOpencart.getOptionId());
                                        opencartDaoService.saveOptionValueDescription(desc);
                                    });
                            searchOptionOpencart.getValues().add(savedOptionValueOpencart);
                        } else {
                            v.setOptionValueId(searchValueOpencart.getOptionValueId());
                            v.setOptionId(searchValueOpencart.getOptionId());
                            v.getDescriptionValue()
                                    .forEach(d -> {
                                        d.setOptionValueId(searchValueOpencart.getOptionValueId());
                                        d.setOptionId(searchValueOpencart.getOptionId());
                                    });
                            boolean equalsImage = v.getImage().equals(searchValueOpencart.getImage());
                            if (!equalsImage) {
                                searchValueOpencart.setImage(v.getImage());
                                opencartDaoService.updateOptionValueOpencart(searchValueOpencart);
                                log.info("Update image option value id: {}", searchValueOpencart.getOptionValueId());
                            }

                        }

                    })
                    .collect(Collectors.toList());
        }
        return o;
    }


    @Override
    public List<CategoryOpencart> recursiveCollectListCategory(CategoryOpencart category, List<CategoryOpencart> supplierCategoryOpencartDB) {
        CategoryOpencart categoryFromDBListByName = findCategoryFromDBListByName(category, supplierCategoryOpencartDB);
        int categoryId = categoryFromDBListByName.getCategoryId();

        List<CategoryOpencart> subCategories = category.getCategoriesOpencart()
                .stream()
                .map(c -> {
                    c.setParentCategory(categoryFromDBListByName);
                    c.setParentId(categoryId);
                    return recursiveCollectListCategory(c, supplierCategoryOpencartDB);
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        subCategories.add(categoryFromDBListByName);

        return subCategories;
    }

    @Override
    public OpencartDto getOpencartInfo(List<ProductOpencart> productFromSite, SupplierApp supplierApp) {

        List<ProductOpencart> productsDBList = opencartDaoService.getAllProductOpencartBySupplierAppName(supplierApp.getName());

        productsDBList
                .stream()
                .forEach(p -> {
                    List<ProductOptionOpencart> productOptions = opencartDaoService.getProductOptionsById(p.getId());
                    p.setProductOptionsOpencart(productOptions);
                });

        List<ProductOpencart> newProducts = productFromSite
                .stream()
                .filter(p -> !productsDBList.contains(p))
                .collect(Collectors.toList());
        log.info("New products count: {}", newProducts.size());

        List<ProductApp> newProductAppList = newProducts
                .stream()
                .map(poc -> new ProductApp.Builder()
                        .withUrl(Objects.isNull(poc.getUrlProduct()) ? "" : poc.getUrlProduct())
                        .withName(poc.getSku())
                        .withStatus("new")
                        .build())
                .collect(Collectors.toList());

        List<ProductOpencart> availableProducts = productFromSite
                .stream()
                .filter(productsDBList::contains)
                .peek(p -> {
                    ProductOpencart productDB = productsDBList.get(productsDBList.indexOf(p));
                    p.setProductOptionsOpencart(productDB.getProductOptionsOpencart());
//                    p.setProductsDescriptionOpencart(productDB.getProductsDescriptionOpencart());
                    p.setId(productDB.getId());
                })
                .collect(Collectors.toList());

        log.info("Available products count: {}", availableProducts.size());


        List<ProductOpencart> againAvailableProducts = productsDBList
                .stream()
                .filter(p -> availableProducts.contains(p) && !p.isStatus())
                .peek(p -> p.setStatus(true))
                .collect(Collectors.toList());

        log.info("Again products count: {}", againAvailableProducts.size());
        List<ProductApp> againProductAppList = againAvailableProducts
                .stream()
                .map(poc -> new ProductApp.Builder()
                        .withUrl(Objects.isNull(poc.getUrlProduct()) ? "" : poc.getUrlProduct())
                        .withName(poc.getSku())
                        .withStatus("again")
                        .build())
                .collect(Collectors.toList());

        againAvailableProducts
                .forEach(opencartDaoService::updateStatusProductOpencart);

        List<ProductOpencart> oldProducts = productsDBList
                .stream()
                .filter(p -> !productFromSite.contains(p))
                .peek(p -> opencartDaoService.deleteFullProductDataById(p.getId()))
                .collect(Collectors.toList());
        log.info("Old products count: {}", oldProducts.size());

        List<ProductApp> oldProductAppList = oldProducts
                .stream()
                .map(op -> new ProductApp.Builder()
                        .withStatus("old")
                        .withName(op.getSku())
                        .build())
                .collect(Collectors.toList());

        oldProducts.forEach(opencartDaoService::updateStatusProductOpencart);

        oldProducts.forEach(p -> log.debug("old price product: {}", p));

        OrderProcessApp order = new OrderProcessApp.Builder()
                .withSupplierAppId(supplierApp.getSupplierAppId())
                .withNewProduct(newProductAppList)
                .withOlrProduct(oldProductAppList)
                .withAgainProductApp(againProductAppList)
                .build();
        OpencartDto opencartDto = new OpencartDto();
        opencartDto.setOrderProcessApp(order);
        opencartDto.setNewProduct(newProducts);
        opencartDto.setAvailableProducts(availableProducts);
        opencartDto.setProductsOpencartDB(productsDBList);
        return opencartDto;

    }

    @Override
    public ProductOpencart setPriceWithMarkup(ProductOpencart product) {
        ProductProfileApp productProfileApp = product.getProductProfileApp();
        int markupManufacturer = productProfileApp.getManufacturerApp().getMarkup();
        int markupCategory = productProfileApp.getCategoryApp().getMarkup();
        int markupSupplier = productProfileApp.getSupplierApp().getMarkup();
        int markupInt = markupManufacturer != 0 ?
                markupManufacturer : markupCategory != 0 ?
                markupCategory : markupSupplier;

        log.info("MS: {}, MM: {}", markupSupplier, markupManufacturer);

        BigDecimal price = product.getPrice();

        double m = markupInt;
        double d = 1 + m / 100;
        BigDecimal markup = new BigDecimal(String.valueOf(d));
        BigDecimal lastPrice = price.multiply(markup).setScale(2, RoundingMode.UP).setScale(4);
        product.setPrice(lastPrice);
        product.setItuaOriginalPrice(lastPrice);
        setOptionPriceWithMarkup(product);
        log.info("sku: {} actual product price: {}", product.getSku(), lastPrice);
        return product;
    }


    public ProductOpencart setOptionPriceWithMarkup(ProductOpencart product) {
        ProductProfileApp productProfileApp = product.getProductProfileApp();
        int markupManufacturer = productProfileApp.getManufacturerApp().getMarkup();
        int markupCategory = productProfileApp.getCategoryApp().getMarkup();
        int markupSupplier = productProfileApp.getSupplierApp().getMarkup();
        int markupInt = markupManufacturer != 0 ?
                markupManufacturer : markupCategory != 0 ?
                markupCategory : markupSupplier;

        log.info("MS: {}, MM: {}", markupSupplier, markupManufacturer);


        List<OptionApp> optionsApp = productProfileApp.getOptions();
        product
                .getProductOptionsOpencart()
                .forEach(po -> {
//                    BigDecimal price = product.getPrice();

                    double m = markupInt;
                    double d = 1 + m / 100;
                    BigDecimal markup = new BigDecimal(String.valueOf(d));

                    po.getOptionValues()
                            .forEach(pv -> {
                                OptionApp optionApp = optionsApp
                                        .stream()
                                        .filter(oapp -> oapp.getValueId() == pv.getProductOptionValueId())
                                        .findFirst()
                                        .orElse(null);
                                if (Objects.nonNull(optionApp)) {
                                    BigDecimal optionPrice = optionApp.getOptionPrice();
                                    BigDecimal result = optionPrice.multiply(markup).setScale(2, RoundingMode.UP).setScale(4);
                                    pv.setPrice(result);
                                }
                            });
                });

        return product;
    }

    @Override
    public String cleanDescription(Element descriptionElement) {
        descriptionElement.select("br").append("\n");
        descriptionElement.select("p").append("\n\n");
        List<String> lines = Arrays.asList(descriptionElement.wholeText().split("\n"));
        return lines
                .stream()
                .map(String::trim)
                .collect(Collectors.joining("\n")).replaceAll("\n{3,}", "\n\n").replaceAll("\\u00a0", "").trim();
    }

    public String wrapToHtml(String text) {
        String result = text.replaceAll("\n", "</br>");
        return "<span class=\"centurapp\" style=\"white-space: pre-wrap; font-size: 16px; \">".concat(result).concat("</span>");
    }

//    public String cleanJsoup(String html) {
//        Document document = Jsoup.parse(html);
//        document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
//        document.select("br").append("\\n");
//        document.select("p").prepend("\\n");
//        String s = document.html().replaceAll("\\\\n", "\n");
//        String clean = Jsoup.clean(s, "", Safelist.none(), new Document.OutputSettings().prettyPrint(false)).replaceAll("&nbsp", "").replaceAll("(> +<)", "><").replaceAll("(</br>){3,}", "</br></br>").replaceAll("^(</br>)+|(</br>)+$", "");
//        return clean;
//    }

    @Override
    public void updateProductSupplierOpencartBySupplierApp(SupplierApp supplierApp) {
        scraperDataUpdateService.updateProductSupplierOpencartBySupplierApp(supplierApp);
    }
}
