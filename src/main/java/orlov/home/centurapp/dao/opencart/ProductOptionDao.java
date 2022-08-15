package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.ProductOptionOpencart;
import orlov.home.centurapp.mapper.opencart.ProductOptionOpencartExtractor;

import java.util.List;

@Repository
@Slf4j
@AllArgsConstructor
public class ProductOptionDao implements Dao<ProductOptionOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(ProductOptionOpencart productOption) {
        String sql = "insert into oc_product_option (product_id, option_id, value, required)\n" +
                "values (:productId, :optionId, :value, :required);";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(productOption), keyHolder);
        int id = keyHolder.getKey().intValue();
        return id;
    }

    @Override
    public ProductOptionOpencart getById(int id) {
        return null;
    }

    public List<ProductOptionOpencart> getProductOptionsById(int productId) {
        String sql = "select *\n" +
                "from oc_product_option o\n" +
                "left join oc_product_option_value ov on ov.product_option_id = o.product_option_id\n" +
                "where o.product_id = :productId";
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("productId", productId), new ProductOptionOpencartExtractor());
    }


    public void deleteProductOptionById(int productId) {
        String sql = "delete from oc_product_option where product_id = :productId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productId", productId));
    }



    @Override
    public void deleteById(int id) {

    }

    @Override
    public ProductOptionOpencart update(ProductOptionOpencart productOption) {
        return null;
    }

    @Override
    public List<ProductOptionOpencart> getAll() {
        return null;
    }
}
