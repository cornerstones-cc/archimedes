package cc.cornerstones.biz.datafacet.entity;

import cc.cornerstones.almond.types.BaseDo;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.springframework.data.domain.Sort;

import javax.persistence.*;

/**
 * Sorting Data Field
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = SortingDataFieldDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "data_facet_uid", columnList = "data_facet_uid", unique = false)
        })
@Where(clause = "is_deleted=0")
public class SortingDataFieldDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_df_appearance_sorting_data_field";
    public static final String RESOURCE_SYMBOL = "Sorting data field";

    /**
     * Field Name
     */
    @Column(name = "file_name", length = 129)
    private String fieldName;

    /**
     * Sorting direction
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "direction")
    private Sort.Direction direction;

    /**
     * Sorting sequence
     * <p>
     * 在所有 Sorting 字段中的序号（从0开始计数）
     */
    @Column(name = "sorting_sequence")
    private Float sortingSequence;

    /**
     * 所属 Data Facet 的 UID
     */
    @Column(name = "data_facet_uid")
    private Long dataFacetUid;
}