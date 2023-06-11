package cc.cornerstones.biz.datapage.share.types;

import lombok.Data;

@Data
public class ChartMeta {
    private Long chartId;
    private Integer height;
    private Integer width;
    private String sliceName;
    private String uuid;
}
