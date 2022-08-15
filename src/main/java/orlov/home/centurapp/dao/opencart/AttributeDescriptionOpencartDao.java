package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.AttributeDescriptionOpencart;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class AttributeDescriptionOpencartDao implements Dao<AttributeDescriptionOpencart> {

   private final NamedParameterJdbcTemplate jdbcTemplateOpencart;


    @Override
    public int save(AttributeDescriptionOpencart attributeDescriptionOpencart) {
        String sql = "insert into oc_attribute_description (attribute_id, language_id, name)" +
                "values (:attributeId, :languageId, :name)";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(attributeDescriptionOpencart));
        return 0;
    }

    @Override
    public AttributeDescriptionOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public AttributeDescriptionOpencart update(AttributeDescriptionOpencart attributeDescriptionOpencart) {
        return null;
    }

    @Override
    public List<AttributeDescriptionOpencart> getAll() {
        return null;
    }
}
