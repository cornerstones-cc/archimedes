package cc.cornerstones.biz.datapage.persistence;

import cc.cornerstones.biz.datapage.entity.DataPageDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface DataPageRepository extends PagingAndSortingRepository<DataPageDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByName(String name);

    DataPageDo findByUid(Long uid);

    Page<DataPageDo> findAll(Specification<DataPageDo> specification, Pageable pageable);

    List<DataPageDo> findAll(Specification<DataPageDo> specification, Sort sort);
}
