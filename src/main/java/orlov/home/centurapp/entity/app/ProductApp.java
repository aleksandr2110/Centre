package orlov.home.centurapp.entity.app;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"orderProcessId", "productAppId", "status"})
public class ProductApp {
    private int productAppId;
    private int orderProcessId;
    private String name = "";
    private String url = "";
    private String status;
    private BigDecimal oldPrice = new BigDecimal("0.0");
    private BigDecimal newPrice = new BigDecimal("0.0");

    public static class Builder {
        private ProductApp productApp;

        public Builder() {
            productApp = new ProductApp();
        }

        public Builder withProductAppId(int productAppId) {
            productApp.productAppId = productAppId;
            return this;
        }

        public Builder withOrderProcessId(int orderProcessId) {
            productApp.orderProcessId = orderProcessId;
            return this;
        }

        public Builder withName(String name) {
            productApp.name = name;
            return this;
        }

        public Builder withUrl(String url) {
            productApp.url = url;
            return this;
        }

        public Builder withStatus(String status) {
            productApp.status = status;
            return this;
        }

        public Builder withOldPrice(BigDecimal oldPrice) {
            productApp.oldPrice = oldPrice;
            return this;
        }

        public Builder withNewPrice(BigDecimal newPrice) {
            productApp.newPrice = newPrice;
            return this;
        }

        public ProductApp build() {
            return productApp;
        }

    }
}
