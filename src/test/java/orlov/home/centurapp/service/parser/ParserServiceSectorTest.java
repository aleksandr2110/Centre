package orlov.home.centurapp.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.dto.api.sector.ProductSector;

import java.io.IOException;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ParserServiceSectorTest {

    @Autowired
    private ParserServiceSector parserServiceSector;

    @Test
    void doProcess() {
        parserServiceSector.doProcess();
    }

    @Test
    void getApiData() throws IOException {
        String apiUrl = "https://store.tildacdn.com/api/getproductslist/?storepartuid=130048338541";
        List<ProductSector> productsByApiUrl = parserServiceSector.getProductsByApiUrl(apiUrl);
        productsByApiUrl.forEach(p -> log.info("P: {}", p));
    }

    @Test
    void getGallery() {
        String gallery = "[{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild6438-6237-4461-b165-646434643562\\/1.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild6430-3730-4532-b865-313836356136\\/2.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild6263-3535-4430-a563-663765393363\\/3.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild3363-6335-4463-a464-656533363435\\/4.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild6439-3334-4639-a334-663834366639\\/5.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild3161-6166-4536-a365-343432316530\\/6.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild3932-3362-4663-b230-386330306139\\/7.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild3430-3562-4230-a637-653963306435\\/9.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild3632-3231-4266-a137-663834343238\\/10.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild3539-6437-4834-a237-643865386165\\/11.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild3562-3038-4762-b131-323763373533\\/-__-__ST_7_2.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild6135-3736-4535-a534-303236373435\\/-__-__ST_7_3.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild6238-6431-4564-b336-393438373463\\/-__-__ST_7_4.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild3237-3338-4935-a431-646634346130\\/-__-__ST_7.jpg\"},{\"img\":\"https:\\/\\/static.tildacdn.com\\/tild3261-3436-4334-b230-353338656431\\/767657567.jpg\",\"video\":\"https:\\/\\/www.youtube.com\\/watch?v=0AX6sO6Mg2M\",\"vtype\":\"youtube.com\",\"videoid\":\"0AX6sO6Mg2M\"}]";
        List<String> imagesUrlFromGallery = parserServiceSector.getImagesUrlFromGallery(gallery);
        imagesUrlFromGallery.forEach(i -> log.info("I: {}", i));

    }

    @Test
    void updateModel(){
        parserServiceSector.updateModel();
    }
}