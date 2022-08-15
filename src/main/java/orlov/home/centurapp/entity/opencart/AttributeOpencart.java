package orlov.home.centurapp.entity.opencart;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import orlov.home.centurapp.util.OCConstant;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = {"attributeId", "attributeGroupId", "sortOrder"})
@ToString
public class AttributeOpencart {
    private int attributeId;
    private int attributeGroupId;
    private int sortOrder = OCConstant.SORT_ORDER;
    private String uuid = OCConstant.EMPTY_STRING;
    private List<AttributeDescriptionOpencart> descriptions = new ArrayList<>();

    public static class Builder{
        private AttributeOpencart attributeOpencart;

        public Builder(){
            attributeOpencart = new AttributeOpencart();
        }

        public Builder(AttributeOpencart attributeOpencart){
            this.attributeOpencart = attributeOpencart;
        }

        public Builder withAttributeId(int attributeId){
            attributeOpencart.attributeId = attributeId;
            return this;
        }

        public Builder withAttributeGroupId(int attributeGroupId){
            attributeOpencart.attributeGroupId = attributeGroupId;
            return this;
        }

        public Builder withSortOrder(int sortOrder){
            attributeOpencart.sortOrder = sortOrder;
            return this;
        }

        public Builder withUuid(String uuid){
            attributeOpencart.uuid = uuid;
            return this;
        }

        public AttributeOpencart build(){
            return attributeOpencart;
        }
    }
}
