package cc.cornerstones.biz.authentication.persistence;

import cc.cornerstones.biz.authentication.entity.UserAuthenticationInstanceDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author bbottong
 */
public interface UserAuthenticationInstanceRepository extends PagingAndSortingRepository<UserAuthenticationInstanceDo, Long> {
    /**
     * 按 user uid 删除
     *
     * @param userUid
     */
    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM UserAuthenticationInstanceDo u WHERE u.userUid = ?1")
    void deleteByUserUid(Long userUid);

    /**
     * 按 user uid 查找还未过期的
     *
     * @param userUid
     * @return
     */
    @Query("SELECT u FROM UserAuthenticationInstanceDo u WHERE u.userUid = ?1 AND u.revoked = 0")
    List<UserAuthenticationInstanceDo> findUnrevokedByUserUid(Long userUid);

    Page<UserAuthenticationInstanceDo> findAll(Specification<UserAuthenticationInstanceDo> specification, Pageable pageable);

}
