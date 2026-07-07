package sri.microservices.riego;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import sri.microservices.riego.config.ServiceProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ServiceProperties.class)
public class RiegoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiegoServiceApplication.class, args);
    }
}
