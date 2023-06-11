package cc.cornerstones.biz.datafacet.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.serviceconnection.dto.UserSynchronizationServiceAgentDto;
import cc.cornerstones.biz.administration.usermanagement.dto.AccountTypeDto;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.dto.*;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetExportService;
import cc.cornerstones.biz.datafacet.share.constants.ExportExtendedTemplateVisibilityEnum;
import cc.cornerstones.biz.datafacet.share.constants.TemplateColumnHeaderSourceEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;

@Tag(name = "[Biz] Build / Data facets / Export")
@RestController
@RequestMapping(value = "/build/data-facets/export")
public class BuildDataFacetExportApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataFacetExportService dataFacetExportService;

    @Operation(summary = "获取指定 Data facet 的 export / basic 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/basic")
    @ResponseBody
    public Response<ExportBasicDto> getExportBasicOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFacetExportService.getExportBasicOfDataFacet(
                        dataFacetUid,
                        operatingUserProfile));
    }

    @Operation(summary = "替换指定 Data facet 的 export / basic 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PutMapping("/basic")
    @ResponseBody
    public Response replaceExportBasicOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody ExportBasicDto exportBasicDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetExportService.replaceExportBasicOfDataFacet(
                dataFacetUid, exportBasicDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "为指定 Data facet 的 export / extended / templates 配置创建 template")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PostMapping("/extended/templates")
    @ResponseBody
    public Response<ExportExtendedTemplateDto> createExportExtendedTemplateForDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody CreateExportExtendedTemplateDto createExportExtendedTemplateDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createExportExtendedTemplateDto.getColumnHeaderSource() == null) {
            createExportExtendedTemplateDto.setColumnHeaderSource(TemplateColumnHeaderSourceEnum.FIELD_NAME);
        }

        return Response.buildSuccess(
                this.dataFacetExportService.createExportExtendedTemplateForDataFacet(
                        dataFacetUid, createExportExtendedTemplateDto,
                        operatingUserProfile));
    }

    @Operation(summary = "修改指定 Export extended template")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Export extended template 的 UID", required = true)
    })
    @PatchMapping("/extended/templates")
    @ResponseBody
    public Response updateExportExtendedTemplate(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @Valid @RequestBody UpdateExportExtendedTemplateDto updateExportExtendedTemplateDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetExportService.updateExportExtendedTemplate(
                uid, updateExportExtendedTemplateDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Export extended template 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Export extended template 的 UID", required = true)
    })
    @GetMapping("/extended/templates/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToExportExtendedTemplate(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFacetExportService.listAllReferencesToExportExtendedTemplate(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Export extended template")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Export extended template 的 UID", required = true)
    })
    @DeleteMapping("/extended/templates")
    @ResponseBody
    public Response deleteExportExtendedTemplate(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetExportService.deleteExportExtendedTemplate(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列表查询指定 Data facet 的 export / extended / templates 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/extended/templates/listing-query")
    @ResponseBody
    public Response<List<ExportExtendedTemplateDto>> listingQueryExportExtendedTemplatesOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam(name = "visibility") ExportExtendedTemplateVisibilityEnum visibility,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "last_modified_by", required = false) String lastModifiedBy,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        // 查询用户 lastModifiedBy
        List<Long> userUidListOfLastModifiedBy = null;
        if (!ObjectUtils.isEmpty(lastModifiedBy)) {
            userUidListOfLastModifiedBy = this.userService.listingQueryUidOfUsers(
                    lastModifiedBy, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                return Response.buildSuccess(null);
            }
        }

        return Response.buildSuccess(
                this.dataFacetExportService.listingQueryExportExtendedTemplatesOfDataFacet(
                        dataFacetUid, visibility,
                        uid, name, description, enabled, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询指定 Data facet 的 export / extended / templates 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/extended/templates/paging-query")
    @ResponseBody
    public Response<Page<ExportExtendedTemplateDto>> pagingQueryExportExtendedTemplatesOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam(name = "visibility") ExportExtendedTemplateVisibilityEnum visibility,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "last_modified_by", required = false) String lastModifiedBy,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        // 查询用户 lastModifiedBy
        List<Long> userUidListOfLastModifiedBy = null;
        if (!ObjectUtils.isEmpty(lastModifiedBy)) {
            userUidListOfLastModifiedBy = this.userService.listingQueryUidOfUsers(
                    lastModifiedBy, operatingUserProfile);
            if (CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                Page<ExportExtendedTemplateDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        return Response.buildSuccess(
                this.dataFacetExportService.pagingQueryExportExtendedTemplatesOfDataFacet(
                        dataFacetUid, visibility,
                        uid, name, description, enabled, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }

}
