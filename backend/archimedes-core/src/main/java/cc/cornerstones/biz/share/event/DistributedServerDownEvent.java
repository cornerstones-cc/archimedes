package cc.cornerstones.biz.share.event;

import lombok.Data;

@Data
public class DistributedServerDownEvent {
    private String hostname;
    private String ipAddress;
}
