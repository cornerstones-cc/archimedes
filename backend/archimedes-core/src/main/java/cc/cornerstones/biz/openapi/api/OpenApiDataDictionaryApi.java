package cc.cornerstones.biz.openapi.api;


import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.authentication.service.inf.OpenApiAuthService;
import cc.cornerstones.biz.datadictionary.service.inf.DictionaryService;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[Open API] Data dictionary")
@RestController
@RequestMapping(value = "/open-api/build/data-dictionary")
public class OpenApiDataDictionaryApi {
    @Autowired
    private OpenApiAuthService openApiAuthService;

    @Autowired
    private DictionaryService dictionaryService;

    @Operation(summary = "树形列出 Dictionary category hierarchy 的所有 Nodes")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false)
    })
    @GetMapping("/dictionary-category-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfDictionaryCategoryHierarchy(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject") String subject) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(
                this.dictionaryService.treeListingAllNodesOfDictionaryCategoryHierarchy(
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Dictionary category 的 Dictionary structure hierarchy 的所有 Nodes")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true)
    })
    @GetMapping("/dictionary-structure-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<TreeNode> treeListingAllNodesOfDictionaryStructureHierarchy(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject") String subject,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(
                this.dictionaryService.treeListingAllNodesOfDictionaryStructureHierarchy(
                        dictionaryCategoryUid,
                        operatingUserProfile));
    }


    @Operation(summary = "树形列出指定 Dictionary category 的 Dictionary content hierarchy 的所有 Nodes")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true)
    })
    @GetMapping("/dictionary-content-hierarchy/tree-listing-all-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeListingAllNodesOfDictionaryContentHierarchy(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject") String subject,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        return Response.buildSuccess(
                this.dictionaryService.treeListingAllNodesOfDictionaryContentHierarchy(
                        dictionaryCategoryUid,
                        operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Dictionary category 的 Dictionary content hierarchy 上的第1级节点")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "dictionary_category_uid", description = "Dictionary category 的 UID", required = true)
    })
    @GetMapping("/dictionary-content-hierarchy/tree-listing-first-level")
    @ResponseBody
    public Response<List<TreeNode>> treeListingFirstLevelOfDictionaryContentHierarchy(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject") String subject,
            @RequestParam(name = "dictionary_category_uid", required = true) Long dictionaryCategoryUid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.treeListingFirstLevelOfDictionaryContentHierarchy(
                        dictionaryCategoryUid, operatingUserProfile));
    }

    @Operation(summary = "树形列出指定 Dictionary category 的 Dictionary content hierarchy 上指定节点的下一级节点")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "dictionary_content_node_uid", description = "Dictionary Content Node 的 UID", required = true)
    })
    @GetMapping("/dictionary-content-hierarchy/tree-listing-next-level")
    @ResponseBody
    public Response<List<TreeNode>> treeListingNextLevelOfDictionaryContentHierarchy(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject") String subject,
            @RequestParam(name = "dictionary_content_node_uid", required = true) Long dictionaryContentNodeUid) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.treeListingNextLevelOfDictionaryContentHierarchy(
                        dictionaryContentNodeUid, operatingUserProfile));
    }

    @Operation(summary = "树形搜索指定 Dictionary category 的 Dictionary content hierarchy 的节点")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "dictionary_category_uid", description = "Dictionary Category 的 UID", required =
                    false),
            @Parameter(name = "value", description = "Dictionary Content Node 的 Value", required =
                    false),
            @Parameter(name = "label", description = "Dictionary Content Node 的 Label", required =
                    false)
    })
    @GetMapping("/dictionary-content-hierarchy/tree-querying-nodes")
    @ResponseBody
    public Response<List<TreeNode>> treeQueryingNodesOfDictionaryContentHierarchy(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject") String subject,
            @RequestParam(name = "dictionary_category_uid", required = false) Long dictionaryCategoryUid,
            @RequestParam(name = "value", required = false) String value,
            @RequestParam(name = "label", required = false) String label) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.treeQueryingNodesOfDictionaryContentHierarchy(
                        dictionaryCategoryUid, value, label,
                        operatingUserProfile));
    }

    @ApiOperation("树形列出指定 Dictionary category 的 Dictionary content hierarchy 上的第1级节点，以及输入选中节点的完整层级")
    @Parameters(value = {
            @Parameter(name = "client_id", description = "使用 Open API 的 Client 的 'ID'", required = true),
            @Parameter(name = "access_token", description = "使用 Open API 的 Client 的 '访问凭证'", required = true),
            @Parameter(name = "subject", description = "使用 Open API 的 Client 的 '主体'", required = false),
            @Parameter(name = "dictionary_category_uid", description = "Dictionary Category 的 UID", required =
                    false),
            @Parameter(name = "value", description = "Dictionary Content Node 的 UID", required =
                    false),
            @Parameter(name = "value", description = "Dictionary Content Node 的 Value", required =
                    false),
            @Parameter(name = "label", description = "Dictionary Content Node 的 Label", required =
                    false)
    })
    @GetMapping("/dictionary-content-hierarchy/tree-listing-first-level-w-selected")
    @ResponseBody
    public Response<List<TreeNode>> treeListingFirstLevelOfDictionaryContentHierarchyWithTaggingSelected(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "access_token") String accessToken,
            @RequestParam(name = "subject") String subject,
            @RequestParam(name = "dictionary_category_uid") Long dictionaryCategoryUid,
            @RequestParam(name = "uid", required = false) List<Long> uidList,
            @RequestParam(name = "value", required = false) List<String> valueList,
            @RequestParam(name = "label", required = false) List<String> labelList) throws Exception {
        //
        // Step 1, Open api authentication & retrieving operating user's profile
        //
        this.openApiAuthService.authenticate(clientId, accessToken);
        UserProfile operatingUserProfile = this.openApiAuthService.getUserProfile(clientId, subject);

        //
        // Step 2, core-processing
        //

        // 业务逻辑
        return Response.buildSuccess(
                this.dictionaryService.treeListingFirstLevelOfDictionaryContentHierarchyWithTaggingSelected(
                        dictionaryCategoryUid, uidList, valueList, labelList, operatingUserProfile));
    }

}
