package orlov.home.centurapp.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.service.appservice.ScraperDataUpdateService;
import orlov.home.centurapp.service.appservice.UpdateDataService;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.app.SupplierAppService;
import orlov.home.centurapp.service.parser.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@AllArgsConstructor

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


    public void translateGoodfood() {
        new Thread(parserServiceGoodfood::translateSupplierProducts).start();
    }


    public void translateRP() {
        new Thread(parserServiceRP::translateSupplierProducts).start();
    }


    public void updateAttributeAppValue() {
        new Thread(() -> {
            try {
                log.info("Start global process update attribute value");
//                parserServiceHator.updateAttributeValue();
//                parserServiceRP.updateAttributeValue();
//                parserServiceGoodfood.updateAttributeValue();
//                parserServiceArtinhead.updateAttributeValue();

                parserServiceMaresto.updateAttributeValue();
                parserServiceKodaki.updateAttributeValue();
                parserServiceNowystyl.updateAttributeValue();
                parserServiceIndigowood.updateAttributeValue();
                parserServiceSector.updateAttributeValue();
                parserServiceTfb2b.updateAttributeValue();
                parserServiceAstim.updateAttributeValue();
                log.info("End global process update attribute value");
                TimeUnit.HOURS.sleep(22);
            } catch (Exception e) {
                log.warn("Exception main process", e);
            }
        }).start();
        log.info("Threat is demon");
    }

    public void processApp() {
        new Thread(() -> {
            while (true) {
                try {
                    log.info("Start global process");
                    parserServiceUhlmash.doProcess();
                    parserServiceTechsnab.doProcess();
                    parserServiceHator.doProcess();
                    parserServiceNoveen.doProcess();
                    parserServiceRP.doProcess();
                    parserServiceGoodfood.doProcess();
                    parserServiceArtinhead.doProcess();
                    parserServiceAstim.doProcess();
                    parserServiceMaresto.doProcess();
                    parserServiceKodaki.doProcess();
                    parserServiceNowystyl.doProcess();
                    parserServiceIndigowood.doProcess();
                    parserServiceSector.doProcess();
                    parserServiceTfb2b.doProcess();
                    log.info("End global process");
                    TimeUnit.HOURS.sleep(22);
                } catch (Exception e) {
                    log.warn("Exception main process", e);
                }

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


}
