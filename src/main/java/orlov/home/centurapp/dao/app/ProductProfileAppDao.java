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
import orlov.home.centurapp.entity.app.ProductProfileApp;
import orlov.home.centurapp.mapper.app.ProductProfileExtractor;
import orlov.home.centurapp.mapper.app.ProductProfileRowMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Slf4j
@Repository
public class ProductProfileAppDao implements Dao<ProductProfileApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;

    @Override
    public int save(ProductProfileApp productProfileApp) {
        String sql = "insert into product_profile_app (url, sku, title, supplier_id, manufacturer_id, category_id, price) " +
                "values (:url, :sku, :title, :supplierId, :manufacturerId, :categoryId, :price)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(productProfileApp), key, new String[]{"product_profile_id"});
        int id = key.getKey().intValue();
        return id;
    }

    @Override
    public ProductProfileApp getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {
        String sql = "delete from product_profile_app where product_profile_id = :productProfileId";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource("productProfileId", id));
    }

    public void deleteAll() {
        String sql = "delete from product_profile_app";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource());
    }

    @Override
    public ProductProfileApp update(ProductProfileApp productProfileApp) {
        String sql = "update product_profile_app " +
                "set price = :price " +
                "where product_profile_id = :productProfileId";

        Map<String, Object> data = new HashMap<>();
        data.put("price", productProfileApp.getPrice());
        data.put("productProfileId", productProfileApp.getProductProfileId());
        jdbcTemplateApp.update(sql, new MapSqlParameterSource(data));
        return productProfileApp;
    }


    @Override
    public List<ProductProfileApp> getAll() {
        String sql = "select * from product_profile_app";
        return jdbcTemplateApp.query(sql, new ProductProfileExtractor());
    }


    public List<ProductProfileApp> getAllBySupplierId(int supplierId) {
        String sql = "select * from centur_app.product_profile_app pp " +
                "left join supplier_app sa on sa.supplier_app_id = pp.supplier_id " +
                "left join category_app ca on ca.category_id =  pp.category_id " +
                "left join manufacturer_app ma on ma.manufacturer_id =  pp.manufacturer_id " +
                "where supplier_app_id = :supplierId";
        Map<String, Object> data = new HashMap<>();
        data.put("supplierId", supplierId);
        return jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new ProductProfileExtractor());
    }

    public List<ProductProfileApp> getProductProfileByManufacturerAppId(int manufacturerAppId) {
        String sql = "select * from product_profile_app " +
                "where manufacturer_id = :manufacturerId";
        Map<String, Object> data = new HashMap<>();
        data.put("manufacturerId", manufacturerAppId);
        return jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new ProductProfileRowMapper());
    }

//    public ProductProfileApp getProductProfileByManufacturerAppId(int manufacturerAppId) {
//        String sql = "select * from product_profile_app " +
//                "where sku = :sku ";
//        Map<String, Object> data = new HashMap<>();
//        data.put("manufacturerId", manufacturerAppId);
//        return jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new ProductProfileRowMapper());
//    }


    public ProductProfileApp getProductProfileBySkyJan(String sku, int supplierAppId) {
        String sql = "select * from product_profile_app " +
                "where sku = :sku and supplier_id = :supplierAppId";
        Map<String, Object> data = new HashMap<>();
        data.put("sku", sku);
        data.put("supplierAppId", supplierAppId);
        List<ProductProfileApp> query = jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new ProductProfileRowMapper());
        return query.isEmpty() ? null : query.get(0);
    }


}
