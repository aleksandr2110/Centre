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
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;
import orlov.home.centurapp.mapper.opencart.CategoryOpencartExtractor;
import orlov.home.centurapp.mapper.opencart.MainSupplierCategoryExtractor;
import orlov.home.centurapp.util.OCConstant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Repository
@AllArgsConstructor
@Slf4j
public class CategoryOpencartDao implements Dao<CategoryOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(CategoryOpencart categoryOpencart) {
        String sql = "insert into oc_category (image, parent_id, top, `column`, sort_order, status, date_added, date_modified, noindex, category_telefs, category_mails, uuid)" +
                "values (:image, :parentId, :top, :column, :sortOrder, :status, :dateAdded, :dateModified, :noindex , :categoryTelefs, :categoryMails, :uuid)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(categoryOpencart), keyHolder);
        int id = keyHolder.getKey().intValue();
        return id;
    }

    public void saveCategoryToPath(CategoryOpencart categoryOpencart) {
        String sql = "insert into oc_category_to_store (category_id, store_id)" +
                "values (:categoryId, :storeId)";
        Map<String, Object> data = new HashMap<>();
        data.put("categoryId", categoryOpencart.getCategoryId());
        data.put("storeId", OCConstant.STORE_ID);
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource(data));
    }

    public List<CategoryOpencart> getAllSupplierCategoryOpencart(SupplierApp supplierApp) {
        String sql = "select * from oc_category " +
                "join oc_category_description ocd on oc_category.category_id = ocd.category_id " +
                "where ocd.description = :description";
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("description", supplierApp.getName()), new CategoryOpencartExtractor());
    }

    public CategoryOpencart getMainSupplierCategoryOpencart(SupplierApp supplierApp) {
        String sql = "select * from oc_category " +
                "left join oc_category_description ocd on oc_category.category_id = ocd.category_id " +
                "where ocd.name = :name";
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("name", supplierApp.getName()), new MainSupplierCategoryExtractor());
    }

    public CategoryOpencart getCategoryByNameAndDescription(String categoryName, String categoryDescription) {
        String sql = "select * from oc_category " +
                "         left join oc_category_description ocd on oc_category.category_id = ocd.category_id " +
                "where ocd.name = :name and ocd.description = :description and ocd.language_id = :languageId";
        Map<String, Object> data = new HashMap<>();
        data.put("name", categoryName);
        data.put("description", categoryDescription);
        data.put("languageId", OCConstant.UA_LANGUAGE_ID);
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new MainSupplierCategoryExtractor());
    }


    @Override
    public CategoryOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public CategoryOpencart update(CategoryOpencart categoryOpencart) {
        return null;
    }

    @Override
    public List<CategoryOpencart> getAll() {
        return null;
    }
}
