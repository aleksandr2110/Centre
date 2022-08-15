package orlov.home.centurapp.entity.opencart;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
@ToString
public class ProductDescriptionOpencart {
    private int productId;
    private int languageId = OCConstant.UA_LANGUAGE_ID;
    private String name = OCConstant.EMPTY_STRING;
    private String description = OCConstant.EMPTY_STRING;
    private String description1 = OCConstant.EMPTY_STRING;
    private String tag = OCConstant.EMPTY_STRING;
    private String metaTitle = OCConstant.EMPTY_STRING;
    private String metaDescription = OCConstant.EMPTY_STRING;
    private String metaKeyword = OCConstant.EMPTY_STRING;
    private String metaH1 = OCConstant.EMPTY_STRING;

    public static class Builder {
        private ProductDescriptionOpencart descriptionOpencart;

        public Builder() {
            descriptionOpencart = new ProductDescriptionOpencart();
        }

        public Builder(ProductDescriptionOpencart descriptionOpencart) {
            descriptionOpencart = descriptionOpencart;
        }

        public Builder withProductId(int productId) {
            descriptionOpencart.productId = productId;
            return this;
        }

        public Builder withLanguageId(int languageId) {
            descriptionOpencart.languageId = languageId;
            return this;
        }

        public Builder withDescription(String description) {
            descriptionOpencart.description = description;
            return this;
        }

        public Builder withDescription1(String description1) {
            descriptionOpencart.description1 = description1;
            return this;
        }

        public Builder withTag(String tag) {
            descriptionOpencart.tag = tag;
            return this;
        }

        public Builder withMetaTitle(String metaTitle) {
            descriptionOpencart.metaTitle = metaTitle;
            return this;
        }

        public Builder withName(String name) {
            descriptionOpencart.name = name;
            return this;
        }

        public Builder withMetaDescription(String metaDescription) {
            descriptionOpencart.metaDescription = metaDescription;
            return this;
        }

        public Builder withMetaKeyword(String metaKeyword) {
            descriptionOpencart.metaKeyword = metaKeyword;
            return this;
        }

        public Builder withMetaH1(String metaH1) {
            descriptionOpencart.metaH1 = metaH1;
            return this;
        }

        public ProductDescriptionOpencart build(){
            return descriptionOpencart;
        }


    }

}
