package cc.cornerstones.biz.app.persistence;

import cc.cornerstones.biz.app.entity.AppDataFacetDo;
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
public interface AppDataFacetRepository extends PagingAndSortingRepository<AppDataFacetDo, Long> {
    boolean existsByUid(Long uid);

    @Query("SELECT COUNT(u) FROM AppDataFacetDo u WHERE u.appUid = ?1 AND u.name = ?2 AND u.parentUid IS NULL")
    Integer existsByAppUidAndNameWithoutParent(Long appUid, String name);

    @Query("SELECT COUNT(u) FROM AppDataFacetDo u WHERE u.appUid = ?1 AND u.name = ?2 AND u.parentUid = ?3")
    Integer existsByAppUidAndNameWithinParent(Long appUid, String name, Long parentUid);

    AppDataFacetDo findByUid(Long uid);

    List<AppDataFacetDo> findByParentUid(Long parentUid);

    @Query("SELECT u FROM AppDataFacetDo u WHERE u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<AppDataFacetDo> findAllWithoutParent();

    @Query("SELECT u FROM AppDataFacetDo u WHERE u.parentUid = ?1 ORDER BY u.sequence ASC")
    List<AppDataFacetDo> findAllWithinParent(Long parentUid);

    @Query("SELECT u FROM AppDataFacetDo u WHERE u.sequence > ?1 AND u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<AppDataFacetDo> findAfterSequenceWithoutParent(Float sequence);

    @Query("SELECT u FROM AppDataFacetDo u WHERE u.sequence > ?1 AND u.parentUid = ?2 ORDER BY u.sequence ASC")
    List<AppDataFacetDo> findAfterSequenceWithinParent(Float sequence, Long parentUid);

    Page<AppDataFacetDo> findAll(Specification<AppDataFacetDo> specification, Pageable pageable);

    List<AppDataFacetDo> findAll(Specification<AppDataFacetDo> specification, Sort sort);

    @Query("SELECT MAX(u.sequence) FROM AppDataFacetDo u WHERE u.parentUid IS NULL")
    Float findMaxSequenceWithoutParent();

    @Query("SELECT MAX(u.sequence) FROM AppDataFacetDo u WHERE u.parentUid = ?1")
    Float findMaxSequenceWithinParent(Long parentUid);

    Iterable<AppDataFacetDo> findAllByAppUid(Long appUid);

    List<AppDataFacetDo> findByAppUidAndUidIn(Long appUid, List<Long> uidList);

    @Query("SELECT COUNT(u) FROM AppDataFacetDo u WHERE u.appUid = ?1 AND u.dataFacetUid = ?2 AND u.parentUid IS " +
            "NULL")
    Integer existsByAppUidAndDataFacetUidWithoutParent(Long appUid, Long dataFacetUid);

    @Query("SELECT COUNT(u) FROM AppDataFacetDo u WHERE u.appUid = ?1 AND u.dataFacetUid = ?2 AND u.parentUid = ?3")
    Integer existsByAppUidAndDataFacetUidWithinParent(Long appUid, Long dataFacetUid, Long parentUid);

    List<AppDataFacetDo> findByDataFacetUid(Long dataFacetUid);

    List<AppDataFacetDo> findByAppUid(Long appUid);
}
