package orlov.home.centurapp.dao.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.app.AttributeApp;
import orlov.home.centurapp.mapper.app.AttributeAppRowMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@AllArgsConstructor
@Slf4j
public class AttributeAppDao implements Dao<AttributeApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;

    @Override
    public int save(AttributeApp attributeApp) {
        String sql = "insert into attribute_app (supplier_id, supplier_title, opencart_title, replacement_from, replacement_to, math_sign, math_number) " +
                "values (:supplierId, :supplierTitle, :opencartTitle, :replacementFrom,:replacementTo, :mathSign, :mathNumber)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(attributeApp), key, new String[]{"attribute_id"});
        int id = key.getKey().intValue();
        return id;
    }

    @Override
    public AttributeApp getById(int id) {
        String sql = "select * from attribute_app where attribute_id = :id";
        List<AttributeApp> attributesApp = jdbcTemplateApp.query(sql, new MapSqlParameterSource("id", id), new AttributeAppRowMapper());
        if (!attributesApp.isEmpty())
            return attributesApp.get(0);
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    public void deleteAll() {
        String sql = "delete from attribute_app";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource());
    }

    @Override
    public AttributeApp update(AttributeApp attributeApp) {
        String sql = "update attribute_app " +
                "set opencart_title = :opencartTitle, " +
                "    replacement_from = :replacementFrom, " +
                "    replacement_to = :replacementTo, " +
                "    math_sign = :mathSign, " +
                "    math_number = :mathNumber " +
                "where attribute_id = :attributeId";

        Map<String, Object> data = new HashMap<>();
        data.put("opencartTitle", attributeApp.getOpencartTitle());
        data.put("replacementFrom", attributeApp.getReplacementFrom());
        data.put("replacementTo", attributeApp.getReplacementTo());
        data.put("mathSign", attributeApp.getMathSign());
        data.put("mathNumber", attributeApp.getMathNumber());
        data.put("attributeId", attributeApp.getAttributeId());

        jdbcTemplateApp.update(sql, new MapSqlParameterSource(data));
        return attributeApp;
    }

    @Override
    public List<AttributeApp> getAll() {
        String sql = "select * from attribute_app";
        return jdbcTemplateApp.query(sql, new AttributeAppRowMapper());
    }

    public List<AttributeApp> getAllByLikeName(String likeName) {
        String sql = "select * from attribute_app where supplier_title like :likeName";
        return jdbcTemplateApp.query(sql, new MapSqlParameterSource("likeName", "%" + likeName + "%"), new AttributeAppRowMapper());
    }

    public List<AttributeApp> getAllBySupplierId(int supplierId) {
        String sql = "select * from attribute_app where supplier_id = :supplierId";
        Map<String, Object> data = new HashMap<>();
        data.put("supplierId", supplierId);
        return jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new AttributeAppRowMapper());
    }

}
