package cc.cornerstones.biz.datatable.api;


import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datatable.dto.DataIndexDto;
import cc.cornerstones.biz.datatable.service.inf.DataIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Biz] Build / Data tables / Data indexes")
@RestController
@RequestMapping(value = "/build/data-tables/data-indexes")
public class BuildDataIndexApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataIndexService dataIndexService;

    @Operation(summary = "列出指定 Data table 的所有 Data indexes")
    @Parameters(value = {
            @Parameter(name = "data_table_uid", description = "Data Table 的 UID", required = true)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataIndexDto>> listingAllDataIndexesOfDataTable(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_table_uid", required = true) Long dataTableUid,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataIndexService.listingQueryDataIndexesOfDataTable(
                        dataTableUid,
                        sort,
                        operatingUserProfile));
    }
}
