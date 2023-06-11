package cc.cornerstones.biz.serve.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.dto.CreateExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.dto.ExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetExportService;
import cc.cornerstones.biz.datafacet.share.constants.ExportExtendedTemplateVisibilityEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "[Biz] Explore / Data facets / Export extended templates")
@RestController
@RequestMapping(value = "/explore/data-facets/export-extended-templates")
public class ExploreExportExtendedTemplateApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataFacetExportService dataFacetExportService;

    @Operation(summary = "列表查询指定 Data facet 的 export / extended / templates 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<ExportExtendedTemplateDto>> listingQueryExportExtendedTemplatesOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam(name = "visibility") ExportExtendedTemplateVisibilityEnum visibility,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
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

        Boolean enabled = Boolean.TRUE;

        return Response.buildSuccess(
                this.dataFacetExportService.listingQueryExportExtendedTemplatesOfDataFacet(
                        dataFacetUid, visibility,
                        uid, name, description, enabled, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 Data facet 的 export / extended / templates 配置创建 template")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PostMapping("")
    @ResponseBody
    public Response<ExportExtendedTemplateDto> createExportExtendedTemplateForDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody CreateExportExtendedTemplateDto createExportExtendedTemplateDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createExportExtendedTemplateDto.getVisibility() == null
                || createExportExtendedTemplateDto.getVisibility().equals(ExportExtendedTemplateVisibilityEnum.PUBLIC)) {
            throw new AbcIllegalParameterException("only accepts private template");
        }

        return Response.buildSuccess(
                this.dataFacetExportService.createExportExtendedTemplateForDataFacet(
                        dataFacetUid, createExportExtendedTemplateDto,
                        operatingUserProfile));
    }

    @Operation(summary = "列出针对指定 Export extended template 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Export extended template 的 UID", required = true)
    })
    @GetMapping("/references")
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
    @DeleteMapping("")
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
}
