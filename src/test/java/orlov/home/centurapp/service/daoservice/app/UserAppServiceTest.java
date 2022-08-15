package orlov.home.centurapp.service.daoservice.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.user.UserApp;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class UserAppServiceTest {


    @Autowired
    private UserAppService userAppService;

    @Test
    void save() {
        UserApp user = new UserApp();
        user.setUserLogin("undino");
        user.setUserFirstName("Orlov");
        user.setUserPassword("409759814");
        userAppService.save(user);
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

    @Test
    void getByLogin() {
    }

    @Test
    void loadUserByUsername() {
    }
}