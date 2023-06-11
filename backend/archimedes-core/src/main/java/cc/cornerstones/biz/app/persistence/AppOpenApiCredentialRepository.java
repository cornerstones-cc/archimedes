package cc.cornerstones.biz.app.persistence;

import cc.cornerstones.biz.app.entity.AppOpenApiCredentialDo;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface AppOpenApiCredentialRepository extends PagingAndSortingRepository<AppOpenApiCredentialDo, Long> {
    AppOpenApiCredentialDo findByAppUid(Long appUid);

    AppOpenApiCredentialDo findByAppKey(String appKey);
}
