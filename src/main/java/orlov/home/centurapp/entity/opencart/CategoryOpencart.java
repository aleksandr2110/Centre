package orlov.home.centurapp.entity.opencart;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import orlov.home.centurapp.dto.api.goodfood.GoodfoodCategory;
import orlov.home.centurapp.util.OCConstant;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = {"categoryId"})
public class CategoryOpencart {
    private int categoryId;
    private String image = OCConstant.EMPTY_STRING;
    private String name;
    private int parentId = OCConstant.ZERO;
    private CategoryOpencart parentCategory;
    private boolean top = OCConstant.IS_TOP;
    private int column = OCConstant.ZERO;
    private int sortOrder = OCConstant.SORT_ORDER;
    private boolean status = OCConstant.STATUS;
    private Timestamp dateAdded = OCConstant.DATA_AVAILABLE;
    private Timestamp dateModified = OCConstant.DATA_MODIFIED;
    private boolean noindex = OCConstant.NOINDEX;
    private String categoryTelefs = OCConstant.CATEGORY_TElEFS;
    private String categoryMails = OCConstant.CATEGORY_MAILS;
    private String uuid = OCConstant.EMPTY_STRING;
    private List<CategoryDescriptionOpencart> descriptions = new ArrayList<>();
    private List<CategoryOpencart> categoriesOpencart = new ArrayList<>();
    private List<GoodfoodCategory> subGoodfoodCategories = new ArrayList<>();

    private String url;
    public static class Builder {
        private CategoryOpencart categoryOpencart;

        public Builder() {
            categoryOpencart = new CategoryOpencart();
        }

        public Builder(CategoryOpencart categoryOpencart) {
            this.categoryOpencart = categoryOpencart;
        }
        public Builder withCategoryId(int categoryId) {
            categoryOpencart.categoryId = categoryId;
            return this;
        }


        public Builder withImage(String image) {
            categoryOpencart.image = image;
            return this;
        }

        public Builder withParentId(int parentId) {
            categoryOpencart.parentId = parentId;
            return this;
        }

        public Builder withTop(boolean top) {
            categoryOpencart.top = top;
            return this;
        }

        public Builder withColumn(int column) {
            categoryOpencart.column = column;
            return this;
        }

        public Builder withSortOrder(int sortOrder) {
            categoryOpencart.sortOrder = sortOrder;
            return this;
        }

        public Builder withStatus(boolean status) {
            categoryOpencart.status = status;
            return this;
        }

        public Builder withParentCategory(CategoryOpencart parentCategory) {
            categoryOpencart.parentCategory = parentCategory;
            return this;
        }

        public Builder withDateAdded(Timestamp dateAdded) {
            categoryOpencart.dateAdded = dateAdded;
            return this;
        }

        public Builder withDateModified(Timestamp dateModified) {
            categoryOpencart.dateModified = dateModified;
            return this;
        }

        public Builder withNoindex(boolean noindex) {
            categoryOpencart.noindex = noindex;
            return this;
        }

        public Builder withUrl(String url) {
            categoryOpencart.url = url;
            return this;
        }

        public Builder withCategoryTelefs(String categoryTelefs) {
            categoryOpencart.categoryTelefs = categoryTelefs;
            return this;
        }
        public Builder withCategoryMails(String categoryMails) {
            categoryOpencart.categoryMails = categoryMails;
            return this;
        }
        public Builder withUuid(String uuid) {
            categoryOpencart.uuid = uuid;
            return this;
        }



        public CategoryOpencart build() {
            return categoryOpencart;
        }

    }
}
