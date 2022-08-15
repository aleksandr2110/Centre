package orlov.home.centurapp.mapper.opencart;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import orlov.home.centurapp.dao.opencart.AttributeOpencartDao;
import orlov.home.centurapp.entity.opencart.AttributeDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.AttributeOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AttributeOpencartExtractor implements ResultSetExtractor<List<AttributeOpencart>> {


    @Override
    public List<AttributeOpencart> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<AttributeOpencart> attributes = new ArrayList<>();

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

            boolean contains = attributes.contains(attribute);
            if (contains){
                AttributeOpencart attributeOpencart = attributes.get(attributes.indexOf(attribute));
                attributeOpencart.getDescriptions().add(description);
            } else {
                attribute.getDescriptions().add(description);
                attributes.add(attribute);
            }




        }


        return attributes;
    }
}
