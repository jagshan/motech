package org.motechproject.mds.service.impl;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.motechproject.mds.builder.MDSConstructor;
import org.motechproject.mds.domain.ComboboxHolder;
import org.motechproject.mds.domain.Entity;
import org.motechproject.mds.domain.EntityDraft;
import org.motechproject.mds.domain.Field;
import org.motechproject.mds.domain.FieldMetadata;
import org.motechproject.mds.domain.FieldSetting;
import org.motechproject.mds.domain.FieldValidation;
import org.motechproject.mds.domain.Lookup;
import org.motechproject.mds.domain.MdsEntity;
import org.motechproject.mds.domain.Type;
import org.motechproject.mds.domain.TypeSetting;
import org.motechproject.mds.domain.TypeValidation;
import org.motechproject.mds.dto.AdvancedSettingsDto;
import org.motechproject.mds.dto.DraftData;
import org.motechproject.mds.dto.DraftResult;
import org.motechproject.mds.dto.EntityDto;
import org.motechproject.mds.dto.FieldBasicDto;
import org.motechproject.mds.dto.FieldDto;
import org.motechproject.mds.dto.FieldValidationDto;
import org.motechproject.mds.dto.LookupDto;
import org.motechproject.mds.dto.LookupFieldDto;
import org.motechproject.mds.dto.MetadataDto;
import org.motechproject.mds.dto.RestOptionsDto;
import org.motechproject.mds.dto.SettingDto;
import org.motechproject.mds.dto.TrackingDto;
import org.motechproject.mds.dto.TypeDto;
import org.motechproject.mds.dto.ValidationCriterionDto;
import org.motechproject.mds.ex.entity.EntityAlreadyExistException;
import org.motechproject.mds.ex.entity.EntityChangedException;
import org.motechproject.mds.ex.entity.EntityNotFoundException;
import org.motechproject.mds.ex.entity.EntityReadOnlyException;
import org.motechproject.mds.ex.field.FieldNotFoundException;
import org.motechproject.mds.ex.type.NoSuchTypeException;
import org.motechproject.mds.helper.ComboboxDataMigrationHelper;
import org.motechproject.mds.helper.EntityHelper;
import org.motechproject.mds.helper.FieldHelper;
import org.motechproject.mds.javassist.MotechClassPool;
import org.motechproject.mds.repository.AllEntities;
import org.motechproject.mds.repository.AllEntityAudits;
import org.motechproject.mds.repository.AllEntityDrafts;
import org.motechproject.mds.repository.AllTypes;
import org.motechproject.mds.service.EntityService;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.mds.util.ClassName;
import org.motechproject.mds.util.Constants;
import org.motechproject.mds.util.LookupName;
import org.motechproject.mds.util.SecurityMode;
import org.motechproject.mds.util.TypeHelper;
import org.motechproject.mds.validation.EntityValidator;
import org.motechproject.osgi.web.util.OSGiServiceUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.motechproject.mds.repository.query.DataSourceReferenceQueryExecutionHelper.DATA_SOURCE_CLASS_NAME;
import static org.motechproject.mds.repository.query.DataSourceReferenceQueryExecutionHelper.createLookupReferenceQuery;
import static org.motechproject.mds.util.Constants.Util.AUTO_GENERATED;
import static org.motechproject.mds.util.Constants.Util.AUTO_GENERATED_EDITABLE;
import static org.motechproject.mds.util.Constants.Util.TRUE;
import static org.motechproject.mds.util.SecurityUtil.getUserRoles;
import static org.motechproject.mds.util.SecurityUtil.getUsername;
import static org.motechproject.mds.util.Constants.MetadataKeys.RELATED_CLASS;
import static org.motechproject.mds.util.Constants.MetadataKeys.RELATIONSHIP_COLLECTION_TYPE;
import static org.motechproject.mds.util.Constants.MetadataKeys.RELATED_FIELD;

/**
 * Default implementation of {@link org.motechproject.mds.service.EntityService} interface.
 */
@Service
public class EntityServiceImpl implements EntityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityServiceImpl.class);

    private static final String NAME_PATH = "basic.name";

    private AllEntities allEntities;
    private AllTypes allTypes;
    private AllEntityDrafts allEntityDrafts;
    private AllEntityAudits allEntityAudits;
    private MDSConstructor mdsConstructor;

    private BundleContext bundleContext;
    private EntityValidator entityValidator;
    private ComboboxDataMigrationHelper comboboxDataMigrationHelper;

    @Override
    @Transactional
    public Long getCurrentSchemaVersion(String className) {
        Entity entity = allEntities.retrieveByClassName(className);
        assertEntityExists(entity);

        return entity.getEntityVersion();
    }

    @Override
    @Transactional
    public void updateComboboxValues(Long entityId, Map<String, Collection> fieldValuesToUpdate) {
        Entity entity = allEntities.retrieveById(entityId);
        assertEntityExists(entity);
        boolean doEntityUpdate = false;

        for (Map.Entry<String, Collection> fieldUpdate : fieldValuesToUpdate.entrySet()) {
            Field field = entity.getField(fieldUpdate.getKey());
            if (field == null) {
                throw new FieldNotFoundException();
            }

            ComboboxHolder cbHolder = new ComboboxHolder(field);
            List<String> cbValues = new ArrayList<>(Arrays.asList(cbHolder.getValues()));
            boolean updateField = false;

            for (Object peristedVal : fieldUpdate.getValue()) {
                String peristedValAsStr = peristedVal.toString();
                peristedValAsStr = peristedValAsStr.trim().replaceAll(" ", "%20");
                if (!cbValues.contains(peristedValAsStr)) {
                    cbValues.add(peristedValAsStr);
                    updateField = true;
                }
            }

            if (updateField) {
                FieldSetting cbValuesSetting = field.getSettingByName(Constants.Settings.COMBOBOX_VALUES);
                if (cbValuesSetting == null) {
                    throw new IllegalArgumentException("Field " + field.getName() + " is not a comboBox");
                }

                cbValuesSetting.setValue(TypeHelper.buildStringFromList(cbValues));

                doEntityUpdate = true;
            }
        }

        if (doEntityUpdate) {
            allEntities.updateAndIncrementVersion(entity);
        }
    }

    @Override
    @Transactional
    public EntityDto createEntity(EntityDto entityDto) {
        String packageName = ClassName.getPackage(entityDto.getClassName());
        boolean fromUI = StringUtils.isEmpty(packageName);
        String username = getUsername();

        if (fromUI) {
            String className;
            if (entityDto.getName().contains(" ")) {
                entityDto.setName(entityDto.getName().trim());
                StringBuilder stringBuilder = new StringBuilder();
                String[] nameParts = entityDto.getName().split(" ");
                for (String part : nameParts) {
                    if (part.length() > 0) {
                        stringBuilder.append(Character.toUpperCase(part.charAt(0)));
                        if (part.length() > 1) {
                            stringBuilder.append(part.substring(1));
                        }
                    }
                }
                className = String.format("%s.%s", Constants.PackagesGenerated.ENTITY, stringBuilder.toString());
            } else {
                // in this situation entity.getName() returns a simple name of class
                className = String.format("%s.%s", Constants.PackagesGenerated.ENTITY, entityDto.getName());
            }
            entityDto.setClassName(className);
        }

        if (allEntities.contains(entityDto.getClassName())) {
            throw new EntityAlreadyExistException();
        }

        Entity entity = allEntities.create(entityDto);

        LOGGER.debug("Adding default fields to the entity which do not extend MdsEntity");
        if (!MdsEntity.class.getName().equalsIgnoreCase(entityDto.getSuperClass())) {
            EntityHelper.addDefaultFields(entity, allTypes);
        }

        if (username != null) {
            allEntityAudits.createAudit(entity, username);
        }

        return entity.toDto();
    }

    @Override
    @Transactional
    public DraftResult saveDraftEntityChanges(Long entityId, DraftData draftData, String username) {
        EntityDraft draft = getEntityDraft(entityId, username);

        if (draftData.isCreate()) {
            createFieldForDraft(draft, draftData);
        } else if (draftData.isEdit()) {
            draftEdit(draft, draftData);
        } else if (draftData.isRemove()) {
            draftRemove(draft, draftData);
        }

        return new DraftResult(draft.isChangesMade(), draft.isOutdated());
    }

    @Override
    @Transactional
    public DraftResult saveDraftEntityChanges(Long entityId, DraftData draftData) {
        return saveDraftEntityChanges(entityId, draftData, getUsername());
    }

    private void draftEdit(EntityDraft draft, DraftData draftData) {
        if (draftData.isForAdvanced()) {
            editAdvancedForDraft(draft, draftData);
        } else if (draftData.isForField()) {
            editFieldForDraft(draft, draftData);
        } else if (draftData.isForSecurity()) {
            editSecurityForDraft(draft, draftData);
        }
    }

    private void editSecurityForDraft(EntityDraft draft, DraftData draftData) {
        List value = (List) draftData.getValue(DraftData.VALUE);
        if (value != null) {
            String securityModeName = (String) value.get(0);
            SecurityMode securityMode = SecurityMode.getEnumByName(securityModeName);

            if (value.size() > 1) {
                List<String> list = (List<String>) value.get(1);
                draft.setSecurity(securityMode, list);
            } else {
                draft.setSecurityMode(securityMode);
            }

            allEntityDrafts.update(draft);
        }
    }

    private void editFieldForDraft(EntityDraft draft, DraftData draftData) {
        String fieldIdStr = draftData.getValue(DraftData.FIELD_ID).toString();
        if (StringUtils.isNotBlank(fieldIdStr)) {
            Long fieldId = Long.valueOf(fieldIdStr);
            Field field = draft.getField(fieldId);

            if (field != null) {
                String path = draftData.getPath();
                List value = (List) draftData.getValue(DraftData.VALUE);

                // Convert to dto for UI updates
                FieldDto dto = field.toDto();
                FieldHelper.setField(dto, path, value);

                //If field name was changed add this change to map
                if (NAME_PATH.equals(path)) {
                    Map<String, String> map = draft.getFieldNameChanges();

                    //Checking if field name was previously changed and updating new name in map or adding new entry
                    if (map.containsValue(field.getName())) {
                        for (String key : map.keySet()) {
                            if (field.getName().equals(map.get(key))) {
                                map.put(key, value.get(0).toString());
                            }
                        }
                    } else {
                        map.put(field.getName(), value.get(0).toString());
                    }
                    draft.setFieldNameChanges(map);
                }

                // Perform update
                field.update(dto);
                allEntityDrafts.update(draft);
            }
        }
    }

    private void editAdvancedForDraft(EntityDraft draft, DraftData draftData) {
        AdvancedSettingsDto advancedDto = draft.advancedSettingsDto();
        String path = draftData.getPath();
        List value = (List) draftData.getValue(DraftData.VALUE);
        entityValidator.validateAdvancedSettingsEdit(draft, path);
        FieldHelper.setField(advancedDto, path, value);
        setLookupMethodNames(advancedDto);

        draft.updateAdvancedSetting(advancedDto);

        allEntityDrafts.update(draft);
    }

    private void setLookupMethodNames(AdvancedSettingsDto advancedDto) {
        for (LookupDto lookup : advancedDto.getIndexes()) {
            lookup.setMethodName(LookupName.lookupMethod(lookup.getLookupName()));
        }
    }

    private void createFieldForDraft(EntityDraft draft, DraftData draftData) {
        String typeClass = draftData.getValue(DraftData.TYPE_CLASS).toString();
        String displayName = draftData.getValue(DraftData.DISPLAY_NAME).toString();
        String name = draftData.getValue(DraftData.NAME).toString();

        Type type = ("textArea".equalsIgnoreCase(typeClass)) ? allTypes.retrieveByClassName("java.lang.String") :
                allTypes.retrieveByClassName(typeClass);

        if (type == null) {
            throw new NoSuchTypeException();
        }

        Set<Lookup> fieldLookups = new HashSet<>();

        Field field = new Field(draft, name, displayName, fieldLookups);
        field.setType(type);

        if (type.hasSettings()) {
            for (TypeSetting setting : type.getSettings()) {
                field.addSetting(new FieldSetting(field, setting));
            }
        }

        if (type.hasValidation()) {
            for (TypeValidation validation : type.getValidations()) {
                field.addValidation(new FieldValidation(field, validation));
            }
        }

        if (TypeDto.BLOB.getTypeClass().equals(typeClass)) {
            field.setUIDisplayable(false);
        } else {
            field.setUIDisplayable(true);
            field.setUIDisplayPosition((long) draft.getFields().size());
        }

        if ("textArea".equalsIgnoreCase(typeClass)) {
            setMetadataForTextArea(field);
        }

        FieldHelper.addMetadataForRelationship(typeClass, field);
        draft.addField(field);
        allEntityDrafts.update(draft);
    }

    private void setMetadataForTextArea(Field field) {
        if (field != null) {
            for (FieldSetting setting : field.getSettings()) {
                if ("mds.form.label.textarea".equalsIgnoreCase(setting.getDetails().getName())) {
                    setting.setValue("true");
                }
            }
        }
    }

    private void draftRemove(EntityDraft draft, DraftData draftData) {
        Long fieldId = Long.valueOf(draftData.getValue(DraftData.FIELD_ID).toString());

        // will throw exception if it is used
        entityValidator.validateFieldNotUsedByLookups(draft, fieldId);

        draft.removeField(fieldId);
        allEntityDrafts.update(draft);
    }


    @Override
    @Transactional
    public void abandonChanges(Long entityId) {
        EntityDraft draft = getEntityDraft(entityId);
        if (draft != null) {
            allEntityDrafts.delete(draft);
        }
    }

    @Override
    @Transactional
    public List<String> commitChanges(Long entityId, String changesOwner) {
        List<String> modulesToRefresh = new ArrayList<>();
        EntityDraft draft = getEntityDraft(entityId, changesOwner);
        if (draft.isOutdated()) {
            throw new EntityChangedException();
        }

        entityValidator.validateEntity(draft);

        Entity parent = draft.getParentEntity();
        String username = draft.getDraftOwnerUsername();

        mdsConstructor.updateFields(parent.getId(), draft.getFieldNameChanges());
        comboboxDataMigrationHelper.migrateComboboxDataIfNecessary(parent, draft);

        configureRelatedFields(parent, draft, modulesToRefresh);
        parent.updateFromDraft(draft);

        if (username != null) {
            allEntityAudits.createAudit(parent, username);
        }

        allEntityDrafts.delete(draft);
        addModuleToRefresh(parent, modulesToRefresh);

        return modulesToRefresh;
    }

    private void addModuleToRefresh(Entity entity, List<String> modulesToRefresh) {
        if (entity.isDDE() && !modulesToRefresh.contains(entity.getModule())) {
            modulesToRefresh.add(entity.getModule());
        }
    }


    private void configureRelatedFields(Entity entity, EntityDraft draft, List<String> modulesToRefresh) {
        Map<String, String> fieldNameChanges = draft.getFieldNameChanges();
        Map<String, Field> draftManyToMany = new HashMap<>();
        Map<String, Field> entityManyToMany = new HashMap<>();
        List<Field> fieldsToRemove = new ArrayList<>();
        retrieveRelatedFields(entity, draft, draftManyToMany, entityManyToMany, fieldsToRemove);

        for (String name : draftManyToMany.keySet()) {
            if (entityManyToMany.containsKey(name)) {
                updateRelatedField(entityManyToMany.get(name), draftManyToMany.get(name), modulesToRefresh);
            } else {
                if (fieldNameChanges.containsValue(name)) {
                    String key = getOldName(fieldNameChanges, name);
                    for (String k : fieldNameChanges.keySet()) {
                        if (fieldNameChanges.get(k).equals(name)) {
                            key = k;
                        }
                    }

                    updateRelatedField(entityManyToMany.get(key), draftManyToMany.get(name), modulesToRefresh);
                } else {
                    addRelatedField(draftManyToMany.get(name), modulesToRefresh);
                }
            }
        }
        removeRelatedFields(fieldsToRemove, modulesToRefresh);
    }

    private void retrieveRelatedFields(Entity entity, EntityDraft draft, Map<String, Field> draftManyToMany, Map<String, Field> entityManyToMany,
                                       List<Field> fieldsToRemove) {
        for (Field draftField : draft.getFields()) {
            if (draftField.getType().getTypeClassName().equals(TypeDto.MANY_TO_MANY_RELATIONSHIP.getTypeClass())) {
                draftManyToMany.put(draftField.getName(), draftField);
            }
        }

        for (Field entityField : entity.getFields()) {
            if (entityField.getType().getTypeClassName().equals(TypeDto.MANY_TO_MANY_RELATIONSHIP.getTypeClass()) && !entityField.isReadOnly()) {
                entityManyToMany.put(entityField.getName(), entityField);
                if (!draftManyToMany.containsKey(entityField.getName()) && !draftManyToMany.containsKey(draft.getFieldNameChanges().get(entityField.getName()))) {
                    fieldsToRemove.add(entityField);
                }
            }
        }
    }

    private String getOldName(Map<String, String> fieldNameChanges, String newName) {
        for (String k : fieldNameChanges.keySet()) {
            if (fieldNameChanges.get(k).equals(newName)) {
                return k;
            }
        }
        return null;
    }

    private void removeRelatedFields(List<Field> fields, List<String> modulesToRefresh) {
        for (Field field : fields) {
            Entity entity = allEntities.retrieveByClassName(field.getMetadataValue(RELATED_CLASS));
            Field relatedField = entity.getField(field.getMetadataValue(RELATED_FIELD));
            entity.removeField(relatedField.getId());
            entity.incrementVersion();
            addModuleToRefresh(entity, modulesToRefresh);
        }
    }

    private void updateRelatedField(Field oldField, Field draftField, List<String> modulesToRefresh) {
        Entity relatedEntity = allEntities.retrieveByClassName(oldField.getMetadataValue(RELATED_CLASS));
        Field relatedField = relatedEntity.getField(oldField.getMetadataValue(RELATED_FIELD));
        boolean fieldChanged = false;
        boolean relatedEntityChanged = false;

        if (!StringUtils.equals(draftField.getMetadataValue(RELATED_CLASS), oldField.getMetadataValue(RELATED_CLASS))) {
            addRelatedField(draftField, modulesToRefresh);
            relatedEntity.removeField(relatedField.getId());
            relatedEntityChanged = true;
        }

        if (!relatedEntityChanged && !StringUtils.equals(draftField.getMetadataValue(RELATIONSHIP_COLLECTION_TYPE),
                oldField.getMetadataValue(RELATIONSHIP_COLLECTION_TYPE))) {
            relatedField.setMetadataValue(RELATIONSHIP_COLLECTION_TYPE, draftField.getMetadataValue(RELATIONSHIP_COLLECTION_TYPE));
            fieldChanged = true;
        }

        if (!relatedEntityChanged && !StringUtils.equals(draftField.getMetadataValue(RELATED_FIELD),
                oldField.getMetadataValue(RELATED_FIELD))) {
            relatedField.setName(draftField.getMetadataValue(RELATED_FIELD));
            fieldChanged = true;
        }

        if (!relatedEntityChanged && !oldField.getName().equals(draftField.getName())) {
            relatedField.setMetadataValue(RELATED_FIELD, draftField.getName());
            fieldChanged = true;
        }

        if (fieldChanged || relatedEntityChanged) {
            relatedEntity.incrementVersion();
        }
        addModuleToRefresh(relatedEntity, modulesToRefresh);
    }

    private void addRelatedField(Field draftField, List<String> modulesToRefresh) {
        Entity entity = allEntities.retrieveByClassName(draftField.getMetadataValue(RELATED_CLASS));

        String fieldName = draftField.getMetadataValue(RELATED_FIELD);
        String collectionType = draftField.getMetadataValue(RELATIONSHIP_COLLECTION_TYPE);
        String relatedClass = draftField.getEntity().getClassName();

        Set<Lookup> fieldLookups = new HashSet<>();
        Field relatedField = new Field(entity, fieldName, fieldName, false, false, null, null, fieldLookups);
        Type type = allTypes.retrieveByClassName(TypeDto.MANY_TO_MANY_RELATIONSHIP.getTypeClass());
        relatedField.setType(type);

        if (type.hasSettings()) {
            for (TypeSetting setting : type.getSettings()) {
                relatedField.addSetting(new FieldSetting(relatedField, setting));
            }
        }
        relatedField.setUIDisplayable(true);
        relatedField.setUIDisplayPosition((long) entity.getFields().size());
        FieldHelper.createMetadataForManyToManyRelationship(relatedField, relatedClass, collectionType, draftField.getName(), false);

        entity.addField(relatedField);
        entity.incrementVersion();
        addModuleToRefresh(entity, modulesToRefresh);
    }

    @Override
    @Transactional
    public List<String> commitChanges(Long entityId) {
        return commitChanges(entityId, getUsername());
    }

    @Override
    @Transactional
    public List<EntityDto> listWorkInProgress() {
        String username = getUsername();
        List<EntityDraft> drafts = allEntityDrafts.retrieveAll(username);

        List<EntityDto> entityDtoList = new ArrayList<>();
        for (EntityDraft draft : drafts) {
            if (draft.isChangesMade()) {
                entityDtoList.add(draft.toDto());
            }
        }

        return entityDtoList;
    }

    @Override
    @Transactional
    public AdvancedSettingsDto getAdvancedSettings(Long entityId) {
        return getAdvancedSettings(entityId, false);
    }

    @Override
    @Transactional
    public AdvancedSettingsDto getAdvancedSettings(Long entityId, boolean committed) {
        if (committed) {
            Entity entity = allEntities.retrieveById(entityId);
            assertEntityExists(entity);
            return addNonPersistentAdvancedSettingsData(entity.advancedSettingsDto(), entity);
        } else {
            Entity entity = getEntityDraft(entityId);
            return addNonPersistentAdvancedSettingsData(entity.advancedSettingsDto(), entity);
        }
    }

    @Override
    @Transactional
    public AdvancedSettingsDto safeGetAdvancedSettingsCommitted(String entityClassName) {
        Entity entity = allEntities.retrieveByClassName(entityClassName);
        if (entity == null) {
            return null;
        } else {
            return addNonPersistentAdvancedSettingsData(entity.advancedSettingsDto(), entity);
        }
    }

    @Override
    @Transactional
    public void updateRestOptions(Long entityId, RestOptionsDto restOptionsDto) {
        Entity entity = allEntities.retrieveById(entityId);
        assertEntityExists(entity);

        entity.updateRestOptions(restOptionsDto);
    }

    @Override
    @Transactional
    public void updateTracking(Long entityId, TrackingDto trackingDto) {
        Entity entity = allEntities.retrieveById(entityId);
        assertEntityExists(entity);

        entity.updateTracking(trackingDto);
    }

    @Override
    @Transactional
    public void addLookups(EntityDto entityDto, LookupDto... lookups) {
        addLookups(entityDto.getId(), Arrays.asList(lookups));
    }

    @Override
    @Transactional
    public void addLookups(EntityDto entityDto, Collection<LookupDto> lookups) {
        addLookups(entityDto.getId(), lookups);
    }

    @Override
    @Transactional
    public void addLookups(Long entityId, LookupDto... lookups) {
        addLookups(entityId, Arrays.asList(lookups));
    }

    @Override
    @Transactional
    public void addLookups(Long entityId, Collection<LookupDto> lookups) {
        Entity entity = allEntities.retrieveById(entityId);
        assertEntityExists(entity);

        removeLookup(entity, lookups);
        addOrUpdateLookups(entity, lookups);
    }

    private void removeLookup(Entity entity, Collection<LookupDto> lookups) {
        Iterator<Lookup> iterator = entity.getLookups().iterator();

        while (iterator.hasNext()) {
            Lookup lookup = iterator.next();

            // don't remove user defined lookups
            if (!lookup.isReadOnly()) {
                continue;
            }

            boolean found = false;

            for (LookupDto lookupDto : lookups) {
                if (lookup.getLookupName().equalsIgnoreCase(lookupDto.getLookupName())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                iterator.remove();
            }
        }
    }

    private void addOrUpdateLookups(Entity entity, Collection<LookupDto> lookups) {
        for (LookupDto lookupDto : lookups) {
            Lookup lookup = entity.getLookupByName(lookupDto.getLookupName());
            List<Field> lookupFields = new ArrayList<>();
            for (LookupFieldDto lookupField : lookupDto.getLookupFields()) {
                String fieldName = lookupField.getName();

                Field field = entity.getField(fieldName);

                if (field == null) {
                    LOGGER.error("No field {} in entity {}", fieldName, entity.getClassName());
                } else {
                    lookupFields.add(field);
                }
            }

            if (lookup == null) {
                Lookup newLookup = new Lookup(lookupDto, lookupFields);
                entity.addLookup(newLookup);
            } else {
                lookup.update(lookupDto, lookupFields);
            }
        }
    }

    @Override
    @Transactional
    public void deleteEntity(Long entityId) {
        Entity entity = allEntities.retrieveById(entityId);

        assertWritableEntity(entity);

        if (entity.isDraft()) {
            entity = ((EntityDraft) entity).getParentEntity();
        }

        allEntityDrafts.deleteAll(entity);
        allEntities.delete(entity);
    }

    @Override
    @Transactional
    public List<EntityDto> listEntities() {
        return listEntities(false);
    }

    @Override
    @Transactional
    public List<EntityDto> listEntities(boolean withSecurityCheck) {
        List<EntityDto> entityDtos = new ArrayList<>();

        for (Entity entity : allEntities.retrieveAll()) {
            if (entity.isActualEntity()) {
                if (!withSecurityCheck || hasAccessToEntity(entity)) {
                    entityDtos.add(entity.toDto());
                }
            }
        }

        return entityDtos;
    }

    private boolean hasAccessToEntity(Entity entity) {
        SecurityMode mode = entity.getSecurityMode();
        Set<String> members = entity.getSecurityMembers();

        if (SecurityMode.USERS.equals(mode)) {
            return members.contains(getUsername());
        } else if (SecurityMode.ROLES.equals(mode)) {
            for (String role : getUserRoles()) {
                if (members.contains(role)) {
                    return true;
                }
            }

            // Only allowed roles can view, but current user
            // doesn't have any of the required roles
            return false;
        }

        // There's no user and role restriction, which means
        // the user can see this entity
        return true;
    }

    @Override
    @Transactional
    public EntityDto getEntity(Long entityId) {
        Entity entity = allEntities.retrieveById(entityId);
        return (entity == null) ? null : entity.toDto();
    }

    @Override
    @Transactional
    public EntityDto getEntityByClassName(String className) {
        Entity entity = allEntities.retrieveByClassName(className);
        return (entity == null) ? null : entity.toDto();
    }

    @Override
    @Transactional
    public List<LookupDto> getEntityLookups(Long entityId) {
        return getLookups(entityId, false);
    }

    private List<LookupDto> getLookups(Long entityId, boolean forDraft) {
        Entity entity = (forDraft) ? getEntityDraft(entityId) : allEntities.retrieveById(entityId);

        assertEntityExists(entity);

        List<LookupDto> lookupDtos = new ArrayList<>();
        for (Lookup lookup : entity.getLookups()) {
            lookupDtos.add(lookup.toDto());
        }

        return lookupDtos;
    }

    @Override
    @Transactional
    public List<FieldDto> getFields(Long entityId) {
        return getFields(entityId, true);
    }

    @Override
    @Transactional
    public List<FieldDto> getEntityFields(Long entityId) {
        return getFields(entityId, false);
    }

    private List<FieldDto> getFields(Long entityId, boolean forDraft) {
        Entity entity = (forDraft) ? getEntityDraft(entityId) : allEntities.retrieveById(entityId);

        assertEntityExists(entity);

        // the returned collection is unmodifiable
        List<Field> fields = new ArrayList<>(entity.getFields());

        // for data browser purposes, we sort the fields by their ui display order
        if (!forDraft) {
            Collections.sort(fields, new Comparator<Field>() {
                @Override
                public int compare(Field o1, Field o2) {
                    // check if one is displayable and the other isn't
                    if (o1.isUIDisplayable() && !o2.isUIDisplayable()) {
                        return -1;
                    } else if (!o1.isUIDisplayable() && o2.isUIDisplayable()) {
                        return 1;
                    }

                    // compare positions
                    Long position1 = o1.getUIDisplayPosition();
                    Long position2 = o2.getUIDisplayPosition();

                    if (position1 == null) {
                        return -1;
                    } else if (position2 == null) {
                        return 1;
                    } else {
                        return (position1 > position2) ? 1 : -1;
                    }
                }
            });
        }

        List<FieldDto> fieldDtos = new ArrayList<>();
        for (Field field : fields) {
            fieldDtos.add(field.toDto());
        }

        return addNonPersistentFieldsData(fieldDtos, entity);
    }

    @Override
    @Transactional
    public FieldDto findFieldByName(Long entityId, String name) {
        Entity entity = getEntityDraft(entityId);

        Field field = entity.getField(name);

        if (field == null) {
            throw new FieldNotFoundException();
        }

        return field.toDto();
    }

    @Override
    @Transactional
    public FieldDto findEntityFieldByName(Long entityId, String name) {
        Entity entity = allEntities.retrieveById(entityId);
        Field field = entity.getField(name);

        if (field == null) {
            throw new FieldNotFoundException();
        }

        return field.toDto();
    }

    @Override
    @Transactional
    public FieldDto getEntityFieldById(Long entityId, Long fieldId) {
        Entity entity = allEntities.retrieveById(entityId);
        Field field = entity.getField(fieldId);

        if (field == null) {
            throw new FieldNotFoundException();
        }

        return field.toDto();
    }


    @Override
    @Transactional
    public EntityDto getEntityForEdit(Long entityId) {
        Entity draft = getEntityDraft(entityId);
        return draft.toDto();
    }

    @Override
    @Transactional
    public EntityDraft getEntityDraft(Long entityId) {
        return getEntityDraft(entityId, getUsername());
    }

    @Override
    @Transactional
    public EntityDraft getEntityDraft(Long entityId, String username) {
        Entity entity = allEntities.retrieveById(entityId);

        assertEntityExists(entity);

        if (entity instanceof EntityDraft) {
            return (EntityDraft) entity;
        }

        if (username == null) {
            throw new AccessDeniedException("Cannot save draft - no user");
        }

        // get the draft
        EntityDraft draft = allEntityDrafts.retrieve(entity, username);

        if (draft == null) {
            draft = allEntityDrafts.create(entity, username);
        }

        return draft;
    }

    @Override
    @Transactional
    public void addFields(EntityDto entity, Collection<FieldDto> fields) {
        addFields(entity.getId(), fields);
    }

    @Override
    @Transactional
    public void addFields(Long entityId, FieldDto... fields) {
        addFields(entityId, Arrays.asList(fields));
    }

    @Override
    @Transactional
    public void addFields(EntityDto entity, FieldDto... fields) {
        addFields(entity.getId(), Arrays.asList(fields));
    }

    @Override
    @Transactional
    public void addFields(Long entityId, Collection<FieldDto> fields) {
        Entity entity = allEntities.retrieveById(entityId);

        assertEntityExists(entity);

        removeFields(entity, fields);

        for (FieldDto fieldDto : fields) {
            Field existing = entity.getField(fieldDto.getBasic().getName());

            if (null != existing) {
                existing.update(fieldDto);
            } else {
                addField(entity, fieldDto);
            }
        }
    }

    private void removeFields(Entity entity, Collection<FieldDto> fields) {
        Iterator<Field> iterator = entity.getFields().iterator();

        while (iterator.hasNext()) {
            Field field = iterator.next();

            // don't remove user defined fields
            if (!field.isReadOnly() || field.getMetadata(AUTO_GENERATED) != null ||
                    field.getMetadata(AUTO_GENERATED_EDITABLE) != null) {
                continue;
            }

            boolean found = false;

            for (FieldDto fieldDto : fields) {
                if (field.getName().equalsIgnoreCase(fieldDto.getBasic().getName())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                iterator.remove();
            }
        }
    }

    private void addField(Entity entity, FieldDto fieldDto) {
        FieldBasicDto basic = fieldDto.getBasic();
        String typeClass = fieldDto.getType().getTypeClass();

        Type type = allTypes.retrieveByClassName(typeClass);
        Field field = new Field(
                entity, basic.getName(), basic.getDisplayName(), basic.isRequired(), fieldDto.isReadOnly(),
                (String) basic.getDefaultValue(), basic.getTooltip(), null
        );
        field.setType(type);

        if (type.hasSettings()) {
            for (TypeSetting setting : type.getSettings()) {
                SettingDto settingDto = fieldDto.getSetting(setting.getName());
                FieldSetting fieldSetting = new FieldSetting(field, setting);

                if (null != settingDto) {
                    fieldSetting.setValue(settingDto.getValueAsString());
                }

                field.addSetting(fieldSetting);
            }
        }

        if (type.hasValidation()) {
            for (TypeValidation validation : type.getValidations()) {
                FieldValidation fieldValidation = new FieldValidation(field, validation);

                FieldValidationDto validationDto = fieldDto.getValidation();
                if (null != validationDto) {
                    ValidationCriterionDto criterion = validationDto
                            .getCriterion(validation.getDisplayName());

                    if (null != criterion) {
                        fieldValidation.setValue(criterion.valueAsString());
                        fieldValidation.setEnabled(criterion.isEnabled());
                    }
                }

                field.addValidation(fieldValidation);
            }
        }

        for (MetadataDto metadata : fieldDto.getMetadata()) {
            field.addMetadata(new FieldMetadata(metadata));
        }

        entity.addField(field);
    }

    @Override
    @Transactional
    public void addFilterableFields(EntityDto entityDto, Collection<String> fieldNames) {
        Entity entity = allEntities.retrieveById(entityDto.getId());

        assertEntityExists(entity);

        for (Field field : entity.getFields()) {
            boolean isUIFilterable = fieldNames.contains(field.getName());
            field.setUIFilterable(isUIFilterable);
        }
    }

    @Override
    @Transactional
    public EntityDto updateDraft(Long entityId) {
        Entity entity = allEntities.retrieveById(entityId);
        EntityDraft draft = getEntityDraft(entityId);

        allEntityDrafts.setProperties(draft, entity);
        draft.setChangesMade(false);

        return draft.toDto();
    }

    @Override
    @Transactional
    public LookupDto getLookupByName(Long entityId, String lookupName) {
        Entity entity = allEntities.retrieveById(entityId);
        assertEntityExists(entity);

        Lookup lookup = entity.getLookupByName(lookupName);
        return (lookup == null) ? null : lookup.toDto();
    }

    @Override
    @Transactional
    public List<FieldDto> getDisplayFields(Long entityId) {
        Entity entity = allEntities.retrieveById(entityId);
        assertEntityExists(entity);

        List<FieldDto> displayFields = new ArrayList<>();
        for (Field field : entity.getFields()) {
            if (field.isUIDisplayable()) {
                displayFields.add(field.toDto());
            }
        }

        return displayFields;
    }

    @Override
    @Transactional
    public void addNonEditableFields(EntityDto entityDto, Collection<String> fieldNames) {
        Entity entity = allEntities.retrieveById(entityDto.getId());

        assertEntityExists(entity);

        List<Field> fields = entity.getFields();

        for (Field field : fields) {
            if (fieldNames.contains(field.getName())) {
                field.setNonEditable(true);
            }
        }
    }

    @Override
    @Transactional
    public void addDisplayedFields(EntityDto entityDto, Map<String, Long> positions) {
        Entity entity = allEntities.retrieveById(entityDto.getId());

        assertEntityExists(entity);

        List<Field> fields = entity.getFields();


        if (MapUtils.isEmpty(positions)) {
            // all fields will be added

            for (long i = 0; i < fields.size(); ++i) {
                Field field = fields.get((int) i);
                // user fields and auto generated fields are ignored
                if (isFieldFromDde(field)) {
                    field.setUIDisplayable(true);
                    field.setUIDisplayPosition(i);
                }
            }
        } else {
            // only fields in map should be added

            for (Field field : fields) {
                String fieldName = field.getName();

                boolean isUIDisplayable = positions.containsKey(fieldName);
                Long uiDisplayPosition = positions.get(fieldName);

                field.setUIDisplayable(isUIDisplayable);
                field.setUIDisplayPosition(uiDisplayPosition);
            }
        }
    }

    @Override
    @Transactional
    public void updateSecurityOptions(Long entityId, SecurityMode securityMode, Set<String> securityMembers) {
        Entity entity = allEntities.retrieveById(entityId);

        assertEntityExists(entity);

        entity.setSecurityMode(securityMode);
        entity.setSecurityMembers(securityMembers);

        allEntities.update(entity);
    }

    @Override
    @Transactional
    public void updateMaxFetchDepth(Long entityId, Integer maxFetchDepth) {
        Entity entity = allEntities.retrieveById(entityId);
        assertEntityExists(entity);

        entity.setMaxFetchDepth(maxFetchDepth);

        allEntities.update(entity);
    }

    private void assertEntityExists(Entity entity) {
        if (entity == null) {
            throw new EntityNotFoundException();
        }
    }

    private void assertWritableEntity(Entity entity) {
        assertEntityExists(entity);

        if (entity.isDDE()) {
            throw new EntityReadOnlyException();
        }
    }

    private boolean isFieldFromDde(Field field) {
        // only readonly fields are considered
        if (field.isReadOnly()) {
            // check metadata for auto generated
            for (String mdKey : Arrays.asList(AUTO_GENERATED, AUTO_GENERATED_EDITABLE)) {
                FieldMetadata metaData = field.getMetadata(mdKey);
                if (metaData != null && TRUE.equals(metaData.getValue())) {
                    return false;
                }
            }
            // readonly and no auto generated metadata
            return true;
        }
        // not readonly, defined by user
        return false;
    }

    private List<FieldDto> addNonPersistentFieldsData(List<FieldDto> fieldDtos, Entity entity) {
        List<LookupDto> lookupDtos = new ArrayList<>();
        for (FieldDto fieldDto : fieldDtos) {
            List<LookupDto> fieldLookups = fieldDto.getLookups();
            if (fieldLookups != null) {
                lookupDtos.addAll(fieldLookups);
            }
        }
        addLookupsReferences(lookupDtos, entity.getClassName());
        return fieldDtos;
    }

    private AdvancedSettingsDto addNonPersistentAdvancedSettingsData(AdvancedSettingsDto advancedSettingsDto, Entity entity) {
        addLookupsReferences(advancedSettingsDto.getIndexes(), entity.getClassName());
        return advancedSettingsDto;
    }

    private void addLookupsReferences(Collection<LookupDto> lookupDtos, String entityClassName) {
        MotechDataService dataSourceDataService = OSGiServiceUtils.findService(
                bundleContext, MotechClassPool.getInterfaceName(DATA_SOURCE_CLASS_NAME));
        if (dataSourceDataService != null) {
            for (LookupDto lookupDto : lookupDtos) {
                Long count = (Long) dataSourceDataService.executeQuery(createLookupReferenceQuery(lookupDto.getLookupName(), entityClassName));
                if (count > 0) {
                    lookupDto.setReferenced(true);
                } else {
                    lookupDto.setReferenced(false);
                }
            }
        }
    }

    @Autowired
    public void setAllEntities(AllEntities allEntities) {
        this.allEntities = allEntities;
    }

    @Autowired
    public void setAllTypes(AllTypes allTypes) {
        this.allTypes = allTypes;
    }

    @Autowired
    public void setAllEntityDrafts(AllEntityDrafts allEntityDrafts) {
        this.allEntityDrafts = allEntityDrafts;
    }

    @Autowired
    public void setAllEntityAudits(AllEntityAudits allEntityAudits) {
        this.allEntityAudits = allEntityAudits;
    }

    @Autowired
    public void setMDSConstructor(MDSConstructor mdsConstructor) {
        this.mdsConstructor = mdsConstructor;
    }

    @Autowired
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Autowired
    public void setEntityValidator(EntityValidator entityValidator) {
        this.entityValidator = entityValidator;
    }

    @Autowired
    public void setComboboxDataMigrationHelper(ComboboxDataMigrationHelper comboboxDataMigrationHelper) {
        this.comboboxDataMigrationHelper = comboboxDataMigrationHelper;
    }
}
