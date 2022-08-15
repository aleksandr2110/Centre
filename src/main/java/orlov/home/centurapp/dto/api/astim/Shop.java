package orlov.home.centurapp.dto.api.astim;

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
@JacksonXmlRootElement(localName = "shop")
public class Shop {
    private String name;
    private String company;
    private String url;
    @JacksonXmlElementWrapper(localName = "currencies")
    @JacksonXmlProperty(localName = "currency")
    private List<Currency> currencies = new ArrayList<>();
    @JacksonXmlElementWrapper(localName = "offers")
    @JacksonXmlProperty(localName = "offer")
    private List<Offer> offers = new ArrayList<>();
}
