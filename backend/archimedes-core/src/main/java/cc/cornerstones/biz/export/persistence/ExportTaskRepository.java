package cc.cornerstones.biz.export.persistence;

import cc.cornerstones.biz.export.entity.ExportTaskDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface ExportTaskRepository extends PagingAndSortingRepository<ExportTaskDo, Long> {
    boolean existsByTaskUid(Long taskUid);

    boolean existsByTaskName(String taskName);

    ExportTaskDo findByTaskUid(Long taskUid);

    Page<ExportTaskDo> findAll(Specification<ExportTaskDo> specification, Pageable pageable);

    List<ExportTaskDo> findAll(Specification<ExportTaskDo> specification, Sort sort);
}
