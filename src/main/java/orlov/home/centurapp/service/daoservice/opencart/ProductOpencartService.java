package orlov.home.centurapp.service.daoservice.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.opencart.*;
import orlov.home.centurapp.dto.ManufacturerUpdateDto;
import orlov.home.centurapp.dto.ProductInfoDto;
import orlov.home.centurapp.dto.ProductToAttributeDto;
import orlov.home.centurapp.entity.app.OptionApp;
import orlov.home.centurapp.entity.app.ProductProfileApp;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.mapper.opencart.ProductDataOpencartExtractor;
import orlov.home.centurapp.mapper.opencart.SupplierOpencartRowMapper;
import orlov.home.centurapp.service.appservice.FileService;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.util.AppConstant;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor
public class ProductOpencartService {

    private final ProductOpencartDao productOpencartDao;
    private final ProductSupplierOpencartDao productSupplierOpencartDao;
    private final ProductDescriptionOpencartDao productDescriptionOpencartDao;
    private final ImageOpencartService imageOpencartService;
    private final FileService fileService;
    private final ProductOptionDao productOptionDao;
    private final ProductOptionValueDao productOptionValueDao;
    private final AppDaoService appDaoService;
    private final OptionOpencartService optionOpencartService;
    private final SupplierOpencartService supplierOpencartService;


    public List<ProductInfoDto> getAllModelByAttributeId(int attrId) {
        return productOpencartDao.getAllModelByAttributeId(attrId);
    }

    public void deleteFullProductById(int productId) {
        log.info("Delete product by id: {}", productId);

        productOpencartDao.deleteProductToStore(productId);
        productOpencartDao.deleteProductDescription(productId);
        productOpencartDao.deleteProductAttribute(productId);
        productOpencartDao.deleteProductToCategoryByProductId(productId);
        ProductOpencart product = productOpencartDao.getById(productId);
        if (Objects.nonNull(product)) {
            fileService.deleteImage(product.getImage());
            ProductOpencart productWithImageById = productOpencartDao.getProductWithImageById(productId);
            productWithImageById.getImagesOpencart()
                    .forEach(i -> fileService.deleteImage(i.getImage()));
        }
        imageOpencartService.deleteByProductId(productId);
        productOptionDao.deleteProductOptionById(productId);
        productOptionValueDao.deleteProductOptionValueById(productId);
        productSupplierOpencartDao.deleteProductSupplierByProductId(productId);
        productOpencartDao.deleteById(productId);


    }

    public void deleteProductOptionValue(int productId, int optionId, int optionValueId) {
        productOptionValueDao.deleteProductOptionValue(productId, optionId, optionValueId);
    }

    public void deleteProductOption(int productId, int optionId) {
        productOptionDao.deleteProductOption(productId, optionId);
    }

    public ProductOpencart getByModel(String model) {
        return productOpencartDao.getByModel(model);
    }

    public ProductSupplierOpencart getAllProductSupplierBySupCodeProductId(int productId, String supCode) {
        return productSupplierOpencartDao.getAllProductSupplierBySupCodeProductId(productId, supCode);
    }

    public ProductSupplierOpencart updateProductSupplierOpencart(ProductSupplierOpencart productSupplierOpencart) {
        return productSupplierOpencartDao.update(productSupplierOpencart);
    }


    public void deleteByProductSupplier(int productId, String supCode) {
        productSupplierOpencartDao.deleteByProductSupplier(productId, supCode);
    }

    public void updateModel(ProductOpencart product) {
        productOpencartDao.updateModel(product);
    }

    public ProductOptionValueOpencart updateProductOptionValueOpencart(ProductOptionValueOpencart productOptionValueOpencart) {
        return productOptionValueDao.update(productOptionValueOpencart);
    }

    public ProductOpencart getProductOpencartById(int id) {
        return productOpencartDao.getById(id);
    }

    public List<ProductOpencart> getSupplierProducts(String supplierName) {
        return productOpencartDao.getSupplierProducts(supplierName);
    }

    public List<ProductOpencart> getProductsSameTitle(String title) {
        return productOpencartDao.getProductsSameTitle(title);
    }

    public List<Integer> getProductsIdBySupplier(SupplierApp supplierApp) {
        return productOpencartDao.getProductsIdBySupplier(supplierApp);
    }

    public ProductOptionOpencart saveProductOptionOpencart(ProductOptionOpencart option) {
        int id = productOptionDao.save(option);
        option.setProductOptionId(id);
        return option;
    }

    public List<ProductOptionOpencart> getProductOptionsById(int productId) {
        return productOptionDao.getProductOptionsById(productId);
    }

    public ProductOptionValueOpencart saveProductOptionValueOpencart(ProductOptionValueOpencart optionValue) {
        int id = productOptionValueDao.save(optionValue);
        optionValue.setProductOptionValueId(id);
        return optionValue;
    }

    public ProductOpencart updateStockStatus(ProductOpencart product) {
        return productOpencartDao.updateStockStatus(product);
    }


    public List<ProductSupplierOpencart> getAllProductSupplierBySupCode(String supCode) {
        return productSupplierOpencartDao.getAllProductSupplierBySupCode(supCode);
    }

    public void updateProductDescriptionBatch(List<ProductDescriptionOpencart> descriptions) {
        productDescriptionOpencartDao.updateBatch(descriptions);
    }

    public List<Integer> getProductsIdByAttributeOpencartId(int attributeOpencartId, String supplierName) {
        return productOpencartDao.getProductsIdByAttributeOpencartId(attributeOpencartId, supplierName);
    }

    public void updateImage(ProductOpencart productOpencart) {
        productOpencartDao.updateImage(productOpencart);
    }

    public ProductDescriptionOpencart updateDescription(ProductDescriptionOpencart productDescriptionOpencart) {
        return productDescriptionOpencartDao.updateDescription(productDescriptionOpencart);
    }

    public ProductOpencart saveProduct(ProductOpencart productOpencart) {
        int id = productOpencartDao.save(productOpencart);
        productOpencart.setId(id);
        log.info("saved product id: {}", id);
        productOpencartDao.saveProductToStore(productOpencart);
        productOpencartDao.saveProductToCategory(productOpencart);

        List<ProductDescriptionOpencart> description = productOpencart.getProductsDescriptionOpencart();
        description
                .forEach(pd -> {
                    pd.setProductId(productOpencart.getId());
                    productDescriptionOpencartDao.save(pd);
                });

        List<ImageOpencart> images = productOpencart.getImagesOpencart();
        images
                .forEach(i -> {
                    i.setProductId(productOpencart.getId());
                    imageOpencartService.save(i);
                });

        saveProductToAttribute(productOpencart);

        ProductProfileApp productProfileApp = productOpencart.getProductProfileApp();
        productOpencart.getOptionsOpencart()
                .forEach(o -> {
                    ProductOptionOpencart productOptionOpencart = o.getProductOptionOpencart();
                    productOptionOpencart.setProductId(productOpencart.getId());
                    ProductOptionOpencart savedProductOptionOpencart = saveProductOptionOpencart(productOptionOpencart);
                    o.getValues()
                            .forEach(v -> {
                                ProductOptionValueOpencart productOptionValueOpencart = v.getProductOptionValueOpencart();
                                productOptionValueOpencart.setProductOptionId(savedProductOptionOpencart.getProductOptionId());
                                productOptionValueOpencart.setProductId(productOpencart.getId());
                                ProductOptionValueOpencart savedOptionValueOpencart = saveProductOptionValueOpencart(productOptionValueOpencart);
                                OptionApp optionApp = new OptionApp();
                                optionApp.setProductProfileId(productProfileApp.getProductProfileId());
                                optionApp.setOptionValue("");
                                optionApp.setOptionId(savedOptionValueOpencart.getOptionId());
                                optionApp.setValueId(savedOptionValueOpencart.getProductOptionValueId());
                                optionApp.setOptionPrice(savedOptionValueOpencart.getPrice());
                                appDaoService.saveOptionApp(optionApp);
                            });
                });

        return productOpencart;
    }

    public void updatePDVProductSupplier(SupplierOpencart supplierOpencart) {
        productSupplierOpencartDao.updatePDVProductSupplier(supplierOpencart);
    }

    public void updatePriceProductSupplier(ProductSupplierOpencart productSupplierOpencart) {
        productSupplierOpencartDao.updatePriceProductSupplier(productSupplierOpencart);
    }

    public void saveProductSupplier(ProductSupplierOpencart productSupplierOpencart) {
        productSupplierOpencartDao.save(productSupplierOpencart);
    }

    public ProductOpencart getProductWithDescriptionById(long id) {
        return productOpencartDao.getProductWithDescriptionById(id);
    }

    public void updateProductManufacturer(ManufacturerUpdateDto manufacturerUpdateDto) {
        productOpencartDao.updateProductManufacturer(manufacturerUpdateDto);
    }

    public ProductOpencart getProductWithImageById(long id) {
        return productOpencartDao.getProductWithImageById(id);
    }

    public void saveProductToAttribute(ProductOpencart productOpencart) {
        productOpencartDao.saveProductToAttribute(productOpencart);
    }

    public List<ProductOpencart> getAll() {
        return productOpencartDao.getAll();
    }


    public List<ProductOpencart> getAllBySupplier(String supplier) {
        return productOpencartDao.getAllBySupplier(supplier);
    }

    public void update(ProductOpencart productOpencart) {
        productOpencartDao.update(productOpencart);
    }

    public void updatePrice(ProductOpencart productOpencart) {
        productOpencartDao.updatePrice(productOpencart);
    }

    public void updateStatus(ProductOpencart productOpencart) {
        productOpencartDao.updateStatus(productOpencart);
    }

    public int lastModel() {
        return productOpencartDao.getLastProductModel();
    }


    public void updateManufacturer(ProductOpencart productOpencart) {
        productOpencartDao.updateManufacturer(productOpencart);
    }

    public List<ProductToAttributeDto> getProductToAttributeBySupplierName(String supplierName) {
        return productOpencartDao.getProductToAttributeBySupplierName(supplierName);
    }

    public List<Integer> getCategoryIdByProductId(int productId) {
        return productOpencartDao.getCategoryIdByProductId(productId);
    }

    public void deleteProductToCategoryByProductId(int productId) {
        productOpencartDao.deleteProductToCategoryByProductId(productId);
    }

    public void saveProductToCategory(ProductOpencart product) {
        productOpencartDao.saveProductToCategory(product);
    }

    public void updateProductToAttribute(ProductToAttributeDto product) {
        productOpencartDao.updateProductToAttribute(product);
    }


    public void deleteImageByImageId(int imageId) {
        imageOpencartService.deleteById(imageId);
    }

    public void updateAttributeOpencartValue(ProductToAttributeDto productToAttributeDto) {
        productOpencartDao.updateAttributeOpencartValue(productToAttributeDto);
    }

    public void deleteProductData(int productId) {
        List<ImageOpencart> images = imageOpencartService.getImageByProductId(productId);
        images
                .forEach(fileService::deleteImageFile);
        imageOpencartService.deleteByProductId(productId);
        productOpencartDao.deleteProductToStore(productId);
        productOpencartDao.deleteProductToCategory(productId);
        productOpencartDao.deleteProductAttribute(productId);
        productOpencartDao.deleteProductDescription(productId);
        productOpencartDao.deleteById(productId);
    }


}
