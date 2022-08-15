package orlov.home.centurapp.dto.api.goodfood;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.annotation.XmlAttribute;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Slf4j
@JacksonXmlRootElement(localName = "yml_catalog")
public class GoodfoodRootXml {
    @JacksonXmlProperty(localName = "shop")
    private GoodfoodShop shop;
    @XmlAttribute
    private String date;
}
