package orlov.home.centurapp.entity.app;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"supplierId", "sku"})
public class ProductProfileApp {
    private int productProfileId;
    private String url;
    private String sku;
    private String title;
    private int supplierId;
    private int manufacturerId;
    private int categoryId;
    private BigDecimal price;
    private SupplierApp supplierApp;
    private CategoryApp categoryApp;
    private ManufacturerApp manufacturerApp;
    private List<OptionApp> options = new ArrayList<>();


    public static class Builder {
        private ProductProfileApp newProductProfile;

        public Builder() {
            newProductProfile = new ProductProfileApp();
        }

        public Builder withProductProfileId(int productProfileId) {
            newProductProfile.productProfileId = productProfileId;
            return this;
        }

        public Builder withUrl(String url) {
            newProductProfile.url = url;
            return this;
        }


        public Builder withSku(String sku) {
            newProductProfile.sku = sku;
            return this;
        }


        public Builder withTitle(String title) {
            newProductProfile.title = title;
            return this;
        }

        public Builder withPrice(BigDecimal price) {
            newProductProfile.price = price;
            return this;
        }

        public Builder withSupplierId(int supplierId) {
            newProductProfile.supplierId = supplierId;
            return this;
        }

        public Builder withManufacturerId(int manufacturerId) {
            newProductProfile.manufacturerId = manufacturerId;
            return this;
        }

        public Builder withCategoryId(int categoryId) {
            newProductProfile.categoryId = categoryId;
            return this;
        }

        public Builder withSupplierApp(SupplierApp supplierApp) {
            newProductProfile.supplierApp = supplierApp;
            return this;
        }

        public Builder withCategoryApp(CategoryApp categoryApp) {
            newProductProfile.categoryApp = categoryApp;
            return this;
        }

        public Builder withManufacturerApp(ManufacturerApp manufacturerApp) {
            newProductProfile.manufacturerApp = manufacturerApp;
            return this;
        }

        public ProductProfileApp build() {
            return newProductProfile;
        }

    }

}
