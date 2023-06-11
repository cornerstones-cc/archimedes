package cc.cornerstones.arbutus.tinyid.persistence;

import cc.cornerstones.arbutus.tinyid.entity.TinyIdDo;

public interface TinyIdDao {
    /**
     * 根据 bizType 获取 db 中的 tinyId 对象
     *
     * @param bizType
     * @return
     */
    TinyIdDo queryByBizType(String bizType);

    /**
     * 根据 id、oldMaxId、version、bizType 更新最新的 maxId
     * @param id
     * @param newMaxId
     * @param oldMaxId
     * @param version
     * @param bizType
     * @return
     */
    int updateMaxId(Long id, Long newMaxId, Long oldMaxId, Long version, String bizType);
    
    void create(TinyIdDo tinyIdDo);
}
