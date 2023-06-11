package cc.cornerstones.biz.datadictionary.persistence;

import cc.cornerstones.biz.datadictionary.entity.DictionaryBuildDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface DictionaryBuildRepository extends PagingAndSortingRepository<DictionaryBuildDo, Long> {
    boolean existsByDictionaryCategoryUid(Long dictionaryCategoryUid);

    DictionaryBuildDo findByDictionaryCategoryUid(Long dictionaryCategoryUid);

    Page<DictionaryBuildDo> findAll(Specification<DictionaryBuildDo> specification, Pageable pageable);
}
