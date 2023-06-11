package cc.cornerstones.biz.datapage.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceDuplicateException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.datapage.dto.CreateDataPageDto;
import cc.cornerstones.biz.datapage.dto.DataPageDto;
import cc.cornerstones.biz.datapage.dto.UpdateDataPageDto;
import cc.cornerstones.biz.datapage.entity.DataPageDo;
import cc.cornerstones.biz.datapage.persistence.DataPageRepository;
import cc.cornerstones.biz.datapage.service.inf.DataPageService;
import cc.cornerstones.biz.share.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataPageServiceImpl implements DataPageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPageServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataPageRepository dataPageRepository;

    @Override
    public DataPageDto createDataPage(
            CreateDataPageDto createDataPageDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        boolean existsDuplicate = this.dataPageRepository.existsByName(createDataPageDto.getName());
        if (existsDuplicate) {
            throw new AbcResourceDuplicateException(String.format("%s::name=%s", DataPageDo.RESOURCE_SYMBOL,
                    createDataPageDto.getName()));
        }

        //
        // Step 2, core-processing
        //
        DataPageDo dataPageDo = new DataPageDo();
        dataPageDo.setUid(this.idHelper.getNextDistributedId(DataPageDo.RESOURCE_NAME));
        dataPageDo.setName(createDataPageDto.getName());
        dataPageDo.setObjectName(createDataPageDto.getName()
                .replaceAll("_", "__")
                .replaceAll("\\s", "_")
                .toLowerCase());
        dataPageDo.setDescription(createDataPageDto.getDescription());
        BaseDo.create(dataPageDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataPageRepository.save(dataPageDo);


        //
        // Step 3, post-processing
        //
        DataPageDto dataPageDto = new DataPageDto();
        BeanUtils.copyProperties(dataPageDo, dataPageDto);
        return dataPageDto;
    }

    @Override
    public void updateDataPage(
            Long dataPageUid,
            UpdateDataPageDto updateDataPageDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPageDo dataPageDo = this.dataPageRepository.findByUid(dataPageUid);
        if (dataPageDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPageDo.RESOURCE_SYMBOL,
                    dataPageUid));
        }

        if (!ObjectUtils.isEmpty(updateDataPageDto.getName())
                && !updateDataPageDto.getName().equals(dataPageDo.getName())) {
            boolean existsDuplicate = this.dataPageRepository.existsByName(updateDataPageDto.getName());
            if (existsDuplicate) {
                throw new AbcResourceDuplicateException(String.format("%s::name=%s", DataPageDo.RESOURCE_SYMBOL,
                        updateDataPageDto.getName()));
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;
        if (!ObjectUtils.isEmpty(updateDataPageDto.getName())
                && !updateDataPageDto.getName().equals(dataPageDo.getName())) {
            dataPageDo.setName(updateDataPageDto.getName());
            requiredToUpdate = true;
        }
        if (updateDataPageDto.getDescription() != null
                && !updateDataPageDto.getDescription().equals(dataPageDo.getDescription())) {
            dataPageDo.setDescription(updateDataPageDto.getDescription());
            requiredToUpdate = true;
        }
        if (requiredToUpdate) {
            BaseDo.update(dataPageDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.dataPageRepository.save(dataPageDo);
        }

    }

    @Override
    public DataPageDto getDataPage(
            Long dataPageUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPageDo dataPageDo = this.dataPageRepository.findByUid(dataPageUid);
        if (dataPageDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPageDo.RESOURCE_SYMBOL,
                    dataPageUid));
        }

        //
        // Step 2, core-processing
        //

        //
        // Step 3, post-processing
        //
        DataPageDto dataPageDto = new DataPageDto();
        BeanUtils.copyProperties(dataPageDo, dataPageDto);
        return dataPageDto;
    }

    @Override
    public List<String> listAllReferencesToDataPage(
            Long dataPageUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteDataPage(
            Long dataPageUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataPageDo dataPageDo = this.dataPageRepository.findByUid(dataPageUid);
        if (dataPageDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataPageDo.RESOURCE_SYMBOL,
                    dataPageUid));
        }

        //
        // Step 2, core-processing
        //
        dataPageDo.setDeleted(Boolean.TRUE);
        BaseDo.update(dataPageDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.dataPageRepository.save(dataPageDo);
    }

    @Override
    public Page<DataPageDto> pagingQueryDataPages(
            Long dataPageUid,
            String dataPageName,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataPageDo> specification = new Specification<DataPageDo>() {
            @Override
            public Predicate toPredicate(Root<DataPageDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataPageUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), dataPageUid));
                }
                if (!ObjectUtils.isEmpty(dataPageName)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + dataPageName + "%"));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Page<DataPageDo> itemDoPage = this.dataPageRepository.findAll(specification, pageable);
        List<DataPageDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            DataPageDto itemDto = new DataPageDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        Page<DataPageDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
    }

    @Override
    public List<DataPageDto> listingQueryDataPages(
            Long dataPageUid,
            String dataPageName,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<DataPageDo> specification = new Specification<DataPageDo>() {
            @Override
            public Predicate toPredicate(Root<DataPageDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataPageUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), dataPageUid));
                }
                if (!ObjectUtils.isEmpty(dataPageName)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + dataPageName + "%"));
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<DataPageDo> itemDoList = this.dataPageRepository.findAll(specification, sort);
        if (CollectionUtils.isEmpty(itemDoList)) {
            return null;
        }
        List<DataPageDto> content = new ArrayList<>(itemDoList.size());
        itemDoList.forEach(itemDo -> {
            DataPageDto itemDto = new DataPageDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            content.add(itemDto);
        });
        return content;
    }
}
