package orlov.home.centurapp.service.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ParserServiceZapovitTest {

    @Autowired
    private ParserServiceZapovit parserServiceZapovit;

    @Test
    void doProcess() {
        parserServiceZapovit.doProcess();
    }
}