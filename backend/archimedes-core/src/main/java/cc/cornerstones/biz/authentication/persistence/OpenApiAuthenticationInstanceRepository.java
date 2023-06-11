package cc.cornerstones.biz.authentication.persistence;

import cc.cornerstones.biz.authentication.entity.OpenApiAuthenticationInstanceDo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author bbottong
 */
public interface OpenApiAuthenticationInstanceRepository extends PagingAndSortingRepository<OpenApiAuthenticationInstanceDo, Long> {
    /**
     * 按 app uid 删除
     *
     * @param appUid
     */
    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM OpenApiAuthenticationInstanceDo u WHERE u.appUid = ?1")
    void deleteByAppUid(Long appUid);

    /**
     * 按 app uid 查找还未过期的
     *
     * @param appUid
     * @return
     */
    @Query("SELECT u FROM OpenApiAuthenticationInstanceDo u WHERE u.appUid = ?1 AND u.revoked = 0")
    List<OpenApiAuthenticationInstanceDo> findUnrevokedByAppUid(Long appUid);

    OpenApiAuthenticationInstanceDo findByAppUidAndRefreshToken(Long appUid, String refreshToken);
}
