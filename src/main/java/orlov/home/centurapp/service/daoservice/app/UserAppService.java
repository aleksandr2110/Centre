package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.app.RoleAppDao;
import orlov.home.centurapp.dao.app.UserAppDao;
import orlov.home.centurapp.entity.user.RoleApp;
import orlov.home.centurapp.entity.user.UserApp;
import orlov.home.centurapp.mapper.app.UserAppExtractor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class UserAppService implements UserDetailsService {

    private final UserAppDao userAppDao;
    private final RoleAppDao roleAppDao;

    public UserApp save(UserApp userApp) {
        int userId = userAppDao.save(userApp);
        userApp.setUserId(userId);
        userApp.getRoles()
                .stream()
                .peek(r -> userAppDao.saveUserRole(userApp.getUserId(), r.getRoleId()))
                .collect(Collectors.toList());
        return userApp;
    }

    public void deleteUserRoleById(int userId) {
        userAppDao.deleteUserRoleByUserId(userId);
    }

    public void saveUserRole(int userId, int roleId) {
        userAppDao.saveUserRole(userId, roleId);
    }

    public void deleteUserRoleByUserRoleId(int userId, int roleId) {
        userAppDao.deleteUserRoleByUserRoleId(userId, roleId);
    }

    public void deleteById(int userId) {
        userAppDao.deleteById(userId);
    }


    public UserApp update(UserApp userApp) {
        return userAppDao.update(userApp);
    }


    public List<UserApp> getAll() {
        return userAppDao.getAll();
    }

    public UserApp getByLogin(String login) {
        return userAppDao.getByLogin(login);
    }


    public UserApp getById(int id) {
        return userAppDao.getById(id);
    }


    @Override
    public UserApp loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAppDao.getByLogin(username);
    }


    public RoleApp saveRole(RoleApp roleApp) {
        int id = roleAppDao.save(roleApp);
        roleApp.setRoleId(id);
        return roleApp;
    }


    public void deleteRoleById(int roleId) {
        roleAppDao.deleteById(roleId);
    }


    public List<RoleApp> getAllRole() {
        return roleAppDao.getAll();
    }

    public RoleApp getRoleByName(String roleName) {
        return roleAppDao.getByName(roleName);
    }
}
