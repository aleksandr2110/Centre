package orlov.home.centurapp.service.job;

import lombok.AllArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import orlov.home.centurapp.service.ManagerService;

@AllArgsConstructor
@DisallowConcurrentExecution
@Component
public class ParserServiceJob {

    private final ManagerService managerService;
    @Scheduled(cron = "${app.cron-job-parser}")
    public void executeParser() {
        managerService.processApp();
    }

    /*
    @Scheduled(cron = "${app.cron-job-attribute}")
    public void executeUpdateAttributeJob() {
        managerService.updateAttributeAppValue();
    }*/
}
