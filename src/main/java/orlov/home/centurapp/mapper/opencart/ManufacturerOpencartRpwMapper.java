package orlov.home.centurapp.mapper.opencart;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.opencart.ManufacturerOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ManufacturerOpencartRpwMapper implements RowMapper<ManufacturerOpencart> {
    @Override
    public ManufacturerOpencart mapRow(ResultSet rs, int rowNum) throws SQLException {
        ManufacturerOpencart manufacturerOpencart = new ManufacturerOpencart.Builder()
                .withManufacturerId(rs.getInt("manufacturer_id"))
                .withName(rs.getString("name"))
                .withImage(rs.getString("image"))
                .withSortOrder(rs.getInt("sort_order"))
                .withNoindex(rs.getBoolean("noindex"))
                .withUuid(rs.getString("uuid"))
                .build();

        return manufacturerOpencart;
    }
}
