package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.UserRoleDo;
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
public interface UserRoleRepository extends PagingAndSortingRepository<UserRoleDo, Long> {
    List<UserRoleDo> findByUserUid(Long userUid);

    List<UserRoleDo> findByUserUidIn(List<Long> userUidList);


    List<UserRoleDo> findByRoleUidIn(List<Long> roleUidList);


    Page<UserRoleDo> findAll(Specification<UserRoleDo> specification, Pageable pageable);

    List<UserRoleDo> findAll(Specification<UserRoleDo> specification, Sort sort);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM UserRoleDo u WHERE u.userUid = ?1")
    void deleteByUserUid(Long userUid);
}
