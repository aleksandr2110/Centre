package orlov.home.centurapp.service.api.translate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.swing.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class TranslateServiceTest {

    @Autowired
    private TranslateService translateService;

    @Test
    void getTranslatedText() {
        String text = "\tПростая и удобная в использовании профессиональная кухонная техника. \n" +
                "Целью оборудования является облегчение рутинной работы повара, быстрое и легкое выполнение разнообразных кулинарных задач почти без вмешательства пользователя. \n" +
                "Овощерезка работает по принципу: толкатель направляет продукты к вращающемуся ножу, после чего они нарезаются установленной насадкой. \n" +
                "В оборудовании есть специальные загрузочные отверстия, благодаря которым продукты попадают внутрь для нарезки на кусочки разной формы и толщины. \n" +
                "Модель имеет функцию автоматического отключения машины с последующим включением. В комплекте с овощерезкой поставляется набор из 5 ножей: слайсер 2 мм и 4 мм, терка 3 мм, 5 мм и 7 мм.";
            log.info("Text: \n{}", text);
        String translatedText = translateService.getTranslatedText(text);
        log.info("Translated text: {}", translatedText);
    }


}