package org.motechproject.mds.service.impl.history;

import org.apache.commons.beanutils.MethodUtils;
import org.motechproject.mds.config.DeleteMode;
import org.motechproject.mds.config.SettingsService;
import org.motechproject.mds.domain.Entity;
import org.motechproject.mds.domain.EntityType;
import org.motechproject.mds.domain.Field;
import org.motechproject.mds.ex.entity.EmptyTrashException;
import org.motechproject.mds.query.Property;
import org.motechproject.mds.query.PropertyBuilder;
import org.motechproject.mds.query.QueryParams;
import org.motechproject.mds.query.QueryUtil;
import org.motechproject.mds.service.HistoryService;
import org.motechproject.mds.service.MdsSchedulerService;
import org.motechproject.mds.service.TrashService;
import org.motechproject.mds.util.ObjectReference;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Default implementation of {@link org.motechproject.mds.service.TrashService} interface.
 */
public class TrashServiceImpl extends BasePersistenceService implements TrashService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrashServiceImpl.class);

    private MdsSchedulerService mdsSchedulerService;
    private SettingsService settingsService;
    private HistoryService historyService;
    private ValueGetter trashValueGetter;

    @Override
    public boolean isTrashMode() {
        return settingsService.getDeleteMode() == DeleteMode.TRASH;
    }

    @PostConstruct
    public void init() {
        trashValueGetter = new TrashValueGetter(this, getBundleContext());
    }

    @Override
    @Transactional
    public void moveToTrash(Object instance, Long entityVersion, boolean recordHistory) {
        Class<?> trashClass = HistoryTrashClassHelper.getClass(instance, EntityType.TRASH,
                getBundleContext());

        if (null != trashClass) {
            LOGGER.debug("Moving {} to trash", instance);

            // create and save a trash instance
            LOGGER.debug("Creating trash instance for: {}", instance);

            Object trash = create(trashClass, instance, EntityType.TRASH, trashValueGetter);

            LOGGER.debug("Created trash instance for: {}", instance);

            try {
                MethodUtils.invokeMethod(trash, "setSchemaVersion", entityVersion);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Failed to set schema version of the trash instance.");
            }

            PersistenceManager manager = getPersistenceManagerFactory().getPersistenceManager();

            manager.makePersistent(trash);

            // set the flag in historical data
            if (recordHistory) {
                historyService.setTrashFlag(instance, trash, true);
            }
        } else {
            throw new IllegalStateException(
                    "Not found the trash class for " + instance.getClass().getName()
            );
        }
    }

    @Override
    @Transactional
    public Object findTrashById(Object instanceId, Object entityId) {
        Long instanceIdAsLong = Long.valueOf(instanceId.toString());
        Long entityIdAsLong = Long.valueOf(entityId.toString());

        Entity entity = getEntity(entityIdAsLong);

        Class<?> trashClass =  HistoryTrashClassHelper.getClass(entity.getClassName(), EntityType.TRASH,
                getBundleContext());

        List<Property> properties = new ArrayList<>();
        properties.add(PropertyBuilder.create("id", instanceIdAsLong, Long.class));

        PersistenceManager manager = getPersistenceManagerFactory().getPersistenceManager();
        Query query = manager.newQuery(trashClass);
        QueryUtil.useFilter(query, properties);
        query.setUnique(true);

        return query.execute(instanceIdAsLong);
    }

    @Override
    @Transactional
    public void moveFromTrash(Object newInstance, Object trash, boolean recordHistory) {
        if (recordHistory) {
            historyService.setTrashFlag(newInstance, trash, false);
        }

        PersistenceManager manager = getPersistenceManagerFactory().getPersistenceManager();
        manager.deletePersistent(trash);
    }

    @Override
    @Transactional
    public Collection getInstancesFromTrash(String className, QueryParams queryParams) {
        Class<?> trashClass =  HistoryTrashClassHelper.getClass(className, EntityType.TRASH,
                getBundleContext());

        Long schemaVersion = getCurrentSchemaVersion(className);

        List<Property> properties = new ArrayList<>();
        properties.add(PropertyBuilder.create("schemaVersion", schemaVersion, Long.class));

        PersistenceManager manager = getPersistenceManagerFactory().getPersistenceManager();

        Query query = manager.newQuery(trashClass);
        QueryUtil.setQueryParams(query, queryParams);
        QueryUtil.useFilter(query, properties);

        return (Collection) query.execute(schemaVersion);
    }

    @Override
    @Transactional
    public long countTrashRecords(String className) {
        Class<?> trashClass =  HistoryTrashClassHelper.getClass(className, EntityType.TRASH,
                getBundleContext());

        Long schemaVersion = getCurrentSchemaVersion(className);

        List<Property> properties = new ArrayList<>();
        properties.add(PropertyBuilder.create("schemaVersion", schemaVersion, Long.class));

        PersistenceManager manager = getPersistenceManagerFactory().getPersistenceManager();
        Query query = manager.newQuery(trashClass);
        QueryUtil.useFilter(query, properties);
        QueryUtil.setCountResult(query);

        return (long) query.execute(schemaVersion);
    }

    @Override
    @Transactional
    public void scheduleEmptyTrashJob() {
        // unchedule previous job
        mdsSchedulerService.unscheduleRepeatingJob();

        // schedule new job only if trashMode is active and emptyTrash flag is set
        if (isTrashMode() && settingsService.isEmptyTrash()) {
            Integer timeValue = settingsService.getTimeValue();
            Long timeUnit = settingsService.getTimeUnit().inMillis();
            long interval = timeValue * timeUnit;

            mdsSchedulerService.scheduleRepeatingJob(interval);
        }
    }

    @Override
    @Transactional
    public void emptyTrash() {
        try {
            PersistenceManager manager = getPersistenceManagerFactory().getPersistenceManager();

            for (Entity entity : getEntities()) {
                Class<?> trashClass =  HistoryTrashClassHelper.getClass(entity.getClassName(), EntityType.TRASH,
                        getBundleContext());

                Query query = manager.newQuery(trashClass);
                Collection instances = (Collection) query.execute();
                if (entity.isRecordHistory()) {
                    for (Object instance : instances) {
                        historyService.remove(instance);
                    }
                }

                manager.deletePersistentAll(instances);
            }
        } catch (Exception e) {
            throw new EmptyTrashException(e);
        }
    }

    @Autowired
    public void setMdsSchedulerService(MdsSchedulerService mdsSchedulerService) {
        this.mdsSchedulerService = mdsSchedulerService;
    }

    @Autowired
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Autowired
    public void setHistoryService(HistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * We do not create deep trash copies, hence this getter implementation.
     */
    private class TrashValueGetter extends ValueGetter {

        public TrashValueGetter(BasePersistenceService persistenceService, BundleContext bundleContext) {
            super(persistenceService, bundleContext);
        }

        @Override
        public Object getValue(Field field, Object src, Object target, EntityType type, ObjectReference objectReference) {
            if (field.getType().isRelationship()) {
                return null;
            } else {
                return super.getValue(field, src, target, type, objectReference);
            }
        }
    }
}
