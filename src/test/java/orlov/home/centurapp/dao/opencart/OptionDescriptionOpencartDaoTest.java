package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.OptionDescriptionOpencart;
import orlov.home.centurapp.util.OCConstant;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class OptionDescriptionOpencartDaoTest {

    @Autowired
    private OptionDescriptionOpencartDao optionDescriptionOpencartDao;

    @Test
    void save() {
        OptionDescriptionOpencart optionDescriptionOpencart = new OptionDescriptionOpencart();
        optionDescriptionOpencart.setOptionId(156);
        optionDescriptionOpencart.setName("Colir");
        optionDescriptionOpencart.setLanguageId(OCConstant.UA_LANGUAGE_ID);
        optionDescriptionOpencartDao.save(optionDescriptionOpencart);
    }

    @Test
    void getById() {
    }

    @Test
    void deleteById() {
    }

    @Test
    void update() {
    }

    @Test
    void getAll() {
    }
}