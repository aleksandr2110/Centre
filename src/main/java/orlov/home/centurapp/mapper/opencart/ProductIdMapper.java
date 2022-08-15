package orlov.home.centurapp.mapper.opencart;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductIdMapper implements RowMapper<Integer> {
    @Override
    public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
        int productId = rs.getInt("p.product_id");
        return productId;
    }
}
