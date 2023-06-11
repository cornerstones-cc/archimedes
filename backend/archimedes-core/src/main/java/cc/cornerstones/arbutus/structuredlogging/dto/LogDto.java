package cc.cornerstones.arbutus.structuredlogging.dto;

import lombok.Data;

/**
 * @author bbottong
 */
@Data
public class LogDto {
    private Long uid;
    private String jobCategory;
    private Long jobUid;
    private String content;
}
