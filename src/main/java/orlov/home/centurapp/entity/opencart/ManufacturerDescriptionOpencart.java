package orlov.home.centurapp.entity.opencart;

import lombok.Getter;
import lombok.Setter;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
public class ManufacturerDescriptionOpencart {
    private int manufacturerId;
    private int languageId = OCConstant.UA_LANGUAGE_ID;
    private String description = OCConstant.EMPTY_STRING;
    private String metaDescription = OCConstant.EMPTY_STRING;
    private String metaKeyword = OCConstant.EMPTY_STRING;
    private String metaTitle = OCConstant.EMPTY_STRING;
    private String metaH1 = OCConstant.EMPTY_STRING;

    public static class Builder {
        private ManufacturerDescriptionOpencart manufacturerDescriptionOpencart;

        public Builder() {
            manufacturerDescriptionOpencart = new ManufacturerDescriptionOpencart();
        }

        public Builder(ManufacturerDescriptionOpencart manufacturerDescriptionOpencart) {
            this.manufacturerDescriptionOpencart = manufacturerDescriptionOpencart;
        }

        public Builder withManufacturerId(int manufacturerId) {
            manufacturerDescriptionOpencart.manufacturerId = manufacturerId;
            return this;
        }

        public Builder withLanguageId(int languageId) {
            manufacturerDescriptionOpencart.languageId = languageId;
            return this;
        }

        public Builder withDescription(String description) {
            manufacturerDescriptionOpencart.description = description;
            return this;
        }

        public Builder withMetaDescription(String metaDescription) {
            manufacturerDescriptionOpencart.metaDescription = metaDescription;
            return this;
        }

        public Builder withMetaKeyword(String metaKeyword) {
            manufacturerDescriptionOpencart.metaKeyword = metaKeyword;
            return this;
        }

        public Builder withMetaTitle(String metaTitle) {
            manufacturerDescriptionOpencart.metaTitle = metaTitle;
            return this;
        }

        public Builder withMetaH1(String metaH1) {
            manufacturerDescriptionOpencart.metaH1 = metaH1;
            return this;
        }

        public ManufacturerDescriptionOpencart build(){
            return manufacturerDescriptionOpencart;
        }

    }
}
