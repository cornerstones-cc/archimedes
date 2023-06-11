package cc.cornerstones.biz.datafacet.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.dto.*;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetAppearanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "[Biz] Build / Data facets / Appearance")
@RestController
@RequestMapping(value = "/build/data-facets/appearance")
public class BuildDataFacetAppearanceApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataFacetAppearanceService dataFacetAppearanceService;

    @Operation(summary = "列表查询指定 Data facet 的 filtering / data fields 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/filtering/data-fields/listing-query")
    @ResponseBody
    public Response<List<FilteringDataFieldAnotherDto>> listingQueryFilteringDataFieldsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataFacetAppearanceService.listingQueryFilteringDataFieldsOfDataFacet(
                        dataFacetUid,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "获取指定 Data facet 的 filtering / extended 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/filtering/extended")
    @ResponseBody
    public Response<FilteringExtendedDto> getFilteringExtendedOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFacetAppearanceService.getFilteringExtendedOfDataFacet(
                        dataFacetUid,
                        operatingUserProfile));
    }

    @Operation(summary = "替换指定 Data facet 的 filtering / data fields 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PutMapping("/filtering/data-fields/all")
    @ResponseBody
    public Response replaceAllFilteringDataFieldsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody List<FilteringDataFieldDto> filteringDataFieldDtoList) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetAppearanceService.replaceAllFilteringDataFieldsOfDataFacet(
                dataFacetUid, filteringDataFieldDtoList,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "替换指定 Data facet 的 filtering / extended 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PutMapping("/filtering/extended")
    @ResponseBody
    public Response replaceFilteringExtendedOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody FilteringExtendedDto filteringExtendedDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetAppearanceService.replaceFilteringExtendedOfDataFacet(
                dataFacetUid, filteringExtendedDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列表查询指定 Data facet 的 listing / data fields 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/listing/data-fields/listing-query")
    @ResponseBody
    public Response<List<ListingDataFieldAnotherDto>> listingQueryListingDataFieldsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataFacetAppearanceService.listingQueryListingDataFieldsOfDataFacet(
                        dataFacetUid,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "获取指定 Data facet 的 listing / extended 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/listing/extended")
    @ResponseBody
    public Response<ListingExtendedDto> getListingExtendedOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFacetAppearanceService.getListingExtendedOfDataFacet(
                        dataFacetUid,
                        operatingUserProfile));
    }

    @Operation(summary = "替换指定 Data facet 的 listing / data fields 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PutMapping("/listing/data-fields/all")
    @ResponseBody
    public Response replaceAllListingDataFieldsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody List<ListingDataFieldDto> listingDataFieldDtoList) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetAppearanceService.replaceAllListingDataFieldsOfDataFacet(
                dataFacetUid, listingDataFieldDtoList,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "替换指定 Data facet 的 listing / extended 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PutMapping("/listing/extended")
    @ResponseBody
    public Response replaceListingExtendedOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody ListingExtendedDto listingExtendedDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetAppearanceService.replaceListingExtendedOfDataFacet(
                dataFacetUid, listingExtendedDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "导出指定 Data facet 的 listing data fields, 按 sequence 从小到大排列")
    @GetMapping("/listing/data-fields/export-sequence")
    @ResponseBody
    public ResponseEntity<Resource> exportSequenceOfAllListingDataFieldsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            HttpServletRequest request) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // Load file as Resource
        File file = this.dataFacetAppearanceService.exportSequenceOfAllListingDataFieldsOfDataFacet(
                dataFacetUid,
                operatingUserProfile);
        Resource resource = new UrlResource(file.toPath().toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/force-download"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + new String(resource.getFilename().getBytes(StandardCharsets.UTF_8),
                                StandardCharsets.ISO_8859_1) + "\"")
                .body(resource);
    }

    @Operation(summary = "导入指定 Data facet 的 listing data fields, 按 sequence 从小到大排列")
    @PostMapping("/listing/data-fields/import-sequence")
    @ResponseBody
    public Response importSequenceOfAllListingDataFieldsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @RequestParam("file") MultipartFile file) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetAppearanceService.importSequenceOfAllListingDataFieldsOfDataFacet(
                dataFacetUid,
                file,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列表查询指定 Data facet 的 sorting / data fields 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/sorting/data-fields/listing-query")
    @ResponseBody
    public Response<List<SortingDataFieldDto>> listingQuerySortingDataFieldsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataFacetAppearanceService.listingQuerySortingDataFieldsOfDataFacet(
                        dataFacetUid,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "替换指定 Data facet 的 sorting / data-fields 配置")
    @Parameters(value = {
            @Parameter(name = "data_facet_uid", description = "Data facet 的 UID", required = true)
    })
    @PutMapping("/sorting/data-fields/all")
    @ResponseBody
    public Response replaceAllSortingDataFieldsOfDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_facet_uid", required = true) Long dataFacetUid,
            @Valid @RequestBody List<SortingDataFieldDto> sortingDataFieldDtoList) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (CollectionUtils.isEmpty(sortingDataFieldDtoList)) {
            throw new AbcIllegalParameterException("should contain at least 1 sorting field, otherwise, end-users " +
                    "cannot " +
                    "use paginated queries");
        }

        this.dataFacetAppearanceService.replaceAllSortingDataFieldsOfDataFacet(
                dataFacetUid, sortingDataFieldDtoList,
                operatingUserProfile);

        return Response.buildSuccess();
    }

}
