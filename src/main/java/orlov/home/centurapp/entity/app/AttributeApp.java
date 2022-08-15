package orlov.home.centurapp.entity.app;

import lombok.*;
import orlov.home.centurapp.util.AppConstant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"supplierId", "supplierTitle"})
public class AttributeApp {
    private int attributeId;
    private int supplierId;
    private String supplierTitle = AppConstant.EMPTY_STRING;
    private String opencartTitle = AppConstant.EMPTY_STRING;
    private String oldOpencartTitle = AppConstant.EMPTY_STRING;
    private String replacementFrom = AppConstant.EMPTY_STRING;
    private String replacementTo = AppConstant.EMPTY_STRING;
    private String mathSign = AppConstant.EMPTY_STRING;
    private int mathNumber = AppConstant.ZERO;

    public static class Builder {
        private AttributeApp attribute = null;

        public Builder() {
            attribute = new AttributeApp();
        }

        public Builder withAttributeId(int attributeId) {
            attribute.attributeId = attributeId;
            return this;
        }

        public Builder withSupplierId(int supplierId) {
            attribute.supplierId = supplierId;
            return this;
        }

        public Builder withSupplierTitle(String supplierTitle) {
            attribute.supplierTitle = supplierTitle;
            return this;
        }

        public Builder withOpencartTitle(String opencartTitle) {
            attribute.opencartTitle = opencartTitle;
            return this;
        }

        public Builder withReplacementFrom(String replacementFrom) {
            attribute.replacementFrom = replacementFrom;
            return this;
        }

        public Builder withReplacementTo(String replacementTo) {
            attribute.replacementTo = replacementTo;
            return this;
        }

        public Builder withMathSign(String mathSign) {
            attribute.mathSign = mathSign;
            return this;
        }

        public Builder withMathNumber(int mathNumber) {
            attribute.mathNumber = mathNumber;
            return this;
        }

        public AttributeApp build() {
            return attribute;
        }

    }
}
