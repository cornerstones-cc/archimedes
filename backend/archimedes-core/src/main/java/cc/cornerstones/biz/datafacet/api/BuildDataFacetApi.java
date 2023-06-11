package cc.cornerstones.biz.datafacet.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.app.dto.AppDto;
import cc.cornerstones.biz.datafacet.dto.CreateDataFacetDto;
import cc.cornerstones.biz.datafacet.dto.DataFacetDto;
import cc.cornerstones.biz.datafacet.dto.DataFieldDto;
import cc.cornerstones.biz.datafacet.dto.UpdateDataFacetDto;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetService;
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

@Tag(name = "[Biz] Build / Data facets")
@RestController
@RequestMapping(value = "/build/data-facets")
public class BuildDataFacetApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataFacetService dataFacetService;

    @Operation(summary = "为指定 Data table 创建 Data facet")
    @PostMapping("")
    @ResponseBody
    public Response<DataFacetDto> createDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_table_uid", required = true) Long dataTableUid,
            @Valid @RequestBody CreateDataFacetDto createDataFacetDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFacetService.createDataFacet(dataTableUid, createDataFacetDto, operatingUserProfile));
    }

    @Operation(summary = "获取指定 Data facet")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("")
    @ResponseBody
    public Response<DataFacetDto> getDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataFacetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFacetService.getDataFacet(dataFacetUid, operatingUserProfile));
    }

    @Operation(summary = "更新指定 Data facet")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data facet 的 UID", required = true)
    })
    @PatchMapping("")
    @ResponseBody
    public Response updateDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataFacetUid,
            @Valid @RequestBody UpdateDataFacetDto updateDataFacetDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetService.updateDataFacet(
                dataFacetUid,
                updateDataFacetDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Data facet 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data facet 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataFacetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataFacetService.listAllReferencesToDataFacet(
                        dataFacetUid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Data facet")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data facet 的 UID", required = true)
    })
    @DeleteMapping("")
    @ResponseBody
    public Response deleteDataFacet(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataFacetUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataFacetService.deleteDataFacet(
                dataFacetUid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "分页查询 Data facets")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data facet 的 UID", required = false),
            @Parameter(name = "name", description = "Data facet 的 Name", required = false),
    })
    @GetMapping("/paging-query")
    @ResponseBody
    public Response<Page<DataFacetDto>> pagingQueryDataFacets(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataFacetUid,
            @RequestParam(name = "name", required = false) String dataFacetName,
            @RequestParam(name = "description", required = false) String description,
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
                Page<DataFacetDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        return Response.buildSuccess(
                this.dataFacetService.pagingQueryDataFacets(
                        dataFacetUid, dataFacetName,
                        description, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Data facets")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data facet 的 UID", required = false),
            @Parameter(name = "name", description = "Data facet 的 Name", required = false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataFacetDto>> listingQueryDataFacets(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataFacetUid,
            @RequestParam(name = "name", required = false) String dataFacetName,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataFacetService.listingQueryDataFacets(
                        dataFacetUid, dataFacetName,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Data source 或者所有 Data sources 的 Data facets")
    @Parameters(value = {
            @Parameter(name = "data_source_uid", description = "Data source 的 UID", required = false)
    })
    @GetMapping("/data-facet-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfDataFacetHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_source_uid", required = false) Long dataSourceUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (dataSourceUid == null) {
            return Response.buildSuccess(
                    this.dataFacetService.treeListingAllDataFacets(
                            operatingUserProfile));
        } else {
            return Response.buildSuccess(
                    this.dataFacetService.treeListingAllDataFacets(
                            dataSourceUid, operatingUserProfile));
        }

    }

    @Operation(summary = "树形列出指定 Data source 或者所有 Data sources 的 Data objects (Data facets & Data tables)")
    @Parameters(value = {
            @Parameter(name = "data_source_uid", description = "Data source 的 UID", required = false)
    })
    @GetMapping("/data-object-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfDataObjectHierarchy(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "data_source_uid", required = false) Long dataSourceUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (dataSourceUid == null) {
            return Response.buildSuccess(
                    this.dataFacetService.treeListingAllDataObjects(
                            operatingUserProfile));
        } else {
            return Response.buildSuccess(
                    this.dataFacetService.treeListingAllDataObjects(
                            dataSourceUid, operatingUserProfile));
        }
    }
}
