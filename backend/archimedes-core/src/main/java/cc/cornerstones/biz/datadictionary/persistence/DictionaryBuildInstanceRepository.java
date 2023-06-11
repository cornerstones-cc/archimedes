package cc.cornerstones.biz.datadictionary.persistence;

import cc.cornerstones.biz.datadictionary.entity.DictionaryBuildInstanceDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface DictionaryBuildInstanceRepository extends PagingAndSortingRepository<DictionaryBuildInstanceDo, Long> {
    Page<DictionaryBuildInstanceDo> findAll(Specification<DictionaryBuildInstanceDo> specification, Pageable pageable);
}
