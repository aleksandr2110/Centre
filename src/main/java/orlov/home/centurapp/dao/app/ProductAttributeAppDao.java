package orlov.home.centurapp.dao.app;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.app.ProductAttributeApp;
import orlov.home.centurapp.mapper.app.ProductAttributeAppRowMapper;

import java.util.List;


@Repository
@AllArgsConstructor
public class ProductAttributeAppDao implements Dao<ProductAttributeApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;

    @Override
    public int save(ProductAttributeApp productAttributeApp) {
        String sql = "insert into product_attribute_app (product_profile_id, attribute_id, attribute_value)\n" +
                "values (:productProfileAppId,:attributeAppId,:attributeValue)";
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(productAttributeApp));
        return 0;
    }

    @Override
    public ProductAttributeApp getById(int id) {
        return null;
    }

    public ProductAttributeApp getProductAttributeId(int productProfileAppId, int attributeAppId) {
        String sql = "select * from product_attribute_app where product_profile_id = :productProfileAppId and attribute_id = :attributeAppId";
        MapSqlParameterSource data = new MapSqlParameterSource();
        data.addValue("productProfileAppId", productProfileAppId);
        data.addValue("attributeAppId", attributeAppId);
        List<ProductAttributeApp> attributesApp = jdbcTemplateApp.query(sql, data, new ProductAttributeAppRowMapper());
        return attributesApp.isEmpty() ? null : attributesApp.get(0);
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public ProductAttributeApp update(ProductAttributeApp productAttributeApp) {
        return null;
    }

    @Override
    public List<ProductAttributeApp> getAll() {
        return null;
    }
}
