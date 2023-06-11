package cc.cornerstones.arbutus.tinyid.service;

import cc.cornerstones.arbutus.tinyid.persistence.TinyIdDao;
import cc.cornerstones.arbutus.tinyid.entity.TinyIdDo;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class IdHelper implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdHelper.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IdGeneratorFactoryServer idGeneratorFactoryServer;

    @Autowired
    private TinyIdDao tinyIdDao;

    @Autowired
    private StandaloneIdGenerator standaloneIdGenerator;

    public Long getNextDistributedId(String bizType) {
        final IdGenerator idGenerator = idGeneratorFactoryServer.getIdGenerator(bizType);
        return idGenerator.nextId();
    }

    public String getNextDistributedIdOfStr(String bizType) {
        final IdGenerator idGenerator = idGeneratorFactoryServer.getIdGenerator(bizType);
        return idGenerator.nextId().toString();
    }

    public Long getNextStandaloneId(String bizType) {
        return this.standaloneIdGenerator.generateId(bizType);
    }

    public String getNextStandaloneIdOfStr(String bizType) {
        return String.valueOf(this.standaloneIdGenerator.generateId(bizType));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Reflections reflections = new Reflections("cc.cornerstones");
        final Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(TinyId.class);
        for (Class<?> clazz : typesAnnotatedWith) {
            final TinyId tinyId = clazz.getAnnotation(TinyId.class);
            final String bizType = tinyId.bizType();
            final long beginId = tinyId.beginId();
            final int delta = tinyId.delta();
            final long maxId = tinyId.maxId();
            final int remainder = tinyId.remainder();
            final int step = tinyId.step();
            TinyIdDo tinyIdDo = tinyIdDao.queryByBizType(bizType);
            if (tinyIdDo == null) {
                java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
                tinyIdDo = new TinyIdDo();
                tinyIdDo.setBeginId(beginId);
                tinyIdDo.setBizType(bizType);
                tinyIdDo.setCreateTimestamp(now);
                tinyIdDo.setDelta(delta);
                tinyIdDo.setMaxId(maxId);
                tinyIdDo.setRemainder(remainder);
                tinyIdDo.setStep(step);
                tinyIdDo.setLastUpdateTimestamp(now);
                tinyIdDo.setVersion(0L);
                tinyIdDao.create(tinyIdDo);
            }
        }
    }

}
