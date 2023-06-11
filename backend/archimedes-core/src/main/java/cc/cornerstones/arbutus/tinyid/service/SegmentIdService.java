package cc.cornerstones.arbutus.tinyid.service;


import cc.cornerstones.arbutus.tinyid.dto.SegmentId;

public interface SegmentIdService {

    /**
     * 根据 bizType 获取下一个 SegmentId 对象
     * @param bizType
     * @return
     */
    SegmentId getNextSegmentId(String bizType);

}
