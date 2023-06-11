package cc.cornerstones.biz.app.persistence;

import cc.cornerstones.biz.app.entity.AppMemberInviteStrategyDo;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface AppMemberInviteStrategyRepository extends PagingAndSortingRepository<AppMemberInviteStrategyDo, Long> {
    AppMemberInviteStrategyDo findByAppUid(Long appUid);
}
