package cc.cornerstones.biz.export.persistence;

import cc.cornerstones.biz.export.entity.ExportConfDo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface ExportConfRepository extends PagingAndSortingRepository<ExportConfDo, Long> {

    boolean existsByPropertyName(String propertyName);

    @Query("SELECT u.propertyValue FROM ExportConfDo u WHERE u.propertyName = ?1")
    String findByPropertyName(String propertyName);
}
