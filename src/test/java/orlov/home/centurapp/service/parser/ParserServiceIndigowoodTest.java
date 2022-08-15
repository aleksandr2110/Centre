package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceIndigowoodTest {
    @Autowired
    private ParserServiceIndigowood parserServiceIndigowood;
    @Test
    void getDescription() {
        Document webDocument = parserServiceIndigowood.getWebDocument("https://indigowood.com.ua/ua/krovat-detskaya-cloud-belaya-naturalnoe-derevo?path=25", new HashMap<>());
        String description = parserServiceIndigowood.getDescription(webDocument);
        log.info("RESULT :{}", description);
    }

    @Test
    void  doProcess(){
        parserServiceIndigowood.doProcess();
    }


    @Test
    void updateModel(){
        parserServiceIndigowood.updateModel();
    }

    @Test
    void sdsdf(){
        String urlProduct = "https://indigowood.com.ua/ua/krovatka-detskaya-bubble-kit-blue-120-60?path=137";
        Document webDocument = parserServiceIndigowood.getWebDocument(urlProduct, new HashMap<>());
        String text = webDocument.select("ul.list-unstyled.description").text();
        String s = text.replaceAll("Артикул:", "").replaceAll("\\*", "");

        log.info("s: {}", s);


    }
}