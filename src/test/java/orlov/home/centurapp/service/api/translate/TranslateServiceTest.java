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
        String text = "Наша компания предлагает Вам широкий выбор облицовки фронтальной части печи серия Inox frontal Pax, производства Morello Forni, Италия.Выполнена облицовка для печи из нержавеющей стали. Все модели включают в себя полку и аналоговый термометр.\n" +
                "\n" +
                "Модельный ряд облицовки доступный к покупке у нас: Inox frontal Pax90,Inox frontal Pax100, Inox frontal Pax110, Inox frontal Pax120, Inox frontal Pax130, Inox frontal Pax140.";
            log.info("Text: \n{}", text);
        String translatedText = translateService.getTranslatedText(text);
        log.info("Translated text: {}", translatedText);
    }


}