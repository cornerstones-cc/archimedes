package cc.cornerstones.biz.datatable.service.impl;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import cc.cornerstones.biz.datatable.dto.CreateDataColumnDto;
import cc.cornerstones.biz.datatable.dto.DataColumnDto;
import cc.cornerstones.biz.datatable.entity.DataColumnDo;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import cc.cornerstones.biz.datatable.persistence.DataColumnRepository;
import cc.cornerstones.biz.datatable.service.inf.DataColumnService;
import cc.cornerstones.biz.share.event.DataSourceDeletedEvent;
import cc.cornerstones.biz.share.event.EventBusManager;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataColumnServiceImpl implements DataColumnService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataColumnServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataColumnRepository dataColumnRepository;

    @Override
    public List<DataColumnDto> listingQueryDataColumns(
            Long dataTableUid,
            Long dataColumnUid,
            String dataColumnName,
            DataColumnTypeEnum dataColumnType,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataColumnDo> specification = new Specification<DataColumnDo>() {
            @Override
            public Predicate toPredicate(Root<DataColumnDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataTableUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataTableUid"), dataTableUid));
                }
                if (dataColumnUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), dataColumnUid));
                }
                if (!ObjectUtils.isEmpty(dataColumnName)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + dataColumnName + "%"));
                }
                if (dataColumnType != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("type"), dataColumnType));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DataColumnDo> dataColumnDoList = this.dataColumnRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(dataColumnDoList)) {
            return null;
        }
        List<DataColumnDto> dataColumnDtoList = new ArrayList<>(dataColumnDoList.size());
        dataColumnDoList.forEach(dataColumnDo -> {
            DataColumnDto dataColumnDto = new DataColumnDto();
            BeanUtils.copyProperties(dataColumnDo, dataColumnDto);
            dataColumnDtoList.add(dataColumnDto);
        });

        return dataColumnDtoList;
    }

    @Override
    public void createDataColumns(
            DataTableDo dataTableDo,
            List<CreateDataColumnDto> createDataColumnDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        LocalDateTime now = LocalDateTime.now();
        List<DataColumnDo> dataColumnDoList = new ArrayList<>(createDataColumnDtoList.size());
        createDataColumnDtoList.forEach(createDataColumnDto -> {
            DataColumnDo dataColumnDo = new DataColumnDo();
            dataColumnDo.setUid(this.idHelper.getNextDistributedId(DataColumnDo.RESOURCE_NAME));
            dataColumnDo.setName(createDataColumnDto.getName());
            dataColumnDo.setObjectName(createDataColumnDto.getName()
                    .replaceAll("_", "__")
                    .replaceAll("\\s", "_")
                    .toLowerCase());
            dataColumnDo.setDescription(createDataColumnDto.getDescription());
            dataColumnDo.setType(createDataColumnDto.getType());
            dataColumnDo.setOrdinalPosition(createDataColumnDto.getOrdinalPosition());
            dataColumnDo.setDataTableUid(dataTableDo.getUid());
            dataColumnDo.setDataSourceUid(dataTableDo.getDataSourceUid());
            BaseDo.create(dataColumnDo, operatingUserProfile.getUid(), now);
            dataColumnDoList.add(dataColumnDo);
        });
        this.dataColumnRepository.saveAll(dataColumnDoList);
    }

    /**
     * 在 event bus 中注册成为 subscriber
     */
    @PostConstruct
    public void init() {
        this.eventBusManager.registerSubscriber(this);
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDataSourceDeletedEvent(DataSourceDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        this.dataColumnRepository.logicalDeleteByDataSourceUid(
                event.getDataSourceDo().getUid(),
                event.getOperatingUserProfile().getUid(),
                LocalDateTime.now());
    }
}
