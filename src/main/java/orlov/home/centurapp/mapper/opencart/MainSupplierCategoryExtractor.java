package orlov.home.centurapp.mapper.opencart;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.opencart.CategoryDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MainSupplierCategoryExtractor implements ResultSetExtractor<CategoryOpencart> {

    @Override
    public CategoryOpencart extractData(ResultSet rs) throws SQLException, DataAccessException {
        CategoryOpencart categoryOpencart = null;
        while (rs.next()) {
            categoryOpencart = new CategoryOpencart.Builder()
                    .withCategoryId(rs.getInt("category_id"))
                    .withImage(rs.getString("image"))
                    .withParentId(rs.getInt("parent_id"))
                    .withTop(rs.getBoolean("top"))
                    .withColumn(rs.getInt("column"))
                    .withSortOrder(rs.getInt("sort_order"))
                    .withStatus(rs.getBoolean("status"))
                    .withDateAdded(rs.getTimestamp("date_added"))
                    .withDateModified(rs.getTimestamp("date_modified"))
                    .withNoindex(rs.getBoolean("noindex"))
                    .withCategoryTelefs(rs.getString("category_telefs"))
                    .withCategoryMails(rs.getString("category_mails"))
                    .withUuid(rs.getString("uuid"))
                    .build();

            CategoryDescriptionOpencart description = new CategoryDescriptionOpencart.Builder()
                    .withCategoryId(rs.getInt("category_id"))
                    .withLanguageId(rs.getInt("language_id"))
                    .withName(rs.getString("name"))
                    .withDescription(rs.getString("description"))
                    .withMetaTitle(rs.getString("meta_title"))
                    .withMetaDescription(rs.getString("meta_description"))
                    .withMetaKeyword(rs.getString("meta_keyword"))
                    .withMetaH1(rs.getString("meta_h1"))
                    .withShortDescription(rs.getString("short_description"))
                    .build();
            categoryOpencart.getDescriptions().add(description);
        }
        return categoryOpencart;
    }
}
