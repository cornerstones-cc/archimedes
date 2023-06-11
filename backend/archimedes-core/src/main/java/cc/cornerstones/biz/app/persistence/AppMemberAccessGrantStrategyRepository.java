package cc.cornerstones.biz.app.persistence;

import cc.cornerstones.biz.app.entity.AppMemberAccessGrantStrategyDo;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface AppMemberAccessGrantStrategyRepository extends PagingAndSortingRepository<AppMemberAccessGrantStrategyDo, Long> {
    AppMemberAccessGrantStrategyDo findByAppUid(Long appUid);
}
