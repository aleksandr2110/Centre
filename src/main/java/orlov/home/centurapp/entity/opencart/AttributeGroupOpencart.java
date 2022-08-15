package orlov.home.centurapp.entity.opencart;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import orlov.home.centurapp.util.OCConstant;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class AttributeGroupOpencart {
    private int attributeGroupId;
    private int sortOrder = OCConstant.SORT_ORDER;
    private String uuid = OCConstant.EMPTY_STRING;
    private List<AttributeGroupDescriptionOpencart>  descriptions = new ArrayList<>();

    public static class Builder {
        private AttributeGroupOpencart attributeGroupOpencart;

        public Builder(){
            attributeGroupOpencart = new AttributeGroupOpencart();
        }

        public Builder(AttributeGroupOpencart attributeGroupOpencart){
            this.attributeGroupOpencart = attributeGroupOpencart;
        }

        public Builder withAttributeGroupId(int attributeGroupId){
            attributeGroupOpencart.attributeGroupId = attributeGroupId;
            return this;
        }

        public Builder withSortOrder(int sortOrder){
            attributeGroupOpencart.sortOrder = sortOrder;
            return this;
        }
        public Builder withUuid(String uuid){
            attributeGroupOpencart.uuid = uuid;
            return this;
        }


        public AttributeGroupOpencart build(){
            return attributeGroupOpencart;
        }

    }

}
