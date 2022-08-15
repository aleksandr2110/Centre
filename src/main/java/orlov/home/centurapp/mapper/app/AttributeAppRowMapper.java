package orlov.home.centurapp.mapper.app;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.app.AttributeApp;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AttributeAppRowMapper implements RowMapper<AttributeApp> {
    @Override
    public AttributeApp mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AttributeApp.Builder()
                .withAttributeId(rs.getInt("attribute_id"))
                .withSupplierId(rs.getInt("supplier_id"))
                .withSupplierTitle(rs.getString("supplier_title"))
                .withOpencartTitle(rs.getString("opencart_title"))
                .withReplacementFrom(rs.getString("replacement_from"))
                .withReplacementTo(rs.getString("replacement_to"))
                .withMathSign(rs.getString("math_sign"))
                .withMathNumber(rs.getInt("math_number"))
                .build();
    }
}
