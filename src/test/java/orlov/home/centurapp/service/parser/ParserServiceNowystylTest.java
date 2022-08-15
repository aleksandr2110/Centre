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
class ParserServiceNowystylTest {
    @Autowired
    private ParserServiceNowystyl parserServiceNowystyl;

    @Test
    void doProcess() {
        parserServiceNowystyl.doProcess();
    }

    @Test
    void updateModel(){
        parserServiceNowystyl.updateModel();
    }

    @Test
    void getDescription() {
        Document doc = parserServiceNowystyl.getWebDocument("https://nowystyl.ua/glory-gtp-white-tilt-pw62", new HashMap<>());
        String description = parserServiceNowystyl.getDescription(doc);
        log.info("Desc res: {}", description);
    }
}