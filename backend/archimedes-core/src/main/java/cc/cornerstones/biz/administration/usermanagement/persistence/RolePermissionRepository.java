package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.RolePermissionDo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author bbottong
 */
public interface RolePermissionRepository extends PagingAndSortingRepository<RolePermissionDo, Long> {
    List<RolePermissionDo> findByRoleUid(Long roleUid);

    List<RolePermissionDo> findByRoleUidIn(List<Long> roleUidList);

    @Query("SELECT u FROM RolePermissionDo u WHERE u.roleUid IS NULL")
    List<RolePermissionDo> findAllWithoutRoleUid();

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM RolePermissionDo u WHERE u.roleUid = ?1")
    void deleteByRoleUid(Long roleUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM RolePermissionDo u WHERE u.roleUid IS NULL")
    void deleteAllWithoutRole();
}
