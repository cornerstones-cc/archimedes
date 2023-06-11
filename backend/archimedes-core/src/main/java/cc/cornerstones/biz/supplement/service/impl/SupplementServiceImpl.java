package cc.cornerstones.biz.supplement.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.supplement.entity.SupplementDo;
import cc.cornerstones.biz.supplement.persistence.SupplementRepository;
import cc.cornerstones.biz.supplement.service.inf.SupplementService;
import cc.cornerstones.biz.supplement.share.types.Supplement;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SupplementServiceImpl implements SupplementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupplementServiceImpl.class);

    @Autowired
    private SupplementRepository supplementRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private Supplement findSupplement(String name) {
        Supplement objectiveSupplement = null;
        Map<String, Supplement> map = this.applicationContext.getBeansOfType(Supplement.class);
        if (!CollectionUtils.isEmpty(map)) {
            for (Map.Entry<String, Supplement> entry : map.entrySet()) {
                Supplement supplement = entry.getValue();
                if (supplement.name().equalsIgnoreCase(name)) {
                    objectiveSupplement = supplement;
                    break;
                }
            }
        }

        return objectiveSupplement;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void enableSupplement(
            String name,
            JSONObject configuration,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        // ensure there is supplement package
        Supplement objectiveSupplement = findSupplement(name);
        if (objectiveSupplement == null) {
            throw new AbcResourceNotFoundException("Cannot find supplement " + name);
        }

        try {
            objectiveSupplement.validate(configuration);
        } catch (Exception e) {
            LOGGER.error("validation error, supplement {}", objectiveSupplement.name(), e);
            throw new IllegalArgumentException("illegal configuration format");
        }

        //
        // Step 2, core-processing
        //
        SupplementDo supplementDo = this.supplementRepository.findByName(objectiveSupplement.name());
        if (supplementDo == null) {
            supplementDo = new SupplementDo();
            supplementDo.setName(objectiveSupplement.name());
            supplementDo.setEnabled(Boolean.TRUE);
            supplementDo.setConfiguration(configuration);
            BaseDo.create(supplementDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.supplementRepository.save(supplementDo);
        } else {
            if (!Boolean.TRUE.equals(supplementDo.getEnabled())) {
                supplementDo.setEnabled(Boolean.TRUE);
                supplementDo.setConfiguration(configuration);
                BaseDo.update(supplementDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.supplementRepository.save(supplementDo);
            }
        }

        //
        // Step 3, post-processing
        //
        try {
            objectiveSupplement.onEnabled(supplementDo, operatingUserProfile);
        } catch (Exception e) {
            LOGGER.error("failed to enable supplement {}", objectiveSupplement.name(), e);
            throw new AbcResourceConflictException(String.format("failed to enable supplement %s",
                    objectiveSupplement.name()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void disableSupplement(
            String name,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        // ensure there is supplement package
        Supplement objectiveSupplement = findSupplement(name);
        if (objectiveSupplement == null) {
            throw new AbcResourceNotFoundException("Cannot find supplement " + name);
        }

        //
        // Step 2, core-processing
        //
        SupplementDo supplementDo = this.supplementRepository.findByName(objectiveSupplement.name());
        if (supplementDo == null) {
            throw new AbcResourceNotFoundException("Cannot find supplement " + name);
        } else {
            if (!Boolean.FALSE.equals(supplementDo.getEnabled())) {
                supplementDo.setEnabled(Boolean.FALSE);
                BaseDo.update(supplementDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.supplementRepository.save(supplementDo);
            }
        }

        //
        // Step 3, post-processing
        //
        try {
            objectiveSupplement.onDisabled(supplementDo, operatingUserProfile);
        } catch (Exception e) {
            LOGGER.error("failed to disable supplement {}", objectiveSupplement.name(), e);
            throw new AbcResourceConflictException(String.format("failed to disable supplement %s",
                    objectiveSupplement.name()));
        }
    }
}
