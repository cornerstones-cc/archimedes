package cc.cornerstones.biz.datasource.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * Database Server
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DatabaseServerDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DatabaseServerDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DatabaseServerDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_ds_database_server";
    public static final String RESOURCE_SYMBOL = "Database server";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Type
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type")
    private DatabaseServerTypeEnum type;

    /**
     * Host Profile
     */
    @Type(type = "json")
    @Column(name = "host_profile", columnDefinition = "json")
    private JSONObject hostProfile;

    /**
     * Hashed Host Profile
     */
    @Column(name = "hashed_host_profile", length = 255)
    private String hashedHostProfile;
}