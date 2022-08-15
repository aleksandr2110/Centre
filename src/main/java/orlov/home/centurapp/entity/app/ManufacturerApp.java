package orlov.home.centurapp.entity.app;


import lombok.*;
import orlov.home.centurapp.util.AppConstant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"manufacturerId", "supplierId"})
public class ManufacturerApp {
    private int manufacturerId;
    private int supplierId;
    private String supplierTitle;
    private String opencartTitle = AppConstant.EMPTY_STRING;
    private int markup;

    public static class Builder {
        private ManufacturerApp manufacturerApp = null;

        public Builder() {
            manufacturerApp = new ManufacturerApp();
        }

        public Builder withManufacturerId(int manufacturerId){
            manufacturerApp.manufacturerId = manufacturerId;
            return this;
        }

        public Builder withSupplierId(int supplierId){
            manufacturerApp.supplierId = supplierId;
            return this;
        }

        public Builder withSupplierTitle(String supplierTitle){
            manufacturerApp.supplierTitle = supplierTitle;
            return this;
        }

        public Builder withOpencartTitle(String opencartTitle){
            manufacturerApp.opencartTitle = opencartTitle;
            return this;
        }

        public Builder withMarkup(int markup){
            manufacturerApp.markup = markup;
            return this;
        }

        public ManufacturerApp build(){
            return manufacturerApp;
        }


    }

}
