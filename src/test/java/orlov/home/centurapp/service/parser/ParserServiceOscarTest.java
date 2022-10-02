package orlov.home.centurapp.service.parser;

import lombok.AllArgsConstructor;
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
class ParserServiceOscarTest {

    @Autowired
    private ParserServiceOscar parserServiceOscar;

    @Test
    void doProcess() {
        parserServiceOscar.doProcess();
    }

    @Test
    void getDescription() {
        String url = "https://oskar.dp.ua/product/blinnyj-apparat-c-1-square";
        Document webDocument = parserServiceOscar.getWebDocument(url, new HashMap<>());
        parserServiceOscar.getDescription(webDocument);
        Document doc = parserServiceOscar.getWebDocument(url, new HashMap<>());
        parserServiceOscar.getCutAttribute(doc);
    }


}