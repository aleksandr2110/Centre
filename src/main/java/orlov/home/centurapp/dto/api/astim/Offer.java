package orlov.home.centurapp.dto.api.astim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Slf4j
@JacksonXmlRootElement(localName = "offer")
@JsonIgnoreProperties(value = {"productSale", "productStorehouse", "productOnWay", "from"})
@EqualsAndHashCode(of = {"url"})
public class Offer {
    public String code;
    public String url;
    public double price;
    public String currencyId;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "picture")
    public List<String> pictures = new ArrayList<>();
    public String name;
    public String vendor;
    @JacksonXmlCData
    public String description;
    @JacksonXmlCData
    @JacksonXmlProperty(localName = "params")
    public String param;
    public String productSale;
    public String productStorehouse;
    public String productOnWay;
    public String from = "xml";
}
