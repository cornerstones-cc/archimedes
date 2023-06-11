package cc.cornerstones.biz.share.types;

import cc.cornerstones.biz.datafacet.share.types.FieldTypeExtensionFile;
import cc.cornerstones.biz.share.constants.NamingPolicyEnum;
import lombok.Data;

@Data
public class ExportAttachment {
    private String columnName;

    /**
     * Source
     */
    private FieldTypeExtensionFile source;

    /**
     * Naming policy of the target
     */
    private NamingPolicyEnum namingPolicy;

    /**
     * if namingPolicy = COMBINE, 需要指定怎么 combine
     */
    private NamingPolicyExtCombine namingPolicyExtCombine;
}
