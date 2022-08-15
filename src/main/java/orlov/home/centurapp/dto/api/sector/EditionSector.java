package orlov.home.centurapp.dto.api.sector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditionSector {
    private String uid;
    private String price;
    private String priceold;
    private String sku;
    private String quantity;
    private String img;
    @JsonProperty(value = "Товари (комплектація)")
    private String editTitle;

    @Override
    public String toString() {
        return "\n\tEditionSector{" + "\n" +
                "\t\teditTitle=" + editTitle + "\n" +
                "\t\tuid=" + uid + "\n" +
                "\t\tprice=" + price + "\n" +
                "\t\tpriceold=" + priceold + "\n" +
                "\t\tsku=" + sku + "\n" +
                "\t\tquantity=" + quantity + "\n" +
                "\t\timg=" + img + "\n" +
                "\t}";
    }
}
