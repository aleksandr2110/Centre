package orlov.home.centurapp.dao.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.mapper.app.SupplierAppRowMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@AllArgsConstructor
@Slf4j
public class SupplierAppDao implements Dao<SupplierApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;

    @Override
    public int save(SupplierApp supplierApp) {
        String sql = "insert into supplier_app (url,name, display_name, markup) " +
                "values (:url, :name, :displayName,:markup)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(supplierApp), keyHolder, new String[]{"supplier_app_id"});
        int id = keyHolder.getKey().intValue();
        return id;
    }

    @Override
    public SupplierApp getById(int id) {
        String sql = "select * from supplier_app where supplier_app_id = :id";
        List<SupplierApp> suppliers = jdbcTemplateApp.query(sql, new MapSqlParameterSource("id", id), new SupplierAppRowMapper());
        if (suppliers.size() > 0)
            return suppliers.get(0);
        return null;
    }

    public SupplierApp getByName(String name) {
        String sql = "select * from supplier_app where name = :name";
        List<SupplierApp> suppliers = jdbcTemplateApp.query(sql, new MapSqlParameterSource("name", name), new SupplierAppRowMapper());
        log.info("suppliers size: {}", suppliers.size());
        return suppliers.size() > 0 ? suppliers.get(0) : null;
    }

    public SupplierApp getByDisplayName(String displayName) {
        SupplierApp supplierApp = null;
        String sql = "select * from supplier_app where display_name = :displayName";
        List<SupplierApp> suppliers = jdbcTemplateApp.query(sql, new MapSqlParameterSource("displayName", displayName), new SupplierAppRowMapper());
        log.info("suppliers size: {}", suppliers.size());
        if (suppliers.size() > 0) {
            supplierApp = suppliers.get(0);
        }
        return supplierApp;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public SupplierApp update(SupplierApp supplierApp) {
        String sql = "update supplier_app set markup = :markup where supplier_app_id = :supplierAppId";
        Map<String, Object> data = new HashMap<>();
        data.put("markup", supplierApp.getMarkup());
        data.put("supplierAppId", supplierApp.getSupplierAppId());
        jdbcTemplateApp.update(sql, new MapSqlParameterSource(data));
        return null;
    }

    @Override
    public List<SupplierApp> getAll() {
        String sql = "select * from supplier_app";
        List<SupplierApp> suppliers = jdbcTemplateApp.query(sql, new SupplierAppRowMapper());
        return suppliers;
    }

}
