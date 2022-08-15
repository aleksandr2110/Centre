package orlov.home.centurapp.entity.opencart;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
@ToString
public class ManufacturerOpencart {
    private int manufacturerId;
    private String name = OCConstant.EMPTY_STRING;
    private String image = OCConstant.EMPTY_STRING;
    private int sortOrder = OCConstant.SORT_ORDER;
    private boolean noindex = OCConstant.NOINDEX;
    private String uuid = OCConstant.EMPTY_STRING;

    public static class Builder {
        private ManufacturerOpencart manufacturerOpencart;

        public Builder() {
            manufacturerOpencart = new ManufacturerOpencart();
        }

        public Builder(ManufacturerOpencart manufacturerOpencart) {
            this.manufacturerOpencart = manufacturerOpencart;
        }

        public Builder withManufacturerId(int manufacturerId) {
            manufacturerOpencart.manufacturerId = manufacturerId;
            return this;
        }

        public Builder withName(String name) {
            manufacturerOpencart.name = name;
            return this;
        }

        public Builder withImage(String image) {
            manufacturerOpencart.image = image;
            return this;
        }

        public Builder withSortOrder(int sortOrder) {
            manufacturerOpencart.sortOrder = sortOrder;
            return this;
        }
        public Builder withNoindex(boolean noindex) {
            manufacturerOpencart.noindex = noindex;
            return this;
        }

        public Builder withUuid(String uuid) {
            manufacturerOpencart.uuid = uuid;
            return this;
        }


        public ManufacturerOpencart build() {
            return manufacturerOpencart;
        }


    }
}
