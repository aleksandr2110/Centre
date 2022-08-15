package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceAbstractTest {

    @Autowired
    private ParserServiceNowystyl parserServiceNowystyl;

    @Test
    void getAttrByNameIfHas() {
    }

    @Test
    void getLastSortedAttribute() {
    }

    @Test
    void getWebDocument() {
        Document doc = parserServiceNowystyl.getWebDocument("https://soundcloud.com/", new HashMap<>());
        log.info("Doc is null: {}", Objects.isNull(doc));
    }

    @Test
    void getParentsCategories() {
    }

    @Test
    void findCategoryFromDBListByName() {
    }

    @Test
    void findMainSupplierCategory() {
    }

    @Test
    void getSupplierApp() {
    }

    @Test
    void recursiveCollectListCategory() {
    }
}