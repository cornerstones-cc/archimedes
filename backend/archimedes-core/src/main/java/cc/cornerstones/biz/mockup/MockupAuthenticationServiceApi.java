package cc.cornerstones.biz.mockup;

import cc.cornerstones.almond.types.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.apache.http.NameValuePair;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Tag(name = "[Biz] Mockup / Authentication service")
@RestController
@RequestMapping(value = "/mockup/authn")
public class MockupAuthenticationServiceApi {

    @Operation(summary = "获取 Access token")
    @GetMapping("/access-token")
    @ResponseBody
    public Response<Map<String, Object>> getAccessToken(
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "client_secret") String clientSecret,
            @RequestParam(name = "grant_type") String clientGrantType,
            @RequestParam(name = "redirect_uri") String redirectUri,
            @RequestParam(name = "code") String code) throws Exception {

        Map<String, Object> result = new HashMap<>();
        result.put("access_token", "xxx");
        result.put("expires_in", 1234567890);
        result.put("token_type", "yyy");

        return Response.buildSuccess(result);
    }

    @Operation(summary = "获取 User info")
    @GetMapping("/user-info")
    @ResponseBody
    public Response<Map<String, Object>> getUserInfo(
            @RequestParam(name = "access_token") String accessToken) throws Exception {

        Map<String, Object> result = new HashMap<>();
        result.put("name", "kkk");
        result.put("preferred_username", "ttt");
        result.put("adusername", "fff");

        return Response.buildSuccess(result);
    }
}
