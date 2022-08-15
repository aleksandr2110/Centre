package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.CategoryDescriptionOpencart;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class CategoryDescriptionOpencartDaoTest {

    @Autowired
    private CategoryDescriptionOpencartDao categoryDescriptionOpencartDao;

    @Test
    void save() {
        CategoryDescriptionOpencart categoryDescriptionOpencart = new CategoryDescriptionOpencart.Builder()
                .withCategoryId(59)
                .withName("test cat")
                .withDescription("test cat desc")
                .withMetaTitle("test cat meta title")
                .withDescription("test cat meta desc")
                .withMetaKeyword("test cat meta key word")
                .withMetaH1("test cat meta h1")
                .build();
        categoryDescriptionOpencartDao.save(categoryDescriptionOpencart);
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