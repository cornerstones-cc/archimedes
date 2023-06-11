package cc.cornerstones.zero.shutdown;

import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class Destroyer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Destroyer.class);

    @Autowired
    private PluginManager pluginManager;

    @PreDestroy
    public void onShutDown() {
        LOGGER.info("closing application context..let's do the final resource cleanup");

        try {
            LOGGER.info("begin to clean up");

            stopPlugins();

            LOGGER.info("end to clean up");
        } catch (Exception e) {
            String logMsg = "fail to clean up" + ". More info:" + e.getMessage();
            LOGGER.error(logMsg);
        }
    }

    private void stopPlugins() {
        LOGGER.info("begin to stop plugins");

        this.pluginManager.stopPlugins();

        LOGGER.info("end to stop plugins");
    }


}
