package orlov.home.centurapp.entity.app;

import lombok.*;
import orlov.home.centurapp.util.AppConstant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"categoryId", "supplierId"})
public class CategoryApp {
    private int categoryId;
    private int supplierId;
    private String supplierTitle;
    private String opencartTitle = AppConstant.EMPTY_STRING;
    private int markup;

    public static class Builder {
        private CategoryApp categoryApp = null;

        public Builder() {
            categoryApp = new CategoryApp();
        }

        public Builder withCategoryId(int categoryId) {
            categoryApp.categoryId = categoryId;
            return this;
        }

        public Builder withSupplierId(int supplierId) {
            categoryApp.supplierId = supplierId;
            return this;
        }

        public Builder withSupplierTitle(String supplierTitle) {
            categoryApp.supplierTitle = supplierTitle;
            return this;
        }

        public Builder withOpencartTitle(String opencartTitle) {
            categoryApp.opencartTitle = opencartTitle;
            return this;
        }

        public Builder withMarkup(int markup) {
            categoryApp.markup = markup;
            return this;
        }

        public CategoryApp build() {
            return categoryApp;
        }


    }

}
