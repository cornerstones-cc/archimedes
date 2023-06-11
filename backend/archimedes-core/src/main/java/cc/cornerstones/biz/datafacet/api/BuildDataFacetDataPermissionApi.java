package cc.cornerstones.biz.datafacet.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.dto.*;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetDataPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Build / Data facets / Data permissions")
@RestController
@RequestMapping(value = "/build/data-facets/data-permissions")
public class BuildDataFacetDataPermissionApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataFacetDataPermissionService dataFacetDataPermissionService;

    @Operation(summary = "为指定 Data facet 创建 Data permission 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PostMapping("")
    @ResponseBody
    public Response<DataPermissionDto> createDataPermissionForDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody CreateDataPermissionDto createDataPermissionDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (CollectionUtils.isEmpty(createDataPermissionDto.getFieldNameList())) {
            throw new AbcIllegalParameterException("field_name_list should not be null or empty");
        }

        return Response.buildSuccess(
                this.dataFacetDataPermissionService.createDataPermissionForDataFacet(
                        dataFacetUid, createDataPermissionDto,
                        operatingUserProfile));
    }

    @Operation(summary = "替换指定 Data permission 配置")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data permission 的 UID", required = true)
    })
    @PutMapping("")
    @ResponseBody
    public Response replaceDataPermission(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataPermissionUid,
            @Valid @RequestBody CreateDataPermissionDto createDataPermissionDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (CollectionUtils.isEmpty(createDataPermissionDto.getFieldNameList())) {
            throw new AbcIllegalParameterException("field_name_list should not be null or empty");
        }

        this.dataFacetDataPermissionService.replaceDataPermission(
                dataPermissionUid, createDataPermissionDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Data permission 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data permission 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToDataPermission(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFacetDataPermissionService.listAllReferencesToDataPermission(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Data permission")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data permission 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteDataPermission(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetDataPermissionService.deleteDataPermission(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列表查询指定 Data facet 的 所有 Data permission 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataPermissionDto>> listingQueryDataPermissionsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataFacetDataPermissionService.listingQueryDataPermissionsOfDataFacet(
                        dataFacetUid, enabled,
                        sort,
                        operatingUserProfile));
    }

}
