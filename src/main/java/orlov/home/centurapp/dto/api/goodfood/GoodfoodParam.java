package orlov.home.centurapp.dto.api.goodfood;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Slf4j
@JacksonXmlRootElement(localName = "param")
public class GoodfoodParam {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlText
    private String text;
}
