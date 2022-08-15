package orlov.home.centurapp.dao.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.user.RoleApp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class RoleAppDao implements Dao<RoleApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;


    @Override
    public int save(RoleApp roleApp) {
        String sql = "insert into role_app (role_name) \n" +
                "values (:roleName)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(roleApp), key);
        int roleId = key.getKey().intValue();
        return roleId;
    }

    @Override
    public RoleApp getById(int id) {

        return null;
    }

    @Override
    public void deleteById(int roleId) {
        String sql = "delete\n" +
                "from role_app\n" +
                "where role_id = :roleId";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource("roleId", roleId));
    }

    @Override
    public RoleApp update(RoleApp roleAppDao) {
        return null;
    }

    @Override
    public List<RoleApp> getAll() {
        String sql = "select *\n" +
                "from role_app";
        List<RoleApp> roles = jdbcTemplateApp.query(sql, (rs, rowNum) -> {
            int roleId = rs.getInt("role_id");
            String roleName = rs.getString("role_name");
            return new RoleApp(roleId, roleName);
        });
        return roles;

    }

    public RoleApp getByName(String roleName) {
        String sql = "select *\n" +
                "from role_app where role_name = :roleName";
        List<RoleApp> roles = jdbcTemplateApp.query(sql, new MapSqlParameterSource("roleName", roleName), (rs, rowNum) -> {
            int roleId = rs.getInt("role_id");
            String name = rs.getString("role_name");
            return new RoleApp(roleId, name);
        });
        return roles.isEmpty() ? null : roles.get(0);
    }
}
