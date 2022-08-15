package orlov.home.centurapp.mapper.opencart;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.entity.opencart.AttributeDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.AttributeOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AttributeWrapperExtractor implements ResultSetExtractor<List<AttributeWrapper>> {
    @Override
    public List<AttributeWrapper> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<AttributeWrapper> attributes = new ArrayList<>();

        while (rs.next()) {
            AttributeOpencart attribute = new AttributeOpencart.Builder()
                    .withAttributeId(rs.getInt("attribute_id"))
                    .withAttributeGroupId(rs.getInt("attribute_group_id"))
                    .withSortOrder(rs.getInt("sort_order"))
                    .withUuid(rs.getString("uuid"))
                    .build();

            AttributeDescriptionOpencart description = new AttributeDescriptionOpencart.Builder()
                    .withAttributeId(rs.getInt("attribute_id"))
                    .withLanguageId(rs.getInt("language_id"))
                    .withName(rs.getString("name"))
                    .build();

            AttributeWrapper attributeWrapper = new AttributeWrapper(description.getName(), rs.getString("text"),attribute);
            attributes.add(attributeWrapper);


        }

        return attributes;
    }
}
