package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.UserGroupDo;
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
public interface UserGroupRepository extends PagingAndSortingRepository<UserGroupDo, Long> {
    List<UserGroupDo> findByUserUid(Long userUid);

    List<UserGroupDo> findByUserUidIn(List<Long> userUidList);

    List<UserGroupDo> findByGroupUidIn(List<Long> groupUidList);

    Page<UserGroupDo> findAll(Specification<UserGroupDo> specification, Pageable pageable);

    List<UserGroupDo> findAll(Specification<UserGroupDo> specification, Sort sort);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM UserGroupDo u WHERE u.userUid = ?1")
    void deleteByUserUid(Long userUid);
}
