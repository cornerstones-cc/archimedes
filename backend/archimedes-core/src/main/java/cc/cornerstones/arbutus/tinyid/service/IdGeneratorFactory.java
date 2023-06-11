package cc.cornerstones.arbutus.tinyid.service;

public interface IdGeneratorFactory {
    /**
     * 根据 bizType 创建 id 生成器
     *
     * @param bizType
     * @return
     */
    IdGenerator getIdGenerator(String bizType);
}
