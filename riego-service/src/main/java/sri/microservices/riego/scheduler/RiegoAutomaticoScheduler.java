package sri.microservices.riego.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sri.microservices.riego.service.RiegoControlService;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class RiegoAutomaticoScheduler {

    private final RiegoControlService riegoControlService;
    private final ZoneId zoneId;

    public RiegoAutomaticoScheduler(RiegoControlService riegoControlService,
                                    @org.springframework.beans.factory.annotation.Value("${sri.time-zone:America/Lima}") String timeZone) {
        this.riegoControlService = riegoControlService;
        this.zoneId = ZoneId.of(timeZone);
    }

    @Scheduled(cron = "0 * * * * *", zone = "${sri.time-zone:America/Lima}")
    public void evaluarRiegoAutomatico() {
        riegoControlService.evaluarProgramacionAutomatica(LocalDateTime.now(zoneId));
    }
}
