package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.CategoryDescriptionOpencart;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class CategoryDescriptionOpencartDao implements Dao<CategoryDescriptionOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(CategoryDescriptionOpencart categoryDescriptionOpencart) {
        String sql = "insert into oc_category_description (category_id, language_id, name, description, meta_title, meta_description, meta_keyword, meta_h1, short_description)" +
                "values (:categoryId, :languageId, :name, :description, :metaTitle, :metaDescription, :metaKeyword, :metaH1, :shortDescription)";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(categoryDescriptionOpencart));
        return 0;
    }


    @Override
    public CategoryDescriptionOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public CategoryDescriptionOpencart update(CategoryDescriptionOpencart categoryDescriptionOpencart) {
        return null;
    }

    @Override
    public List<CategoryDescriptionOpencart> getAll() {
        return null;
    }
}
