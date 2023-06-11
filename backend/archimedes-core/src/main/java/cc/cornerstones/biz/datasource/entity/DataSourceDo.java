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
 * Data Source
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = DataSourceDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = DataSourceDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class DataSourceDo extends BaseDo {
    public static final String RESOURCE_NAME = "a3_ds_data_source";
    public static final String RESOURCE_SYMBOL = "Data source";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * Name
     * <p>
     * A name is used to identify the object.
     */
    @Column(name = "name", length = 129)
    private String name;

    /**
     * Object Name
     * <p>
     * An object name is how the object is referenced programmatically.
     */
    @Column(name = "object_name", length = 150)
    private String objectName;

    /**
     * Description
     * <p>
     * A meaning description helps you remembers the differences between objects.
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Type
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type")
    private DatabaseServerTypeEnum type;

    /**
     * Connection Profile
     */
    @Type(type = "json")
    @Column(name = "connection_profile", columnDefinition = "json")
    private JSONObject connectionProfile;

    /**
     * Connection Profile hashed string
     */
    @Column(name = "connection_profile_hashed_string", length = 255)
    private String connectionProfileHashedString;

    /**
     * 所属 Database Server 的 UID
     */
    @Column(name = "database_server_uid")
    private Long databaseServerUid;
}