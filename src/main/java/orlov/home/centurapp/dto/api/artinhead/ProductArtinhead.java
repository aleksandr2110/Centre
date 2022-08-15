package orlov.home.centurapp.dto.api.artinhead;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductArtinhead {
    private Map<String, String> attributes;
    @JsonProperty(value = "availability_html")
    private String availabilityHtml;
    @JsonProperty(value = "backorders_allowed")
    private Boolean backordersAllowed;
    @JsonProperty(value = "dimensions")
    private DimensionArtinhead dimension;
    @JsonProperty(value = "dimensions_html")
    private String dimensionsHtml;
    @JsonProperty(value = "display_price")
    private Integer displayPrice;
    @JsonProperty(value = "display_regular_price")
    private Integer displayRegularPrice;
    private ImageArtinhead image;
    @JsonProperty(value = "image_id")
    private Integer imageId;
    @JsonProperty(value = "is_downloadable")
    private Boolean isDownloadable;
    @JsonProperty(value = "is_in_stock")
    private Boolean isInStock;
    @JsonProperty(value = "is_purchasable")
    private Boolean isPurchasable;
    @JsonProperty(value = "is_sold_individually")
    private String isSoldIndividually;
    @JsonProperty(value = "is_virtual")
    private Boolean isVirtual;
    @JsonProperty(value = "max_qty")
    private String maxQty;
    @JsonProperty(value = "min_qty")
    private Integer minQty;
    @JsonProperty(value = "price_html")
    private String priceHtml;
    @JsonProperty(value = "sku")
    private String sku;
    @JsonProperty(value = "variation_description")
    private String variationDescription;
    @JsonProperty(value = "variation_id")
    private Integer variationId;
    @JsonProperty(value = "variation_is_active")
    private Boolean variationIsActive;
    @JsonProperty(value = "variation_is_visible")
    private Boolean variationIsVisible;
    @JsonProperty(value = "weight")
    private String weight;
    @JsonProperty(value = "weight_html")
    private String weightHtml;
    @JsonIgnore
    private boolean isDefault = false;

    @Override
    public String toString() {
        return "ProductArtinhead{" + "\n" +
                "attributes=" + attributes + "\n" +
                "\tavailabilityHtml=" + availabilityHtml + "\n" +
                "\tbackordersAllowed=" + backordersAllowed + "\n" +
                "\tdimension=" + dimension + "\n" +
                "\tdimensionsHtml=" + dimensionsHtml + "\n" +
                "\tdisplayPrice=" + displayPrice + "\n" +
                "\tdisplayRegularPrice=" + displayRegularPrice + "\n" +
                "\timage=" + image + "\n" +
                "\timageId=" + imageId + "\n" +
                "\tisDownloadable=" + isDownloadable + "\n" +
                "\tisInStock=" + isInStock + "\n" +
                "\tisPurchasable=" + isPurchasable + "\n" +
                "\tisSoldIndividually=" + isSoldIndividually + "\n" +
                "\tisVirtual=" + isVirtual + "\n" +
                "\tmaxQty=" + maxQty + "\n" +
                "\tminQty=" + minQty + "\n" +
                "\tpriceHtml=" + priceHtml + "\n" +
                "\tsku=" + sku + "\n" +
                "\tvariationDescription=" + variationDescription + "\n" +
                "\tvariationId=" + variationId + "\n" +
                "\tvariationIsActive=" + variationIsActive + "\n" +
                "\tvariationIsVisible=" + variationIsVisible + "\n" +
                "\tweight=" + weight + "\n" +
                "\tweightHtml=" + weightHtml + "}";
    }
}
