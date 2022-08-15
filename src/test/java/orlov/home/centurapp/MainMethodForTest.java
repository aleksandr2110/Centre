package orlov.home.centurapp;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.entity.app.ProductProfileApp;
import orlov.home.centurapp.service.parser.ParserServiceAstim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class MainMethodForTest {
    public static void main(String[] args) {
       String txt = "shkaf_odezhn_y_spetsyaln_y_s_ventylyatsyonnoy_systemoy_1800hkh700kh500";
       txt = txt.substring(txt.length() - 64);
        System.out.println(txt.length());
    }

}
