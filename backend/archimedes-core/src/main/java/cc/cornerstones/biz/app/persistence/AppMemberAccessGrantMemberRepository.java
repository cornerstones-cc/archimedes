package cc.cornerstones.biz.app.persistence;

import cc.cornerstones.biz.app.entity.AppMemberAccessGrantMemberDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author bbottong
 */
public interface AppMemberAccessGrantMemberRepository extends PagingAndSortingRepository<AppMemberAccessGrantMemberDo, Long> {
    Page<AppMemberAccessGrantMemberDo> findAll(Specification<AppMemberAccessGrantMemberDo> specification, Pageable pageable);

    List<AppMemberAccessGrantMemberDo> findByAppUidAndUserUid(Long appUid, Long userUid);

    List<AppMemberAccessGrantMemberDo> findByAppUidAndUserUidIn(Long appUid, List<Long> userUidList);

    Iterable<AppMemberAccessGrantMemberDo> findAll(Specification<AppMemberAccessGrantMemberDo> specification, Sort sort);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM AppMemberAccessGrantMemberDo u WHERE u.appUid = ?1 AND u.userUid = ?2")
    void deleteByAppUidAndUserUid(Long appUid, Long userUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM AppMemberAccessGrantMemberDo u WHERE u.appUid = ?1 AND u.userUid IN ?2")
    void deleteByAppUidAndUserUidIn(Long appUid, List<Long> userUidList);

    List<AppMemberAccessGrantMemberDo> findByUserUid(Long userUid);

    List<AppMemberAccessGrantMemberDo> findByDataFacetHierarchyNodeUidIn(List<Long> dataFacetHierarchyNodeUidList);

    List<AppMemberAccessGrantMemberDo> findByAppUid(Long appUid);
}
