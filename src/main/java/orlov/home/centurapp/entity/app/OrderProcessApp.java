package orlov.home.centurapp.entity.app;

import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"orderParserId"})
public class OrderProcessApp {
    private int orderProcessId;
    private int supplierAppId;
    private Timestamp startProcess;
    private Timestamp endProcess;
    private SupplierApp supplierApp;
    private List<ProductApp> newProduct = new ArrayList<>();
    private List<ProductApp> newPriceProduct = new ArrayList<>();
    private List<ProductApp> olrProduct = new ArrayList<>();
    private List<ProductApp> againProductApp = new ArrayList<>();

    public static class Builder {
        private OrderProcessApp processApp = null;

        public Builder() {
            processApp = new OrderProcessApp();
        }

        public Builder withOrderProcessId(int orderProcessId) {
            processApp.orderProcessId = orderProcessId;
            return this;
        }

        public Builder withSupplierAppId(int supplierAppId) {
            processApp.supplierAppId = supplierAppId;
            return this;
        }

        public Builder withStartProcess(Timestamp startProcess) {
            processApp.startProcess = startProcess;
            return this;
        }

        public Builder withEndProcess(Timestamp endProcess) {
            processApp.endProcess = endProcess;
            return this;
        }

        public Builder withNewProduct(List<ProductApp> newProduct) {
            processApp.newProduct = newProduct;
            return this;
        }

        public Builder withNewPriceProduct(List<ProductApp> newPriceProduct) {
            processApp.newPriceProduct = newPriceProduct;
            return this;
        }

        public Builder withOlrProduct(List<ProductApp> olrProduct) {
            processApp.olrProduct = olrProduct;
            return this;
        }

        public Builder withAgainProductApp(List<ProductApp> againProductApp) {
            processApp.againProductApp = againProductApp;
            return this;
        }

        public Builder withSupplierApp(SupplierApp supplierApp) {
            processApp.supplierApp = supplierApp;
            return this;
        }

        public OrderProcessApp build() {
            return processApp;
        }

    }

}
