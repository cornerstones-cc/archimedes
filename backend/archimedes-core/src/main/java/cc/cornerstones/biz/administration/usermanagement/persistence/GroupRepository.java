package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.GroupDo;
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
public interface GroupRepository extends PagingAndSortingRepository<GroupDo, Long> {
    boolean existsByUid(Long uid);

    @Query("SELECT COUNT(u) FROM GroupDo u WHERE u.name = ?1 AND u.parentUid IS NULL")
    Integer existsByNameWithoutParent(String name);

    @Query("SELECT COUNT(u) FROM GroupDo u WHERE u.name = ?1 AND u.parentUid = ?2")
    Integer existsByNameWithinParent(String name, Long parentUid);

    GroupDo findByUid(Long uid);

    List<GroupDo> findByParentUid(Long parentUid);

    @Query("SELECT u FROM GroupDo u WHERE u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<GroupDo> findAllWithoutParent();

    @Query("SELECT u FROM GroupDo u WHERE u.parentUid = ?1 ORDER BY u.sequence ASC")
    List<GroupDo> findAllWithinParent(Long parentUid);

    @Query("SELECT u FROM GroupDo u WHERE u.sequence > ?1 AND u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<GroupDo> findAfterSequenceWithoutParent(Float sequence);

    @Query("SELECT u FROM GroupDo u WHERE u.sequence > ?1 AND u.parentUid = ?2 ORDER BY u.sequence ASC")
    List<GroupDo> findAfterSequenceWithinParent(Float sequence, Long parentUid);

    Page<GroupDo> findAll(Specification<GroupDo> specification, Pageable pageable);

    List<GroupDo> findAll(Specification<GroupDo> specification, Sort sort);

    @Query("SELECT MAX(u.sequence) FROM GroupDo u WHERE u.parentUid IS NULL")
    Float findMaxSequenceWithoutParent();

    @Query("SELECT MAX(u.sequence) FROM GroupDo u WHERE u.parentUid = ?1")
    Float findMaxSequenceWithinParent(Long parentUid);
}
