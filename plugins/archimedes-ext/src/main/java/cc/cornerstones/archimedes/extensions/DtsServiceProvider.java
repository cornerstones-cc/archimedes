package cc.cornerstones.archimedes.extensions;

import cc.cornerstones.archimedes.extensions.constants.DtsExecutorBlockStrategyEnum;
import cc.cornerstones.archimedes.extensions.constants.DtsExecutorRouteStrategyEnum;
import org.pf4j.ExtensionPoint;

public abstract class DtsServiceProvider implements ExtensionPoint {
    public String getConfigurationTemplate() throws Exception {
        return null;
    }

    public void createJobButDoNotStartJob(
            String name,
            String cronExpression,
            String executorHandler,
            String executorParam,
            DtsExecutorRouteStrategyEnum executorRouteStrategy,
            DtsExecutorBlockStrategyEnum executorBlockStrategy) throws Exception {

    }

    public void createJobAndStartJob(
            String name,
            String cronExpression,
            String executorHandler,
            String executorParam,
            DtsExecutorRouteStrategyEnum executorRouteStrategy,
            DtsExecutorBlockStrategyEnum executorBlockStrategy) throws Exception {

    }

    public void startJob(String name) throws Exception {

    }

    public void stopJob(String name) throws Exception {

    }

    public void deleteJob(String name) throws Exception {

    }
}
