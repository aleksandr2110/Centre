package orlov.home.centurapp.dao.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.user.UserApp;
import orlov.home.centurapp.mapper.app.UserAppExtractor;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class UserAppDao implements Dao<UserApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;


    @Override
    public int save(UserApp userApp) {
        String sql = "insert into user_app (user_name, user_login, user_password) " +
                "values (:userFirstName, :userLogin, :userPassword)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(userApp), key);
        int userId = key.getKey().intValue();
        return userId;
    }


    public void saveUserRole(int userId, int roleId) {
        String sql = "insert into user_role_app (user_id, role_id) " +
                "values (:suerId, :roleId)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("suerId", userId);
        params.addValue("roleId", roleId);
        jdbcTemplateApp.update(sql, params);

    }


    @Override
    public UserApp getById(int id) {
        String sql = "select *\n" +
                "from user_app u\n" +
                "left join user_role_app ur on ur.user_id = u.user_id\n" +
                "left join role_app r on r.role_id = ur.role_id\n" +
                "where u.user_id = :userId";
        List<UserApp> users = jdbcTemplateApp.query(sql, new MapSqlParameterSource("userId", id), new UserAppExtractor());
        return users.isEmpty() ? null : users.get(0);
    }

    @Override
    public void deleteById(int userId) {
        deleteUserRoleByUserId(userId);
        String sql = "delete\n" +
                "from user_app\n" +
                "where user_id = :userId";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource("userId", userId));
    }

    public void deleteUserRoleByUserId(int userId) {
        String sql = "delete\n" +
                "from user_role_app \n" +
                "where user_id = :userId";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource("userId", userId));
    }

    public void deleteUserRoleByUserRoleId(int userId, int roleId) {
        String sql = "delete\n" +
                "from user_role_app \n" +
                "where user_id = :userId and role_id = :roleId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("roleId", roleId);
        jdbcTemplateApp.update(sql, params);
    }

    @Override
    public UserApp update(UserApp userApp) {
        String sql = "update user_app\n" +
                "set user_name = :userFirstName, user_password = :userPassword\n, user_login = :userLogin " +
                "where user_id = :userId";
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(userApp));
        return userApp;
    }

    @Override
    public List<UserApp> getAll() {
        String sql = "select *\n" +
                "from user_app u\n" +
                "left join user_role_app ur on ur.user_id = u.user_id\n" +
                "left join role_app r on r.role_id = ur.role_id";
        return jdbcTemplateApp.query(sql, new UserAppExtractor());
    }

    public UserApp getByLogin(String login) {
        String sql = "select *\n" +
                "from user_app u\n" +
                "left join user_role_app ur on ur.user_id = u.user_id\n" +
                "left join role_app r on r.role_id = ur.role_id\n" +
                "where user_login = :userLogin";
        List<UserApp> users = jdbcTemplateApp.query(sql, new MapSqlParameterSource("userLogin", login), new UserAppExtractor());
        return users.isEmpty() ? null : users.get(0);
    }


}
