package orlov.home.centurapp.mapper.app;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.app.ManufacturerApp;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ManufacturerAppRowMapper implements RowMapper<ManufacturerApp> {

    @Override
    public ManufacturerApp mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ManufacturerApp.Builder()
                .withManufacturerId(rs.getInt("manufacturer_id"))
                .withSupplierId(rs.getInt("supplier_id"))
                .withSupplierTitle(rs.getString("supplier_title"))
                .withOpencartTitle(rs.getString("opencart_title"))
                .withMarkup(rs.getInt("markup"))
                .build();
    }
}
