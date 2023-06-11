package cc.cornerstones.biz.administration.systemsettings.persistence;

import cc.cornerstones.biz.administration.systemsettings.entity.SettingsDo;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface SettingsRepository extends PagingAndSortingRepository<SettingsDo, Long> {
    SettingsDo findByName(String name);

    boolean existsByName(String name);
}
