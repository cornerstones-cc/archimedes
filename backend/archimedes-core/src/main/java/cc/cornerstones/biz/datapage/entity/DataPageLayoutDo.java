package cc.cornerstones.biz.datapage.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.biz.datapage.share.types.Layout;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Data page 的 Layout
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataPageLayoutDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DataPageLayoutDo extends BaseDo {
    public static final String RESOURCE_NAME = "a1_dp_data_page_layout";
    public static final String RESOURCE_SYMBOL = "Data page layout";

    /**
     * Layout
     */
    @Type(type = "json")
    @Column(name = "layout", columnDefinition = "json")
    private Layout layout;

    /**
     * 所属 Data page 的 UID
     */
    @Column(name = "data_page_uid")
    private Long dataPageUid;

}