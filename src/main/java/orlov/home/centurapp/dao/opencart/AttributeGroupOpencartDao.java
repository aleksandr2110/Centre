package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.AttributeGroupOpencart;
import orlov.home.centurapp.mapper.opencart.AttributeGroupOpencartExtractor;
import orlov.home.centurapp.util.OCConstant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@AllArgsConstructor
@Slf4j
public class AttributeGroupOpencartDao implements Dao<AttributeGroupOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(AttributeGroupOpencart attributeGroupOpencart) {
        String sql = "insert into oc_attribute_group (sort_order, uuid)" +
                "values (:sortOrder, :uuid)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(attributeGroupOpencart), keyHolder);
        int id = keyHolder.getKey().intValue();
        return id;
    }

    public AttributeGroupOpencart getDefaultGlobalAttributeGroupByName(String name) {
        String sql = "select * from oc_attribute_group as attgr " +
                "join oc_attribute_group_description as attgrdesc on attgrdesc.attribute_group_id = attgr.attribute_group_id " +
                "where attgrdesc.name = :name and language_id = :languageUAId";
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("languageUAId", OCConstant.UA_LANGUAGE_ID);
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new AttributeGroupOpencartExtractor());

    }

    @Override
    public AttributeGroupOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public AttributeGroupOpencart update(AttributeGroupOpencart attributeGroupOpencart) {
        return null;
    }

    @Override
    public List<AttributeGroupOpencart> getAll() {
        return null;
    }
}
