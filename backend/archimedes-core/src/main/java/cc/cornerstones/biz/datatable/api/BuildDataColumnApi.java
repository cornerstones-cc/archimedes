package cc.cornerstones.biz.datatable.api;


import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import cc.cornerstones.biz.datatable.dto.DataColumnDto;
import cc.cornerstones.biz.datatable.service.inf.DataColumnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Biz] Build / Data tables / Data columns")
@RestController
@RequestMapping(value = "/build/data-tables/data-columns")
public class BuildDataColumnApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataColumnService dataColumnService;

    @Operation(summary = "列表查询指定 Data table 的 Data columns")
    @Parameters(value = {
            @Parameter(name = "data_table_uid", description = "Data table 的 UID", required = true),
            @Parameter(name = "uid", description = "Data column 的 UID", required = false),
            @Parameter(name = "name", description = "Data column 的 Name", required = false),
            @Parameter(name = "type", description = "Data column 的 Type", required = false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataColumnDto>> listingQueryDataColumns(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_table_uid") Long dataTableUid,
            @RequestParam(name = "uid", required = false) Long dataColumnUid,
            @RequestParam(name = "name", required = false) String dataColumnName,
            @RequestParam(name = "type", required = false) DataColumnTypeEnum dataColumnType,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataColumnService.listingQueryDataColumns(
                        dataTableUid, dataColumnUid, dataColumnName, dataColumnType,
                        sort,
                        operatingUserProfile));
    }
}
