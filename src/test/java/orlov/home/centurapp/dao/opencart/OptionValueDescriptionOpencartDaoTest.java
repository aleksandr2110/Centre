package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.OptionValueDescriptionOpencart;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
class OptionValueDescriptionOpencartDaoTest {
    @Autowired
    private OptionValueDescriptionOpencartDao optionValueDescriptionOpencartDao;

    @Test
    void save() {
        OptionValueDescriptionOpencart valueDescription = new OptionValueDescriptionOpencart();
        valueDescription.setName("bubu");
        valueDescription.setOptionValueId(2539);
        valueDescription.setOptionId(17);
        optionValueDescriptionOpencartDao.save(valueDescription);
    }
}