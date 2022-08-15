package orlov.home.centurapp.mapper.opencart;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import orlov.home.centurapp.dto.ProductToAttributeDto;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductToAttributeExtractor implements ResultSetExtractor<List<ProductToAttributeDto>> {
    @Override
    public List<ProductToAttributeDto> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<ProductToAttributeDto> products = new ArrayList<>();
        while (rs.next()) {
            ProductToAttributeDto to = new ProductToAttributeDto(rs.getInt("product_id"), rs.getString("sku"), rs.getInt("attribute_id"), 0, rs.getString("text"));
            if (!products.contains(to))
                products.add(to);
        }

        return products;
    }
}
