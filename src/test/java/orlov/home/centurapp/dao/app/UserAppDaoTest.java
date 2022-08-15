package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.user.UserApp;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class UserAppDaoTest {

    @Autowired
    private UserAppDao userAppDao;

    @Test
    void save() {
        UserApp user = new UserApp();
        user.setUserFirstName("Vas9");
        user.setUserLogin("@Vas4@");
        user.setUserPassword("password_vas9");
        userAppDao.save(user);
    }

    @Test
    void getById() {

    }

    @Test
    void deleteById() {
        userAppDao.deleteById(1);
    }

    @Test
    void update() {
        UserApp user = new UserApp();
        user.setUserId(4);
        user.setUserFirstName("sdfsdfgsdfg");
        user.setUserLogin("sdfdsfgsdfgsdfg");
        user.setUserPassword("sdfgsdfgsdfg");
        userAppDao.update(user);
    }

    @Test
    void getAll() {
        List<UserApp> all = userAppDao.getAll();
        all.forEach(u ->   log.info("User: {}", u));
    }

    @Test
    void getByLogin(){
        UserApp byLogin = userAppDao.getByLogin("@sdfsfsdf@");
        log.info("By login: {}", byLogin);
    }
}