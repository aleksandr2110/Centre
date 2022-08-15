package orlov.home.centurapp.dto.api.sector;

import com.fasterxml.jackson.annotation.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@Getter
@Setter
@JsonRootName(value = "product")
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = {"url"})
public class ProductSector {
    private String uid;
    private String title;
    private String sku;
    private String brand;
    private String text;
    private String mark;
    private Integer quantity;
    private String unit;
    private String single;
    private String price;
    private String descr;
    private String gallery;
    private String externalid;
    private String url;
    @JsonProperty(value = "characteristics")
    private List<CharacteristicSector> characteristicSectorList = new ArrayList<>();
    @JsonProperty(value = "editions")
    private List<EditionSector> editionSectorList = new ArrayList<>();
    @JsonIgnore
    private List<String> imagesUrlList = new ArrayList<>();



    @Override
    public String toString() {
        return "ProductSector{\n" +
                "\tuid=" + uid + "\n" +
                "\ttitle=" + title + "\n" +
                "\tsku=" + sku +  "\n" +
                "\tbrand=" + brand + "\n" +
                "\ttext=" + text +  "\n" +
                "\tmark=" + mark +  "\n" +
                "\tquantity=" + quantity + "\n" +
                "\tunit=" + unit + "\n" +
                "\tsingle=" + single  + "\n" +
                "\tprice=" + price + "\n" +
                "\tdescr=" + descr + "\n" +
                "\tgallery=" + gallery + "\n" +
                "\texternalid=" + externalid + "\n" +
                "\turl=" + url + "\n" +
                "\tcharacteristicSectorList=" + characteristicSectorList + "\n" +
                "\teditionSectorList=" + editionSectorList + "\n" +
                "\timagesUrlList=" + imagesUrlList + "\n" +
                '}';
    }
}
