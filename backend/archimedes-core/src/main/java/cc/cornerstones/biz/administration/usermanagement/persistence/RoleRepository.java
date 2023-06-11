package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.RoleDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface RoleRepository extends PagingAndSortingRepository<RoleDo, Long> {
    boolean existsByUid(Long uid);

    @Query("SELECT COUNT(u) FROM RoleDo u WHERE u.name = ?1 AND u.parentUid IS NULL")
    Integer existsByNameWithoutParent(String name);

    @Query("SELECT COUNT(u) FROM RoleDo u WHERE u.name = ?1 AND u.parentUid = ?2")
    Integer existsByNameWithinParent(String name, Long parentUid);

    RoleDo findByUid(Long uid);

    List<RoleDo> findByParentUid(Long parentUid);

    @Query("SELECT u FROM RoleDo u WHERE u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<RoleDo> findAllWithoutParent();

    @Query("SELECT u FROM RoleDo u WHERE u.parentUid = ?1 ORDER BY u.sequence ASC")
    List<RoleDo> findAllWithinParent(Long parentUid);

    @Query("SELECT u FROM RoleDo u WHERE u.sequence > ?1 AND u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<RoleDo> findAfterSequenceWithoutParent(Float sequence);

    @Query("SELECT u FROM RoleDo u WHERE u.sequence > ?1 AND u.parentUid = ?2 ORDER BY u.sequence ASC")
    List<RoleDo> findAfterSequenceWithinParent(Float sequence, Long parentUid);

    Page<RoleDo> findAll(Specification<RoleDo> specification, Pageable pageable);

    List<RoleDo> findAll(Specification<RoleDo> specification, Sort sort);

    @Query("SELECT MAX(u.sequence) FROM RoleDo u WHERE u.parentUid IS NULL")
    Float findMaxSequenceWithoutParent();

    @Query("SELECT MAX(u.sequence) FROM RoleDo u WHERE u.parentUid = ?1")
    Float findMaxSequenceWithinParent(Long parentUid);
}
