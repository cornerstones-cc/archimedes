package cc.cornerstones.biz.administration.usermanagement.api;


import cc.cornerstones.almond.constants.DatabaseConstants;
import cc.cornerstones.almond.constants.DatabaseFieldTypeEnum;
import cc.cornerstones.almond.constants.NetworkingConstants;
import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.UserProfile;

import cc.cornerstones.almond.utils.AbcApiUtils;
import cc.cornerstones.biz.administration.usermanagement.dto.*;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserSchemaService;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
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

@Tag(name = "[Biz] Admin / User management / User schema")
@RestController
@RequestMapping(value = "/admin/user-mgmt/user-schema")
public class UserSchemaApi {
    @Autowired
    private UserService userService;

    @Autowired
    private UserSchemaService userSchemaService;

    @Operation(summary = "创建 Extended property (扩展属性)")
    @PostMapping("/extended-properties")
    @ResponseBody
    public Response<UserSchemaExtendedPropertyDto> createExtendedProperty(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @Valid @RequestBody CreateUserSchemaExtendedPropertyDto createUserSchemaExtendedPropertyDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("display_name")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("display name")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("uid")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("id")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("deleted")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("created_by")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("created by")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("created_timestamp")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("created timestamp")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("last_modified_by")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("last modified by")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("last_modified_timestamp")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("last modified timestamp")
                || createUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("owner")) {
            throw new AbcIllegalParameterException(String.format("%s is reserved name, cannot use it",
                    createUserSchemaExtendedPropertyDto.getName()));
        }

        if (createUserSchemaExtendedPropertyDto.getSequence() == null) {
            createUserSchemaExtendedPropertyDto.setSequence(1.0F);
        }
        if (createUserSchemaExtendedPropertyDto.getType() == null) {
            createUserSchemaExtendedPropertyDto.setType(DatabaseFieldTypeEnum.VARCHAR);
            createUserSchemaExtendedPropertyDto.setLength(String.valueOf(DatabaseConstants.VARCHAR_DEFAULT_LENGTH));
        } else {
            switch (createUserSchemaExtendedPropertyDto.getType()) {
                case VARCHAR:
                case CHAR:
                    if (ObjectUtils.isEmpty(createUserSchemaExtendedPropertyDto.getLength())) {
                        createUserSchemaExtendedPropertyDto.setLength(String.valueOf(DatabaseConstants.VARCHAR_DEFAULT_LENGTH));
                    }
                    break;
                default:
                    break;
            }
        }
        if (createUserSchemaExtendedPropertyDto.getNullable() == null) {
            createUserSchemaExtendedPropertyDto.setNullable(Boolean.TRUE);
        }
        if (createUserSchemaExtendedPropertyDto.getShowInFilter() == null) {
            createUserSchemaExtendedPropertyDto.setShowInFilter(Boolean.FALSE);
        }
        if (createUserSchemaExtendedPropertyDto.getShowInBriefInformation() == null) {
            createUserSchemaExtendedPropertyDto.setShowInBriefInformation(Boolean.FALSE);
        }
        if (createUserSchemaExtendedPropertyDto.getShowInDetailedInformation() == null) {
            createUserSchemaExtendedPropertyDto.setShowInDetailedInformation(Boolean.FALSE);
        }

        return Response.buildSuccess(
                this.userSchemaService.createExtendedProperty(
                        createUserSchemaExtendedPropertyDto,
                        operatingUserProfile));
    }

    @Operation(summary = "更新指定 Extended property (扩展属性)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Extended property 的 UID", required = true)
    })
    @PatchMapping("/extended-properties")
    @ResponseBody
    public Response updateExtendedProperty(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid,
            @RequestBody UpdateUserSchemaExtendedPropertyDto updateUserSchemaExtendedPropertyDto) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        if (updateUserSchemaExtendedPropertyDto.getName() != null) {
            if (updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("display_name")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("display name")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("uid")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("id")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("deleted")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("created_by")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("created by")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("created_timestamp")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("created timestamp")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("last_modified_by")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("last modified by")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("last_modified_timestamp")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("last modified timestamp")
                    || updateUserSchemaExtendedPropertyDto.getName().equalsIgnoreCase("owner")) {
                throw new AbcIllegalParameterException(String.format("%s is reserved name, cannot use it",
                        updateUserSchemaExtendedPropertyDto.getName()));
            }
        }

        this.userSchemaService.updateExtendedProperty(
                uid, updateUserSchemaExtendedPropertyDto,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "列出针对指定 Extended property (扩展属性) 的所有引用")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Extended property 的 UID", required = true)
    })
    @GetMapping("/extended-properties/references")
    @ResponseBody
    public Response<List<String>> listAllReferencesToExtendedProperty(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.userSchemaService.listAllReferencesToExtendedProperty(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "删除指定 Extended property (扩展属性)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Extended property 的 UID", required = true)
    })
    @DeleteMapping("/extended-properties")
    @ResponseBody
    public Response deleteExtendedProperty(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        this.userSchemaService.deleteExtendedProperty(
                uid,
                operatingUserProfile);

        return Response.buildSuccess();
    }

    @Operation(summary = "获取指定 Extended property (扩展属性)")
    @Parameters(value = {
            @Parameter(name = "uid", description = "Extended property 的 UID", required = true)
    })
    @GetMapping("/extended-properties")
    @ResponseBody
    public Response<UserSchemaExtendedPropertyDto> getExtendedProperty(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = true) Long uid) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        return Response.buildSuccess(
                this.userSchemaService.getExtendedProperty(
                        uid,
                        operatingUserProfile));
    }

    @Operation(summary = "列表查询 Extended property (扩展属性)")
    @GetMapping("/extended-properties/listing-query")
    public Response<List<UserSchemaExtendedPropertyDto>> listingQueryExtendedProperties(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "show_in_filter", required = false) Boolean showInFilter,
            @RequestParam(name = "show_in_detailed_information", required = false) Boolean showInDetailedInformation,
            @RequestParam(name = "show_in_brief_information", required = false) Boolean showInBriefInformation,
            Sort sort) throws Exception {
        // Operating User's Profile
        UserProfile operatingUserProfile =
                this.userService.getUserProfile(operatingUserUid);

        // sort 特殊处理
        sort = AbcApiUtils.transformSort(sort);

        // 业务逻辑
        return Response.buildSuccess(
                this.userSchemaService.listingQueryExtendedProperties(
                        uid, name, showInFilter, showInDetailedInformation, showInBriefInformation,
                        sort,
                        operatingUserProfile));
    }

    @Operation(summary = "分页查询 Extended property (扩展属性)")
    @GetMapping("/extended-properties/paging-query")
    public Response<Page<UserSchemaExtendedPropertyDto>> pagingQueryExtendedProperties(
            @RequestHeader(NetworkingConstants.HEADER_USER_UID) Long operatingUserUid,
            @RequestParam(name = "uid", required = false) Long uid,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "last_modified_by", required = false) String lastModifiedBy,
            @RequestParam(name = "last_modified_timestamp", required = false) List<String> lastModifiedTimestampAsStringList,
            @RequestParam(name = "show_in_filter", required = false) Boolean showInFilter,
            @RequestParam(name = "show_in_detailed_information", required = false) Boolean showInDetailedInformation,
            @RequestParam(name = "show_in_brief_information", required = false) Boolean showInBriefInformation,
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
                Page<UserSchemaExtendedPropertyDto> result = new PageImpl<>(new LinkedList<>(), pageable, 0);
                return Response.buildSuccess(result);
            }
        }

        // 业务逻辑
        return Response.buildSuccess(
                this.userSchemaService.pagingQueryExtendedProperties(
                        uid, name, description, userUidListOfLastModifiedBy, lastModifiedTimestampAsStringList,
                        showInFilter, showInDetailedInformation, showInBriefInformation,
                        pageable,
                        operatingUserProfile));
    }
}
