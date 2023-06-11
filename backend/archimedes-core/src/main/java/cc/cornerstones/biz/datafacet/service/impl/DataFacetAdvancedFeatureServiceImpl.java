package cc.cornerstones.biz.datafacet.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datafacet.dto.AdvancedFeatureContentDto;
import cc.cornerstones.biz.datafacet.dto.AdvancedFeatureDto;
import cc.cornerstones.biz.datafacet.entity.AdvancedFeatureDo;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.persistence.AdvancedFeatureRepository;
import cc.cornerstones.biz.datafacet.persistence.DataFacetRepository;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetAdvancedFeatureService;
import cc.cornerstones.biz.share.event.*;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

@Service
public class DataFacetAdvancedFeatureServiceImpl implements DataFacetAdvancedFeatureService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFacetAdvancedFeatureServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private AdvancedFeatureRepository advancedFeatureRepository;

    @Override
    public AdvancedFeatureDto getAdvancedFeatureOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        AdvancedFeatureDo advancedFeatureDo = this.advancedFeatureRepository.findByDataFacetUid(dataFacetUid);
        if (advancedFeatureDo == null) {
            return null;
        }

        AdvancedFeatureDto advancedFeatureDto = new AdvancedFeatureDto();
        BeanUtils.copyProperties(advancedFeatureDo, advancedFeatureDto);
        return advancedFeatureDto;
    }

    @Override
    public void replaceAdvancedFeatureOfDataFacet(
            Long dataFacetUid,
            AdvancedFeatureDto advancedFeatureDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        AdvancedFeatureDo advancedFeatureDo = this.advancedFeatureRepository.findByDataFacetUid(dataFacetUid);

        //
        // Step 2, core-processing
        //
        if (advancedFeatureDo == null) {
            advancedFeatureDo = new AdvancedFeatureDo();
            advancedFeatureDo.setContent(advancedFeatureDto.getContent());
            advancedFeatureDo.setDataFacetUid(dataFacetUid);
            BaseDo.create(advancedFeatureDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.advancedFeatureRepository.save(advancedFeatureDo);
        } else {
            boolean requiredToUpdate = false;

            if (advancedFeatureDto.getContent() != null) {
                advancedFeatureDo.setContent(advancedFeatureDto.getContent());
                requiredToUpdate = true;
            }

            if (requiredToUpdate) {
                BaseDo.update(advancedFeatureDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.advancedFeatureRepository.save(advancedFeatureDo);
            }
        }

        //
        // Step 3, post-processing
        //

        // step 3.1, event post
        DataFacetChangedEvent dataFacetChangedEvent = new DataFacetChangedEvent();
        dataFacetChangedEvent.setDataFacetDo(dataFacetDo);
        dataFacetChangedEvent.setOperatingUserProfile(operatingUserProfile);
        this.eventBusManager.send(dataFacetChangedEvent);
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
    @Transactional(rollbackFor = Exception.class)
    @Subscribe
    public void handleDataFacetCreatedEvent(DataFacetCreatedEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        AdvancedFeatureDo advancedFeatureDo = this.advancedFeatureRepository.findByDataFacetUid(event.getDataFacetDo().getUid());
        if (advancedFeatureDo == null) {
            advancedFeatureDo = new AdvancedFeatureDo();

            AdvancedFeatureContentDto content = new AdvancedFeatureContentDto();
            content.setEnabledHeader(Boolean.TRUE);
            content.setEnabledTable(Boolean.TRUE);
            content.setEnabledPivotTable(Boolean.FALSE);
            content.setEnabledChart(Boolean.FALSE);
            content.setEnabledMaintenanceWindow(Boolean.FALSE);
            advancedFeatureDo.setContent(content);

            advancedFeatureDo.setDataFacetUid(event.getDataFacetDo().getUid());
            BaseDo.create(advancedFeatureDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
            this.advancedFeatureRepository.save(advancedFeatureDo);
        }

        //
        // Step 3, post-processing
        //
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDataFacetDeletedEvent(DataFacetDeletedEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        AdvancedFeatureDo advancedFeatureDo = this.advancedFeatureRepository.findByDataFacetUid(event.getDataFacetDo().getUid());
        if (advancedFeatureDo != null) {
            this.advancedFeatureRepository.delete(advancedFeatureDo);
        }

        //
        // Step 3, post-processing
        //
    }

    /**
     * event handler
     *
     * @param event
     */
    @Subscribe
    public void handleDataFacetChangedEvent(DataFacetChangedEvent event) {
        LOGGER.info("rcv event:{}", event);

        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //


        //
        // Step 3, post-processing
        //
    }
}
