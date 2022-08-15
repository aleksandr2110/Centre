package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.entity.app.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class AppDaoService {

    private final AttributeAppService attributeAppService;
    private final CategoryAppService categoryAppService;
    private final ManufacturerAppService manufacturerAppService;
    private final OrderAppService orderAppService;
    private final OrderProcessAppService orderProcessAppService;
    private final ProductAppService productAppService;
    private final ProductProfileAppService productProfileAppService;
    private final SupplierAppService supplierAppService;
    private final OptionAppService optionAppService;
    private final ProductAttributeAppService productAttributeAppService;

    public ProductProfileApp getProductProfileBySkyJan(String sku, int supplierAppId) {
        return productProfileAppService.getProductProfileBySkyJan(sku, supplierAppId);
    }

    public void saveProductAttributeApp(ProductAttributeApp productAttributeApp) {
        productAttributeAppService.save(productAttributeApp);
    }


    public ProductAttributeApp getProductAttributeById(int productProfileAppId, int attributeAppId) {
        return productAttributeAppService.getProductAttributeId(productProfileAppId, attributeAppId);
    }


    public AttributeApp saveAttributeApp(AttributeApp attributeApp) {
        return attributeAppService.save(attributeApp);
    }

    public List<AttributeApp> getAllAttributeAppBySupplierAppId(int supplierId) {
        return attributeAppService.getAllBySupplierId(supplierId);
    }

    public AttributeApp getAttributeAppById(int id) {
        return attributeAppService.getById(id);
    }

    public AttributeApp updateAttributeApp(AttributeApp attributeApp) {
        return attributeAppService.update(attributeApp);
    }

    public void deleteAllAttributeApp() {
        attributeAppService.deleteAll();
    }

    public CategoryApp saveCategoryApp(CategoryApp categoryApp) {
        return categoryAppService.save(categoryApp);
    }

    public CategoryApp updateCategoryApp(CategoryApp categoryApp) {
        return categoryAppService.update(categoryApp);
    }

    public List<CategoryApp> getAllCategoryApp() {
        return categoryAppService.getAll();
    }

    public List<CategoryApp> getAllCategoryAppBySupplierAppId(int supplierId) {
        return categoryAppService.getAllCategoryAppBySupplierAppId(supplierId);
    }

    public void deleteAllCategoryApp() {
        categoryAppService.deleteAll();
    }

    public ManufacturerApp saveManufacturerApp(ManufacturerApp manufacturerApp) {
        return manufacturerAppService.save(manufacturerApp);

    }

    public ManufacturerApp updateManufacturerApp(ManufacturerApp manufacturerApp) {
        return manufacturerAppService.update(manufacturerApp);
    }

    public List<ManufacturerApp> getAllManufacturerApp() {
        return manufacturerAppService.getAll();
    }

    public List<ManufacturerApp> getAllManufacturerAppBySupplierId(int supplierId) {
        return manufacturerAppService.getAllBySupplierId(supplierId);
    }

    public ManufacturerApp getManufacturerAppById(int manufacturerAppId) {
        return manufacturerAppService.getManufacturerAppById(manufacturerAppId);
    }

    public void deleteAllManufacturerApp() {
        manufacturerAppService.deleteAll();
    }

    public void saveOrderDataApp(OrderProcessApp orderProcessApp) {
        orderAppService.saveOrderData(orderProcessApp);
    }


    public OrderProcessApp saveOrderProcessApp(OrderProcessApp orderProcess) {
        return orderProcessAppService.save(orderProcess);
    }

    public List<OrderProcessApp> getAllOrderProcessAppLimited(int begin, int limit) {
        return orderProcessAppService.getAllLimited(begin, limit);
    }

    public void deleteAllOrderProcessApp() {
        orderProcessAppService.deleteAll();
    }


    public ProductApp saveProductApp(ProductApp productApp) {
        return productAppService.save(productApp);
    }

    public List<ProductApp> getProductAppByOrderAndStatus(int orderProcessAppId, String status) {
        return productAppService.getByOrderAndStatus(orderProcessAppId, status);
    }

    public void saveProductAppBatch(List<ProductApp> productApps) {
        productAppService.saveBatch(productApps);
    }

    public void deleteAllProductApp() {
        productAppService.deleteAll();
    }

    public ProductProfileApp saveProductProfileApp(ProductProfileApp productProfileApp) {
        return productProfileAppService.save(productProfileApp);
    }

    public ProductProfileApp updateProductProfileApp(ProductProfileApp productProfileApp) {
        productProfileAppService.update(productProfileApp);
        return productProfileApp;
    }

    public List<ProductProfileApp> getAllProductProfileApp() {
        return productProfileAppService.getAll();
    }

    public List<ProductProfileApp> getAllProductProfileAppBySupplierId(int supplierId) {
        List<ProductProfileApp> products = productProfileAppService.getAllBySupplierId(supplierId);
        products
                .stream()
                .peek(p -> p.setOptions(optionAppService.getOptionsByProductId(p.getProductProfileId())))
                .collect(Collectors.toList());
        return products;
    }

    public List<ProductProfileApp> getProductProfileAppByManufacturerAppId(int manufacturerAppId) {
        return productProfileAppService.getProductProfileByManufacturerAppId(manufacturerAppId);
    }


    public void deleteAllProductProfileApp() {
        productProfileAppService.deleteAll();
    }


    public SupplierApp saveSupplierApp(SupplierApp supplierApp) {
        return supplierAppService.save(supplierApp);

    }


    public SupplierApp getSupplierAppByName(String name) {
        return supplierAppService.getByName(name);
    }

    public SupplierApp getSupplierAppByDisplayName(String displayName) {
        return supplierAppService.getByDisplayName(displayName);
    }

    public List<SupplierApp> getAllSupplierApp() {
        return supplierAppService.getAll();
    }

    public SupplierApp getSupplierAppById(int id) {
        return supplierAppService.getById(id);
    }

    public SupplierApp updateSupplierApp(SupplierApp supplierApp) {
        return supplierAppService.update(supplierApp);
    }

    public OptionApp saveOptionApp(OptionApp optionApp) {
        return optionAppService.saveOptionApp(optionApp);
    }

    public OptionApp updateOptionApp(OptionApp optionApp) {
        optionAppService.updateOptionApp(optionApp);
        return optionApp;
    }

    public List<OptionApp> getAllOptionApp(OptionApp optionApp) {
        return optionAppService.getAllOptionApp();
    }

    public List<OptionApp> getOptionsByProductId(int productId) {
        return optionAppService.getOptionsByProductId(productId);
    }

}
