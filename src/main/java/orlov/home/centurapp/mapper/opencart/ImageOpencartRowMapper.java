package orlov.home.centurapp.mapper.opencart;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.opencart.ImageOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ImageOpencartRowMapper implements RowMapper<ImageOpencart> {
    @Override
    public ImageOpencart mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ImageOpencart(rs.getInt("product_image_id"), rs.getInt("product_id"), rs.getString("image"), rs.getInt("sort_order"), rs.getString("uuid"));
    }
}
