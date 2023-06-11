package cc.cornerstones.biz.mockup;

import cc.cornerstones.almond.types.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "[Biz] Mockup / User synchronization service")
@RestController
@RequestMapping(value = "/mockup/usync")
public class MockupUserSynchronizationServiceApi {

    @Operation(summary = "列出所有用户")
    @GetMapping("/listing-users")
    @ResponseBody
    public Response<List<Map<String, Object>>> listingAllUsers(
            @RequestHeader("X-USERNAME") String username) throws Exception {
        List<Map<String, Object>> result = new LinkedList<>();

        Map<String, Object> item1 = new HashMap<>();
        item1.put("code", "6783");
        item1.put("name", "AK6783");
        item1.put("ad_number", "liuz13");
        item1.put("email", "zhiping.liu@effem.com");

        result.add(item1);

        //
        Map<String, Object> item2 = new HashMap<>();
        item2.put("code", "9876");
        item2.put("name", "AK9876");
        item2.put("ad_number", "szq56");
        item2.put("email", "ziqiang.song@effem.com");

        result.add(item2);

        //
        Map<String, Object> item3 = new HashMap<>();
        item3.put("code", "9823");
        item3.put("name", "AK9823");

        result.add(item3);

        //
        Map<String, Object> item4 = new HashMap<>();
        item4.put("code", "4563");
        item4.put("name", "AK4563");

        result.add(item4);

        return Response.buildSuccess(result);
    }
}
