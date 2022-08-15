package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.OptionValueOpencart;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class OptionValueOpencartDaoTest {
    @Autowired
    private OptionValueOpencartDao optionValueOpencartDao;

    @Test
    void save() {
        OptionValueOpencart value = new OptionValueOpencart();
        value.setOptionId(17);
        value.setImage("test");
        int save = optionValueOpencartDao.save(value);
        log.info("ID: {}", save);
    }
}