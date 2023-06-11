package cc.cornerstones.biz.datapage.api;

import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datapage.dto.CreateDataPageDto;
import cc.cornerstones.biz.datapage.dto.DataPageDto;
import cc.cornerstones.biz.datapage.dto.UpdateDataPageDto;
import cc.cornerstones.biz.datapage.service.inf.DataPageService;
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


@Tag(name = "[Biz] Build / Data pages")
@RestController
@RequestMapping(value = "/build/data-pages")
public class BuildDataPageApi {
    @Autowired
    private UserService userService;

    @Autowired
    private DataPageService dataPageService;

    @Operation(summary = "创建 Data Page")
    @PostMapping("")
    @ResponseBody
    public Response<DataPageDto> createDataPage(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateDataPageDto createDataPageDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataPageService.createDataPage(
                         createDataPageDto, operatingUserProfile));
    }

    @Operation(summary = "更新指定 Data Page")
    @PatchMapping("")
    @ResponseBody
    public Response updateDataPage(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataPageUid,
            @Valid @RequestBody UpdateDataPageDto updateDataPageDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataPageService.updateDataPage(
                dataPageUid, updateDataPageDto, operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Data Page")
    @GetMapping("")
    @ResponseBody
    public Response<DataPageDto> getDataPage(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataPageUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataPageService.getDataPage(
                        dataPageUid, operatingUserProfile));
    }

    @Operation(summary = "列出针对指定 Data Page 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data Page 的 UID", required = true)
    })
    @GetMapping("/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToDataPage(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataPageUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.dataPageService.listAllReferencesToDataPage(
                        dataPageUid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Data Page")
    @DeleteMapping("")
    @ResponseBody
    public Response deleteDataPage(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long dataPageUid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.dataPageService.deleteDataPage(
                dataPageUid, operatingUserProfile);

        return Response.buildSuccess();
    }


    @Operation(summary = "分页查询 Data Pages")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data Page 的 UID", required = false),
            @Parameter(name = "name", description = "Data Page 的 Name", required = false)
    })
    @GetMapping("/paging-query")
    @ResponseBody
    public Response<Page<DataPageDto>> pagingQueryDataPages(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataPageUid,
            @RequestParam(name = "name", required = false) String dataPageName,
            Pageable pageable) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // pageable 中的 sort 特殊处理
        pageable = AbcApiUtils.transformPageable(pageable);

        return Response.buildSuccess(
                this.dataPageService.pagingQueryDataPages(
                        dataPageUid, dataPageName,
                        pageable,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Data Pages")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Data Page 的 UID", required = false),
            @Parameter(name = "name", description = "Data Page 的 Name", required = false)
    })
    @GetMapping("/listing-query")
    @ResponseBody
    public Response<List<DataPageDto>> listingQueryDataPages(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long dataPageUid,
            @RequestParam(name = "name", required = false) String dataPageName,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        return Response.buildSuccess(
                this.dataPageService.listingQueryDataPages(
                        dataPageUid, dataPageName,
                        sort,
                        operatingUserProfile));
    }
}
