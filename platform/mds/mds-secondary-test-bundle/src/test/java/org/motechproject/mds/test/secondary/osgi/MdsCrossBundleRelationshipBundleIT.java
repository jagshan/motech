package org.motechproject.mds.test.secondary.osgi;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.mds.test.domain.Animal;
import org.motechproject.mds.test.domain.EntityB;
import org.motechproject.mds.test.domain.EntityC;
import org.motechproject.mds.test.domain.Priority;
import org.motechproject.mds.test.secondary.domain.EntityA;
import org.motechproject.mds.test.secondary.service.EntityADataService;
import org.motechproject.mds.test.service.EntityBDataService;
import org.motechproject.mds.test.service.EntityCDataService;
import org.motechproject.mds.util.Constants;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class MdsCrossBundleRelationshipBundleIT extends BasePaxIT {

    public static final String A_NAME = "a";
    public static final String B_NAME = "b";
    public static final String C_NAME = "c";

    public static final String A_UPDATED_NAME = "a_updated";
    public static final String B_UPDATED_NAME = "b_updated";
    public static final String C_UPDATED_NAME = "c_updated";

    @Inject
    private EntityADataService entityADataService;

    @Inject
    private EntityBDataService entityBDataService;

    @Inject
    private EntityCDataService entityCDataService;

    @After
    public void tearDown() {
        entityADataService.deleteAll();
        entityBDataService.deleteAll();
        entityCDataService.deleteAll();
    }

    @Test
    public void testEntitiesEnhancement() {
        assertEntityEnhanced(EntityA.class);
        assertEntityEnhanced(EntityB.class);
        assertEntityEnhanced(EntityC.class);
    }

    @Test
    public void testCrossBundleRelationshipCreate() {
        EntityA a = new EntityA();
        EntityB b = new EntityB();
        EntityC c = new EntityC();

        setEntitiesRelations(a, b, c);
        setEntitiesFields(a, b, c, A_NAME, B_NAME, Priority.LOW, C_NAME, Animal.CAT);

        entityADataService.create(a);

        a = entityADataService.findById(a.getId());
        b = entityBDataService.findById(b.getId());
        c = entityCDataService.findById(c.getId());

        assertEntitiesFields(a, b, c, A_NAME, B_NAME, Priority.LOW, C_NAME, Animal.CAT);

        setEntitiesFields(a, b, c, A_UPDATED_NAME, B_UPDATED_NAME, Priority.HIGH, C_UPDATED_NAME, Animal.DUCK);

        setEntitiesRelations(a, b, c);
        entityADataService.update(a);

        a = entityADataService.findById(a.getId());
        b = entityBDataService.findById(b.getId());
        c = entityCDataService.findById(c.getId());

        assertEntitiesFields(a, b, c, A_UPDATED_NAME, B_UPDATED_NAME, Priority.HIGH, C_UPDATED_NAME, Animal.DUCK);
    }

    private void setEntitiesFields(EntityA a, EntityB b, EntityC c,
                                   String aName, String bName, Priority bPriority, String cName, Animal cAnimal) {
        a.setName(aName);
        b.setName(bName);
        b.setPriority(bPriority);
        c.setName(cName);
        c.setAnimal(cAnimal);
    }

    private void setEntitiesRelations(EntityA a, EntityB b, EntityC c) {
        a.setEntityB(b);
        b.setEntityC(c);
    }

    private void assertEntitiesFields(EntityA a, EntityB b, EntityC c,
                                      String aName, String bName, Priority bPriority, String cName, Animal cAnimal) {
        assertEquals(aName, a.getName());
        assertNotNull(a.getEntityB());
        assertEquals(bName, a.getEntityB().getName());
        assertEquals(bPriority, a.getEntityB().getPriority());

        assertEquals(bName, b.getName());
        assertEquals(bPriority, b.getPriority());
        assertNotNull(b.getEntityC());
        assertEquals(cName, b.getEntityC().getName());
        assertEquals(cAnimal, b.getEntityC().getAnimal());

        assertEquals(cName, c.getName());
        assertEquals(cAnimal, c.getAnimal());
    }

    private void assertEntityEnhanced(Class<?> clazz) {
        for (String field : Constants.Util.GENERATED_FIELD_NAMES) {
            assertTrue(hasField(clazz, field));
        }
    }

    private boolean hasField(Class<?> clazz, String field) {
        try {
            clazz.getDeclaredField(field);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
