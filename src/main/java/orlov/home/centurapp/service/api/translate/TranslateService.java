package orlov.home.centurapp.service.api.translate;


import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class TranslateService {
    private final String KEY_API = "AIzaSyCfFgVYcIVu3j_RnZk8VtTtV1IaOulHyBY";

    public String getTranslatedText(String text) {
//            TODO off translate
        try {
            Translate translate = TranslateOptions.newBuilder().setApiKey(KEY_API).build().getService();
            Translation translation = translate.translate(text, Translate.TranslateOption.sourceLanguage("ru"), Translate.TranslateOption.targetLanguage("uk"), Translate.TranslateOption.format("text"));
            text = translation.getTranslatedText();
            log.info("Translated text: {}", text);
        } catch (Exception e) {
            log.warn("Exception google translate", e);
        }
        return text;
    }


}
