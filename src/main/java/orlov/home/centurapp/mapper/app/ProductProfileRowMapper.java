package orlov.home.centurapp.mapper.app;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.app.ProductProfileApp;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductProfileRowMapper implements RowMapper<ProductProfileApp> {
    @Override
    public ProductProfileApp mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ProductProfileApp.Builder()
                .withProductProfileId(rs.getInt("product_profile_id"))
                .withSupplierId(rs.getInt("supplier_id"))
                .withUrl(rs.getString("url"))
                .withSku(rs.getString("sku"))
                .withTitle(rs.getString("title"))
                .withManufacturerId(rs.getInt("manufacturer_id"))
                .withCategoryId(rs.getInt("category_id"))
                .build();
    }
}
