package cc.cornerstones.biz.app.persistence;

import cc.cornerstones.biz.app.entity.AppOpenApiSettingsDo;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface AppOpenApiSettingsRepository extends PagingAndSortingRepository<AppOpenApiSettingsDo, Long> {
    AppOpenApiSettingsDo findByAppUid(Long appUid);
}
