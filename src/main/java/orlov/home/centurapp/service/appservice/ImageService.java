package orlov.home.centurapp.service.appservice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.util.AppConstant;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ImageService {


    public String createOptionImage(String hexColor) {
        int width = 26;
        int height = 26;
        String imageFormat = "png";

        String fileName = hexColor.replaceAll("#", "").concat("." + imageFormat);
        log.info("File name: {}", fileName);

        Path path = Paths.get(FileService.imageDirOC.concat(AppConstant.PART_DIR_OC_IMAGE.concat(fileName)));
        log.info("Path : {}", path);

        String dbImagePath = AppConstant.PART_DIR_OC_IMAGE.concat(fileName);
        log.info("Path DB: {}", dbImagePath);

        if (Files.notExists(path)) {
            log.info("New option image by hex: {}", hexColor);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Color rgb = Color.decode(hexColor);

            Graphics2D graphics = image.createGraphics();
            graphics.setColor(rgb);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

            try {
                ImageIO.write(image, imageFormat, new File(path.toString()));
            } catch (IOException e) {
                log.warn("Exception create image.", e);
            }
        }

        return dbImagePath;
    }

    public String createOptionMultiImage(List<String> images) {
        String imageFormat = "png";

        String fileName = images
                .stream()
                .peek(s -> log.info("Image name: {}", s))
                .map(s -> s.substring(0, s.indexOf(".")))
                .collect(Collectors.joining("-")).concat("." + imageFormat);
        int imageCount = images.size();
        log.info("Image count: {}", imageCount);
        log.info("File name: {}", fileName);

        int width = 26;
        int height = 26;


        Path path = Paths.get(FileService.imageDirOC.concat(AppConstant.PART_DIR_OC_IMAGE.concat(fileName)));
        log.info("Path : {}", path);

        String dbImagePath = AppConstant.PART_DIR_OC_IMAGE.concat(fileName);
        log.info("Path DB: {}", dbImagePath);


        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            List<BufferedImage> imageBufferList = images
                    .stream()
                    .map(i -> {
                        try {
                            Path pathGallery = Paths.get(FileService.imageDirOC.concat(AppConstant.PART_DIR_OC_IMAGE.concat(i)));
                            BufferedImage galleryImage = ImageIO.read(new File(pathGallery.toString()));
                            return galleryImage;
                        } catch (Exception ee) {
                            log.warn("Exception ee.", ee);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            int remainder = width % imageCount;
            log.info("remainder: {}", remainder);
            int partWidthPixel = (width - remainder) / imageCount;
            log.info("partWidthPixel: {}", partWidthPixel);


            Map<Integer, BufferedImage> map = new HashMap<>();
            int lasRange = 0;
            for (int i = 0; i < imageCount; i++) {
                int range = partWidthPixel;
                if (remainder != 0) {
                    range = (partWidthPixel + 1);
                    remainder--;
                }

                for (int j = lasRange; j < (range + lasRange); j++) {
                    map.put(j, imageBufferList.get(i));
                }
                lasRange += range;
            }


            Graphics2D graphics = image.createGraphics();
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    BufferedImage image1 = map.get(i);
                    Color galleryColor = new Color(image1.getRGB(i, j));
                    graphics.setColor(new Color(galleryColor.getRGB()));
                    graphics.fillRect(i, j, image.getWidth(), image.getHeight());
                }
            }




            ImageIO.write(image, imageFormat, new File(path.toString()));
        } catch (IOException e) {
            log.warn("Exception create image.", e);
        }


        return dbImagePath;
    }


}
