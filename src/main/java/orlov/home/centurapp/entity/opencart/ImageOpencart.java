package orlov.home.centurapp.entity.opencart;

import lombok.*;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"productImageId", "productId", "image"})
public class ImageOpencart {
    private int productImageId;
    private int productId;
    private String image = OCConstant.EMPTY_STRING;
    private int sortOrder = OCConstant.SORT_ORDER;
    private String uuid = OCConstant.EMPTY_STRING;

    public static class Builder {
        private ImageOpencart imageOpencart;

        public Builder() {
            imageOpencart = new ImageOpencart();
        }

//        public Builder(ImageOpencart imageOpencart){
//            this.imageOpencart = imageOpencart;
//        }

        public Builder withProductImageId(int productImageId) {
            imageOpencart.productImageId = productImageId;
            return this;
        }

        public Builder withProductId(int productId) {
            imageOpencart.productId = productId;
            return this;
        }

        public Builder withImage(String image) {
            imageOpencart.image = image;
            return this;
        }

        public Builder withSortOrder(int sortOrder) {
            imageOpencart.sortOrder = sortOrder;
            return this;
        }

        public Builder withUuid(String uuid) {
            imageOpencart.uuid = uuid;
            return this;
        }

        public ImageOpencart build() {
            return imageOpencart;
        }

    }
}
