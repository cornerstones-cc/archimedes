package cc.cornerstones.arbutus.tinyid.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdGeneratorFactoryServer extends AbstractIdGeneratorFactory {
    private static final Logger logger = LoggerFactory.getLogger(IdGeneratorFactoryServer.class);

    @Autowired
    private SegmentIdService segmentIdService;

    @Override
    public IdGenerator createIdGenerator(String bizType) {
        logger.info("[tinyid] createIdGenerator:{}", bizType);

        return new CachedIdGenerator(bizType, segmentIdService);
    }
}
