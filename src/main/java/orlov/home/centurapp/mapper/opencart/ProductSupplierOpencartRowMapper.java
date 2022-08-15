package orlov.home.centurapp.mapper.opencart;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.opencart.ProductSupplierOpencart;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductSupplierOpencartRowMapper implements RowMapper<ProductSupplierOpencart> {
    @Override
    public ProductSupplierOpencart mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ProductSupplierOpencart(rs.getInt("product_id"), rs.getString("sup_code"), rs.getBigDecimal("price"), rs.getString("is_pdv"), rs.getString("currency"), rs.getString("availability"), "");
    }
}
