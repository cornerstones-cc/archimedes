package cc.cornerstones.biz.datadictionary.service.assembly;

import cc.cornerstones.biz.datatable.share.constants.ResolverEnum;
import lombok.Data;

@Data
public class DictionaryBuildSqlLogic {
    private Long dataSourceUid;
    private ResolverEnum resolver;
    private String queryStatement;
}
