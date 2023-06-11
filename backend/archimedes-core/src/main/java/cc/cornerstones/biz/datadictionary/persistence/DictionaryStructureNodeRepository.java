package cc.cornerstones.biz.datadictionary.persistence;

import cc.cornerstones.biz.datadictionary.entity.DictionaryStructureNodeDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;


/**
 * @author bbottong
 */
public interface DictionaryStructureNodeRepository extends PagingAndSortingRepository<DictionaryStructureNodeDo, Long> {
    DictionaryStructureNodeDo findByUid(Long uid);

    boolean existsByDictionaryCategoryUidAndName(Long dictionaryCategoryUid, String name);

    boolean existsByParentUid(Long parentUid);

    List<DictionaryStructureNodeDo> findByDictionaryCategoryUid(Long dictionaryCategoryUid);

    Page<DictionaryStructureNodeDo> findAll(Specification<DictionaryStructureNodeDo> specification, Pageable pageable);

    @Query("SELECT u FROM DictionaryStructureNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.parentUid IS NULL")
    DictionaryStructureNodeDo findByDictionaryCategoryUidWithNullParentUid(Long dictionaryCategoryUid);

    @Query("SELECT u FROM DictionaryStructureNodeDo u WHERE u.dictionaryCategoryUid = ?1 AND u.parentUid = ?2")
    DictionaryStructureNodeDo findByDictionaryCategoryUidWithParentUid(Long dictionaryCategoryUid, Long parentUid);

}
