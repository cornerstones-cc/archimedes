package cc.cornerstones.biz.share.event;

import lombok.Data;

@Data
public class DistributedServerUpEvent {
    private String hostname;
    private String ipAddress;
}
