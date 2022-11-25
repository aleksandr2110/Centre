package orlov.home.centurapp.service.daoservice.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.ManufacturerUpdateDto;
import orlov.home.centurapp.dto.ProductInfoDto;
import orlov.home.centurapp.dto.ProductToAttributeDto;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.*;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class OpencartDaoService {

    private final AttributeOpencartService attributeOpencartService;
    private final CategoryOpencartService categoryOpencartService;
    private final CurrencyOpencartService currencyOpencartService;
    private final ImageOpencartService imageOpencartService;
    private final ManufacturerOpencartService manufacturerOpencartService;
    private final ProductOpencartService productOpencartService;
    private final SupplierOpencartService supplierOpencartService;
    private final OptionOpencartService optionOpencartService;


    public ProductOpencart getProductOpencartByModel(String model) {
        return productOpencartService.getByModel(model);
    }

    public ProductSupplierOpencart getAllProductSupplierBySupCodeProductId(int productId, String supCode) {
        return productOpencartService.getAllProductSupplierBySupCodeProductId(productId, supCode);
    }
    public List<ProductInfoDto> getAllModelByAttributeId(int attrId) {
        return productOpencartService.getAllModelByAttributeId(attrId);
    }

    public void updateSubImage(ImageOpencart imageOpencart){
        imageOpencartService.updateSubImage(imageOpencart);
    }

    public void deleteProductOption(int productId, int optionId) {
        productOpencartService.deleteProductOption(productId, optionId);
    }
    public void deleteFullProductDataById(int productId){
        productOpencartService.deleteFullProductById(productId);
    }

    public void deleteImageByImageId(int imageId){
        productOpencartService.deleteImageByImageId(imageId);
    }

    public ProductSupplierOpencart updateProductSupplierOpencart(ProductSupplierOpencart productSupplierOpencart) {
        return productOpencartService.updateProductSupplierOpencart(productSupplierOpencart);
    }

    public void deleteProductOptionValue(int productId, int optionId, int optionValueId) {
        productOpencartService.deleteProductOptionValue(productId, optionId, optionValueId);
    }

    public void deleteByProductSupplier(int productId, String supCode) {
        productOpencartService.deleteByProductSupplier(productId, supCode);
    }

    public ProductOpencart getProductOpencartById(int id) {
        return productOpencartService.getProductOpencartById(id);
    }

    public void updateModel(ProductOpencart product) {
        productOpencartService.updateModel(product);
    }

    public ProductOptionValueOpencart updateProductOptionValueOpencart(ProductOptionValueOpencart productOptionValueOpencart) {
        return productOpencartService.updateProductOptionValueOpencart(productOptionValueOpencart);
    }


    public List<ProductOpencart> getSupplierProducts(String supplierName) {
        return productOpencartService.getSupplierProducts(supplierName);
    }

    public List<ProductOpencart> getProductsSameTitle(String title) {
        return productOpencartService.getProductsSameTitle(title);
    }

    public List<ProductOptionOpencart> getProductOptionsById(int productId) {
        return productOpencartService.getProductOptionsById(productId);
    }

    public ProductOptionOpencart saveProductOptionOpencart(ProductOptionOpencart option) {
        return productOpencartService.saveProductOptionOpencart(option);
    }

    public ProductOptionValueOpencart saveProductOptionValueOpencart(ProductOptionValueOpencart optionValue) {
        return productOpencartService.saveProductOptionValueOpencart(optionValue);
    }

    public OptionOpencart saveOption(OptionOpencart option) {
        return optionOpencartService.saveOption(option);
    }

    public List<OptionOpencart> getAllOptionOpencartFullData() {
        return optionOpencartService.getAllOptionOpencartFullData();
    }

    public OptionDescriptionOpencart saveOptionDescription(OptionDescriptionOpencart optionDescription) {
        return optionOpencartService.saveOptionDescription(optionDescription);

    }

    public OptionValueOpencart saveOptionValueOpencart(OptionValueOpencart optionValue) {
        return optionOpencartService.saveOptionValueOpencart(optionValue);

    }

    public OptionValueDescriptionOpencart saveOptionValueDescription(OptionValueDescriptionOpencart optionValueDescription) {
        return optionOpencartService.saveOptionValueDescription(optionValueDescription);
    }

    public AttributeGroupOpencart getDefaultGlobalAttributeOpencartGroupByName(String name) {
        return attributeOpencartService.getDefaultGlobalAttributeGroupByName(name);
    }

    public ProductOpencart updateStockStatus(ProductOpencart product) {
        return productOpencartService.updateStockStatus(product);
    }

    public List<AttributeWrapper> getAttributeWrapperByProduct(ProductOpencart product) {
        return attributeOpencartService.getAttributesWrapperByProduct(product);
    }

    public AttributeOpencart getAttributeOpencartByName(String name) {
        return attributeOpencartService.getByName(name);
    }

    public void batchUpdateAttributeOpencart(List<ProductToAttributeDto> attributesDto) {
        attributeOpencartService.batchUpdateAttribute(attributesDto);
    }

    public AttributeOpencart saveAttributeOpencart(AttributeOpencart attributeOpencart) {
        return attributeOpencartService.saveAttribute(attributeOpencart);
    }

    public AttributeDescriptionOpencart saveAttributeDescriptionOpencart(AttributeDescriptionOpencart attributeDescriptionOpencart) {
        attributeOpencartService.saveAttributeDescription(attributeDescriptionOpencart);
        return attributeDescriptionOpencart;
    }

    public List<AttributeOpencart> getAllAttributeOpencartWithDesc() {
        return attributeOpencartService.getAllWithDesc();
    }

    public List<AttributeOpencart> getAllAttributeBySearchWithDesc(String likeName) {
        return attributeOpencartService.getAllBySearchWithDesc(likeName);
    }

    public ProductToAttributeDto getProductToAttributeById(int productId, int attributeId) {
        return attributeOpencartService.getProductToAttributeById(productId, attributeId);
    }

    public List<CategoryOpencart> getAllSupplierCategoryOpencart(SupplierApp supplierApp) {
        return categoryOpencartService.getSupplierCategoryOpencart(supplierApp);
    }

    public CategoryOpencart saveCategoryOpencart(CategoryOpencart categoryOpencart) {
        return categoryOpencartService.save(categoryOpencart);
    }

    public CategoryOpencart getMainSupplierCategoryOpencart(SupplierApp supplierApp) {
        return categoryOpencartService.getMainSupplierCategoryOpencart(supplierApp);
    }

    public CategoryOpencart getCategoryOpencartByNameAndDescription(String categoryName, String categoryDescription) {
        return categoryOpencartService.getCategoryByNameAndDescription(categoryName, categoryDescription);
    }

    public List<CurrencyOpencart> getAllCurrencyOpencart() {
        return currencyOpencartService.getAllCurrency();
    }

    public void deleteImageOpencartById(int id) {
        imageOpencartService.deleteById(id);
    }

    public void deleteImageOpencartByName(String imageName) {
        imageOpencartService.deleteByName(imageName);
    }

    public void deleteImageOpencartByProductId(int productId) {
        imageOpencartService.deleteByProductId(productId);
    }

    public int saveImageOpencart(ImageOpencart imageOpencart) {
        return imageOpencartService.save(imageOpencart);
    }

    public List<ImageOpencart> getImageOpencartByProductId(int productId) {
        return imageOpencartService.getImageByProductId(productId);
    }

    public ManufacturerOpencart saveManufacturerOpencart(ManufacturerOpencart manufacturerOpencart) {
        return manufacturerOpencartService.save(manufacturerOpencart);
    }

    public ProductDescriptionOpencart updateDescription(ProductDescriptionOpencart productDescriptionOpencart) {
        return productOpencartService.updateDescription(productDescriptionOpencart);
    }

    public ManufacturerOpencart getManufacturerOpencartByName(String name) {
        return manufacturerOpencartService.getByName(name);
    }

    public List<ManufacturerOpencart> getAllManufacturerOpencart() {
        return manufacturerOpencartService.getAll();
    }

    public List<Integer> getAllProductOpencartIdBySupplier(SupplierApp supplierApp) {
        return productOpencartService.getProductsIdBySupplier(supplierApp);
    }

    public List<ProductSupplierOpencart> getAllProductSupplierOpencartBySupCode(String supCode) {
        return productOpencartService.getAllProductSupplierBySupCode(supCode);
    }

    public void updateProductDescriptionOpencartBatch(List<ProductDescriptionOpencart> descriptions) {
        productOpencartService.updateProductDescriptionBatch(descriptions);
    }

    public List<Integer> getAllProductOpencartIdByAttributeOpencartId(int attributeOpencartId, String supplierName) {
        return productOpencartService.getProductsIdByAttributeOpencartId(attributeOpencartId, supplierName);
    }

    public OptionValueOpencart updateOptionValueOpencart(OptionValueOpencart optionValueOpencart) {
        return optionOpencartService.updateOptionValueOpencart(optionValueOpencart);
    }

    public void updateMainProductImageOpencart(ProductOpencart productOpencart) {
        productOpencartService.updateImage(productOpencart);
    }


    public ProductOpencart saveProductOpencart(ProductOpencart productOpencart) {
        return productOpencartService.saveProduct(productOpencart);
    }

    public void updatePDVProductSupplier(SupplierOpencart supplierOpencart) {
        productOpencartService.updatePDVProductSupplier(supplierOpencart);
    }

    public void updatePriceProductSupplier(ProductSupplierOpencart productSupplierOpencart) {
        productOpencartService.updatePriceProductSupplier(productSupplierOpencart);
    }

    public void saveProductSupplierOpencart(ProductSupplierOpencart productSupplierOpencart) {
        productOpencartService.saveProductSupplier(productSupplierOpencart);
    }

    public ProductOpencart getProductOpencartWithDescriptionById(long id) {
        return productOpencartService.getProductWithDescriptionById(id);
    }

    public void updateProductOpencartManufacturer(ManufacturerUpdateDto manufacturerUpdateDto) {
        productOpencartService.updateProductManufacturer(manufacturerUpdateDto);
    }

    public ProductOpencart getProductOpencartWithImageById(long id) {
        return productOpencartService.getProductWithImageById(id);
    }

    public void saveProductOpencartToAttribute(ProductOpencart productOpencart) {
        productOpencartService.saveProductToAttribute(productOpencart);
    }

    public List<ProductOpencart> getAllProductOpencart() {
        return productOpencartService.getAll();
    }


    public List<ProductOpencart> getAllProductOpencartBySupplierAppName(String supplierName) {
        return productOpencartService.getAllBySupplier(supplierName);
    }

    public void updateProductOpencart(ProductOpencart productOpencart) {
        productOpencartService.update(productOpencart);
    }

    public void updatePriceProductOpencart(ProductOpencart productOpencart) {
        productOpencartService.updatePrice(productOpencart);
    }

    public void updateStatusProductOpencart(ProductOpencart productOpencart) {
        productOpencartService.updateStatus(productOpencart);
    }

    public int lastModelProductOpencart() {
        return productOpencartService.lastModel();
    }


    public void updateProductOpencartManufacturer(ProductOpencart productOpencart) {
        productOpencartService.updateManufacturer(productOpencart);
    }

    public List<ProductToAttributeDto> getProductToAttributeBySupplierName(String supplierName) {
        return productOpencartService.getProductToAttributeBySupplierName(supplierName);
    }

    public List<Integer> getCategoryOpencartIdByProductId(int productId) {
        return productOpencartService.getCategoryIdByProductId(productId);
    }

    public void deleteProductToCategoryByProductId(int productId) {
        productOpencartService.deleteProductToCategoryByProductId(productId);
    }

    public void saveProductToCategory(ProductOpencart product) {
        productOpencartService.saveProductToCategory(product);
    }

    public void updateProductToAttribute(ProductToAttributeDto product) {
        productOpencartService.updateProductToAttribute(product);
    }

    public void updateAttributeOpencartValue(ProductToAttributeDto productToAttributeDto) {
        productOpencartService.updateAttributeOpencartValue(productToAttributeDto);
    }

    public void deleteProductOpencartData(int productId) {
        productOpencartService.deleteProductData(productId);
    }

    public List<SupplierOpencart> getAllSupplierOpencart() {
        return supplierOpencartService.getAll();
    }

    public SupplierOpencart getSupplierOpencartBySupId(int supId) {
        return supplierOpencartService.getBySupId(supId);
    }

    public SupplierOpencart updateSupplierOpencart(SupplierOpencart supplierOpencart) {
        return supplierOpencartService.update(supplierOpencart);
    }

    public SupplierOpencart getSupplierOpencartBySubCode(String subCode) {
        return supplierOpencartService.getBySubCode(subCode);
    }


}
