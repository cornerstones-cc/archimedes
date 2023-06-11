package cc.cornerstones.biz.datafacet.share.types;

import cc.cornerstones.biz.share.constants.NamingPolicyEnum;
import cc.cornerstones.biz.share.types.NamingPolicyExtCombine;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ListingFieldTypeExtensionFile {
    private Boolean enabledFileDownload;

    private NamingPolicyEnum namingPolicy;

    private NamingPolicyExtCombine namingPolicyExtCombine;
}
