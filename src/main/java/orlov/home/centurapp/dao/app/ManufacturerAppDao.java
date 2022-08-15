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
import orlov.home.centurapp.entity.app.AttributeApp;
import orlov.home.centurapp.entity.app.ManufacturerApp;
import orlov.home.centurapp.mapper.app.AttributeAppRowMapper;
import orlov.home.centurapp.mapper.app.ManufacturerAppRowMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
@AllArgsConstructor
public class ManufacturerAppDao implements Dao<ManufacturerApp> {
    private final NamedParameterJdbcTemplate jdbcTemplateApp;


    @Override
    public int save(ManufacturerApp manufacturerApp) {
        String sql = "insert into manufacturer_app (supplier_id, supplier_title, opencart_title, markup) " +
                "values (:supplierId, :supplierTitle, :opencartTitle, :markup)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(manufacturerApp), key, new String[]{"manufacturer_id"});
        int id = key.getKey().intValue();
        return id;
    }

    @Override
    public ManufacturerApp getById(int id) {
        String sql = "select * from manufacturer_app where manufacturer_id = :manufacturerId";
        List<ManufacturerApp> manufacturerAppList = jdbcTemplateApp.query(sql, new MapSqlParameterSource("manufacturerId", id),new ManufacturerAppRowMapper());
        if (!manufacturerAppList.isEmpty())
            return manufacturerAppList.get(0);
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public ManufacturerApp update(ManufacturerApp manufacturerApp) {
        String sql = "update manufacturer_app " +
                "set opencart_title = :opencartTitle, " +
                "    markup = :markup " +
                "where manufacturer_id = :manufacturerId";

        Map<String, Object> data = new HashMap<>();
        data.put("opencartTitle", manufacturerApp.getOpencartTitle());
        data.put("markup", manufacturerApp.getMarkup());
        data.put("manufacturerId", manufacturerApp.getManufacturerId());

        jdbcTemplateApp.update(sql, new MapSqlParameterSource(data));
        return manufacturerApp;
    }

    public void deleteAll() {
        String sql = "delete from manufacturer_app";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource());
    }

    @Override
    public List<ManufacturerApp> getAll() {
        String sql = "select * from manufacturer_app";
        return jdbcTemplateApp.query(sql, new ManufacturerAppRowMapper());
    }

    public List<ManufacturerApp> getAllBySupplierId(int supplierId) {
        String sql = "select * from manufacturer_app where supplier_id = :supplierId";
        Map<String, Object> data = new HashMap<>();
        data.put("supplierId", supplierId);
        return jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new ManufacturerAppRowMapper());
    }
}
