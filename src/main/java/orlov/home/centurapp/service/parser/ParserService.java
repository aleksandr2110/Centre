package orlov.home.centurapp.service.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.OpencartDto;
import orlov.home.centurapp.entity.app.*;
import orlov.home.centurapp.entity.opencart.AttributeOpencart;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import javax.websocket.server.ServerEndpoint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public interface ParserService {
    void doProcess();

    SupplierApp buildSupplierApp(String supplierName, String displayName, String supplierUrl);

    CategoryOpencart findMainSupplierCategory(SupplierApp supplierApp);

    List<CategoryOpencart> getSiteCategories(SupplierApp supplierApp);

    List<CategoryOpencart> getParentsCategories(CategoryOpencart lowerCategory, List<CategoryOpencart> siteCategory);

    List<CategoryOpencart> recursiveCollectListCategory(CategoryOpencart category, List<CategoryOpencart> supplierCategoryOpencartDB);

    CategoryOpencart findCategoryFromDBListByName(CategoryOpencart newCategory, List<CategoryOpencart> categoriesOpencartDB);

    CategoryOpencart recursiveWalkSiteCategory(CategoryOpencart category);

    List<ProductOpencart> getProductsInitDataByCategory(List<CategoryOpencart> categoriesWithProduct, SupplierApp supplierApp);

    List<ProductOpencart> getFullProductsData(List<ProductOpencart> products, SupplierApp supplierApp);

    //READY ABSTRACT
    Document getWebDocument(String url, Map<String, String> cookies);

    //READY ABSTRACT
    AttributeOpencart getAttributeOpencartByNameIfHas(List<AttributeOpencart> attributes, String name);

    //READY ABSTRACT
    int getLastSortedAttribute(List<AttributeOpencart> attributesOpencart);

    void checkPrice(OpencartDto opencartInfo, SupplierApp supplierApp);

    ProductOpencart setManufacturer(ProductOpencart product, SupplierApp supplierApp);

    ManufacturerApp getManufacturerApp(String manufacturerName, SupplierApp supplierApp);

    AttributeWrapper getAttribute(AttributeWrapper a, SupplierApp supplierApp, ProductProfileApp productProfileApp);

    AttributeApp getAttributeAppByNameIfHas(List<AttributeApp> attributesAppDB, String name);

    OpencartDto getOpencartInfo(List<ProductOpencart> productFromSite, SupplierApp supplierApp);

    ProductOpencart setPriceWithMarkup(ProductOpencart product);

    ProductProfileApp getProductProfile(ProductProfileApp productProfileApp, SupplierApp supplierApp);

    CategoryApp getCategoryApp(String name, SupplierApp supplierApp);

    CategoryOpencart getGlobalCategory();

    void updateProductSupplierOpencartBySupplierApp(SupplierApp supplierApp);

    void downloadImage(String url, String imageName);

    String cleanDescription(Element descriptionElement);

//    String getProductSupplierModel(Element productElement);


}
