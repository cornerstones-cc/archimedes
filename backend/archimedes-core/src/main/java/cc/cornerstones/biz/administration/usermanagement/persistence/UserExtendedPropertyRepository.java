package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.UserExtendedPropertyDo;
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
public interface UserExtendedPropertyRepository extends PagingAndSortingRepository<UserExtendedPropertyDo, Long> {
    List<UserExtendedPropertyDo> findByUserUid(Long userUid);

    List<UserExtendedPropertyDo> findByUserUidIn(List<Long> userUidList);

    Page<UserExtendedPropertyDo> findAll(Specification<UserExtendedPropertyDo> specification, Pageable pageable);

    List<UserExtendedPropertyDo> findAll(Specification<UserExtendedPropertyDo> specification, Sort sort);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM UserExtendedPropertyDo u WHERE u.userUid = ?1")
    void deleteByUserUid(Long userUid);
}
