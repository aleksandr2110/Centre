package orlov.home.centurapp.service.appservice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.FontStyleDto;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ImageOpencart;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;
import orlov.home.centurapp.util.AppConstant;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@AllArgsConstructor
@Slf4j
public class FileService {


//    public static final String imageDirOC = "C:/xampp/htdocs/ocapp/image/";
        public static final String imageDirOC = "/home/centurwebstarsco/public_html/image/";
    public static final String PART_PATH = "catalog/app/";

    public void writeToFile(String filename, String format, String text) {
        String titleFile = filename.concat(".").concat(format);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(titleFile));
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteImage(String imageName) {
        Path path = Paths.get(imageDirOC.concat(imageName));
        if (Files.isExecutable(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                log.error("Bad delete image: {}", path, e);
            }
        }
    }

    public void downloadImg(String url, String imageName) {
        Path path = Paths.get(imageDirOC.concat(imageName));
        if (Files.notExists(path)) {
            log.info("File does not exist: {}", path);
            try {
                URL urlObj = new URL(url);
                URLConnection hc = urlObj.openConnection();
                hc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36");
                Files.copy(hc.getInputStream(), path);
            } catch (IOException e) {
                log.info("Exception download image by link: {}", url, e);
            }
        } else {
            log.info("File exists: {}", path);
        }
    }


    public List<String> updateImageUseZipFile(InputStream in, List<String> supplierProductsImages) throws IOException {
        log.info("in: {}", in);

        List<String> stringNameList = new ArrayList<>();


        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry zipEntry = zis.getNextEntry();
        int countImage = 0;
        while (zipEntry != null) {
            try {


                log.info("");
                String fileName = zipEntry.getName().substring(zipEntry.getName().lastIndexOf("/") + 1);
                log.info("{}. file name: {}", ++countImage, fileName);


                String pathWithImage = supplierProductsImages
                        .stream()
                        .filter(fi -> fi.contains(fileName))
                        .findFirst()
                        .orElse(null);

                log.info("{}. path with file: {}", countImage, pathWithImage);
                if (Objects.nonNull(pathWithImage)) {


                    String replace = imageDirOC.concat(pathWithImage).replace(fileName, "");
                    log.info("{}. replace: {}", countImage, replace);


                    File destDir = new File(replace);

                    File newFile = new File(destDir, fileName);

                    if (!zipEntry.isDirectory()) {

                        File parent = newFile.getParentFile();
                        log.info("path parent: {}", parent.getAbsolutePath());
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Failed to create directory " + parent);
                        }

                        FileOutputStream fos = new FileOutputStream(newFile);

                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                        fos.close();

                        log.info("DbName: {}", pathWithImage);
                        stringNameList.add(pathWithImage);

                    }
                }
                zipEntry = zis.getNextEntry();
            } catch (Exception ex) {

            }
        }

        zis.closeEntry();
        zis.close();

        return stringNameList;
    }


    public Path createZipImage(List<String> imagesPath) {
        List<String> imageFiles = imagesPath
                .stream()
                .filter(i -> Files.isRegularFile(Paths.get(i)))
                .collect(Collectors.toList());

        File currDir = new File(".");
        String path = currDir.getAbsolutePath();
        String fileLocation = path.substring(0, path.length() - 1) + "images.zip";

        if (Files.exists(Paths.get(fileLocation))) {
            try {
                Files.delete(Paths.get(fileLocation));
            } catch (IOException e) {
                log.error("Exception delete image zip file", e);
            }
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileLocation))) {

            imageFiles
                    .stream()
                    .peek(i -> {

                        try {

                            FileInputStream fis = new FileInputStream(i);
                            ZipEntry zipEntry = new ZipEntry(Paths.get(i).getFileName().toString());
                            zos.putNextEntry(zipEntry);

                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }

                        } catch (Exception ex) {
                            log.error("Exception zip file", ex);
                        }
                    })
                    .collect(Collectors.toList());


        } catch (IOException e) {
            log.error("Exception zip file", e);
        }

        return Paths.get(fileLocation);
    }


    public Path createExcelProductFile(List<ProductOpencart> products) {

        Integer maxAttributeSize = products
                .stream()
                .map(p -> p.getAttributesWrapper().size())
                .max(Comparator.comparingInt(p -> p))
                .orElse(0);

        String supplierName = products.size() > 0 ? products.get(0).getJan() : "none";

        log.info("Create excel file with product size: {}", products.size());
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        writeProductHeader(maxAttributeSize, workbook, sheet);
        writeProductData(products, sheet, workbook);

        File currDir = new File(".");
        String path = currDir.getAbsolutePath();
        String fileLocation = path.substring(0, path.length() - 1) + supplierName + ".xlsx";
        try {
            FileOutputStream outputStream = new FileOutputStream(fileLocation);
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            log.warn("Filed create file with product data", e);
        }
        log.info("maxAttribute: {}", maxAttributeSize);
        return Paths.get(fileLocation);
    }


    public List<ProductOpencart> getProductsFromFile(InputStream in) {
        log.info("Excel file to objects");
        List<ProductOpencart> products = new ArrayList<>();
        try {
            ZipSecureFile.setMinInflateRatio(0);
            XSSFWorkbook workbook = new XSSFWorkbook(in);
            Sheet sheetAt = workbook.getSheetAt(0);
            Iterator<Row> iteratorRow = sheetAt.iterator();


            int idxRow = 0;

            while (iteratorRow.hasNext()) {

                Row row = iteratorRow.next();
                Iterator<Cell> iteratorCell = row.iterator();
                int idxCell = 0;


                ProductOpencart product = new ProductOpencart();
                ProductDescriptionOpencart productDescription = new ProductDescriptionOpencart();
                product.setProductsDescriptionOpencart(Collections.singletonList(productDescription));
                List<AttributeWrapper> attributesWrapper = new ArrayList<>();
                product.setAttributesWrapper(attributesWrapper);
                AttributeWrapper attributeWrapper = new AttributeWrapper();
                if (idxRow > 0)
                    while (iteratorCell.hasNext()) {


                        Cell cell = iteratorCell.next();
                        String result = "";
                        int id = 0;

                        switch (cell.getCellType()) {
                            case STRING:
                                result = cell.getStringCellValue();
                                break;
                            case NUMERIC:
                                double numericCellValue = cell.getNumericCellValue();
                                BigDecimal idBig = new BigDecimal(numericCellValue);
                                result = String.valueOf((int) numericCellValue);
                                id = idBig.intValue();
                                break;
                            default:
                                break;
                        }

                        switch (idxCell) {
                            case 0:
                                product.setId(id);
                                productDescription.setProductId(id);
                                break;
                            case 1:
                                product.setModel(result);
                                break;
                            case 2:
                                productDescription.setName(result);
                                break;
                            case 3:
                                result = convertTextExcelToHtml(cell, workbook);
                                productDescription.setDescription(result);
                                break;
                            default:
                                break;
                        }

                        if (idxCell > 3) {
                            if (idxCell % 2 == 0) {
                                attributeWrapper = new AttributeWrapper();
                                attributeWrapper.setKeySite(result);
                            } else {
                                attributeWrapper.setValueSite(result);
                                attributesWrapper.add(attributeWrapper);
                            }

                        }

                        idxCell++;
                    }


                products.add(product);
                idxRow++;
            }


        } catch (IOException e) {
            log.error("Exception read excel file", e);
        }

        return products;
    }


    public String convertTextExcelToHtml(Cell cell, XSSFWorkbook workbook) {


        XSSFRichTextString richText = (XSSFRichTextString) cell.getRichStringCellValue();
        int formattingRuns = cell.getRichStringCellValue().numFormattingRuns();
        String resultDesc = "";
        log.info("formattingRuns: {}", formattingRuns);

        if (formattingRuns == 0) {
            String result = "";
            switch (cell.getCellType()) {
                case STRING:
                    result = cell.getStringCellValue();
                    break;
                case NUMERIC:
                    double numericCellValue = cell.getNumericCellValue();
                    result = String.valueOf((int) numericCellValue);
                    break;
                default:
                    break;
            }
            return result;
        }

        for (int i = 0; i < formattingRuns; i++) {

            int startIdx = richText.getIndexOfFormattingRun(i);
            int length = richText.getLengthOfFormattingRun(i);
            String partDescription = richText.getString().substring(startIdx, startIdx + length);


            XSSFFont font = richText.getFontOfFormattingRun(i);

            XSSFColor xssfColor = font.getXSSFColor();
            String hexColor = toHexString(xssfColor);
            boolean isBold = font.getBold();
            boolean isStrikeout = font.getStrikeout();
            short fontSize = font.getFontHeightInPoints();
            String fontName = font.getFontName();
            boolean isItalic = font.getItalic();
            byte underline = font.getUnderline();

            FontStyleDto fontStyle = new FontStyleDto(hexColor, isBold, isStrikeout, fontSize, fontName, isItalic, underline);
            String partDescHtml = wrapToHtml(partDescription, fontStyle);
            resultDesc = resultDesc.concat(partDescHtml);


        }

        String readyDesc = resultDesc.replaceAll("\n", "</br>");
        return readyDesc;
    }

    private String wrapToHtml(String partDesc, FontStyleDto fontStyle) {
        String result = "<span class=\"centurapp\" style=\"white-space: pre-wrap; font-family: " + fontStyle.getFontName() +
                "; font-size: " + fontStyle.getFontSize() +
                "px; color: " + fontStyle.getHexColor() +
                "; font-weight: " + (fontStyle.isBold() ? "bold" : "normal") +
                "; text-decoration: " + (fontStyle.isStrikeout() ? " line-through " : "") +
                " " + (fontStyle.getUnderline() == 0 ? "" : " underline ") +
                "; font-style: " + (fontStyle.isItalic() ? " italic " : "") + " ; \">" +
                partDesc + "</span>";

        return result;
    }

    public java.awt.Color hexToColor(String hex) {
        hex = hex.replace("#", "");

        switch (hex.length()) {
            case 6:
                return new java.awt.Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16));
            case 8:
                return new java.awt.Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16),
                        Integer.valueOf(hex.substring(6, 8), 16));
        }
        return null;
    }

    private String toHexString(XSSFColor color) {
        if (Objects.isNull(color))
            return AppConstant.HEX_BLACK_COLOR;
        byte[] arrayOfNumbers = color.getRGB();
        String hex = "#";
        for (byte b : arrayOfNumbers) {
            hex += String.format("%02X", b);
        }
        return hex;
    }

    public void writeProductData(List<ProductOpencart> products, Sheet sheet, XSSFWorkbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setWrapText(true);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        cellStyle.setAlignment(HorizontalAlignment.LEFT);

        for (int i = 0; i < products.size(); i++) {
            try {
                ProductOpencart p = products.get(i);
                Row row = sheet.createRow(i + 1);
                row.setHeight((short) 400);

                Cell cellProductId = row.createCell(0);
                cellProductId.setCellStyle(cellStyle);
                cellProductId.setCellValue(p.getId());

                Cell cellJan = row.createCell(1);
                cellJan.setCellStyle(cellStyle);
                cellJan.setCellValue(p.getModel());
                ProductDescriptionOpencart desc = p.getProductsDescriptionOpencart().get(0);
                if (Objects.nonNull(desc)) {
                    Cell cellName = row.createCell(2);
                    cellName.setCellStyle(cellStyle);
                    cellName.setCellValue(desc.getName());
                    Cell cellDescription = row.createCell(3);
                    cellDescription.setCellStyle(cellStyle);

                    String description = desc.getDescription().replaceAll("</br>", "\n");
                    Document htmlDesc = Jsoup.parse(description, "", Parser.xmlParser());

                    Elements spans = htmlDesc.select("span.centurapp");
                    XSSFRichTextString rt = new XSSFRichTextString();

                    if (!spans.isEmpty()) {
                        for (Element span : spans) {

                            XSSFFont font = workbook.createFont();


                            String styleText = span.attr("style");

                            String[] styleList = styleText.split(";");
                            for (String style : styleList) {
                                String[] split = style.split(":");

                                if (split[0].contains("font-family")) {
                                    String fontName = split[1].trim();
                                    if (!fontName.isEmpty())
                                        font.setFontName(fontName);
                                } else if (split[0].contains("font-size")) {
                                    String fontSize = split[1].trim().replaceAll("\\D", "");
                                    if (!fontSize.isEmpty())
                                        font.setFontHeightInPoints(Short.parseShort(fontSize));
                                } else if (split[0].contains("color")) {
                                    String colorString = split[1].trim();
                                    java.awt.Color color;
                                    if (!colorString.isEmpty()) {
                                        color = hexToColor(colorString);
                                    } else {
                                        color = hexToColor(AppConstant.HEX_BLACK_COLOR);
                                    }
                                    IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
                                    XSSFColor xssfColor = new XSSFColor(color, colorMap);
                                    font.setColor(xssfColor);
                                } else if (split[0].contains("font-weight")) {
                                    boolean bold = split[1].contains("bold");
                                    if (bold)
                                        font.setBold(true);
                                } else if (split[0].contains("text-decoration")) {
                                    boolean isThrough = split[1].contains("line-through");
                                    boolean underline = split[1].contains("underline");
                                    if (isThrough)
                                        font.setStrikeout(true);
                                    if (underline) {
                                        font.setUnderline((byte) 1);
                                    }
                                } else if (split[0].contains("font-style")) {
                                    boolean italic = split[1].contains("italic");
                                    if (italic) {
                                        font.setItalic(true);
                                    }
                                }


                            }

                            String partDesc = cleanJsoup(span.outerHtml());
                            log.info("partDesc: {}", partDesc);
                            rt.append(partDesc, font);

                        }
                        cellDescription.setCellValue(rt);
                    } else {
                        cellDescription.setCellValue(description);
                    }


                }

                int idxCell = 3;

                List<AttributeWrapper> attributesWrapper = p.getAttributesWrapper();
                for (int j = 0; j < attributesWrapper.size(); j++) {
                    AttributeWrapper attributeWrapper = attributesWrapper.get(j);
                    String keySite = attributeWrapper.getKeySite();
                    String valueSite = attributeWrapper.getValueSite();
                    Cell cellAttrKet = row.createCell(++idxCell);
                    cellAttrKet.setCellValue(keySite);
                    cellAttrKet.setCellStyle(cellStyle);
                    Cell cellDescription = row.createCell(++idxCell);
                    cellDescription.setCellValue(valueSite);
                    cellDescription.setCellStyle(cellStyle);
                }

            } catch (Exception ex) {
                log.error("Filed create file", ex);
            }
        }
    }

    public String cleanJsoup(String html) {
        if (html == null)
            return html;
        Document document = Jsoup.parse(html);
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        String s = document.html().replaceAll("\\\\n", "\n");
        return Jsoup.clean(s, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }

    private void writeProductHeader(int maxAttributeSize, XSSFWorkbook workbook, Sheet sheet) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);


        Row rowHead = sheet.createRow(0);
        rowHead.setHeight((short) 500);
        Cell cellProductId = rowHead.createCell(0);
        cellProductId.setCellStyle(cellStyle);
        cellProductId.setCellValue("product_id");
        Cell cellModel = rowHead.createCell(1);
        cellModel.setCellStyle(cellStyle);
        cellModel.setCellValue("model");
        Cell cellName = rowHead.createCell(2);
        cellName.setCellStyle(cellStyle);
        cellName.setCellValue("name");
        Cell cellDescription = rowHead.createCell(3);
        cellDescription.setCellStyle(cellStyle);
        cellDescription.setCellValue("description");


        int idxCell = 3;

        for (int i = 0; i < maxAttributeSize; i++) {
            Cell cellAttrKey = rowHead.createCell(++idxCell);
            cellAttrKey.setCellStyle(cellStyle);
            cellAttrKey.setCellValue("attribute_key " + (i + 1));
            Cell cellAttrValue = rowHead.createCell(++idxCell);
            cellAttrValue.setCellStyle(cellStyle);
            cellAttrValue.setCellValue("attribute_value " + (i + 1));
        }

    }

    public void deleteImageFile(ImageOpencart imageOpencart) {
        try {
            Files.deleteIfExists(Paths.get(imageDirOC.concat(imageOpencart.getImage())));
        } catch (IOException e) {
            log.warn("Bad delete image", e);
        }
    }

}
