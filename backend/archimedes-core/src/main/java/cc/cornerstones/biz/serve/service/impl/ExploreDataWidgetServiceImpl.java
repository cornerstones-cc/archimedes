package cc.cornerstones.biz.serve.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datawidget.dto.DataWidgetDto;
import cc.cornerstones.biz.datawidget.entity.DataWidgetDo;
import cc.cornerstones.biz.datawidget.persistence.DataWidgetRepository;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import cc.cornerstones.biz.serve.service.inf.ExploreDataWidgetService;
import cc.cornerstones.biz.serve.service.assembly.datawidget.ExecuteDataWidgetHandler;
import cc.cornerstones.biz.share.types.QueryContentResult;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class ExploreDataWidgetServiceImpl implements ExploreDataWidgetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreDataWidgetServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataWidgetRepository dataWidgetRepository;

    @Override
    public List<DataWidgetDto> listingQueryDataWidgetsOfDataFacet(
            Long dataFacetUid,
            Long uid,
            String name,
            DataWidgetTypeEnum type,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        Specification<DataWidgetDo> specification = new Specification<DataWidgetDo>() {
            @Override
            public Predicate toPredicate(Root<DataWidgetDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (type != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("type"), type));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (sort == null) {
            sort = Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME));
        } else if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME));
        }

        List<DataWidgetDo> itemDoList = this.dataWidgetRepository.findAll(specification, sort);

        //
        // Step 2, core-processing
        //

        // generate serve characteristics
        List<DataWidgetDto> itemDtoList = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            DataWidgetDto itemDto = new DataWidgetDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            ExecuteDataWidgetHandler objectiveExecuteDataWidgetHandler = null;
            Map<String, ExecuteDataWidgetHandler> map =
                    this.applicationContext.getBeansOfType(ExecuteDataWidgetHandler.class);
            if (!CollectionUtils.isEmpty(map)) {
                for (Map.Entry<String, ExecuteDataWidgetHandler> entry : map.entrySet()) {
                    ExecuteDataWidgetHandler executeDataWidgetHandler = entry.getValue();
                    if (executeDataWidgetHandler.type().equals(itemDto.getType())) {
                        objectiveExecuteDataWidgetHandler = executeDataWidgetHandler;
                        break;
                    }
                }
            }
            if (objectiveExecuteDataWidgetHandler == null) {
                throw new AbcResourceConflictException(
                        String.format("cannot find execute data widget handler of data widget type:%s",
                                itemDto.getType()));
            }

            Object serveCharacteristics =
                    objectiveExecuteDataWidgetHandler.generateServeCharacteristics(dataFacetUid);

            itemDto.setServeCharacteristics(serveCharacteristics);

            itemDtoList.add(itemDto);
        });
        return itemDtoList;
    }

    @Override
    public DataWidgetDto getDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataWidgetDo dataWidgetDo = this.dataWidgetRepository.findByUid(uid);
        if (dataWidgetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataWidgetDo.RESOURCE_SYMBOL, uid));
        }
        DataWidgetDto dataWidgetDto = new DataWidgetDto();
        BeanUtils.copyProperties(dataWidgetDo, dataWidgetDto);

        //
        // Step 2, core-processing
        //

        // generate serve characteristics
        ExecuteDataWidgetHandler objectiveExecuteDataWidgetHandler = null;
        Map<String, ExecuteDataWidgetHandler> map =
                this.applicationContext.getBeansOfType(ExecuteDataWidgetHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, ExecuteDataWidgetHandler> entry : map.entrySet()) {
                ExecuteDataWidgetHandler executeDataWidgetHandler = entry.getValue();
                if (executeDataWidgetHandler.type().equals(dataWidgetDo.getType())) {
                    objectiveExecuteDataWidgetHandler = executeDataWidgetHandler;
                    break;
                }
            }
        }
        if (objectiveExecuteDataWidgetHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find execute data widget handler of data widget type:%s",
                            dataWidgetDo.getType()));
        }

        Object serveCharacteristics =
                objectiveExecuteDataWidgetHandler.generateServeCharacteristics(dataWidgetDo.getDataFacetUid());

        dataWidgetDto.setServeCharacteristics(serveCharacteristics);

        return dataWidgetDto;
    }

    @Override
    public QueryContentResult queryContent(
            Long uid,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataWidgetDo dataWidgetDo = this.dataWidgetRepository.findByUid(uid);
        if (dataWidgetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataWidgetDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        ExecuteDataWidgetHandler objectiveExecuteDataWidgetHandler = null;
        Map<String, ExecuteDataWidgetHandler> map =
                this.applicationContext.getBeansOfType(ExecuteDataWidgetHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, ExecuteDataWidgetHandler> entry : map.entrySet()) {
                ExecuteDataWidgetHandler executeDataWidgetHandler = entry.getValue();
                if (executeDataWidgetHandler.type().equals(dataWidgetDo.getType())) {
                    objectiveExecuteDataWidgetHandler = executeDataWidgetHandler;
                    break;
                }
            }
        }
        if (objectiveExecuteDataWidgetHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find execute data widget handler of data widget type:%s",
                            dataWidgetDo.getType()));
        }
        //
        // perform content query
        //
        return objectiveExecuteDataWidgetHandler.queryContent(
                dataWidgetDo.getDataFacetUid(),
                dataWidgetDo.getBuildCharacteristics(),
                request,
                operatingUserProfile);
    }

    @Override
    public Long exportContent(
            Long uid,
            JSONObject request,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataWidgetDo dataWidgetDo = this.dataWidgetRepository.findByUid(uid);
        if (dataWidgetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataWidgetDo.RESOURCE_SYMBOL,
                    uid));
        }

        //
        // Step 2, core-processing
        //
        ExecuteDataWidgetHandler objectiveExecuteDataWidgetHandler = null;
        Map<String, ExecuteDataWidgetHandler> map =
                this.applicationContext.getBeansOfType(ExecuteDataWidgetHandler.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, ExecuteDataWidgetHandler> entry : map.entrySet()) {
                ExecuteDataWidgetHandler executeDataWidgetHandler = entry.getValue();
                if (executeDataWidgetHandler.type().equals(dataWidgetDo.getType())) {
                    objectiveExecuteDataWidgetHandler = executeDataWidgetHandler;
                    break;
                }
            }
        }
        if (objectiveExecuteDataWidgetHandler == null) {
            throw new AbcResourceConflictException(
                    String.format("cannot find execute data widget handler of data widget type:%s",
                            dataWidgetDo.getType()));
        }
        //
        // perform content export
        //
        return objectiveExecuteDataWidgetHandler.exportContent(
                dataWidgetDo.getDataFacetUid(),
                dataWidgetDo.getBuildCharacteristics(),
                request,
                operatingUserProfile);
    }
}
