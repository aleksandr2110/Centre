package orlov.home.centurapp.service.appservice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.ManufacturerUpdateDto;
import orlov.home.centurapp.dto.ProductToAttributeDto;
import orlov.home.centurapp.entity.app.*;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.service.daoservice.app.*;
import orlov.home.centurapp.service.daoservice.opencart.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
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
public class UpdateDataService {

    private final ProductOpencartService productOpencartService;
    private final CategoryOpencartService categoryOpencartService;
    private final AttributeOpencartService attributeOpencartService;
    private final AttributeAppService attributeAppService;
    private final ManufacturerAppService manufacturerAppService;
    private final ManufacturerOpencartService manufacturerOpencartService;
    private final AppDaoService appDaoService;
    private final SupplierAppService supplierAppService;
    private final ProductProfileAppService productProfileAppService;
    private final FileService fileService;
    private final ImageOpencartService imageOpencartService;
    private final CategoryAppService categoryAppService;
    private final OrderProcessAppService orderProcessAppService;
    private final ProductAppService productAppService;
    private final String dirImage = FileService.imageDirOC;

    public Path getZipFileImage(int supplierId) {
        SupplierApp supplier = supplierAppService.getById(supplierId);
        log.info("Supplier: {}", supplier);

        List<Integer> productsIdByCategory = productOpencartService.getProductsIdBySupplier(supplier);

        List<String> imagesPath = productsIdByCategory
                .stream()
                .map(productOpencartService::getProductWithImageById)
                .filter(Objects::nonNull)
                .map(p -> {
                    List<ImageOpencart> imagesOpencart = p.getImagesOpencart();


                    List<String> imagesAbsolutePath = imagesOpencart
                            .stream()
                            .map(i -> {
                                String image = i.getImage();
                                return dirImage.concat(image);
                            })
                            .collect(Collectors.toList());
                    String mainImage = dirImage.concat(p.getImage());
                    imagesAbsolutePath.add(mainImage);
                    return imagesAbsolutePath;
                })
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

        AtomicInteger count = new AtomicInteger();
        return fileService.createZipImage(imagesPath);
    }


    public void uploadZipFileImage(MultipartFile fileUpload, SupplierApp supplierApp) {
        try {
            Set<ProductOpencart> productForChange = new HashSet<>();
            List<ProductOpencart> products = productOpencartService.getAllBySupplier(supplierApp.getName());
            List<String> productImagesData = new ArrayList<>();

            products = products
                    .stream()
                    .peek(p -> {
                        String image = p.getImage();
                        if (!image.isEmpty()) {
                            productImagesData.add(image);
                        }
                        List<ImageOpencart> imageByProductId = imageOpencartService.getImageByProductId(p.getId());
                        imageByProductId.forEach(i -> productImagesData.add(i.getImage()));
                        p.setImagesOpencart(imageByProductId);
                    }).collect(Collectors.toList());

            log.info("Count images: {}", productImagesData.stream().distinct().collect(Collectors.toList()).size());
            List<String> imagesFromFile = fileService.updateImageUseZipFile(fileUpload.getInputStream(), productImagesData);


            products
                    .forEach(p -> {
                        String imageProduct = p.getImage();
                        if (imagesFromFile.contains(imageProduct)) {
                            productForChange.add(p);
                        } else {
                            List<ImageOpencart> imagesOpencart = p.getImagesOpencart();
                            imagesOpencart.forEach(i -> {
                                if (imagesFromFile.contains(i.getImage())) {
                                    productForChange.add(p);
                                }
                            });
                        }
                    });

            List<ProductOpencart> collect = products
                    .stream().filter(p -> !productForChange.contains(p)).collect(Collectors.toList());
            collect.forEach(pp -> log.info("PP ID: {}", pp.getId()));
            log.info("Count product: {}", productForChange.size());


            //  TODO


            productForChange
                    .forEach(p -> {
                        String image = p.getImage();

                        if (!imagesFromFile.contains(image)) {
                            p.setImage("");
                            productOpencartService.updateImage(p);
                        }

                        List<ImageOpencart> imagesOpencart = p.getImagesOpencart();
                        imagesOpencart
                                .forEach(i -> {
                                    if (!imagesFromFile.contains(i.getImage())) {
                                        imageOpencartService.deleteById(i.getProductImageId());
                                    }
                                });

                    });


        } catch (IOException e) {
            log.warn("Bad update images.", e);
        }

    }


    public Path getExcelFile(int supplierId) {
        SupplierApp supplier = supplierAppService.getById(supplierId);
        log.info("Supplier: {}", supplier);

        List<Integer> productsIdBySupplier = productOpencartService.getProductsIdBySupplier(supplier);

        List<ProductOpencart> products = productsIdBySupplier
                .stream()
                .map(p -> {
                    ProductOpencart product = productOpencartService.getProductWithDescriptionById(p);
                    List<AttributeWrapper> attributesWrapperByProduct = attributeOpencartService.getAttributesWrapperByProduct(product);
                    product.setAttributesWrapper(attributesWrapperByProduct);
                    return product;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        return fileService.createExcelProductFile(products);

    }


    public void uploadExcelFile(MultipartFile fileUpload) {
        log.info("Update product use file excel: {}", fileUpload.getSize());
        try {
            List<ProductOpencart> productOpencartList = fileService.getProductsFromFile(fileUpload.getInputStream());
            List<ProductDescriptionOpencart> descriptions = productOpencartList
                    .stream()
                    .map(ProductOpencart::getProductsDescriptionOpencart)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            productOpencartService.updateProductDescriptionBatch(descriptions);


            List<ProductToAttributeDto> productToAttributeDtoForUpdate = productOpencartList
                    .stream()
                    .map(p -> {
                        List<AttributeWrapper> oldAttributesWrapperByProduct = attributeOpencartService.getAttributesWrapperByProduct(p);
                        List<AttributeWrapper> newAttributesWrapper = p.getAttributesWrapper();
                        return newAttributesWrapper
                                .stream()
                                .filter(na -> {
                                    boolean result = false;
                                    boolean contains = oldAttributesWrapperByProduct.contains(na);
                                    if (contains) {
                                        AttributeWrapper oldAttribute = oldAttributesWrapperByProduct.get(oldAttributesWrapperByProduct.indexOf(na));
                                        result = !oldAttribute.getValueSite().equals(na.getValueSite());
                                        na.setAttributeOpencart(oldAttribute.getAttributeOpencart());
                                    }
                                    return result;
                                })
                                .map(a -> new ProductToAttributeDto(p.getId(), p.getSku(), a.getAttributeOpencart().getAttributeId(), 0, a.getValueSite()))
                                .collect(Collectors.toList());
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());


            attributeOpencartService.batchUpdateAttribute(productToAttributeDtoForUpdate);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateManufacturer(ManufacturerApp manufacturerApp, SupplierApp supplierApp) {

        manufacturerAppService.update(manufacturerApp);

        List<ProductProfileApp> productProfileAppList = productProfileAppService.getProductProfileByManufacturerAppId(manufacturerApp.getManufacturerId());
        ManufacturerOpencart manufacturerOpencart = manufacturerOpencartService.getByName(manufacturerApp.getOpencartTitle());

        if (Objects.nonNull(manufacturerOpencart)) {
            productProfileAppList
                    .forEach(p -> productOpencartService.updateProductManufacturer(new ManufacturerUpdateDto(supplierApp.getName(), p.getSku(), manufacturerOpencart.getManufacturerId())));
        } else {
            log.info("Manufacturer by title not found: {}", manufacturerApp.getOpencartTitle());
        }


    }

    public void updateAttribute(AttributeApp attributeApp, SupplierApp supplierApp) {


        String attributeNameToSet = attributeApp.getOpencartTitle();
        AttributeOpencart attributeOpencartToSet = attributeOpencartService.getByName(attributeNameToSet);

        String attributeNameToDelete = attributeApp.getOldOpencartTitle().isEmpty() ? attributeApp.getSupplierTitle() : attributeApp.getOldOpencartTitle();

        AttributeOpencart attributeOpencartToDelete = attributeOpencartService.getByName(attributeNameToDelete);
        log.info("Attribute opencart: {}", attributeOpencartToDelete);
        if (Objects.nonNull(attributeOpencartToDelete)) {

            List<Integer> productIdList = productOpencartService.getProductsIdByAttributeOpencartId(attributeOpencartToDelete.getAttributeId(), supplierApp.getName());

            productIdList
                    .forEach(i -> productOpencartService.updateProductToAttribute(new ProductToAttributeDto(i, null, attributeOpencartToDelete.getAttributeId(), attributeOpencartToSet.getAttributeId(), null)));
        }

        attributeAppService.update(attributeApp);
    }


    public void updateAttributeData(AttributeApp attributeApp, SupplierApp supplierApp) {

        attributeAppService.update(attributeApp);

        String attributeSearchName = attributeApp.getOpencartTitle().isEmpty() ? attributeApp.getSupplierTitle() : attributeApp.getOpencartTitle();

        AttributeOpencart attributeOpencart = attributeOpencartService.getByName(attributeSearchName);

        List<Integer> productIdList = productOpencartService.getProductsIdByAttributeOpencartId(attributeOpencart.getAttributeId(), supplierApp.getName());

        productIdList
                .forEach(id -> {
                    ProductOpencart productOpencartById = productOpencartService.getProductOpencartById(id);
                    if (Objects.nonNull(productOpencartById)) {
                        ProductToAttributeDto productToAttributeById = attributeOpencartService.getProductToAttributeById(id, attributeOpencart.getAttributeId());
                        if (Objects.nonNull(productToAttributeById)) {
                            ProductProfileApp productProfileBySkyJan = appDaoService.getProductProfileBySkyJan(productOpencartById.getSku(), supplierApp.getSupplierAppId());
                            if (Objects.nonNull(productProfileBySkyJan)) {
                                ProductAttributeApp productAttributeById = appDaoService.getProductAttributeById(productProfileBySkyJan.getProductProfileId(), attributeApp.getAttributeId());

                                String attributeValueDefault = productAttributeById.getAttributeValue();
                                String attributeValue = changeAttributeValue(attributeApp, attributeValueDefault);
                                //  TODO get attribute default value
                                productToAttributeById.setText(attributeValue);
                                productOpencartService.updateAttributeOpencartValue(productToAttributeById);
                            }
                        }
                    }
                });

    }

    public String changeAttributeValue(AttributeApp attributeApp, String value) {

        //  TODO get allproducts with the attribute and change default value
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


    public void updatePrice(int supplierId) {
        SupplierApp supplier = supplierAppService.getById(supplierId);
        log.info("Supplier: {}", supplier);
        List<ProductProfileApp> productProfiles = appDaoService.getAllProductProfileAppBySupplierId(supplierId);
        List<ProductOpencart> products = productOpencartService.getAllBySupplier(supplier.getName());
        log.info("productProfiles: {}", productProfiles.size());
        log.info("products: {}", products.size());
        List<ProductOpencart> resultList = productProfiles
                .stream()
//                .filter(p -> !p.getManufacturerApp().getOpencartTitle().isEmpty())
                .map(p -> {
                    log.info("P: {}", p);
                    ProductOpencart productOpencartBySku = getProductOpencartBySku(products, p.getSku());
                    if (Objects.nonNull(productOpencartBySku)) {

                        productOpencartBySku.setProductProfileApp(p);
                        List<ProductOptionOpencart> options = productOpencartService.getProductOptionsById(productOpencartBySku.getId());
                        productOpencartBySku.setProductOptionsOpencart(options);
                        setPriceWithMarkup(productOpencartBySku);
                        return productOpencartBySku;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        resultList.forEach(p -> {
            productOpencartService.updatePrice(p);
            p.getProductOptionsOpencart()
                    .forEach(po -> {
                        po.getOptionValues().forEach(productOpencartService::updateProductOptionValueOpencart);
                    });
        });

        log.info("Result list size: {}", resultList.size());
    }


    public ProductOpencart getProductOpencartBySku(List<ProductOpencart> productOpencartList, String sku) {
        return productOpencartList.stream().filter(p -> p.getSku().equals(sku)).findFirst().orElse(null);
    }


    public ProductOpencart setPriceWithMarkup(ProductOpencart product) {
        ProductProfileApp productProfileApp = product.getProductProfileApp();
        int markupManufacturer = productProfileApp.getManufacturerApp().getMarkup();
        int markupCategory = productProfileApp.getCategoryApp().getMarkup();
        int markupSupplier = productProfileApp.getSupplierApp().getMarkup();
        int markupInt = markupManufacturer != 0 ?
                markupManufacturer : markupCategory != 0 ?
                markupCategory : markupSupplier;


        BigDecimal price = product.getProductProfileApp().getPrice();

        double m = markupInt;
        double d = 1 + m / 100;
        BigDecimal markup = new BigDecimal(String.valueOf(d));
        BigDecimal lastPrice = price.multiply(markup).setScale(2, RoundingMode.UP).setScale(4);
        log.info("sku: {}, lastPrice: {}", product.getSku(), lastPrice);
        product.setPrice(lastPrice);
        product.setItuaOriginalPrice(lastPrice);

        setOptionPriceWithMarkup(product);
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

        double m = markupInt;
        double d = 1 + m / 100;
        BigDecimal markup = new BigDecimal(String.valueOf(d));

        List<OptionApp> optionsApp = productProfileApp.getOptions();
        log.info("Markup options (option app size: {}): {}", optionsApp.size(), product.getProductOptionsOpencart());
        product
                .getProductOptionsOpencart()
                .forEach(po -> {
                    po.getOptionValues()
                            .forEach(pv -> {
                                OptionApp optionApp = optionsApp
                                        .stream()
                                        .filter(oapp -> oapp.getValueId() == pv.getProductOptionValueId())
                                        .findFirst()
                                        .orElse(null);
                                log.info("Option app: {}", optionApp);
                                if (Objects.nonNull(optionApp)) {
                                    BigDecimal optionPrice = optionApp.getOptionPrice();
                                    BigDecimal result = optionPrice.multiply(markup).setScale(2, RoundingMode.UP).setScale(4);
                                    pv.setPrice(result);
                                }
                            });
                });

        return product;
    }

    public void deleteAllProductData() {
        List<SupplierApp> supplierAppList = supplierAppService.getAll();
        log.info("supplierAppList: {}", supplierAppList.size());
        List<Integer> productIdList = supplierAppList
                .stream()
                .map(productOpencartService::getProductsIdBySupplier)
                .flatMap(Collection::stream)
                .peek(productOpencartService::deleteProductData)
                .collect(Collectors.toList());

        log.info("productIdList: {}", productIdList.size());


        productProfileAppService.deleteAll();
        attributeAppService.deleteAll();
        categoryAppService.deleteAll();
        manufacturerAppService.deleteAll();
        productAppService.deleteAll();
        orderProcessAppService.deleteAll();


    }

}
