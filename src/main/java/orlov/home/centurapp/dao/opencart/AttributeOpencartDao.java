package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.ProductToAttributeDto;
import orlov.home.centurapp.entity.opencart.AttributeOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;
import orlov.home.centurapp.mapper.opencart.AttributeOpencartExtractor;
import orlov.home.centurapp.mapper.opencart.AttributeWrapperExtractor;
import orlov.home.centurapp.mapper.opencart.ProductToAttributeRowMapper;
import orlov.home.centurapp.util.OCConstant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@AllArgsConstructor
@Slf4j
public class AttributeOpencartDao implements Dao<AttributeOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(AttributeOpencart attributeOpencart) {
        String sql = "insert into oc_attribute (attribute_group_id, sort_order, uuid)" +
                "values (:attributeGroupId, :sortOrder, :uuid)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(attributeOpencart), keyHolder);
        int id = keyHolder.getKey().intValue();
        return id;
    }

    public List<AttributeWrapper> getAttributesByProduct(ProductOpencart product) {
        String sql = "select * from oc_product_attribute pa " +
                "left join oc_attribute a on pa.attribute_id = a.attribute_id " +
                "left join oc_attribute_description ad on a.attribute_id = ad.attribute_id " +
                "where pa.product_id = :productId and ad.language_id = :languageId";
        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getId());
        data.put("languageId", OCConstant.UA_LANGUAGE_ID);
        List<AttributeWrapper> attributes = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new AttributeWrapperExtractor());
        return attributes;
    }


    public ProductToAttributeDto getProductToAttributeById(int productId, int attributeId) {
        String sql = "select * from oc_product_attribute " +
                "where product_id = :productId and attribute_id = :attributeId and language_id = :languageId";
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("attributeId", attributeId);
        data.put("languageId", OCConstant.UA_LANGUAGE_ID);
        List<ProductToAttributeDto> productToAttributeDtoList = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new ProductToAttributeRowMapper());
        if (!productToAttributeDtoList.isEmpty())
            return productToAttributeDtoList.get(0);
        return null;
    }

    public void batchUpdateAttribute(List<ProductToAttributeDto> attributesDto) {
        String sql = "update oc_product_attribute " +
                "set text = :text " +
                "where product_id = :productId and attribute_id = :attributeId";
        SqlParameterSource[] sources = SqlParameterSourceUtils.createBatch(attributesDto.toArray());
        jdbcTemplateOpencart.batchUpdate(sql, sources);
    }

    @Override
    public AttributeOpencart getById(int id) {
        return null;
    }

    public AttributeOpencart getByName(String name) {
        String sql = "select * from oc_attribute as attr " +
                "         left join oc_attribute_description as attdesc on attdesc.attribute_id = attr.attribute_id " +
                "where attdesc.language_id = :languageId " +
                "and attdesc.name = :name";
        Map<String, Object> data = new HashMap<>();
        data.put("languageId", OCConstant.UA_LANGUAGE_ID);
        data.put("name", name);
        List<AttributeOpencart> attributeOpencartList = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new AttributeOpencartExtractor());
        return attributeOpencartList
                .stream()
                .filter(a -> name.equals(a.getDescriptions().get(0).getName()))
                .findFirst()
                .orElse(null);

    }

    @Override
    public void deleteById(int id) {

    }


    @Override
    public AttributeOpencart update(AttributeOpencart attributeOpencart) {
        return null;
    }

    @Override
    public List<AttributeOpencart> getAll() {
        return null;
    }

    public List<AttributeOpencart> getAllWithDesc() {
        String sql = "select * from oc_attribute as attr " +
                "         left join oc_attribute_description as attdesc on attdesc.attribute_id = attr.attribute_id " +
                "where attdesc.language_id = :languageId ";
        Map<String, Object> data = new HashMap<>();
        data.put("languageId", OCConstant.UA_LANGUAGE_ID);
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new AttributeOpencartExtractor());
    }

    public List<AttributeOpencart> getAllBySearchWithDesc(String likeName) {
        String sql = "select * from oc_attribute as attr " +
                "         left join oc_attribute_description as attdesc on attdesc.attribute_id = attr.attribute_id " +
                "where attdesc.language_id = :languageId  and attdesc.name like :likeName";
        Map<String, Object> data = new HashMap<>();
        data.put("languageId", OCConstant.UA_LANGUAGE_ID);
        data.put("likeName", "%" + likeName + "%");
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new AttributeOpencartExtractor());
    }
}
