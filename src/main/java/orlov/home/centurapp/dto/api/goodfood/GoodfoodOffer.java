package orlov.home.centurapp.dto.api.goodfood;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Slf4j
@JacksonXmlRootElement(localName = "offer")
public class GoodfoodOffer {
    private String url;
    private String price;
    private String currencyId;
    private String categoryId;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "picture")
    private List<String> picture = new ArrayList<>();
    private String vendor;
    private String name;
    @JacksonXmlCData
    private String description;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "param")
    private List<GoodfoodParam> params = new ArrayList<>();
    @XmlAttribute(name = "id")
    private String id;
    @XmlAttribute(name = "available")
    private boolean available;
}
