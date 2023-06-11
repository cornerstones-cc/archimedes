package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.FunctionDo;
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
public interface FunctionRepository extends PagingAndSortingRepository<FunctionDo, Long> {
    boolean existsByUid(Long uid);

    @Query("SELECT COUNT(u) FROM FunctionDo u WHERE u.name = ?1 AND u.parentUid IS NULL")
    Integer existsByNameWithoutParent(String name);

    @Query("SELECT COUNT(u) FROM FunctionDo u WHERE u.name = ?1 AND u.parentUid = ?2")
    Integer existsByNameWithinParent(String name, Long parentUid);

    FunctionDo findByUid(Long uid);

    List<FunctionDo> findByParentUid(Long parentUid);

    @Query("SELECT u FROM FunctionDo u WHERE u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<FunctionDo> findAllWithoutParent();

    @Query("SELECT u FROM FunctionDo u WHERE u.parentUid = ?1 ORDER BY u.sequence ASC")
    List<FunctionDo> findAllWithinParent(Long parentUid);

    @Query("SELECT u FROM FunctionDo u WHERE u.sequence > ?1 AND u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<FunctionDo> findAfterSequenceWithoutParent(Float sequence);

    @Query("SELECT u FROM FunctionDo u WHERE u.sequence > ?1 AND u.parentUid = ?2 ORDER BY u.sequence ASC")
    List<FunctionDo> findAfterSequenceWithinParent(Float sequence, Long parentUid);

    Page<FunctionDo> findAll(Specification<FunctionDo> specification, Pageable pageable);

    List<FunctionDo> findAll(Specification<FunctionDo> specification, Sort sort);

    @Query("SELECT MAX(u.sequence) FROM FunctionDo u WHERE u.parentUid IS NULL")
    Float findMaxSequenceWithoutParent();

    @Query("SELECT MAX(u.sequence) FROM FunctionDo u WHERE u.parentUid = ?1")
    Float findMaxSequenceWithinParent(Long parentUid);
}
