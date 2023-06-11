package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.GroupRoleDo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author bbottong
 */
public interface GroupRoleRepository extends PagingAndSortingRepository<GroupRoleDo, Long> {
    List<GroupRoleDo> findByGroupUid(Long groupUid);

    List<GroupRoleDo> findByGroupUidIn(List<Long> groupUidList);

    @Query("SELECT u FROM GroupRoleDo u WHERE u.groupUid IS NULL")
    List<GroupRoleDo> findAllWithoutGroupUid();

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM GroupRoleDo u WHERE u.groupUid = ?1")
    void deleteByGroupUid(Long groupUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM GroupRoleDo u WHERE u.groupUid IS NULL")
    void deleteAllWithoutGroup();
}
