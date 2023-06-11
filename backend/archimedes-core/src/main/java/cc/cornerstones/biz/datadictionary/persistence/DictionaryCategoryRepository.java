package cc.cornerstones.biz.datadictionary.persistence;

import cc.cornerstones.biz.datadictionary.entity.DictionaryCategoryDo;
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
public interface DictionaryCategoryRepository extends PagingAndSortingRepository<DictionaryCategoryDo, Long> {
    boolean existsByUid(Long uid);

    @Query("SELECT COUNT(u) FROM DictionaryCategoryDo u WHERE u.name = ?1 AND u.parentUid IS NULL")
    Integer existsByNameWithoutParent(String name);

    @Query("SELECT COUNT(u) FROM DictionaryCategoryDo u WHERE u.name = ?1 AND u.parentUid = ?2")
    Integer existsByNameWithinParent(String name, Long parentUid);

    DictionaryCategoryDo findByUid(Long uid);

    List<DictionaryCategoryDo> findByParentUid(Long parentUid);

    @Query("SELECT u FROM DictionaryCategoryDo u WHERE u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<DictionaryCategoryDo> findAllWithoutParent();

    @Query("SELECT u FROM DictionaryCategoryDo u WHERE u.parentUid = ?1 ORDER BY u.sequence ASC")
    List<DictionaryCategoryDo> findAllWithinParent(Long parentUid);

    @Query("SELECT u FROM DictionaryCategoryDo u WHERE u.sequence > ?1 AND u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<DictionaryCategoryDo> findAfterSequenceWithoutParent(Float sequence);

    @Query("SELECT u FROM DictionaryCategoryDo u WHERE u.sequence > ?1 AND u.parentUid = ?2 ORDER BY u.sequence ASC")
    List<DictionaryCategoryDo> findAfterSequenceWithinParent(Float sequence, Long parentUid);

    Page<DictionaryCategoryDo> findAll(Specification<DictionaryCategoryDo> specification, Pageable pageable);

    List<DictionaryCategoryDo> findAll(Specification<DictionaryCategoryDo> specification, Sort sort);

    @Query("SELECT MAX(u.sequence) FROM DictionaryCategoryDo u WHERE u.parentUid IS NULL")
    Float findMaxSequenceWithoutParent();

    @Query("SELECT MAX(u.sequence) FROM DictionaryCategoryDo u WHERE u.parentUid = ?1")
    Float findMaxSequenceWithinParent(Long parentUid);

    List<DictionaryCategoryDo> findByUidIn(List<Long> uidList);
}
