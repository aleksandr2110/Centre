package orlov.home.centurapp.mapper.opencart;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import orlov.home.centurapp.entity.opencart.AttributeGroupDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.AttributeGroupOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class AttributeGroupOpencartExtractor implements ResultSetExtractor<AttributeGroupOpencart> {

    @Override
    public AttributeGroupOpencart extractData(ResultSet rs) throws SQLException, DataAccessException {
        AttributeGroupOpencart attributeGroupOpencart = null;
        while (rs.next()) {
            if (Objects.isNull(attributeGroupOpencart)) {
                attributeGroupOpencart = new AttributeGroupOpencart.Builder()
                        .withAttributeGroupId(rs.getInt("attribute_group_id"))
                        .withSortOrder(rs.getInt("sort_order"))
                        .withUuid(rs.getString("uuid"))
                        .build();
            }

            AttributeGroupDescriptionOpencart description = new AttributeGroupDescriptionOpencart.Builder()
                    .withAttributeGroupId(rs.getInt("attribute_group_id"))
                    .withName(rs.getString("name"))
                    .withLanguageId(rs.getInt("language_id"))
                    .build();
            attributeGroupOpencart.getDescriptions().add(description);

        }

        return attributeGroupOpencart;
    }

}
