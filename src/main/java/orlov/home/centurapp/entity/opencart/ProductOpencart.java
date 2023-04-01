package orlov.home.centurapp.entity.opencart;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.api.artinhead.OptionDto;
import orlov.home.centurapp.entity.app.ProductProfileApp;
import orlov.home.centurapp.util.OCConstant;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(of = {"sku", "jan"})
@ToString
public class ProductOpencart {
    private int id;
    private String model = OCConstant.MODEL;
    private String sku = OCConstant.SKU;
    private String upc = OCConstant.UPC;
    private String ean = OCConstant.EAN;
    private String jan = OCConstant.JAN;
    private String isbn = OCConstant.ISBN;
    private String mpn = OCConstant.MPN;
    private String location = OCConstant.LOCATION;
    private int quantity = OCConstant.QUANTITY;
    private int stockStatusId = OCConstant.STOCK_STATUS_ID;
    private String image = OCConstant.IMAGE;
    private int manufacturerId = OCConstant.MANUFACTURER_ID;
    private String manufacturerName = OCConstant.EMPTY_STRING;
    private boolean shipping = OCConstant.SHIPPING;
    private BigDecimal price = OCConstant.PRICE;
    private int points = OCConstant.POINTS;
    private int taxClassId = OCConstant.TAX_CLASS_ID;
    private Timestamp dataAvailable = OCConstant.DATA_AVAILABLE;
    private BigDecimal weight = OCConstant.WEIGHT;
    private int weightClassId = OCConstant.WEIGHT_CLASS_ID;
    private BigDecimal length = OCConstant.LENGTH;
    private BigDecimal width = OCConstant.WIDTH;
    private BigDecimal height = OCConstant.HEIGHT;
    private int lengthClassId = OCConstant.LENGTH_CLASS_ID;
    private boolean subtract = OCConstant.SUBTRACT;
    private int minimum = OCConstant.MINIMUM;
    private int sortOrder = OCConstant.SORT_ORDER;
    private boolean status = OCConstant.STATUS;
    private int viewed = OCConstant.VIEWED;
    private Timestamp dataAdded = OCConstant.DATA_ADDED;
    private Timestamp dataModified = OCConstant.DATA_MODIFIED;
    private boolean noindex = OCConstant.NOINDEX;
    private int currencyId = OCConstant.CURRENCY_ID;
    private int obmenId = OCConstant.ZERO;
    private int ituaOriginalCurId = OCConstant.CURRENCY_ID;
    private BigDecimal ituaOriginalPrice = OCConstant.PRICE;
    private int stickerId = OCConstant.ZERO;
    private int sticker2Id = OCConstant.ZERO;
    private String uuid = OCConstant.EMPTY_STRING;
    private String afValues = OCConstant.EMPTY_STRING;
    private String afTags = OCConstant.EMPTY_STRING;
    private String youtubeVideo1 = OCConstant.EMPTY_STRING;
    private String youtubeVideo2 = OCConstant.EMPTY_STRING;
    private String youtubeVideo3 = OCConstant.EMPTY_STRING;
    private String youtubeVideo4 = OCConstant.EMPTY_STRING;
    private String youtubeVideo5 = OCConstant.EMPTY_STRING;

    private List<OptionOpencart> optionsOpencart = new ArrayList<>();
    private List<ProductOptionOpencart> productOptionsOpencart = new ArrayList<>();
    private List<ProductOpencart> sameTitleProductsOpencart = new ArrayList<>();
    private List<ProductDescriptionOpencart> productsDescriptionOpencart = new ArrayList<>();
    private ProductProfileApp productProfileApp;
    private boolean ready;
    private List<CategoryOpencart> categoriesOpencart = new ArrayList<>();
    private List<ImageOpencart> imagesOpencart = new ArrayList<>();
    private List<AttributeOpencart> attributesOpencart = new ArrayList<>();
    private List<AttributeWrapper> attributesWrapper = new ArrayList<>();
    private List<String> subImages = new ArrayList<>();
    private List<OptionDto> optionDtoList = new ArrayList<>();
    private String urlProduct;
    private String urlImage;
    private String title;


    public static class Builder {
        private ProductOpencart product;

        public Builder() {
            product = new ProductOpencart();
        }

        public Builder(ProductOpencart product) {
            this.product = product;
        }

        public Builder withProductId(int productId) {
            product.id = productId;
            return this;
        }

        public Builder withAttributesWrapper(List<AttributeWrapper> attributesWrapper) {
            product.attributesWrapper = attributesWrapper;
            return this;
        }

        public Builder withCurrencyId(int currencyId) {
            product.currencyId = currencyId;
            return this;
        }

        public Builder withObmenId(int obmenId) {
            product.obmenId = obmenId;
            return this;
        }

        public Builder withItuaOriginalCurId(int ituaOriginalCurId) {
            product.ituaOriginalCurId = ituaOriginalCurId;
            return this;
        }

        public Builder withItuaOriginalPrice(BigDecimal ituaOriginalPrice) {
            product.ituaOriginalPrice = ituaOriginalPrice;
            return this;
        }

        public Builder withProductsDescriptionOpencart(List<ProductDescriptionOpencart> productsDescriptionOpencart) {
            product.productsDescriptionOpencart = productsDescriptionOpencart;
            return this;
        }

        public Builder withSubImages(List<String> subImages) {
            product.subImages = subImages;
            return this;
        }

        public Builder withUuid(String uuid) {
            product.uuid = uuid;
            return this;
        }

        public Builder withSticker2Id(int sticker2Id) {
            product.sticker2Id = sticker2Id;
            return this;
        }


        public Builder withAfValues(String afValues) {
            product.afValues = afValues;
            return this;
        }

        public Builder withProductProfileApp(ProductProfileApp productProfileApp) {
            product.productProfileApp = productProfileApp;
            return this;
        }

        public Builder withStickerId(int stickerId) {
            product.stickerId = stickerId;
            return this;
        }

        public Builder withAfTags(String afTags) {
            product.afTags = afTags;
            return this;
        }

        public Builder withModel(String model) {
            product.model = model;
            return this;
        }

        public Builder withSku(String sku) {
            product.sku = sku;
            return this;
        }

        public Builder withUpc(String upc) {
            product.upc = upc;
            return this;
        }

        public Builder withEan(String ean) {
            product.ean = ean;
            return this;
        }

        public Builder withJan(String jan) {
            product.jan = jan;
            return this;
        }

        public Builder withIsbn(String isbn) {
            product.isbn = isbn;
            return this;
        }

        public Builder withTitle(String title) {
            product.title = title;
            return this;
        }

        public Builder withMpn(String mpn) {
            product.mpn = mpn;
            return this;
        }

        public Builder withLocation(String location) {
            product.location = location;
            return this;
        }

        public Builder withQuantity(int quantity) {
            product.quantity = quantity;
            return this;
        }

        public Builder withStockStatusId(int stockStatusId) {
            product.stockStatusId = stockStatusId;
            return this;
        }

        public Builder withImage(String image) {
            product.image = image;
            return this;
        }

        public Builder withManufacturerId(int manufacturerId) {
            product.manufacturerId = manufacturerId;
            return this;
        }

        public Builder withManufacturerName(String manufacturerName) {
            product.manufacturerName = manufacturerName;
            return this;
        }

        public Builder withShipping(boolean shipping) {
            product.shipping = shipping;
            return this;
        }

        public Builder withPrice(BigDecimal price) {
            product.price = price;
            return this;
        }

        public Builder withPoints(int points) {
            product.points = points;
            return this;
        }

        public Builder withTaxClassId(int taxClassId) {
            product.taxClassId = taxClassId;
            return this;
        }

        public Builder withDataAvailable(Timestamp dataAvailable) {
            product.dataAvailable = dataAvailable;
            return this;
        }

        public Builder withWeight(BigDecimal weight) {
            product.weight = weight;
            return this;
        }

        public Builder withWeightClassId(int weightClassId) {
            product.weightClassId = weightClassId;
            return this;
        }

        public Builder withLength(BigDecimal length) {
            product.length = length;
            return this;
        }

        public Builder withWidth(BigDecimal width) {
            product.width = width;
            return this;
        }

        public Builder withHeight(BigDecimal height) {
            product.height = height;
            return this;
        }

        public Builder withLengthClassId(int lengthClassId) {
            product.lengthClassId = lengthClassId;
            return this;
        }

        public Builder withSubtract(boolean subtract) {
            product.subtract = subtract;
            return this;
        }

        public Builder withMinimum(int minimum) {
            product.minimum = minimum;
            return this;
        }

        public Builder withSortOrder(int sortOrder) {
            product.sortOrder = sortOrder;
            return this;
        }

        public Builder withReady(boolean ready) {
            product.ready = ready;
            return this;
        }


        public Builder withStatus(boolean status) {
            product.status = status;
            return this;
        }

        public Builder withViewed(int viewed) {
            product.viewed = viewed;
            return this;
        }

        public Builder withDataAdded(Timestamp dataAdded) {
            product.dataAdded = dataAdded;
            return this;
        }

        public Builder withDataModified(Timestamp dataModified) {
            product.dataModified = dataModified;
            return this;
        }

        public Builder withNoindex(boolean noindex) {
            product.noindex = noindex;
            return this;
        }

        public Builder withUrlProduct(String url) {
            product.urlProduct = url;
            return this;
        }

        public Builder withUrlImage(String url) {
            product.urlImage = url;
            return this;
        }

        public Builder withYoutubeVideo1(String youtubeVideo1) {
            product.youtubeVideo1 = youtubeVideo1;
            return this;
        }
        public Builder withYoutubeVideo2(String youtubeVideo2) {
            product.youtubeVideo2 = youtubeVideo2;
            return this;
        }

        public Builder withYoutubeVideo3(String youtubeVideo3) {
            product.youtubeVideo3 = youtubeVideo3;
            return this;
        }
        public Builder withYoutubeVideo4(String youtubeVideo4) {
            product.youtubeVideo4 = youtubeVideo4;
            return this;
        }
        public Builder withYoutubeVideo5(String youtubeVideo5) {
            product.youtubeVideo5 = youtubeVideo5;
            return this;
        }
        public ProductOpencart build() {
            return product;
        }


    }

}
