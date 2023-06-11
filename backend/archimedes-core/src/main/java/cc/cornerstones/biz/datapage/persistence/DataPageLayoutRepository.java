package cc.cornerstones.biz.datapage.persistence;

import cc.cornerstones.biz.datapage.entity.DataPageLayoutDo;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface DataPageLayoutRepository extends PagingAndSortingRepository<DataPageLayoutDo, Long> {
    DataPageLayoutDo findByDataPageUid(Long dataPageUid);
}
