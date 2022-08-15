package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.OptionApp;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class OptionAppDaoTest {

    @Autowired
    private OptionAppDao optionAppDao;

    @Test
    void save() {
        OptionApp optionApp = new OptionApp();
        optionApp.setProductProfileId(12508);
        optionApp.setValueId(55);
        optionApp.setOptionValue("test value");
        optionApp.setOptionPrice(new BigDecimal("13.77"));
        int save = optionAppDao.save(optionApp);
        log.info("Option app id: {}", save);
    }

    @Test
    void update() {
        OptionApp optionApp = new OptionApp();
        optionApp.setOptionId(1);
        optionApp.setProductProfileId(12508);
        optionApp.setValueId(55);
        optionApp.setOptionValue("test value");
        optionApp.setOptionPrice(new BigDecimal("13.89"));
        optionAppDao.update(optionApp);
        log.info("Option app updated");
    }

    @Test
    void getByProductId(){
        List<OptionApp> options = optionAppDao.getOptionsByProductId(12508);
        options.forEach(o -> log.info("Option by product id: {} option: {}", 12508, o));
    }

    @Test
    void getAll(){
        List<OptionApp> options = optionAppDao.getOptionsByProductId(12508);
        options.forEach(o -> log.info("Option by product id: {} option: {}", 12508, o));
    }
}