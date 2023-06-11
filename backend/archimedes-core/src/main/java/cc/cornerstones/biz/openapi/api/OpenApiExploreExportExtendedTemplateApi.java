package cc.cornerstones.biz.openapi.api;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import cc.cornerstones.biz.datafacet.dto.CreateExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.dto.ExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetExportService;
import cc.cornerstones.biz.datafacet.share.constants.ExportExtendedTemplateVisibilityEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;

@Tag(name = "[Open API] Explore / Data facets / Export extended templates")
@RestController
@RequestMapping(value = "/open-api/explore/data-facets/export-extended-templates")
public class OpenApiExploreExportExtendedTemplateApi {
    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private DataFacetExportService dataFacetExportService;

    @Operation(summary = "列表查询指定 Data facet 的 export / extended / templates 配置")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true),
            @Parameter(name = "visibility", description = "Export extended template  的 Visibility", required = false),
            @Parameter(name = "uid", description = "Export extended template  的 UID", required = false),
            @Parameter(name = "name", description = "Export extended template  的 Name", required = false),
            @Parameter(name = "description", description = "Export extended template  的 Description", required = false),
            @Parameter(name = "enabled", description = "Export extended template  的 Enabled", required = false),
            @Parameter(name = "last_modified_by", description = "Export extended template  的 Last modified by", required = false),
            @Parameter(name = "last_modified_timestamp", description = "Export extended template  的 Last modified timestamp", required =
                    false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<ExportExtendedTemplateDto>> listingQueryExportExtendedTemplatesOfDataFacet(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam(name = "visibility") ExportExtendedTemplateVisibilityEnum visibility,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "last_modified_by", required = false) String lastModifiedBy,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            Sort sort) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);
        this.openApiAuthService.authorizeDataFacet(dataFacetUid, operatingUserProfile);

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

        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(
                this.dataFacetExportService.listingQueryExportExtendedTemplatesOfDataFacet(
                        dataFacetUid, visibility,
                        uid, name, description, enabled, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "为指定 Data facet 的 export / extended / templates 配置创建 template")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PostMapping("")
    @ResponseBody
    public Response<ExportExtendedTemplateDto> createExportExtendedTemplateForDataFacet(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody CreateExportExtendedTemplateDto createExportExtendedTemplateDto) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);
        this.openApiAuthService.authorizeDataFacet(dataFacetUid, operatingUserProfile);

        //
        // Step 2, core-processing
        //
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
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "Export extended template 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToExportExtendedTemplate(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        ExportExtendedTemplateDto exportExtendedTemplateDto =
                this.dataFacetExportService.getExportExtendedTemplate(uid,
                operatingUserProfile);
        if (exportExtendedTemplateDto == null) {
            return Response.buildSuccess();
        } else {
            this.openApiAuthService.authorizeDataFacet(
                    exportExtendedTemplateDto.getDataFacetUid(), operatingUserProfile);

            return Response.buildSuccess(
                    this.dataFacetExportService.listAllReferencesToExportExtendedTemplate(
                            uid,
                            operatingUserProfile));
        }

    }

    @Operation(summary = "删除指定 Export extended template")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "uid", description = "Export extended template 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteExportExtendedTemplate(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        ExportExtendedTemplateDto exportExtendedTemplateDto =
                this.dataFacetExportService.getExportExtendedTemplate(uid,
                        operatingUserProfile);
        if (exportExtendedTemplateDto == null) {
            return Response.buildSuccess();
        } else {
            this.openApiAuthService.authorizeDataFacet(
                    exportExtendedTemplateDto.getDataFacetUid(), operatingUserProfile);

            this.dataFacetExportService.deleteExportExtendedTemplate(
                    uid,
                    operatingUserProfile);

            return Response.buildSuccess();
        }
    }
}
