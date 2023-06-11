package cc.cornerstones.biz.datafacet.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.dto.*;
import cc.cornerstones.biz.datafacet.service.inf.DataFieldService;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Build / Data facets / Data fields")
@RestController
@RequestMapping(value = "/build/data-facets/data-fields")
public class BuildDataFieldApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataFieldService dataFieldService;

    @Operation(summary = "为指定 Data facet 创建 Data field")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PostMapping("")
    @ResponseBody
    public Response<DataFieldDto> createDataField(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody CreateDataFieldDto createDataFieldDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFieldService.createDataField(createDataFieldDto, operatingUserProfile));
    }

    @Operation(summary = "获取指定 Data field")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data field 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<DataFieldDto> getDataField(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataFieldUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFieldService.getDataField(dataFieldUid, operatingUserProfile));
    }

    @Operation(summary = "更新指定 Data field")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data field 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateDataField(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataFieldUid,
            @Valid @RequestBody UpdateDataFieldDto updateDataFieldDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFieldService.updateDataField(
                dataFieldUid,
                updateDataFieldDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Data field 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data field 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToDataField(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataFieldUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFieldService.listAllReferencesToDataField(
                        dataFieldUid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Data field")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data field 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteDataField(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataFieldUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFieldService.deleteDataField(
                dataFieldUid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "替换指定 Data facet 的所有 Data fields")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PutMapping("/all")
    @ResponseBody
    public Response replaceAllDataFieldsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody List<DataFieldDto> dataFieldDtoList) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFieldService.replaceAllDataFieldsOfDataFacet(
                dataFacetUid, dataFieldDtoList,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "分页查询指定 Data facet 的 Data Fields")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true),
            @Parameter(name = "uid", description = "Data field 的 UID", required = false),
            @Parameter(name = "name", description = "Data field 的 Name", required = false),
            @Parameter(name = "type", description = "Data field 的 Type", required = false)
    })
    @GetMapping("/paging-query")
    @ResponseBody
    public Response<Page<DataFieldAnotherDto>> pagingQueryDataFields(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam(name = "uid", required = false) Long dataFieldUid,
            @RequestParam(name = "name", required = false) String dataFieldName,
            @RequestParam(name = "type", required = false) List<DataFieldTypeEnum> dataFieldTypeList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        return Response.buildSuccess(
                this.dataFieldService.pagingQueryDataFields(
                        dataFacetUid, dataFieldUid, dataFieldName, dataFieldTypeList,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询指定 Data facet 的 Data Fields")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true),
            @Parameter(name = "uid", description = "Data field 的 UID", required = false),
            @Parameter(name = "name", description = "Data field 的 Name", required = false),
            @Parameter(name = "type", description = "Data field 的 Type", required = false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataFieldAnotherDto>> listingQueryDataFields(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam(name = "uid", required = false) Long dataFieldUid,
            @RequestParam(name = "name", required = false) String dataFieldName,
            @RequestParam(name = "type", required = false) List<DataFieldTypeEnum> dataFieldTypeList,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataFieldService.listingQueryDataFields(
                        dataFacetUid, dataFieldUid, dataFieldName, dataFieldTypeList,
                        sort,
                        operatingUserProfile));
    }

}
