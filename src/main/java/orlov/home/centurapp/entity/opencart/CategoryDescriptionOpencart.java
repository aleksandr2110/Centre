package orlov.home.centurapp.entity.opencart;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"categoryId", "languageId"})
public class CategoryDescriptionOpencart {
    private int categoryId;
    private int languageId = OCConstant.UA_LANGUAGE_ID;
    private String name = OCConstant.EMPTY_STRING;
    private String description = OCConstant.EMPTY_STRING;
    private String metaTitle = OCConstant.EMPTY_STRING;
    private String metaDescription = OCConstant.EMPTY_STRING;
    private String metaKeyword = OCConstant.EMPTY_STRING;
    private String metaH1 = OCConstant.EMPTY_STRING;
    private String shortDescription = OCConstant.EMPTY_STRING;

    public static class Builder {
        private CategoryDescriptionOpencart categoryDescriptionOpencart;

        public Builder(){
            categoryDescriptionOpencart = new CategoryDescriptionOpencart();
        }
        public Builder(CategoryDescriptionOpencart categoryDescriptionOpencart){
            this.categoryDescriptionOpencart = categoryDescriptionOpencart;
        }


        public Builder withCategoryId(int categoryId){
            categoryDescriptionOpencart.categoryId = categoryId;
            return this;
        }
        public Builder withLanguageId(int languageId){
            categoryDescriptionOpencart.languageId = languageId;
            return this;
        }

        public Builder withName(String name){
            categoryDescriptionOpencart.name = name;
            return this;
        }
        public Builder withDescription(String description){
            categoryDescriptionOpencart.description = description;
            return this;
        }
        public Builder withMetaTitle(String metaTitle){
            categoryDescriptionOpencart.metaTitle = metaTitle;
            return this;
        }
        public Builder withMetaDescription(String metaDescription){
            categoryDescriptionOpencart.description = metaDescription;
            return this;
        }
        public Builder withMetaKeyword(String metaKeyword){
            categoryDescriptionOpencart.metaKeyword = metaKeyword;
            return this;
        }
        public Builder withMetaH1(String metaH1){
            categoryDescriptionOpencart.metaH1 = metaH1;
            return this;
        }

        public Builder withShortDescription(String shortDescription){
            categoryDescriptionOpencart.shortDescription = shortDescription;
            return this;
        }


        public CategoryDescriptionOpencart build(){
            return categoryDescriptionOpencart;
        }



    }
}
