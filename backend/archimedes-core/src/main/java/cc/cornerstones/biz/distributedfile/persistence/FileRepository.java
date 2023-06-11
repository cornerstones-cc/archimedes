package cc.cornerstones.biz.distributedfile.persistence;

import cc.cornerstones.biz.distributedfile.entity.FileDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface FileRepository extends PagingAndSortingRepository<FileDo, Long> {
    boolean existsByFileId(String fileId);

    FileDo findByFileId(String fileId);

    Page<FileDo> findAll(Specification<FileDo> specification, Pageable pageable);

    Iterable<FileDo> findAll(Specification<FileDo> specification, Sort sort);
}
