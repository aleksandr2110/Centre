package orlov.home.centurapp.dto.api.artinhead;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageArtinhead {
    private String title;
    private String caption;
    private String url;
    private String alt;
    private String src;
    private String srcset;
    private String sizes;
    @JsonProperty(value = "full_src")
    private String fullSrc;
    @JsonProperty(value = "full_src_w")
    private Integer fullSrcW;
    @JsonProperty(value = "full_src_h")
    private Integer fullSrcH;
    @JsonProperty(value = "gallery_thumbnail_src")
    private String galleryThumbnailSrc;
    @JsonProperty(value = "gallery_thumbnail_src_w")
    private Integer galleryThumbnailSrcW;
    @JsonProperty(value = "gallery_thumbnail_src_h")
    private Integer galleryThumbnailSrcH;
    @JsonProperty(value = "thumb_src")
    private String thumbSrc;
    @JsonProperty(value = "thumb_src_w")
    private Integer thumbSrcW;
    @JsonProperty(value = "thumb_src_h")
    private Integer thumbSrcH;
    @JsonProperty(value = "src_w")
    private Integer srcW;
    @JsonProperty(value = "src_h")
    private Integer srcH;
    @JsonProperty(value = "thumb_srcset")
    private String thumbSrcset;
    @JsonProperty(value = "thumb_sizes")
    private String thumbSizes;

    @Override
    public String toString() {
        return "ImageArtinhead{" + "\n" +
                "\t\ttitle=" + title + "\n" +
                "\t\tcaption=" + caption + "\n" +
                "\t\turl=" + url + "\n" +
                "\t\talt=" + alt + "\n" +
                "\t\tsrc=" + src + "\n" +
                "\t\tsrcset=" + srcset + "\n" +
                "\t\tsizes=" + sizes + "\n" +
                "\t\tfullSrc=" + fullSrc + "\n" +
                "\t\tfullSrcW=" + fullSrcW + "\n" +
                "\t\tfullSrcH=" + fullSrcH + "\n" +
                "\t\tgalleryThumbnailSrc=" + galleryThumbnailSrc + "\n" +
                "\t\tgalleryThumbnailSrcW=" + galleryThumbnailSrcW + "\n" +
                "\t\tgalleryThumbnailSrcH=" + galleryThumbnailSrcH + "\n" +
                "\t\tthumbSrc=" + thumbSrc + "\n" +
                "\t\tthumbSrcW=" + thumbSrcW + "\n" +
                "\t\tthumbSrcH=" + thumbSrcH + "\n" +
                "\t\tsrcW=" + srcW + "\n" +
                "\t\tsrcH=" + srcH + "\n" +
                "\t\tthumbSrcset=" + thumbSrcset + "\n" +
                "\t\tthumbSizes=" + thumbSizes + "}";
    }
}
