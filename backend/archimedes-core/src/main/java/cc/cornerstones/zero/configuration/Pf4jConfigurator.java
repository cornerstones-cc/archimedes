package cc.cornerstones.zero.configuration;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Pf4jConfigurator {
    private Logger LOGGER = LoggerFactory.getLogger(Pf4jConfigurator.class);

    @Bean
    public PluginManager pluginManager() {
        LOGGER.info("init pf4j plugin manager");

        PluginManager pluginManager = new DefaultPluginManager();

        return pluginManager;
    }
}
