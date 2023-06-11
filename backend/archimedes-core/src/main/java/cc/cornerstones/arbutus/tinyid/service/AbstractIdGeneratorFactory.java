package cc.cornerstones.arbutus.tinyid.service;


import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractIdGeneratorFactory implements IdGeneratorFactory {

    private static ConcurrentHashMap<String, IdGenerator> generators = new ConcurrentHashMap<>();

    @Override
    public IdGenerator getIdGenerator(String bizType) {
        if (generators.containsKey(bizType)) {
            return generators.get(bizType);
        }
        synchronized (this) {
            if (generators.containsKey(bizType)) {
                return generators.get(bizType);
            }
            IdGenerator idGenerator = createIdGenerator(bizType);
            generators.put(bizType, idGenerator);
            return idGenerator;
        }
    }

    /**
     * 根据 bizType 创建 id 生成器
     *
     * @param bizType
     * @return
     */
    protected abstract IdGenerator createIdGenerator(String bizType);
}
