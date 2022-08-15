package orlov.home.centurapp.mapper.app;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.app.ProductApp;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductAppRowMapper implements RowMapper<ProductApp> {
    @Override
    public ProductApp mapRow(ResultSet rs, int rowNum) throws SQLException {
        ProductApp productApp = new ProductApp.Builder()
                .withProductAppId(rs.getInt("product_app_id"))
                .withOrderProcessId(rs.getInt("order_process_id"))
                .withName(rs.getString("name"))
                .withUrl(rs.getString("url"))
                .withStatus(rs.getString("status"))
                .withOldPrice(rs.getBigDecimal("old_price"))
                .withNewPrice(rs.getBigDecimal("new_price"))
                .build();
        return productApp;
    }
}
