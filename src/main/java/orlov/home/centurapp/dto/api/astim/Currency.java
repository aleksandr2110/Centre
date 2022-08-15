package orlov.home.centurapp.dto.api.astim;

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
@JacksonXmlRootElement(localName = "currency")
public class Currency {
    @XmlAttribute(name = "id")
    private String id;
    @XmlAttribute(name = "rate")
    private double rate;
}
