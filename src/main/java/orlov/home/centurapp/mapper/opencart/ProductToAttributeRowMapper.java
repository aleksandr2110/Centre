package orlov.home.centurapp.mapper.opencart;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.dto.ProductToAttributeDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductToAttributeRowMapper implements RowMapper<ProductToAttributeDto> {

    @Override
    public ProductToAttributeDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        ProductToAttributeDto dto = new ProductToAttributeDto(rs.getInt("product_id"), null, rs.getInt("attribute_id"), 0, rs.getString("text"));
        return dto;
    }

}
