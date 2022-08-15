package orlov.home.centurapp.mapper.app;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.app.ProductAttributeApp;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductAttributeAppRowMapper implements RowMapper<ProductAttributeApp> {
    @Override
    public ProductAttributeApp mapRow(ResultSet resultSet, int i) throws SQLException {
        int productProfileId = resultSet.getInt("product_profile_id");
        int attributeId = resultSet.getInt("attribute_id");
        String attributeValue = resultSet.getString("attribute_value");
        return new ProductAttributeApp(productProfileId, attributeId, attributeValue);
    }
}
