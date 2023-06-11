package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datafacet.dto.DataPermissionContentDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Data Permission
 *
 * @author bbottong
 */
@TinyId(bizType = DataPermissionDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataPermissionDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DataPermissionDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_data_permission";
    public static final String RESOURCE_SYMBOL = "Data permission";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Enabled
     */
    @Column(name = "is_enabled", columnDefinition = "boolean default true")
    private Boolean enabled;

    /**
     * Content
     *
     */
    @Type(type = "json")
    @Column(name = "content", columnDefinition = "json")
    private DataPermissionContentDto content;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}