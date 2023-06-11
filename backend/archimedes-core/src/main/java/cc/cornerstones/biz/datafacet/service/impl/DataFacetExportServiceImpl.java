package cc.cornerstones.biz.datafacet.service.impl;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserBriefInformation;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.biz.administration.usermanagement.service.inf.UserService;
import cc.cornerstones.biz.datafacet.dto.ExportBasicDto;
import cc.cornerstones.biz.datafacet.dto.CreateExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.dto.ExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.dto.UpdateExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.entity.ExportBasicDo;
import cc.cornerstones.biz.datafacet.entity.ExportExtendedTemplateDo;
import cc.cornerstones.biz.datafacet.persistence.DataFacetRepository;
import cc.cornerstones.biz.datafacet.persistence.ExportBasicRepository;
import cc.cornerstones.biz.datafacet.persistence.ExportExtendedTemplateRepository;
import cc.cornerstones.biz.datafacet.service.inf.DataFacetExportService;
import cc.cornerstones.biz.datafacet.share.constants.ExportExtendedTemplateVisibilityEnum;
import cc.cornerstones.biz.export.share.constants.ExportCsvStrategyEnum;
import cc.cornerstones.biz.share.event.*;
import com.google.common.eventbus.Subscribe;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DataFacetExportServiceImpl implements DataFacetExportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFacetExportServiceImpl.class);

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private ExportBasicRepository exportBasicRepository;

    @Autowired
    private ExportExtendedTemplateRepository exportExtendedTemplateRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private UserService userService;


    @Override
    public ExportBasicDto getExportBasicOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportBasicDo exportBasicDo = this.exportBasicRepository.findByDataFacetUid(dataFacetUid);
        if (exportBasicDo == null) {
            return null;
        }

        ExportBasicDto exportBasicDto = new ExportBasicDto();
        BeanUtils.copyProperties(exportBasicDo, exportBasicDto);
        return exportBasicDto;
    }

    @Override
    public void replaceExportBasicOfDataFacet(
            Long dataFacetUid,
            ExportBasicDto exportBasicDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        DataFacetDo dataFacetDo = this.dataFacetRepository.findByUid(dataFacetUid);
        if (dataFacetDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d", DataFacetDo.RESOURCE_SYMBOL,
                    dataFacetUid));
        }

        ExportBasicDo exportBasicDo = this.exportBasicRepository.findByDataFacetUid(dataFacetUid);

        //
        // Step 2, core-processing
        //
        if (exportBasicDo == null) {
            exportBasicDo = new ExportBasicDo();
            exportBasicDo.setEnabledExportCsv(exportBasicDto.getEnabledExportCsv());
            exportBasicDo.setExportCsvStrategy(exportBasicDto.getExportCsvStrategy());
            exportBasicDo.setEnabledExportExcel(exportBasicDto.getEnabledExportExcel());
            exportBasicDo.setEnabledExportDataAndImages(exportBasicDto.getEnabledExportDataAndImages());
            exportBasicDo.setEnabledExportDataAndFiles(exportBasicDto.getEnabledExportDataAndFiles());
            exportBasicDo.setEnabledExportAsTemplates(exportBasicDto.getEnabledExportAsTemplates());
            exportBasicDo.setDataFacetUid(dataFacetUid);
            BaseDo.create(exportBasicDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.exportBasicRepository.save(exportBasicDo);
        } else {
            boolean requiredToUpdate = false;

            if (exportBasicDto.getEnabledExportCsv() != null
                    && !exportBasicDto.getEnabledExportCsv().equals(exportBasicDo.getEnabledExportCsv())) {
                exportBasicDo.setEnabledExportCsv(exportBasicDto.getEnabledExportCsv());
                requiredToUpdate = true;
            }

            if (exportBasicDto.getExportCsvStrategy() != null
                    && !exportBasicDto.getExportCsvStrategy().equals(exportBasicDo.getExportCsvStrategy())) {
                exportBasicDo.setExportCsvStrategy(exportBasicDto.getExportCsvStrategy());
                requiredToUpdate = true;
            }

            if (exportBasicDto.getEnabledExportExcel() != null
                    && !exportBasicDto.getEnabledExportExcel().equals(exportBasicDo.getEnabledExportExcel())) {
                exportBasicDo.setEnabledExportExcel(exportBasicDto.getEnabledExportExcel());
                requiredToUpdate = true;
            }

            if (exportBasicDto.getEnabledExportDataAndImages() != null
                    && !exportBasicDto.getEnabledExportDataAndImages().equals(exportBasicDo.getEnabledExportDataAndImages())) {
                exportBasicDo.setEnabledExportDataAndImages(exportBasicDto.getEnabledExportDataAndImages());
                requiredToUpdate = true;
            }

            if (exportBasicDto.getEnabledExportDataAndFiles() != null
                    && !exportBasicDto.getEnabledExportDataAndFiles().equals(exportBasicDo.getEnabledExportDataAndFiles())) {
                exportBasicDo.setEnabledExportDataAndFiles(exportBasicDto.getEnabledExportDataAndFiles());
                requiredToUpdate = true;
            }

            if (exportBasicDto.getEnabledExportAsTemplates() != null
                    && !exportBasicDto.getEnabledExportAsTemplates().equals(exportBasicDo.getEnabledExportAsTemplates())) {
                exportBasicDo.setEnabledExportAsTemplates(exportBasicDto.getEnabledExportAsTemplates());
                requiredToUpdate = true;
            }

            if (requiredToUpdate) {
                BaseDo.update(exportBasicDo, operatingUserProfile.getUid(), LocalDateTime.now());
                this.exportBasicRepository.save(exportBasicDo);
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

    @Override
    public ExportExtendedTemplateDto createExportExtendedTemplateForDataFacet(
            Long dataFacetUid,
            CreateExportExtendedTemplateDto createExportExtendedTemplateDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        switch (createExportExtendedTemplateDto.getVisibility()) {
            case PUBLIC: {
                boolean existsDuplicate =
                        this.exportExtendedTemplateRepository.existsByNameAndDataFacetUidAndVisibility(
                                createExportExtendedTemplateDto.getName(), dataFacetUid,
                                ExportExtendedTemplateVisibilityEnum.PUBLIC);
                if (existsDuplicate) {
                    throw new AbcResourceConflictException(String.format("%s::name=%s,data_facet_uid=%d",
                            ExportExtendedTemplateDo.RESOURCE_SYMBOL, createExportExtendedTemplateDto.getName(), dataFacetUid));
                }
            }
            break;
            case PRIVATE: {
                boolean existsDuplicate =
                        this.exportExtendedTemplateRepository.existsByNameAndDataFacetUidAndVisibilityAndCreatedBy(
                                createExportExtendedTemplateDto.getName(), dataFacetUid,
                                ExportExtendedTemplateVisibilityEnum.PRIVATE, operatingUserProfile.getUid());
                if (existsDuplicate) {
                    throw new AbcResourceConflictException(String.format("%s::name=%s,data_facet_uid=%d",
                            ExportExtendedTemplateDo.RESOURCE_SYMBOL, createExportExtendedTemplateDto.getName(), dataFacetUid));
                }
            }
            break;
            default:
                break;
        }

        //
        // Step 2, core-processing
        //
        ExportExtendedTemplateDo exportExtendedTemplateDo = new ExportExtendedTemplateDo();
        exportExtendedTemplateDo.setUid(this.idHelper.getNextDistributedId(ExportExtendedTemplateDo.RESOURCE_NAME));
        exportExtendedTemplateDo.setName(createExportExtendedTemplateDto.getName());
        exportExtendedTemplateDo.setObjectName(
                createExportExtendedTemplateDto.getName()
                        .replaceAll("_", "__")
                        .replaceAll("\\s", "_")
                        .toLowerCase());
        exportExtendedTemplateDo.setDescription(createExportExtendedTemplateDto.getDescription());
        exportExtendedTemplateDo.setEnabled(createExportExtendedTemplateDto.getEnabled());
        exportExtendedTemplateDo.setVisibility(createExportExtendedTemplateDto.getVisibility());
        exportExtendedTemplateDo.setColumnHeaderSource(createExportExtendedTemplateDto.getColumnHeaderSource());
        exportExtendedTemplateDo.setFileId(createExportExtendedTemplateDto.getFileId());
        exportExtendedTemplateDo.setDfsServiceAgentUid(createExportExtendedTemplateDto.getDfsServiceAgentUid());
        exportExtendedTemplateDo.setDataFacetUid(dataFacetUid);
        BaseDo.create(exportExtendedTemplateDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportExtendedTemplateRepository.save(exportExtendedTemplateDo);

        //
        // Step 3, post-processing
        //
        ExportExtendedTemplateDto exportExtendedTemplateDto = new ExportExtendedTemplateDto();
        BeanUtils.copyProperties(exportExtendedTemplateDo, exportExtendedTemplateDto);
        return exportExtendedTemplateDto;
    }

    @Override
    public ExportExtendedTemplateDto getExportExtendedTemplate(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        ExportExtendedTemplateDo exportExtendedTemplateDo = this.exportExtendedTemplateRepository.findByUid(uid);
        if (exportExtendedTemplateDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    ExportExtendedTemplateDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 3, post-processing
        //
        ExportExtendedTemplateDto exportExtendedTemplateDto = new ExportExtendedTemplateDto();
        BeanUtils.copyProperties(exportExtendedTemplateDo, exportExtendedTemplateDto);
        return exportExtendedTemplateDto;
    }

    @Override
    public void updateExportExtendedTemplate(
            Long uid,
            UpdateExportExtendedTemplateDto updateExportExtendedTemplateDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        ExportExtendedTemplateDo exportExtendedTemplateDo = this.exportExtendedTemplateRepository.findByUid(uid);
        if (exportExtendedTemplateDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    ExportExtendedTemplateDo.RESOURCE_SYMBOL, uid));
        }

        Long dataFacetUid = exportExtendedTemplateDo.getDataFacetUid();

        if (!ObjectUtils.isEmpty(updateExportExtendedTemplateDto.getName())
                && !updateExportExtendedTemplateDto.getName().equalsIgnoreCase(exportExtendedTemplateDo.getName())) {
            switch (exportExtendedTemplateDo.getVisibility()) {
                case PUBLIC: {
                    boolean existsDuplicate =
                            this.exportExtendedTemplateRepository.existsByNameAndDataFacetUidAndVisibility(
                                    updateExportExtendedTemplateDto.getName(), dataFacetUid,
                                    ExportExtendedTemplateVisibilityEnum.PUBLIC);
                    if (existsDuplicate) {
                        throw new AbcResourceConflictException(String.format("%s::name=%s,data_facet_uid=%d",
                                ExportExtendedTemplateDo.RESOURCE_SYMBOL, updateExportExtendedTemplateDto.getName(), dataFacetUid));
                    }
                }
                break;
                case PRIVATE: {
                    boolean existsDuplicate =
                            this.exportExtendedTemplateRepository.existsByNameAndDataFacetUidAndVisibilityAndCreatedBy(
                                    updateExportExtendedTemplateDto.getName(), dataFacetUid,
                                    ExportExtendedTemplateVisibilityEnum.PRIVATE, operatingUserProfile.getUid());
                    if (existsDuplicate) {
                        throw new AbcResourceConflictException(String.format("%s::name=%s,data_facet_uid=%d",
                                ExportExtendedTemplateDo.RESOURCE_SYMBOL, updateExportExtendedTemplateDto.getName(), dataFacetUid));
                    }
                }
                break;
                default:
                    break;
            }
        }

        //
        // Step 2, core-processing
        //
        boolean requiredToUpdate = false;

        if (!ObjectUtils.isEmpty(updateExportExtendedTemplateDto.getName())
                && !updateExportExtendedTemplateDto.getName().equalsIgnoreCase(exportExtendedTemplateDo.getName())) {
            exportExtendedTemplateDo.setName(updateExportExtendedTemplateDto.getName());
            exportExtendedTemplateDo.setObjectName(
                    updateExportExtendedTemplateDto.getName()
                            .replaceAll("_", "__")
                            .replaceAll("\\s", "_")
                            .toLowerCase());
            requiredToUpdate = true;
        }

        if (updateExportExtendedTemplateDto.getDescription() != null
                && !updateExportExtendedTemplateDto.getDescription().equalsIgnoreCase(exportExtendedTemplateDo.getDescription())) {
            exportExtendedTemplateDo.setDescription(updateExportExtendedTemplateDto.getDescription());
            requiredToUpdate = true;
        }

        if (updateExportExtendedTemplateDto.getEnabled() != null
                && !updateExportExtendedTemplateDto.getEnabled().equals(exportExtendedTemplateDo.getEnabled())) {
            exportExtendedTemplateDo.setEnabled(updateExportExtendedTemplateDto.getEnabled());
            requiredToUpdate = true;
        }

        if (!ObjectUtils.isEmpty(updateExportExtendedTemplateDto.getFileId())
                && !updateExportExtendedTemplateDto.getFileId().equals(exportExtendedTemplateDo.getFileId())) {
            exportExtendedTemplateDo.setFileId(updateExportExtendedTemplateDto.getFileId());
            requiredToUpdate = true;
        }

        if (updateExportExtendedTemplateDto.getDfsServiceAgentUid() != null
                && !updateExportExtendedTemplateDto.getDfsServiceAgentUid().equals(exportExtendedTemplateDo.getDfsServiceAgentUid())) {
            exportExtendedTemplateDo.setDfsServiceAgentUid(updateExportExtendedTemplateDto.getDfsServiceAgentUid());
            requiredToUpdate = true;
        }

        if (updateExportExtendedTemplateDto.getColumnHeaderSource() != null
                && !updateExportExtendedTemplateDto.getColumnHeaderSource().equals(exportExtendedTemplateDo.getColumnHeaderSource())) {
            exportExtendedTemplateDo.setColumnHeaderSource(updateExportExtendedTemplateDto.getColumnHeaderSource());
            requiredToUpdate = true;
        }

        if (requiredToUpdate) {
            BaseDo.update(exportExtendedTemplateDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.exportExtendedTemplateRepository.save(exportExtendedTemplateDo);
        }

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<String> listAllReferencesToExportExtendedTemplate(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public void deleteExportExtendedTemplate(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // Step 1, pre-processing
        //
        ExportExtendedTemplateDo exportExtendedTemplateDo = this.exportExtendedTemplateRepository.findByUid(uid);
        if (exportExtendedTemplateDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::uid=%d",
                    ExportExtendedTemplateDo.RESOURCE_SYMBOL, uid));
        }

        //
        // Step 2, core-processing
        //
        exportExtendedTemplateDo.setDeleted(Boolean.TRUE);
        BaseDo.update(exportExtendedTemplateDo, operatingUserProfile.getUid(), LocalDateTime.now());
        this.exportExtendedTemplateRepository.save(exportExtendedTemplateDo);

        //
        // Step 3, post-processing
        //
    }

    @Override
    public List<ExportExtendedTemplateDto> listingQueryExportExtendedTemplatesOfDataFacet(
            Long dataFacetUid,
            ExportExtendedTemplateVisibilityEnum visibility,
            Long uid,
            String name,
            String description,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<ExportExtendedTemplateDo> specification = new Specification<ExportExtendedTemplateDo>() {
            @Override
            public Predicate toPredicate(Root<ExportExtendedTemplateDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                if (visibility != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("visibility"), visibility));

                    // private for self
                    if (visibility.equals(ExportExtendedTemplateVisibilityEnum.PRIVATE)) {
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.CREATED_BY_FIELD_NAME),
                                operatingUserProfile.getUid()));
                    }
                }
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (!ObjectUtils.isEmpty(description)) {
                    predicateList.add(criteriaBuilder.like(root.get("description"), "%" + description + "%"));
                }
                if (enabled != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                }
                if (!CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("lastModifiedBy"));
                    userUidListOfLastModifiedBy.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(lastModifiedTimestampAsStringList)) {
                    if (lastModifiedTimestampAsStringList.size() == 2) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0),
                                dateTimeFormatter);
                        LocalDateTime dateTime1 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(1), dateTimeFormatter);
                        if (dateTime0.isAfter(dateTime1)) {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime1, dateTime0));
                        } else {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime0, dateTime1));
                        }
                    } else if (lastModifiedTimestampAsStringList.size() == 1) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0), dateTimeFormatter);
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                dateTime0));
                    } else {
                        CriteriaBuilder.In<LocalDateTime> in =
                                criteriaBuilder.in(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME));
                        lastModifiedTimestampAsStringList.forEach(createdTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        List<ExportExtendedTemplateDo> itemDoList = this.exportExtendedTemplateRepository.findAll(specification,
                sort);
        List<ExportExtendedTemplateDto> itemDtoList = new LinkedList<>();
        itemDoList.forEach(itemDo -> {
            ExportExtendedTemplateDto itemDto = new ExportExtendedTemplateDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            itemDtoList.add(itemDto);
        });
        return itemDtoList;
    }

    @Override
    public Page<ExportExtendedTemplateDto> pagingQueryExportExtendedTemplatesOfDataFacet(
            Long dataFacetUid,
            ExportExtendedTemplateVisibilityEnum visibility,
            Long uid,
            String name,
            String description,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        //
        // step 1, pre-processing
        //

        //
        // step 2, core-processing
        //
        Specification<ExportExtendedTemplateDo> specification = new Specification<ExportExtendedTemplateDo>() {
            @Override
            public Predicate toPredicate(Root<ExportExtendedTemplateDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (dataFacetUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("dataFacetUid"), dataFacetUid));
                }
                if (visibility != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("visibility"), visibility));
                }
                if (uid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("uid"), uid));
                }
                if (!ObjectUtils.isEmpty(name)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
                }
                if (!ObjectUtils.isEmpty(description)) {
                    predicateList.add(criteriaBuilder.like(root.get("description"), "%" + description + "%"));
                }
                if (enabled != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                }
                if (!CollectionUtils.isEmpty(userUidListOfLastModifiedBy)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("lastModifiedBy"));
                    userUidListOfLastModifiedBy.forEach(item -> {
                        in.value(item);
                    });
                    predicateList.add(in);
                }
                if (!CollectionUtils.isEmpty(lastModifiedTimestampAsStringList)) {
                    if (lastModifiedTimestampAsStringList.size() == 2) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0),
                                dateTimeFormatter);
                        LocalDateTime dateTime1 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(1), dateTimeFormatter);
                        if (dateTime0.isAfter(dateTime1)) {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime1, dateTime0));
                        } else {
                            predicateList.add(criteriaBuilder.between(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                    dateTime0, dateTime1));
                        }
                    } else if (lastModifiedTimestampAsStringList.size() == 1) {
                        LocalDateTime dateTime0 = LocalDateTime.parse(lastModifiedTimestampAsStringList.get(0), dateTimeFormatter);
                        predicateList.add(criteriaBuilder.equal(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME),
                                dateTime0));
                    } else {
                        CriteriaBuilder.In<LocalDateTime> in =
                                criteriaBuilder.in(root.get(BaseDo.LAST_MODIFIED_TIMESTAMP_FIELD_NAME));
                        lastModifiedTimestampAsStringList.forEach(createdTimestampAsString -> {
                            LocalDateTime dateTime0 = LocalDateTime.parse(createdTimestampAsString, dateTimeFormatter);
                            in.value(dateTime0);
                        });
                        predicateList.add(in);
                    }
                }
                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        Page<ExportExtendedTemplateDo> itemDoPage = this.exportExtendedTemplateRepository.findAll(specification, pageable);

        //
        // step 3, post-processing
        //

        //
        // step 3.1, 为 created by, last modified by 补充 user brief information
        //
        List<Long> userUidList = new LinkedList<>();
        itemDoPage.forEach(itemDo -> {
            if (itemDo.getCreatedBy() != null && !userUidList.contains(itemDo.getCreatedBy())) {
                userUidList.add(itemDo.getCreatedBy());
            }
            if (itemDo.getLastModifiedBy() != null && !userUidList.contains(itemDo.getLastModifiedBy())) {
                userUidList.add(itemDo.getLastModifiedBy());
            }
        });

        Map<Long, UserBriefInformation> userBriefInformationMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(userUidList)) {
            List<UserBriefInformation> userBriefInformationList =
                    this.userService.listingUserBriefInformation(userUidList, operatingUserProfile);
            if (!CollectionUtils.isEmpty(userBriefInformationList)) {
                userBriefInformationList.forEach(userBriefInformation -> {
                    userBriefInformationMap.put(userBriefInformation.getUid(), userBriefInformation);
                });
            }
        }

        //
        // step 3.2, 构造返回内容
        //
        List<ExportExtendedTemplateDto> content = new ArrayList<>(itemDoPage.getSize());
        itemDoPage.forEach(itemDo -> {
            ExportExtendedTemplateDto itemDto = new ExportExtendedTemplateDto();
            BeanUtils.copyProperties(itemDo, itemDto);

            // 为 created by, last modified by 补充 user brief information
            itemDto.setCreatedByUser(userBriefInformationMap.get(itemDo.getCreatedBy()));
            itemDto.setLastModifiedByUser(userBriefInformationMap.get(itemDo.getLastModifiedBy()));

            content.add(itemDto);
        });
        Page<ExportExtendedTemplateDto> itemDtoPage = new PageImpl<>(content, pageable, itemDoPage.getTotalElements());
        return itemDtoPage;
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
        ExportBasicDo exportBasicDo = this.exportBasicRepository.findByDataFacetUid(event.getDataFacetDo().getUid());
        if (exportBasicDo == null) {
            exportBasicDo = new ExportBasicDo();
            exportBasicDo.setEnabledExportCsv(Boolean.TRUE);
            exportBasicDo.setExportCsvStrategy(ExportCsvStrategyEnum.RFC);
            exportBasicDo.setEnabledExportExcel(Boolean.TRUE);
            exportBasicDo.setEnabledExportDataAndImages(Boolean.FALSE);
            exportBasicDo.setEnabledExportDataAndFiles(Boolean.FALSE);
            exportBasicDo.setEnabledExportAsTemplates(Boolean.FALSE);
            exportBasicDo.setDataFacetUid(event.getDataFacetDo().getUid());
            BaseDo.create(exportBasicDo, event.getOperatingUserProfile().getUid(), LocalDateTime.now());
            this.exportBasicRepository.save(exportBasicDo);
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
        ExportBasicDo exportBasicDo = this.exportBasicRepository.findByDataFacetUid(event.getDataFacetDo().getUid());
        if (exportBasicDo != null) {
            this.exportBasicRepository.delete(exportBasicDo);
        }

        this.exportExtendedTemplateRepository.deleteByDataFacetUid(event.getDataFacetDo().getUid());

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
