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
import orlov.home.centurapp.entity.app.CategoryApp;
import orlov.home.centurapp.entity.app.ProductProfileApp;
import orlov.home.centurapp.mapper.app.CategoryAppRowMapper;
import orlov.home.centurapp.mapper.app.ProductProfileRowMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
@AllArgsConstructor
public class CategoryAppDao implements Dao<CategoryApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;

    @Override
    public int save(CategoryApp categoryApp) {
        String sql = "insert into category_app (supplier_id, supplier_title, opencart_title, markup) " +
                "values (:supplierId, :supplierTitle, :opencartTitle, :markup)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(categoryApp), key, new String[]{"category_id"});
        int id = key.getKey().intValue();
        return id;
    }

    @Override
    public CategoryApp getById(int id) {
        String sql = "select * from category_app where category_id = :categoryId";
        Map<String, Object> data = new HashMap<>();
        data.put("categoryId", id);
        List<CategoryApp> query = jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new CategoryAppRowMapper());
        CategoryApp categoryApp = query.size() > 0 ? query.get(0) : null;
        return categoryApp;
    }

    @Override
    public void deleteById(int id) {

    }

    public void deleteAll() {
        String sql = "delete from category_app";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource());
    }

    @Override
    public CategoryApp update(CategoryApp categoryApp) {
        String sql = "update category_app " +
                "set opencart_title = :opencartTitle, " +
                "    markup = :markup " +
                "where category_id = :categoryId";

        Map<String, Object> data = new HashMap<>();
        data.put("opencartTitle", categoryApp.getOpencartTitle());
        data.put("markup", categoryApp.getMarkup());
        data.put("categoryId", categoryApp.getCategoryId());
        jdbcTemplateApp.update(sql, new MapSqlParameterSource(data));
        return categoryApp;
    }

    @Override
    public List<CategoryApp> getAll() {
        String sql = "select * from category_app";
        return jdbcTemplateApp.query(sql, new CategoryAppRowMapper());
    }

    public List<CategoryApp> getAllCategoryAppBySupplierAppId(int supplierId) {
        String sql = "select * from category_app where supplier_id = :supplierId";
        Map<String, Object> data = new HashMap<>();
        data.put("supplierId", supplierId);
        return jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new CategoryAppRowMapper());
    }

}
