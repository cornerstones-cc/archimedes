package cc.cornerstones.biz.distributedfile.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import cc.cornerstones.biz.distributedjob.share.constants.JobExecutorRoutingAlgorithmEnum;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.Date;

/**
 * Distributed File
 *
 * @author bbottong
 */
@TypeDef(name = "json", typeClass = JsonStringType.class)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = FileDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "file_id", columnList = "file_id", unique = false)
        })
@Where(clause = "is_deleted=0")
public class FileDo extends BaseDo {
    public static final String RESOURCE_NAME = "t7_file";
    public static final String RESOURCE_SYMBOL = "Distributed file";

    /**
     * 文件唯一标识
     */
    @Column(name = "file_id", length = 64)
    private String fileId;

    /**
     * 文件名
     */
    @Column(name = "file_name", length = 64)
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_length_in_bytes")
    private Long fileLengthInBytes;

    /**
     * 文件大小备注（自适应单位：GB/MB/KB/B）
     */
    @Column(name = "file_length_remark", length = 45)
    private String fileLengthRemark;

    /**
     * 文件所在 File Server 的 Hostname
     */
    @Column(name = "server_hostname")
    private String serverHostname;

    /**
     * 文件所在 File Server 的 IP Address
     */
    @Column(name = "server_ip_address", length = 45)
    private String serverIpAddress;

    /**
     * 文件完整路径
     */
    @Column(name = "file_path")
    private String filePath;
}