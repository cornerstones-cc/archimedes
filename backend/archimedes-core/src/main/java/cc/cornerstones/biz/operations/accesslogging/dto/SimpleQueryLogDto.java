package cc.cornerstones.biz.operations.accesslogging.dto;

import cc.cornerstones.almond.types.BaseDto;
import cc.cornerstones.almond.types.UserBriefInformation;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.time.LocalDate;

/**
 * Simple query log
 *
 * @author bbottong
 */
@Data
public class SimpleQueryLogDto extends BaseDto {

    /**
     * Tracking serial number
     */
    private String trackingSerialNumber;

    /**
     * Data facet uid
     */
    private Long dataFacetUid;

    /**
     * Data facet name
     */
    private String dataFacetName;

    /**
     * Request
     */
    private JSONObject request;

    /**
     * Query duration remark
     */
    private String queryDurationRemark;

    /**
     * Created date
     */
    private LocalDate createdDate;

    private UserBriefInformation user;
}