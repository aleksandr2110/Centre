package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.ImageOpencart;
import orlov.home.centurapp.mapper.opencart.ImageOpencartRowMapper;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class ImageOpencartDao implements Dao<ImageOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(ImageOpencart imageOpencart) {
        String sql = "insert into oc_product_image (product_id, image, sort_order, uuid)" +
                "values (:productId, :image, :sortOrder, :uuid)";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(imageOpencart));
        return 0;
    }

    @Override
    public ImageOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {
        String sql = "delete from oc_product_image where product_image_id = :productImageId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productImageId", id));
    }

    public ImageOpencart getByImage(String image){
        String sql = "select * from oc_product_image where image = :image";
        List<ImageOpencart> query = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("image", image), new ImageOpencartRowMapper());
        return query.isEmpty() ? null : query.get(0);
    }

    public void deleteByProductId(int productId) {
        String sql = "delete from oc_product_image where product_id = :productId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productId", productId));
    }



    public void deleteByName(String imageName) {
        String sql = "delete from oc_product_image where image = :image";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("image", imageName));
    }

    public List<ImageOpencart> getImageByProductId(int productId) {
        String sql = "select * from oc_product_image where product_id = :productId";
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("productId", productId), new ImageOpencartRowMapper());
    }

    @Override
    public ImageOpencart update(ImageOpencart imageOpencart) {
        String sql = "update oc_product_image set image = :image where product_id = :productId and product_image_id = :productImageId";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(imageOpencart));
        return imageOpencart;
    }

    @Override
    public List<ImageOpencart> getAll() {
        return null;
    }
}
