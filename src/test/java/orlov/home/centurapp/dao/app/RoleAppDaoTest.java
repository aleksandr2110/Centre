package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.user.RoleApp;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class RoleAppDaoTest {

    @Autowired
    private RoleAppDao roleAppDao;

    @Test
    void save() {
        RoleApp roleApp = new RoleApp();
        roleApp.setRoleName("ADMIN");
        roleAppDao.save(roleApp);

//        roleApp.setRoleName("USER");
//        roleAppDao.save(roleApp);
    }

    @Test
    void getById() {
    }

    @Test
    void deleteById() {
        roleAppDao.deleteById(1);
    }

    @Test
    void update() {
    }

    @Test
    void getAll() {
        List<RoleApp> all = roleAppDao.getAll();
        all.forEach(r -> log.info("Role: {}", r));
    }

    @Test
    void getByName() {
        RoleApp user = roleAppDao.getByName("ddd");
        log.info("role user: {}", user);

    }
}