package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.AttributeGroupDescriptionOpencart;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class AttributeGroupDescriptionOpencartDao implements Dao<AttributeGroupDescriptionOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(AttributeGroupDescriptionOpencart attributeGroupDescriptionOpencart) {
        String sql = "insert into oc_attribute_group_description (attribute_group_id, language_id, name)" +
                "values (:attributeGroupId, :languageId, :name)";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(attributeGroupDescriptionOpencart));
        return 0;
    }

    @Override
    public AttributeGroupDescriptionOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public AttributeGroupDescriptionOpencart update(AttributeGroupDescriptionOpencart attributeGroupDescriptionOpencart) {
        return null;
    }

    @Override
    public List<AttributeGroupDescriptionOpencart> getAll() {
        return null;
    }
}
