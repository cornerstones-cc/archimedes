package cc.cornerstones.zero.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author bbottong
 */
@Component
public class Initializer implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Initializer.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private InfraInitializer infraInitializer;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private LogicInitializer logicInitializer;

    @Value("${spring.profiles.active}")
    private String runningMode;

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            LOGGER.info("begin to init infrastructure layer");
            initInfrastructureLayer();
            LOGGER.info("end to init infrastructure layer");
        } catch (Exception e) {
            LOGGER.error("fail to init infrastructure layer");
            SpringApplication.exit(this.applicationContext, () -> 101);
            return;
        }

        try {
            LOGGER.info("begin to init application layer");
            initApplicationLayer();
            LOGGER.info("end to init application layer");
        } catch (Exception e) {
            LOGGER.error("fail to init application layer");
            SpringApplication.exit(this.applicationContext, () -> 102);
            return;
        }
    }

    /**
     * 初始化 Infrastructure Layer
     *
     * @throws Exception
     */
    private void initInfrastructureLayer() throws Exception {
        try {
            LOGGER.info("begin to execute environment initializer");
            this.infraInitializer.execute();
            LOGGER.info("end to execute environment initializer");
        } catch (Exception e) {
            LOGGER.error("fail to execute environment initializer", e);
            throw e;
        }
    }

    /**
     * 初始化 Application Layer
     *
     * @throws Exception
     */
    private void initApplicationLayer() throws Exception {
        try {
            LOGGER.info("begin to execute data initializer");
            this.dataInitializer.execute();
            LOGGER.info("end to execute data initializer");
        } catch (Exception e) {
            LOGGER.error("fail to execute data initializer", e);
            throw e;
        }

        try {
            LOGGER.info("begin to execute logic initializer");
            this.logicInitializer.execute();
            LOGGER.info("end to execute logic initializer");
        } catch (Exception e) {
            LOGGER.error("fail to execute logic initializer", e);
            throw e;
        }
    }
}