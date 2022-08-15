package orlov.home.centurapp.mapper.opencart;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.opencart.SupplierOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SupplierOpencartRowMapper implements RowMapper<SupplierOpencart> {
    @Override
    public SupplierOpencart mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SupplierOpencart(rs.getString("sup_id"), rs.getString("sup_code"), rs.getString("name"), rs.getString("is_pdv"), rs.getString("currency"), rs.getString("contacts"), rs.getString("sort_order"), null, null,null);
    }
}
