package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;

import java.util.List;

@AllArgsConstructor
@Repository
@Slf4j
public class ProductDescriptionOpencartDao implements Dao<ProductDescriptionOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(ProductDescriptionOpencart productDescriptionOpencart) {
        String sql = "insert into oc_product_description (product_id, language_id, name, description, tag, meta_title, meta_description, meta_keyword, meta_h1, description1)" +
                "values (:productId, :languageId, :name, :description, :tag, :metaTitle, :metaDescription, :metaKeyword, :metaH1, :description1)";
        try {
            jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(productDescriptionOpencart));
        } catch (Exception e) {
            log.error("Error during saving description :", e);  // FIXME: 05.01.2023 Save exception
        }
        return 0;
    }


    @Override
    public ProductDescriptionOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public ProductDescriptionOpencart update(ProductDescriptionOpencart desc) {

        return null;
    }


    public ProductDescriptionOpencart updateDescription(ProductDescriptionOpencart productDescriptionOpencart) {
        String sql = "update oc_product_description " +
                "set description = :description, name = :name " +
                "where product_id = :productId and language_id = :languageId";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(productDescriptionOpencart));
        return productDescriptionOpencart;
    }

    public void updateBatch(List<ProductDescriptionOpencart> descriptions) {
        String SQL = "update oc_product_description " +
                "set name             = :name, " +
                "    description      = :description " +
                "where language_id = :languageId " +
                "  and product_id = :productId;";
        SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(descriptions.toArray());

        int[] updateCounts = jdbcTemplateOpencart.batchUpdate(SQL, batch);
        log.info("Records updated! Update counts: {}", updateCounts);
    }

    @Override
    public List<ProductDescriptionOpencart> getAll() {
        return null;
    }
}
