package orlov.home.centurapp.entity.opencart;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
@ToString
public class AttributeGroupDescriptionOpencart {
    private int attributeGroupId;
    private int languageId = OCConstant.UA_LANGUAGE_ID;
    private String name = OCConstant.EMPTY_STRING;

    public static class Builder {
         private AttributeGroupDescriptionOpencart attributeGroupDescriptionOpencart;

         public Builder(){
             attributeGroupDescriptionOpencart = new AttributeGroupDescriptionOpencart();
         }

        public Builder(AttributeGroupDescriptionOpencart attributeGroupDescriptionOpencart){
            this.attributeGroupDescriptionOpencart = attributeGroupDescriptionOpencart;
        }

        public Builder withAttributeGroupId(int attributeGroupId){
             attributeGroupDescriptionOpencart.attributeGroupId = attributeGroupId;
             return this;
        }

        public Builder withLanguageId(int languageId){
            attributeGroupDescriptionOpencart.languageId = languageId;
            return this;
        }

        public Builder withName(String name){
            attributeGroupDescriptionOpencart.name = name;
            return this;
        }

        public AttributeGroupDescriptionOpencart build(){
             return attributeGroupDescriptionOpencart;
        }


    }

}
