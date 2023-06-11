package cc.cornerstones.biz.datatable.service.impl;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datatable.dto.DataIndexDto;
import cc.cornerstones.biz.datatable.entity.DataIndexDo;
import cc.cornerstones.biz.datatable.persistence.DataIndexRepository;
import cc.cornerstones.biz.datatable.service.inf.DataIndexService;
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

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataIndexServiceImpl implements DataIndexService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataIndexServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataIndexRepository dataIndexRepository;

    @Override
    public List<DataIndexDto> listingQueryDataIndexesOfDataTable(
            Long dataTableUid,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataIndexDo> specification = new Specification<DataIndexDo>() {
            @Override
            public Predicate toPredicate(Root<DataIndexDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataTableUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataTableUid"), dataTableUid));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc(BaseDo.ID_FIELD_NAME));
        }
        List<DataIndexDo> dataIndexDoList = this.dataIndexRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(dataIndexDoList)) {
            return null;
        }
        List<DataIndexDto> dataIndexDtoList = new ArrayList<>(dataIndexDoList.size());
        dataIndexDoList.forEach(dataIndexDo -> {
            DataIndexDto dataIndexDto = new DataIndexDto();
            BeanUtils.copyProperties(dataIndexDo, dataIndexDto);
            dataIndexDtoList.add(dataIndexDto);
        });

        return dataIndexDtoList;
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

        this.dataIndexRepository.deleteByDataSourceUid(event.getDataSourceDo().getUid());
    }
}
