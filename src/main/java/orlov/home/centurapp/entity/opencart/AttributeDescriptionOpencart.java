package orlov.home.centurapp.entity.opencart;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
@ToString
public class AttributeDescriptionOpencart {
    private int attributeId;
    private int languageId = OCConstant.UA_LANGUAGE_ID;
    private String name = OCConstant.EMPTY_STRING;

    public static class Builder{
        private AttributeDescriptionOpencart attributeDescriptionOpencart;

        public Builder(){
            attributeDescriptionOpencart = new AttributeDescriptionOpencart();
        }

        public Builder(AttributeDescriptionOpencart attributeDescriptionOpencart){
            this.attributeDescriptionOpencart = attributeDescriptionOpencart;
        }

        public Builder withAttributeId(int attributeId){
            attributeDescriptionOpencart.attributeId = attributeId;
            return this;
        }

        public Builder withLanguageId(int languageId){
            attributeDescriptionOpencart.languageId = languageId;
            return this;
        }

        public Builder withName(String name){
            attributeDescriptionOpencart.name = name;
            return this;
        }

        public AttributeDescriptionOpencart build(){
            return attributeDescriptionOpencart;
        }



    }
}
