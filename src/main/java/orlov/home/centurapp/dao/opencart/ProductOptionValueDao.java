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
import orlov.home.centurapp.entity.opencart.ProductOptionValueOpencart;

import java.util.List;

@Slf4j
@Repository
@AllArgsConstructor
public class ProductOptionValueDao implements Dao<ProductOptionValueOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(ProductOptionValueOpencart productOptionValueOpencart) {
        String sql = "insert into oc_product_option_value (product_option_id, product_id, option_id, option_value_id, quantity, subtract,\n" +
                "                                     price, price_prefix, points, points_prefix, weight, weight_prefix, optsku)\n" +
                "values (:productOptionId, :productId, :optionId, :optionValueId, :quantity, :subtract,\n" +
                "        :price, :pricePrefix, :points, :pointsPrefix, :weight, :weightPrefix, :optsku);";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(productOptionValueOpencart), keyHolder);
        int id = keyHolder.getKey().intValue();
        return id;
    }

    @Override
    public ProductOptionValueOpencart getById(int id) {
        return null;
    }

    public void deleteProductOptionValueById(int productId) {
        String sql = "delete from oc_product_option_value where product_id = :productId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productId", productId));
    }

    public void deleteProductOptionValue(int productId, int optionId, int optionValueId) {
        String sql = "delete from oc_product_option_value where product_id = :productId and option_id = :optionId and option_value_id = :optionValueId";

        MapSqlParameterSource data = new MapSqlParameterSource();
        data.addValue("productId", productId);
        data.addValue("optionId", optionId);
        data.addValue("optionValueId", optionValueId);

        jdbcTemplateOpencart.update(sql, data);
    }


    @Override
    public void deleteById(int id) {

    }


    @Override
    public ProductOptionValueOpencart update(ProductOptionValueOpencart productOptionValueOpencart) {
        String sql = "update oc_product_option_value\n" +
                "set  quantity = :quantity,\n" +
                "     subtract = :subtract,\n" +
                "     price = :price,\n" +
                "     price_prefix = :pricePrefix,\n" +
                "     points = :points,\n" +
                "     points_prefix = :pointsPrefix,\n" +
                "     weight = :weight,\n" +
                "     weight_prefix = :weightPrefix,\n" +
                "     reward = :reward,\n" +
                "     reward_prefix = :rewardPrefix,\n" +
                "     optsku = :optsku\n" +
                "where product_option_value_id = :productOptionValueId;";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(productOptionValueOpencart));
        return productOptionValueOpencart;
    }

    @Override
    public List<ProductOptionValueOpencart> getAll() {
        return null;
    }
}
