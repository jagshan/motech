package org.motechproject.mds.annotations.internal;

import org.motechproject.mds.annotations.Access;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.domain.MdsEntity;
import org.motechproject.mds.dto.AdvancedSettingsDto;
import org.motechproject.mds.dto.EntityDto;
import org.motechproject.mds.dto.FieldDto;
import org.motechproject.mds.dto.RestOptionsDto;
import org.motechproject.mds.dto.TrackingDto;
import org.motechproject.mds.helper.EntityDefaultFieldsHelper;
import org.motechproject.mds.javassist.MotechClassPool;
import org.motechproject.mds.reflections.ReflectionsUtil;
import org.motechproject.mds.service.EntityService;
import org.motechproject.mds.service.TypeService;
import org.motechproject.mds.util.ClassName;
import org.motechproject.mds.util.Constants;
import org.motechproject.mds.util.SecurityMode;
import org.motechproject.osgi.web.util.BundleHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.motechproject.mds.util.Constants.AnnotationFields.HISTORY;
import static org.motechproject.mds.util.Constants.AnnotationFields.MAX_FETCH_DEPTH;
import static org.motechproject.mds.util.Constants.AnnotationFields.MODULE;
import static org.motechproject.mds.util.Constants.AnnotationFields.NAME;
import static org.motechproject.mds.util.Constants.AnnotationFields.NAMESPACE;
import static org.motechproject.mds.util.Constants.AnnotationFields.TABLE_NAME;

/**
 * The <code>EntityProcessor</code> provides a mechanism, allowing adding public classes from other
 * modules as entities in the MDS module. When the entity is successfully added into MDS database,
 * related processors are starting to process the class definitions in order to add other
 * information into the MDS database.
 *
 * @see org.motechproject.mds.annotations.Entity
 */
@Component
class EntityProcessor extends AbstractListProcessor<Entity, EntityDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityProcessor.class);

    private EntityService entityService;
    private TypeService typeService;
    private FieldProcessor fieldProcessor;
    private UIFilterableProcessor uiFilterableProcessor;
    private UIDisplayableProcessor uiDisplayableProcessor;
    private RestIgnoreProcessor restIgnoreProcessor;
    private RestOperationsProcessor restOperationsProcessor;
    private CrudEventsProcessor crudEventsProcessor;
    private NonEditableProcessor nonEditableProcessor;

    private List<EntityProcessorOutput> processingResult;

    @Override
    public Class<Entity> getAnnotationType() {
        return Entity.class;
    }

    @Override
    public List<EntityProcessorOutput> getProcessingResult() {
        return processingResult;
    }

    @Override
    protected Set<? extends AnnotatedElement> getElementsToProcess() {
        return ReflectionsUtil.getClasses(getAnnotationType(), getBundle());
    }

    @Override
    protected void process(AnnotatedElement element) {
        BundleHeaders bundleHeaders = new BundleHeaders(getBundle());

        EntityProcessorOutput entityProcessorOutput = new EntityProcessorOutput();

        Class clazz = (Class) element;
        Class<Entity> ann = ReflectionsUtil.getAnnotationClass(clazz, Entity.class);
        Annotation annotation = AnnotationUtils.findAnnotation(clazz, ann);

        if (null != annotation) {
            String className = clazz.getName();

            String name = ReflectionsUtil.getAnnotationValue(
                    annotation, NAME, ClassName.getSimpleName(className)
            );
            String module = ReflectionsUtil.getAnnotationValue(
                    annotation, MODULE, bundleHeaders.getName(), bundleHeaders.getSymbolicName()
            );
            String namespace = ReflectionsUtil.getAnnotationValue(annotation, NAMESPACE);
            String tableName = ReflectionsUtil.getAnnotationValue(annotation, TABLE_NAME);

            boolean recordHistory = Boolean.parseBoolean(ReflectionsUtil.getAnnotationValue(annotation, HISTORY));

            try {
                EntityDto entity = entityService.getEntityByClassName(className);
                RestOptionsDto restOptions = new RestOptionsDto();
                TrackingDto tracking = new TrackingDto();
                Collection<FieldDto> fields;

                if (entity == null) {
                    LOGGER.debug("Creating DDE for {}", className);

                    entity = new EntityDto(
                            null, className, name, module, namespace, tableName, recordHistory,
                            SecurityMode.EVERYONE, null, clazz.getSuperclass().getName(),
                            Modifier.isAbstract(clazz.getModifiers()), false
                    );
                } else {
                    LOGGER.debug("DDE for {} already exists, updating if necessary", className);
                    entity.setRecordHistory(recordHistory);

                    AdvancedSettingsDto advancedSettings = entityService.getAdvancedSettings(entity.getId(), true);
                    restOptions = advancedSettings.getRestOptions();
                    tracking = advancedSettings.getTracking();
                }
                tracking.setRecordHistory(recordHistory);

                setSecurityOptions(element, entity);

                // per entity maxFetchDepth that will be passed to the Persistence Manager
                setMaxFetchDepth(entity, annotation);

                entityProcessorOutput.setEntityProcessingResult(entity);

                fields = findFields(clazz, entity);
                addDefaultFields(entity, fields);

                entityProcessorOutput.setFieldProcessingResult(fields);

                entityProcessorOutput.setUiFilterableProcessingResult(findFilterableFields(clazz));
                entityProcessorOutput.setUiDisplayableProcessingResult(findDisplayedFields(clazz));
                entityProcessorOutput.setCrudProcessingResult(processCrudEvents(clazz, tracking));

                restOptions = processRestOperations(clazz, restOptions);
                entityProcessorOutput.setRestOptionsProcessingResult(restOptions);

                restOptions = findRestFields(clazz, restOptions, fields);
                entityProcessorOutput.setRestIgnoreProcessingResult(restOptions);

                entityProcessorOutput.setNonEditableProcessingResult(findNonEditableFields(clazz));

                add(entity);
                processingResult.add(entityProcessorOutput);
                MotechClassPool.registerDDE(entity.getClassName());
            } catch (Exception e) {
                LOGGER.error(
                        "Failed to create an entity for class {} from bundle {}",
                        clazz.getName(), getBundle().getSymbolicName()
                );
                LOGGER.error("because of: ", e);
            }
        } else {
            LOGGER.debug("Did not find Entity annotation in class: {}", clazz.getName());
        }
    }

    private void setMaxFetchDepth(EntityDto entity, Annotation annotation) {
        int maxFetchDepth = Integer.parseInt(ReflectionsUtil.getAnnotationValue(annotation, MAX_FETCH_DEPTH));
        if (maxFetchDepth != Constants.FetchDepth.MDS_DEFAULT) {
            entity.setMaxFetchDepth(maxFetchDepth);
        }
    }

    private void addDefaultFields(EntityDto entity, Collection<FieldDto> fields) {
        if (!MdsEntity.class.getName().equalsIgnoreCase(entity.getSuperClass())) {
            fields.addAll(EntityDefaultFieldsHelper.defaultFields(typeService));
        }
    }

    @Override
    protected void beforeExecution() {
        processingResult = new ArrayList<>();
        super.beforeExecution();
    }

    @Override
    protected void afterExecution() {
        LOGGER.debug("Execution complete for bundle {}", getBundle().getSymbolicName());
    }

    private RestOptionsDto processRestOperations(Class clazz, RestOptionsDto restOptions) {
        restOperationsProcessor.setClazz(clazz);
        restOperationsProcessor.setRestOptions(restOptions);
        restOperationsProcessor.execute(getBundle());
        return restOperationsProcessor.getProcessingResult();
    }

    private TrackingDto processCrudEvents(Class clazz, TrackingDto tracking) {
        crudEventsProcessor.setClazz(clazz);
        crudEventsProcessor.setTrackingDto(tracking);
        crudEventsProcessor.execute(getBundle());
        return crudEventsProcessor.getProcessingResult();
    }

    private Collection<FieldDto> findFields(Class clazz, EntityDto entity) {
        fieldProcessor.setClazz(clazz);
        fieldProcessor.setEntity(entity);
        fieldProcessor.execute(getBundle());
        return fieldProcessor.getProcessingResult();
    }

    private Collection<String> findFilterableFields(Class clazz) {
        uiFilterableProcessor.setClazz(clazz);
        uiFilterableProcessor.execute(getBundle());
        return uiFilterableProcessor.getProcessingResult();
    }

    private Map<String, Long> findDisplayedFields(Class clazz) {
        uiDisplayableProcessor.setClazz(clazz);
        uiDisplayableProcessor.execute(getBundle());
        return uiDisplayableProcessor.getProcessingResult();
    }

    private RestOptionsDto findRestFields(Class clazz, RestOptionsDto restOptions, Collection<FieldDto> fields) {
        restIgnoreProcessor.setClazz(clazz);
        restIgnoreProcessor.setRestOptions(restOptions);
        restIgnoreProcessor.setFields(new ArrayList<>(fields));
        restIgnoreProcessor.execute(getBundle());
        return restIgnoreProcessor.getProcessingResult();
    }

    private Collection<String> findNonEditableFields(Class clazz) {
        nonEditableProcessor.setClazz(clazz);
        nonEditableProcessor.execute(getBundle());
        return nonEditableProcessor.getProcessingResult();
    }

    private void setSecurityOptions(AnnotatedElement element, EntityDto entity) {
        Access access = element.getAnnotation(Access.class);
        if (null != access && !entity.isSecurityOptionsModified()) {
            SecurityMode securityMode = access.value();
            Boolean hasMembers = access.members() != null && access.members().length > 0;

            if (securityMode == SecurityMode.USERS || securityMode == SecurityMode.ROLES) {
                if (hasMembers) {
                    Set<String> members = new HashSet<String>(Arrays.asList(access.members()));
                    entity.setSecurityMembers(members);
                } else {
                    throw new IllegalArgumentException(
                            "Failed to process Access annotation: the security mode is set to "
                                    + securityMode + " but there are no members specified."
                    );
                }
            } else {
                entity.setSecurityMembers(new HashSet<String>());
                if (hasMembers) {
                    throw new IllegalArgumentException(
                            "Failed to process Access annotation: the members attribute can be only used with USERS or ROLES security mode."
                    );
                }
            }
            entity.setSecurityMode(securityMode);
        }
    }

    @Autowired
    public void setEntityService(EntityService entityService) {
        this.entityService = entityService;
    }

    @Autowired
    public void setTypeService(TypeService typeService) {
        this.typeService = typeService;
    }

    @Autowired
    public void setFieldProcessor(FieldProcessor fieldProcessor) {
        this.fieldProcessor = fieldProcessor;
    }

    @Autowired
    public void setUIFilterableProcessor(UIFilterableProcessor uiFilterableProcessor) {
        this.uiFilterableProcessor = uiFilterableProcessor;
    }

    @Autowired
    public void setUIDisplayableProcessor(UIDisplayableProcessor uiDisplayableProcessor) {
        this.uiDisplayableProcessor = uiDisplayableProcessor;
    }

    @Autowired
    public void setRestIgnoreProcessor(RestIgnoreProcessor restIgnoreProcessor) {
        this.restIgnoreProcessor = restIgnoreProcessor;
    }

    @Autowired
    public void setRestOperationsProcessor(RestOperationsProcessor restOperationsProcessor) {
        this.restOperationsProcessor = restOperationsProcessor;
    }

    @Autowired
    public void setCrudEventsProcessor(CrudEventsProcessor crudEventsProcessor) {
        this.crudEventsProcessor = crudEventsProcessor;
    }

    @Autowired
    public void setNonEditableProcessor(NonEditableProcessor nonEditableProcessor) {
        this.nonEditableProcessor = nonEditableProcessor;
    }
}
