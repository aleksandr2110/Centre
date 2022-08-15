package orlov.home.centurapp.mapper.app;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.app.SupplierApp;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SupplierAppRowMapper implements RowMapper<SupplierApp> {

    @Override
    public SupplierApp mapRow(ResultSet rs, int rowNum) throws SQLException {
        SupplierApp supplierApp = new SupplierApp();
        supplierApp.setSupplierAppId(rs.getInt("supplier_app_id"));
        supplierApp.setName(rs.getString("name"));
        supplierApp.setUrl(rs.getString("url"));
        supplierApp.setDisplayName(rs.getString("display_name"));
        supplierApp.setMarkup(rs.getInt("markup"));
        return supplierApp;
    }
}
