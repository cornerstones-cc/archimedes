package cc.cornerstones.biz.serve.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.serve.share.constants.SessionStatusEnum;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Session
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = SessionDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = SessionDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class SessionDo extends BaseDo {
    public static final String RESOURCE_NAME = "a5_session";
    public static final String RESOURCE_SYMBOL = "Session";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * name
     */
    @Column(name = "name", length = 96)
    private String name;

    /**
     * Content
     */
    @Type(type = "json")
    @Column(name = "content", columnDefinition = "json")
    private JSONObject content;

    /**
     * Status
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status")
    private SessionStatusEnum status;

    /**
     * Data widget uid
     */
    @Column(name = "data_widget_uid")
    private Long dataWidgetUid;
}