package orlov.home.centurapp.dao.app;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.app.ProductApp;
import orlov.home.centurapp.mapper.app.ProductAppRowMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@AllArgsConstructor
public class ProductAppDao implements Dao<ProductApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;


    public void saveBatch(List<ProductApp> productsApp) {
        String sql = "insert into product_app (order_process_id, name, url, status, old_price, new_price) " +
                "values (:orderProcessId, :name, :url, :status, :oldPrice, :newPrice)";

        SqlParameterSource[] data = SqlParameterSourceUtils.createBatch(productsApp);
        jdbcTemplateApp.batchUpdate(sql, data);
    }

    @Override
    public int save(ProductApp productApp) {
        String sql = "insert into product_app (order_process_id, name, url, status, old_price, new_price) " +
                "values (:orderProcessId, :name, :url, :status, :oldPrice, :newPrice)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(productApp), keyHolder, new String[]{"product_app_id"});
        int id = keyHolder.getKey().intValue();
        return id;
    }

    public List<ProductApp> getByOrderAndStatus(int orderProcessAppId, String status){
        String sql = "select * from product_app where order_process_id = :orderProcessId and status = :status";
        Map<String, Object> data = new HashMap<>();
        data.put("orderProcessId", orderProcessAppId);
        data.put("status", status);
        List<ProductApp> products = jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new ProductAppRowMapper());
        return products;
    }

    @Override
    public ProductApp getById(int id) {
        return null;
    }

    public void deleteAll() {
        String sql = "delete from product_app";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource());
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public ProductApp update(ProductApp productApp) {
        return null;
    }

    @Override
    public List<ProductApp> getAll() {
        return null;
    }
}
