package orlov.home.centurapp.entity.app;

import lombok.*;
import orlov.home.centurapp.dto.api.goodfood.GoodfoodCategory;
import orlov.home.centurapp.dto.api.goodfood.GoodfoodOffer;
import orlov.home.centurapp.entity.opencart.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(of = {"supplierAppId", "url", "name", "markup",})
public class SupplierApp {
    private int supplierAppId;
    private String url;
    private String displayName;
    private String name;
    private int markup;
    private List<CategoryOpencart> categoryOpencartDB = new ArrayList<>();
    private List<CategoryOpencart> siteCategories = new ArrayList<>();
    private List<CategoryApp> categoryAppDB = new ArrayList<>();
    private AttributeGroupOpencart defaultGlobalAttributeGroup;
    private List<AttributeOpencart> attributesOpencartDB = new ArrayList<>();
    private List<AttributeApp> attributesAppDB = new ArrayList<>();
    private List<ManufacturerOpencart> manufacturerOpencartDB = new ArrayList<>();
    private List<ManufacturerApp> manufacturersAppDB = new ArrayList<>();
    private List<ProductProfileApp> productProfilesApp = new ArrayList<>();
    private CategoryOpencart globalSupplierCategory;
    private CategoryOpencart mainSupplierCategory;
    private List<OptionOpencart> optionOpencartList = new ArrayList<>();
    private List<GoodfoodCategory> goodfoodCategories = new ArrayList<>();
    private List<GoodfoodOffer> goodfoodOffers = new ArrayList<>();

    public SupplierApp(String url, String displayName, String name, int markup) {
        this.url = url;
        this.displayName = displayName;
        this.name = name;
        this.markup = markup;
    }

    public static class Builder {
        private SupplierApp supplierApp;

        public Builder() {
            supplierApp = new SupplierApp();
        }

        public Builder withSupplierAppId(int supplierAppId) {
            supplierApp.supplierAppId = supplierAppId;
            return this;
        }

        public Builder withUrl(String url) {
            supplierApp.url = url;
            return this;
        }

        public Builder withName(String name) {
            supplierApp.name = name;
            return this;
        }

        public Builder withDisplayName(String displayName) {
            supplierApp.displayName = displayName;
            return this;
        }


        public Builder withMarkup(int markup) {
            supplierApp.markup = markup;
            return this;
        }


        public SupplierApp build() {
            return supplierApp;
        }

    }


}
