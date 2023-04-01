package orlov.home.centurapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ImageOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;
import orlov.home.centurapp.service.api.translate.TranslateService;
import orlov.home.centurapp.service.appservice.FileService;
import orlov.home.centurapp.service.appservice.UpdateDataService;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;
import orlov.home.centurapp.service.parser.*;
import orlov.home.centurapp.util.AppConstant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@Scope("singleton")
public class ManagerService {

    private final ParserServiceMaresto parserServiceMaresto;
    private final ParserServiceKodaki parserServiceKodaki;
    private final ParserServiceNowystyl parserServiceNowystyl;
    private final ParserServiceIndigowood parserServiceIndigowood;
    private final ParserServiceAstim parserServiceAstim;
    private final ParserServiceSector parserServiceSector;
    private final ParserServiceArtinhead parserServiceArtinhead;
    private final ParserServiceGoodfood parserServiceGoodfood;
    private final ParserServiceRP parserServiceRP;
    private final ParserServiceHator parserServiceHator;
    private final ParserServiceTfb2b parserServiceTfb2b;
    private final ParserServiceTechsnab parserServiceTechsnab;
    private final ParserServiceNoveen parserServiceNoveen;
    private final ParserServiceUhlmash parserServiceUhlmash;
    private final ParserServiceKirovogradvesy parserServiceKirovogradvesy;
    private final ParserServiceVivat parserServiceVivat;
    private final ParserServiceCanadapech parserServiceCanadapech;
    private final ParserServiceOscar parserServiceOscar;
    private final ParserServiceAnshar parserServiceAnshar;
    private final ParserServiceFrizel parserServiceFrizel;
    private final ParserServiceSiker parserServiceSiker;
    private final ParserServiceZapovit parserServiceZapovit;
    private final ParserServiceGrillex parserServiceGrillex;
    private final ParserServiceArmEco parserServiceArmEco;
    private final ParserServiceTechnostyle parserServiceTechnostyle;
    private final ParserServiceRixo parserServiceRixo;
    private final ParserServiceKidigo parserServiceKidigo;
    private final ParserServiceSolaris parserServiceSolaris;
    private final OpencartDaoService opencartDaoService;
    private final AppDaoService appDaoService;
    private final UpdateDataService updateDataService;
    private final TranslateService translateService;

    private final  FileService fileService;

    private long runIdCoreParser = -1L;
    private long runIdUpdateAttribute = -1;

    public void updateNewModel() {
        new Thread(() -> {
            log.info("Start update new model parserServiceArtinhead");
            parserServiceArtinhead.updateNewModel();
            log.info("End update new model parserServiceArtinhead");
            log.info("Start update new model parserServiceAstim");
            parserServiceAstim.updateNewModel();
            log.info("End update new model parserServiceAstim");
            log.info("Start update new model parserServiceMaresto");
            parserServiceMaresto.updateNewModel();
            log.info("End update new model parserServiceMaresto");
            log.info("Start update new model parserServiceKodaki");
            parserServiceKodaki.updateNewModel();
            log.info("End update new model parserServiceKodaki");
            log.info("Start update new model parserServiceNowystyl");
            parserServiceNowystyl.updateNewModel();
            log.info("End update new model parserServiceNowystyl");
            log.info("Start update new model parserServiceIndigowood");
            parserServiceIndigowood.updateNewModel();
            log.info("End update new model parserServiceIndigowood");
            log.info("Start update new model parserServiceSector");
            parserServiceSector.updateNewModel();
            log.info("End update new model parserServiceSector");
        }).start();
    }

    public void testbuildWebDriver() {
        WebDriver webDriver = translateService.buildWebDriver();
        webDriver.get("https://bot.sannysoft.com/");

        WebDriverWait driverWait = new WebDriverWait(webDriver, 10, 200);
        List<WebElement> rows = driverWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//tr")));
        log.info("-    ");
        log.info("-        Web driver info");
        log.info("-    ");
        rows.forEach(r -> {
            List<WebElement> rowData = r.findElements(By.xpath(".//td"));
            if (rowData.size() == 2) {
                WebElement keyElement = rowData.get(0);
                WebElement valueElement = rowData.get(1);
                log.info("-    {} - [{}]", keyElement.getText().replaceAll("\\n", " "), valueElement.getText().replaceAll("\\n", " "));
            }
        });
        log.info("-    ");
        log.info("-    ");
        log.info("-    ");
        webDriver.quit();
    }


    public void deleteCustomProducts() {
        updateDataService.deleteUsersProduct();
    }


    public void translateGoodfood() {
        new Thread(parserServiceGoodfood::translateSupplierProducts).start();
    }

    public void translateFrizel() {
        new Thread(parserServiceFrizel::translateProducts).start();
    }


    public void translateRP() {
        new Thread(parserServiceRP::translateSupplierProducts).start();
    }


    public synchronized void  updateAttributeAppValue() {
        if(runIdUpdateAttribute != -1) {
            log.info("Threat updateAttributeAppValue is running id: {}", runIdUpdateAttribute);
            return;
        }
        new Thread(() -> {
            try {
                runIdUpdateAttribute = Thread.currentThread().getId();
                log.info("Start global process update attribute value");
//??                parserServiceHator.updateAttributeValue();
//??                parserServiceRP.updateAttributeValue();
//??                parserServiceGoodfood.updateAttributeValue();
//??                parserServiceArtinhead.updateAttributeValue();

               //!! parserServiceUhlmash.updateAttributeValue();

                parserServiceMaresto.updateAttributeValue();
                parserServiceKodaki.updateAttributeValue();
                parserServiceNowystyl.updateAttributeValue();
                parserServiceIndigowood.updateAttributeValue();
                parserServiceSector.updateAttributeValue();
                parserServiceTfb2b.updateAttributeValue();
                parserServiceAstim.updateAttributeValue();

                log.info("End global process update attribute value");
                //TimeUnit.HOURS.sleep(22);
            } catch (Exception e) {
                log.warn("Exception main process", e);
            } finally {
                runIdUpdateAttribute = -1;
            }
        }).start();
        log.info("Threat is demon");
    }

    public void updateImageHator() {
        new Thread(parserServiceHator::updateImages).start();
    }

    public synchronized void processApp() {
        if(runIdCoreParser != -1) {
            log.info("Threat processApp is running id: {}", runIdCoreParser);
            return;
        }
        new Thread(() -> {
                try {
                    runIdCoreParser = Thread.currentThread().getId();
                    log.info("Current Thread Name: {} ", Thread.currentThread().getName());

                    log.info("Current Thread ID: {}", Thread.currentThread().getId());
                    log.info("Start global process");

                    parserServiceSolaris.doProcess();

                    parserServiceKidigo.doProcess();

                    parserServiceRixo.doProcess();

                    parserServiceArmEco.doProcess();
                    parserServiceTechnostyle.doProcess();
                    parserServiceGrillex.doProcess();
                    parserServiceZapovit.doProcess();

                    parserServiceFrizel.doProcess();

                    parserServiceAnshar.doProcess();

                    parserServiceOscar.doProcess();
                    parserServiceCanadapech.doProcess();

                    parserServiceRP.doProcess();

                    parserServiceHator.doProcess();
                    parserServiceKirovogradvesy.doProcess();

                    parserServiceUhlmash.doProcess();

                    parserServiceTechsnab.doProcess();
                    parserServiceNoveen.doProcess();
                    parserServiceGoodfood.doProcess();

                    parserServiceArtinhead.doProcess();

                    parserServiceAstim.doProcess();
                    parserServiceMaresto.doProcess();
                    parserServiceKodaki.doProcess();

                    parserServiceNowystyl.doProcess();

                    parserServiceIndigowood.doProcess();

                    parserServiceSector.doProcess();

                    parserServiceTfb2b.doProcess();

                    parserServiceSiker.doProcess();

                    log.info("End global process");




                } catch (Exception e) {
                    log.warn("Exception main process", e);
                } finally {
                    runIdCoreParser = -1;
                }
        }).start();
        log.info("Threat is demon");
    }

    public void downloadImageRP() {
        new Thread(parserServiceRP::downloadImages).start();
    }

    public void downloadMainImageRP() {
        new Thread(parserServiceRP::updateMainImage).start();
    }

    public void updateModel() {
        new Thread(() -> {
            log.info("START UPDATE Artinhead");
            parserServiceArtinhead.updateModel();
            log.info("START UPDATE Astim");
            parserServiceAstim.updateModel();
            log.info("START UPDATE Maresto");
            parserServiceMaresto.updateModel();
            log.info("START UPDATE Kodaki");
            parserServiceKodaki.updateModel();
            log.info("START UPDATE Nowystyl");
            parserServiceNowystyl.updateModel();
            log.info("START UPDATE Indigowood");
            parserServiceIndigowood.updateModel();
            log.info("START UPDATE Sector");
            parserServiceSector.updateModel();
            log.info("End global process");
        }).start();

    }


    public void updateDescriptioKodki() {
        new Thread(() -> {
            while (true) {
                try {
                    log.info("Start KODAKI desc update");
                    parserServiceKodaki.updateDescription();
                    log.info("End KODAKI desc update");
                    TimeUnit.HOURS.sleep(24);
                } catch (Exception e) {
                    log.warn("Exception main process", e);
                }

            }
        }).start();
        log.info("Threat is demon");
    }

    public void updateDescriptionMaresto() {
        new Thread(() -> {
            try {
                log.info("Start MARESTO desc update");
                parserServiceMaresto.updateDescription();
                log.info("End MARESTO desc update");
            } catch (Exception e) {
                log.warn("Exception main process", e);
            }
        }).start();
        log.info("Threat is demon");
    }

    public void updateDescriptionAstim() {
        new Thread(() -> {
            try {
                log.info("Start ASTIM desc update");
//                parserServiceAstim.updateDescription();
                log.info("End ASTIM desc update");
            } catch (Exception e) {
                log.warn("Exception main process", e);
            }
        }).start();
        log.info("Threat is demon");
    }


    public void importVivat() {
        new Thread(() -> {
            try {
                log.info("Start ASTIM desc update");
                parserServiceVivat.doProcess();
                log.info("End ASTIM desc update");
            } catch (Exception e) {
                log.warn("Exception main process", e);
            }
        }).start();
        log.info("Threat is demon");
    }


    public void testFrizel() {
        new Thread(() -> {
            try {
                log.info("Start FRIZEL test");
                parserServiceFrizel.doProcess();
                log.info("End FRIZEL test ");
            } catch (Exception e) {
                log.warn("Exception main process", e);
            }
        }).start();
        log.info("Threat is demon");
    }

    public void delDupli() {
        parserServiceHator.dataDuplicateProduct();
    }


    public void oscarImageUpdate() {
        new Thread(() -> {
            try {
                log.info("Start FRIZEL test");
                parserServiceOscar.downloadImages();
                log.info("End FRIZEL test ");
            } catch (Exception e) {
                log.warn("Exception main process", e);
            }
        }).start();
        log.info("Threat is demon");
    }

    public void hatorChangeImage() {
        new Thread(() -> {
            try {
                log.info("Start FRIZEL test");
                parserServiceHator.changeFirstSecondImage();
                log.info("End FRIZEL test ");
            } catch (Exception e) {
                log.warn("Exception main process", e);
            }
        }).start();
        log.info("Threat is demon");
    }

    public void moveImagesToSupplierDir() {
        String imageDirOC = fileService.getImageDirOc();
        String partPath = AppConstant.PART_DIR_OC_IMAGE;
        List<SupplierApp> supplierAppList = appDaoService.getAllSupplierApp();
        Set<Path> oldImageList = new HashSet<>();
        supplierAppList
                .stream()
                .forEach(s -> {
                    Path supplierDir = Paths.get(imageDirOC.concat(partPath).concat(s.getDisplayName().concat("/")));
                    if (Files.notExists(supplierDir)) {
                        try {
                            Files.createDirectory(supplierDir);
                        } catch (Exception exd) {
                            log.warn("Bad create dir", exd);
                        }
                    }

                    List<ProductOpencart> productsList = opencartDaoService.getAllProductOpencartBySupplierAppName(s.getName());
                    productsList.forEach(p -> {
                        try {


                            String mainImage = p.getImage();
                            String mainImageAbsolutePath = imageDirOC.concat(mainImage);
                            String newMainImageAbsolutePath = mainImageAbsolutePath.replace(partPath, partPath.concat(s.getDisplayName().concat("/")));
                            log.info("Product model: [{}], img ab path: [{}]", p.getModel(), mainImageAbsolutePath);
                            log.info("Product model: [{}], new img ab path: [{}]", p.getModel(), newMainImageAbsolutePath);

                            Path newMainPath = Paths.get(newMainImageAbsolutePath);

                            if (Files.notExists(newMainPath))
                                Files.move(Paths.get(mainImageAbsolutePath), newMainPath);

                            p.setImage(mainImage.replaceAll(partPath, partPath.concat(s.getDisplayName().concat("/"))));
                            opencartDaoService.updateMainProductImageOpencart(p);
                            oldImageList.add(Paths.get(mainImageAbsolutePath));


                            List<ImageOpencart> imageList = opencartDaoService.getImageOpencartByProductId(p.getId());
                            imageList
                                    .forEach(i -> {
                                        try {
                                            String image = i.getImage();
                                            String imageAbsolutePath = imageDirOC.concat(image);
                                            String newImageAbsolutePath = imageAbsolutePath.replaceAll(partPath, partPath.concat(s.getDisplayName().concat("/")));
                                            log.info("  IMG ab path: [{}]", imageAbsolutePath);
                                            log.info("  IMG NEW ab path: [{}]", newImageAbsolutePath);

                                            Path newPath = Paths.get(newImageAbsolutePath);
//
                                            if (Files.notExists(newPath))
                                                Files.move(Paths.get(imageAbsolutePath), newPath);

                                            i.setImage(image.replaceAll(partPath, partPath.concat(s.getDisplayName().concat("/"))));
                                            opencartDaoService.updateSubImage(i);
                                            oldImageList.add(Paths.get(imageAbsolutePath));

                                        } catch (Exception ee) {
                                            log.warn("Bad move sub image", ee);
                                        }

                                    });

                        } catch (Exception e) {
                            log.warn("Bad move image", e);
                        }
                    });


                });

        oldImageList.stream().forEach(i -> {
            try {
                Files.deleteIfExists(i);
            } catch (IOException e) {
                log.warn("Bad delete old product", e);
            }
        });
    }


    public synchronized void updateFullDescription() {
        new Thread(() -> {
            try {
                parserServiceUhlmash.updateDescription();
            } catch (Exception ex) {
                log.warn("Bad update description", ex);
            }
        }).start();

    }

    public void updateImages() {
        parserServiceUhlmash.updateImage();
    }
}
