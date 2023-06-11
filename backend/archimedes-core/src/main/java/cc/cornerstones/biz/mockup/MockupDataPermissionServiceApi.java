package cc.cornerstones.biz.mockup;

import cc.cornerstones.almond.types.Response;
import cc.cornerstones.almond.types.TreeNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Tag(name = "[Biz] Mockup / Data permission service")
@RestController
@RequestMapping(value = "/mockup/dataperm")
public class MockupDataPermissionServiceApi {

    @Operation(summary = "列出所有资源类目(resource categories)及每个资源类目(resource category)的资源结构Hierarchy(resource structure hierarchy)")
    @GetMapping("/resource-structure")
    @ResponseBody
    public Response<TreeNode> listingAllNodesOfResourceStructureHierarchy(
            @RequestHeader("X-USERNAME") String username) throws Exception {
        TreeNode result = new TreeNode();
        result.setName("ROOT");
        result.setDescription("ROOT");
        result.setType("ROOT");
        result.setUid(-1L);
        result.setIds(UUID.randomUUID().toString());
        result.setChildren(new LinkedList<>());

        //
        // resource category 1
        //
        TreeNode resourceCategory1 = new TreeNode();
        resourceCategory1.setName("玛氏地理");
        resourceCategory1.setDescription("玛氏地理");
        resourceCategory1.setType("resource_category");
        resourceCategory1.setUid(10L);
        resourceCategory1.setIds(UUID.randomUUID().toString());
        resourceCategory1.setChildren(new LinkedList<>());

        result.getChildren().add(resourceCategory1);

        TreeNode resourceStructureNode11 = new TreeNode();
        resourceStructureNode11.setName("大区");
        resourceStructureNode11.setDescription("大区");
        resourceStructureNode11.setType("entity");
        resourceStructureNode11.setUid(11L);
        resourceStructureNode11.setIds(UUID.randomUUID().toString());
        resourceStructureNode11.setChildren(new LinkedList<>());

        resourceCategory1.getChildren().add(resourceStructureNode11);

        TreeNode resourceStructureNode12 = new TreeNode();
        resourceStructureNode12.setName("省份");
        resourceStructureNode12.setDescription("省份");
        resourceStructureNode12.setType("entity");
        resourceStructureNode12.setUid(12L);
        resourceStructureNode12.setIds(UUID.randomUUID().toString());
        resourceStructureNode12.setChildren(new LinkedList<>());

        resourceStructureNode11.getChildren().add(resourceStructureNode12);

        TreeNode resourceStructureNode13 = new TreeNode();
        resourceStructureNode13.setName("城市群");
        resourceStructureNode13.setDescription("城市群");
        resourceStructureNode13.setType("entity");
        resourceStructureNode13.setUid(13L);
        resourceStructureNode13.setIds(UUID.randomUUID().toString());
        resourceStructureNode13.setChildren(new LinkedList<>());

        resourceStructureNode12.getChildren().add(resourceStructureNode13);

        TreeNode resourceStructureNode14 = new TreeNode();
        resourceStructureNode14.setName("城市");
        resourceStructureNode14.setDescription("城市");
        resourceStructureNode14.setType("entity");
        resourceStructureNode14.setUid(14L);
        resourceStructureNode14.setIds(UUID.randomUUID().toString());

        resourceStructureNode13.getChildren().add(resourceStructureNode14);

        //
        // resource category 2
        //
        TreeNode resourceCategory2 = new TreeNode();
        resourceCategory2.setName("门店渠道");
        resourceCategory2.setDescription("门店渠道");
        resourceCategory2.setType("resource_category");
        resourceCategory2.setUid(20L);
        resourceCategory2.setIds(UUID.randomUUID().toString());
        resourceCategory2.setChildren(new LinkedList<>());

        result.getChildren().add(resourceCategory2);

        TreeNode resourceStructureNode21 = new TreeNode();
        resourceStructureNode21.setName("一级渠道");
        resourceStructureNode21.setDescription("一级渠道");
        resourceStructureNode21.setType("entity");
        resourceStructureNode21.setUid(21L);
        resourceStructureNode21.setIds(UUID.randomUUID().toString());
        resourceStructureNode21.setChildren(new LinkedList<>());

        resourceCategory2.getChildren().add(resourceStructureNode21);

        TreeNode resourceStructureNode22 = new TreeNode();
        resourceStructureNode22.setName("二级渠道");
        resourceStructureNode22.setDescription("二级渠道");
        resourceStructureNode22.setType("entity");
        resourceStructureNode22.setUid(22L);
        resourceStructureNode22.setIds(UUID.randomUUID().toString());
        resourceStructureNode22.setChildren(new LinkedList<>());

        resourceStructureNode21.getChildren().add(resourceStructureNode22);

        TreeNode resourceStructureNode23 = new TreeNode();
        resourceStructureNode23.setName("三级渠道");
        resourceStructureNode23.setDescription("三级渠道");
        resourceStructureNode23.setType("entity");
        resourceStructureNode23.setUid(23L);
        resourceStructureNode23.setIds(UUID.randomUUID().toString());
        resourceStructureNode23.setChildren(new LinkedList<>());

        resourceStructureNode22.getChildren().add(resourceStructureNode23);

        return Response.buildSuccess(result);
    }

    @Operation(summary = "列出指定用户在指定资源类目(resource category)领域所授权的内容Hierarchy(resource content hierarchy)")
    @Parameters(value = {
            @Parameter(name = "username", description = "指定用户（人员工号）", required = false),
            @Parameter(name = "resource_category_uid", description = "指定资源类目", required = false)
    })
    @GetMapping("/resource-content")
    @ResponseBody
    public Response<TreeNode> listingAllNodesOfResourceContentHierarchy(
            @RequestParam(name = "username", required = true) String username,
            @RequestParam(name = "resource_category_uid", required = true) List<Long> resourceCategoryUidList) throws Exception {
        TreeNode result = new TreeNode();
        result.setName("ROOT");
        result.setDescription("ROOT");
        result.setType("ROOT");
        result.setUid(-1L);
        result.setIds(UUID.randomUUID().toString());
        result.setChildren(new LinkedList<>());

        if (resourceCategoryUidList.contains(10L)) {
            TreeNode resourceCategory1 = new TreeNode();
            resourceCategory1.setName("玛氏地理");
            resourceCategory1.setDescription("玛氏地理");
            resourceCategory1.setType("resource_category");
            resourceCategory1.setUid(10L);
            resourceCategory1.setIds(UUID.randomUUID().toString());
            resourceCategory1.setChildren(new LinkedList<>());

            result.getChildren().add(resourceCategory1);

            TreeNode resourceContentNode11 = new TreeNode();
            resourceContentNode11.setName("华南大区");
            resourceContentNode11.setDescription("华南大区");
            resourceContentNode11.setType("resource_content");
            resourceContentNode11.setUid(11L);
            resourceContentNode11.setIds(UUID.randomUUID().toString());
            resourceContentNode11.setChildren(new LinkedList<>());

            resourceCategory1.getChildren().add(resourceContentNode11);

            TreeNode resourceContentNode12 = new TreeNode();
            resourceContentNode12.setName("广东");
            resourceContentNode12.setDescription("广东");
            resourceContentNode12.setType("resource_content");
            resourceContentNode12.setUid(12L);
            resourceContentNode12.setIds(UUID.randomUUID().toString());
            resourceContentNode12.setChildren(new LinkedList<>());

            resourceContentNode11.getChildren().add(resourceContentNode12);

            TreeNode resourceContentNode13 = new TreeNode();
            resourceContentNode13.setName("广州");
            resourceContentNode13.setDescription("广州");
            resourceContentNode13.setType("resource_content");
            resourceContentNode13.setUid(13L);
            resourceContentNode13.setIds(UUID.randomUUID().toString());
            resourceContentNode13.setChildren(new LinkedList<>());

            resourceContentNode12.getChildren().add(resourceContentNode13);

            TreeNode resourceContentNode14 = new TreeNode();
            resourceContentNode14.setName("清远");
            resourceContentNode14.setDescription("清远");
            resourceContentNode14.setType("resource_content");
            resourceContentNode14.setUid(14L);
            resourceContentNode14.setIds(UUID.randomUUID().toString());

            resourceContentNode13.getChildren().add(resourceContentNode14);

            TreeNode resourceContentNode132 = new TreeNode();
            resourceContentNode132.setName("深圳");
            resourceContentNode132.setDescription("深圳");
            resourceContentNode132.setType("resource_content");
            resourceContentNode132.setUid(132L);
            resourceContentNode132.setIds(UUID.randomUUID().toString());

            resourceContentNode12.getChildren().add(resourceContentNode132);

            TreeNode resourceContentNode112 = new TreeNode();
            resourceContentNode112.setName("华中大区");
            resourceContentNode112.setDescription("华中大区");
            resourceContentNode112.setType("resource_content");
            resourceContentNode112.setUid(112L);
            resourceContentNode112.setIds(UUID.randomUUID().toString());
            resourceContentNode112.setChildren(new LinkedList<>());

            resourceCategory1.getChildren().add(resourceContentNode112);
        }

        if (resourceCategoryUidList.contains(20L)) {
            TreeNode resourceCategory2 = new TreeNode();
            resourceCategory2.setName("门店渠道");
            resourceCategory2.setDescription("门店渠道");
            resourceCategory2.setType("resource_category");
            resourceCategory2.setUid(20L);
            resourceCategory2.setIds(UUID.randomUUID().toString());
            resourceCategory2.setChildren(new LinkedList<>());

            result.getChildren().add(resourceCategory2);

            TreeNode resourceContentNode21 = new TreeNode();
            resourceContentNode21.setName("A渠道");
            resourceContentNode21.setDescription("A渠道");
            resourceContentNode21.setType("resource_content");
            resourceContentNode21.setUid(21L);
            resourceContentNode21.setIds(UUID.randomUUID().toString());
            resourceContentNode21.setChildren(new LinkedList<>());

            resourceCategory2.getChildren().add(resourceContentNode21);

            TreeNode resourceContentNode22 = new TreeNode();
            resourceContentNode22.setName("B渠道");
            resourceContentNode22.setDescription("B渠道");
            resourceContentNode22.setType("resource_content");
            resourceContentNode22.setUid(22L);
            resourceContentNode22.setIds(UUID.randomUUID().toString());
            resourceContentNode22.setChildren(new LinkedList<>());

            resourceCategory2.getChildren().add(resourceContentNode22);
        }

        return Response.buildSuccess(result);
    }
}
