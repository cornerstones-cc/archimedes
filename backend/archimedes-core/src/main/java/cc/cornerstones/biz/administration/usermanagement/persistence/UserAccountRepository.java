package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.UserAccountDo;
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
public interface UserAccountRepository extends PagingAndSortingRepository<UserAccountDo, Long> {
    boolean existsByAccountTypeUidAndName(Long accountTypeUid, String name);

    UserAccountDo findByAccountTypeUidAndName(Long accountTypeUid, String name);

    List<UserAccountDo> findByName(String name);

    Page<UserAccountDo> findAll(Specification<UserAccountDo> specification, Pageable pageable);

    List<UserAccountDo> findAll(Specification<UserAccountDo> specification, Sort sort);

    List<UserAccountDo> findByUserUid(Long userUid);

    List<UserAccountDo> findByUserUidIn(List<Long> userUidList);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM UserAccountDo u WHERE u.userUid = ?1")
    void deleteByUserUid(Long userUid);

    UserAccountDo findByUserUidAndAccountTypeUid(Long userUid, Long accountTypeUid);

    List<UserAccountDo> findByAccountTypeUid(Long accountTypeUid);
}
