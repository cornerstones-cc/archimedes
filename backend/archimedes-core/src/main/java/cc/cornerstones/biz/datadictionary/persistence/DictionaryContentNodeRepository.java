package cc.cornerstones.biz.datadictionary.persistence;

import cc.cornerstones.biz.datadictionary.entity.DictionaryContentNodeDo;
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
public interface DictionaryContentNodeRepository extends PagingAndSortingRepository<DictionaryContentNodeDo, Long> {
    boolean existsByUid(Long uid);

    DictionaryContentNodeDo findByUid(Long uid);

    List<DictionaryContentNodeDo> findByUidIn(List<Long> uidList);

    @Query("SELECT u FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.version = ?2 AND u" +
            ".parentUid IS NULL")
    List<DictionaryContentNodeDo> findByDictionaryCategoryUidAndVersionAndNullParentUid(
            Long dictionaryCategoryUid,
            Long version);

    List<DictionaryContentNodeDo> findByParentUid(Long parentUid);


    @Query("SELECT u FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.version = ?2 AND u.parentUid IS NULL AND u" +
            ".value = ?3")
    List<DictionaryContentNodeDo> findByDictionaryCategoryUidAndVersionAndNullParentUidAndValue(
            Long dictionaryCategoryUid,
            Long version,
            String value);


    @Query("SELECT u FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.version = ?2 AND u" +
            ".parentUid IS NULL " +
            "AND u" +
            ".value IS NULL")
    List<DictionaryContentNodeDo> findByDictionaryCategoryUidAndVersionAndNullParentUidAndNullValue(
            Long dictionaryCategoryUid,
            Long version);


    @Query("SELECT u FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.version = ?2 AND u.parentUid IS NULL AND u" +
            ".label = ?3")
    List<DictionaryContentNodeDo> findByDictionaryCategoryUidAndVersionAndNullParentUidAndLabel(
            Long dictionaryCategoryUid,
            Long version,
            String label);


    @Query("SELECT u FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.version = ?2 AND u" +
            ".parentUid =?3 AND u" +
            ".value = ?4")
    List<DictionaryContentNodeDo> findByDictionaryCategoryUidAndVersionAndParentUidAndValue(
            Long dictionaryCategoryUid,
            Long version,
            Long parentUid,
            String value);

    @Query("SELECT u FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.version = ?2 AND u.parentUid =?2 AND u" +
            ".value IS NULL")
    List<DictionaryContentNodeDo> findByDictionaryCategoryUidAndVersionAndParentUidAndNullValue(
            Long dictionaryCategoryUid,
            Long version,
            Long parentUid);


    @Query("SELECT u FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.version = ?2 AND u" +
            ".parentUid =?3 AND u" +
            ".label = ?4")
    List<DictionaryContentNodeDo> findByDictionaryCategoryUidAndVersionAndParentUidAndLabel(
            Long dictionaryCategoryUid,
            Long version,
            Long parentUid,
            String label);

    boolean existsByParentUid(Long parentUid);

    Page<DictionaryContentNodeDo> findByDictionaryCategoryUidAndVersion(
            Long dictionaryCategoryUid,
            Long version,
            Pageable pageable);

    Page<DictionaryContentNodeDo> findAll(
            Specification<DictionaryContentNodeDo> specification,
            Pageable pageable);

    List<DictionaryContentNodeDo> findAll(
            Specification<DictionaryContentNodeDo> specification,
            Sort sort);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.version = ?2")
    void deleteByDictionaryCategoryUidAndVersion(
            Long dictionaryCategoryUid,
            Long version);


    @Query("SELECT MAX(u.sequence) FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.parentUid IS NULL")
    Float findMaxSequenceWithoutParent(Long dictionaryCategoryUid);

    @Query("SELECT MAX(u.sequence) FROM DictionaryContentNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.parentUid " +
            "= ?2")
    Float findMaxSequenceWithinParent(Long dictionaryCategoryUid, Long parentUid);
}
