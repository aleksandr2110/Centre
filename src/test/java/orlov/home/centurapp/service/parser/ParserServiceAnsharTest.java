package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.dto.api.artinhead.OptionDto;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceAnsharTest {

    @Autowired
    private ParserServiceAnshar parserServiceAnshar;
    @Test
    void doProcess() {
        parserServiceAnshar.doProcess();
    }

    @Test
    void setOptions() {
//        String url = "https://www.anshar.com.ua/uk/catalog/magazin-dityachiy-igroviy";
        String url = "https://www.anshar.com.ua/uk/catalog/stinka-dityacha-z-5-ti-elementiv-z-fotodrukom";

        Document doc = parserServiceAnshar.getWebDocument(url, new HashMap<>());
        ProductOpencart product = new ProductOpencart();
        product.setSku("13879");
        List<OptionDto> optionDtos = parserServiceAnshar.setOptions(doc, product);

//        catalog/app/laym_zelena_voda_34.jpg
//        catalog/app/laym_zelena_voda_35.jpg


    }
}