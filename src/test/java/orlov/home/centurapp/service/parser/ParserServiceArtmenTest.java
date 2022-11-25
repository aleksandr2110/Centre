package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.unbescape.json.JsonEscape;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceArtmenTest {

    @Autowired
    private ParserServiceArtmen parserServiceArtmen;

    @Test
    void doProcess() {
        parserServiceArtmen.doProcess();
    }

    @Test
    void getOptions() {
        Document doc = parserServiceArtmen.getWebDocument("https://artmen.rv.ua/ua/p885890777-kreslo-parikmaherskoe-globe.html", new HashMap<>());
        Elements script = doc.select("script");
        log.info("JS size: {}", script.size());
        Element optionElement = script
                .stream()
                .filter(s -> s.data().contains("ProductVariations"))
                .findFirst()
                .orElse(null);

       if (Objects.nonNull(optionElement)){
           String textScript = optionElement.data().trim();
//           log.info("Text option script: \n{}", textScript);

           int idxStart = textScript.indexOf("\"value\"");
           int idxEnd= textScript.indexOf("\"queue\"");


           String optionDataString = textScript.substring(idxStart + 10, idxEnd -3);
           optionDataString = optionDataString.replaceAll("\\\\\"", "\"");
           log.info("Option data string: {}", optionDataString);

       }

    }

    public String decodeUnicode(String s) {
        int i = 0, len = s.length();
        char c;
        StringBuffer sb = new StringBuffer(len);
        while (i < len) {
            c = s.charAt(i++);
            if (c == '\\') {
                if (i < len) {
                    c = s.charAt(i++);
                    if (c == 'u') {
                        // TODO: check that 4 more chars exist and are all hex digits
                        c = (char) Integer.parseInt(s.substring(i, i + 4), 16);
                        i += 4;
                    } // add other cases here as desired...
                }
            } // fall through: \ escapes itself, quotes any character but u
            sb.append(c);
        }
        return sb.toString();
    }
}