package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.OptionOpencart;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class OptionOpencartDaoTest {

    @Autowired
    private OptionOpencartDao optionOpencartDao;

    @Test
    void save() {
        OptionOpencart option = new OptionOpencart();
        option.setType("radio");
        int id = optionOpencartDao.save(option);
        log.info("Option id: {}", id);
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
        List<OptionOpencart> all = optionOpencartDao.getAll();
        all.forEach(o -> log.info("Option opencart: {}", o));
    }
}