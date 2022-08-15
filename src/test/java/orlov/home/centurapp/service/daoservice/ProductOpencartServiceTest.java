package orlov.home.centurapp.service.daoservice;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;
import orlov.home.centurapp.entity.opencart.ImageOpencart;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;
import orlov.home.centurapp.service.daoservice.opencart.CategoryOpencartService;
import orlov.home.centurapp.service.daoservice.opencart.ProductOpencartService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ProductOpencartServiceTest {

    @Autowired
    private ProductOpencartService productOpencartService;
    @Autowired
    private CategoryOpencartService categoryOpencartService;
    private final String DEFAULT_CATEGORY_NAME = "New products";

    @Test
    void deleteFullProduct(){
        productOpencartService.deleteFullProductById(188371382);
    }


    @Test
    void saveProduct() {
        List<ImageOpencart> imagesOpencart = Arrays.asList(
                new ImageOpencart.Builder()
                        .withImage("catalog/app/953041070f000d45c05c912005f63724.jpg")
                        .withSortOrder(0)
                        .build(),
                new ImageOpencart.Builder()
                        .withImage("catalog/app/kartinka_motivatsiya_tsitata_9.jpg")
                        .withSortOrder(0)
                        .build());

        List<ProductDescriptionOpencart> descriptions = Arrays.asList(new ProductDescriptionOpencart.Builder()
                .withDescription("service test description")
                .withName("Test service product name")
                .withMetaDescription("service test meta description")
                .withMetaKeyword("service test meta keyword")
                .withMetaTitle("service test meta title")
                .withMetaH1("service test meta H1")
                .withTag("service test tad")
                .build());
        SupplierApp supplierApp = new SupplierApp();
        supplierApp.setMarkup(10);
        supplierApp.setName("nowystyl");
        List<CategoryOpencart> supplierCategoryOpencart = categoryOpencartService.getSupplierCategoryOpencart(supplierApp);

        ProductOpencart product = new ProductOpencart.Builder()
                .withImage("catalog/app/playstation-ps5-ps4-sony.jpg")
                .withPrice(new BigDecimal("777.77"))
                .withModel("service test model")
                .build();
        product.getImagesOpencart().addAll(imagesOpencart);
        product.getProductsDescriptionOpencart().addAll(descriptions);
//        product.getCategoriesOpencart().add(category);

        productOpencartService.saveProduct(product);

    }

    @Test
    public void testDo() {
        String title = "Крісло NAVIGO R NET white WA ST PL71";
        List<ProductOpencart> productsSameTitle = productOpencartService.getProductsSameTitle(title);
        productsSameTitle
                .forEach(p -> {
                    int id = p.getId();
                    String sku = p.getSku();
                    String name = p.getProductsDescriptionOpencart().get(0).getName();
                    log.info("ID: {} SKU: {} NAME: {}", id, sku, name);
                });

        ProductOpencart p1 = productsSameTitle.get(0);
        ProductOpencart p2 = productsSameTitle.get(1);
        String sku1 = p1.getSku();
        String sku2 = p2.getSku();
        sku2 = Jsoup.parse(sku2).text();
        log.info("p1: {}", sku1);
        log.info("p2: {}", sku2);
        log.info("equals: {}", sku1.equals(sku2));


    }


}