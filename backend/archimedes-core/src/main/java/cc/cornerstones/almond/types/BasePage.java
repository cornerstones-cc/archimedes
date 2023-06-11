package cc.cornerstones.almond.types;

import com.fasterxml.jackson.databind.ser.Serializers;
import lombok.Data;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 分页查询数据传输
 * @Author: fdc
 * @Date: 2022-07-21
 */
@Data
public class BasePage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> content = new ArrayList();
    private Integer page;

    private Integer size;
    private long total;

    public BasePage(List<T> content, Pageable pageable, long total) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.total = total;
    }
}
