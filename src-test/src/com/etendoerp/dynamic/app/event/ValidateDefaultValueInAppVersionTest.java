package com.etendoerp.dynamic.app.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.openbravo.base.util.Check.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.dynamic.app.data.DynamicAppVersion;
import com.etendoerp.dynamic.app.data.DynamicApp;

/**
 * Test class for {@link ValidateDefaultValueInAppVersion}.
 * This class validates the behavior of the default value validation logic
 * for DynamicAppVersion entities.
 * <p>
 * The tests cover both new entity creation and update scenarios, ensuring
 * that the default version constraints are properly enforced.
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidateDefaultValueInAppVersionTest {


    private static final String DEFAULT_VERSION_EXISTS_MESSAGE = "Another default version exists";
    private static final String MESSAGE_BD_KEY = "ETDAPP_ExistsOtherRecordAsDefault";
    private static final String EXISTS_OTHER_RECORD_METHOD = "existsOtherRecord";

    private ValidateDefaultValueInAppVersion validator;

    @Mock
    private EntityUpdateEvent updateEvent;

    @Mock
    private EntityNewEvent newEvent;

    @Mock
    private DynamicAppVersion appVersion;

    @Mock
    private DynamicApp app;

    @Mock
    private OBDal obDal;

    @Mock
    private OBCriteria<DynamicAppVersion> criteria;

    /**
     * Sets up the test environment before each test case.
     * Initializes the validator and configures common mock behaviors.
     */
    @Before
    public void setUp() {
        validator = new ValidateDefaultValueInAppVersion();
        when(appVersion.getId()).thenReturn("testId");
        when(appVersion.getEtdappApp()).thenReturn(app);
        when(updateEvent.getTargetInstance()).thenReturn(appVersion);
        when(newEvent.getTargetInstance()).thenReturn(appVersion);

    }

    /**
     * Tests that no validation is performed when updating a non-default version.
     */
    @Test
    public void testOnUpdateNonDefaultVersionNoValidationNeeded() {
        try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
            obDalMock.when(OBDal::getInstance).thenReturn(obDal);

            validator.onUpdate(updateEvent);

            verify(obDal, never()).createCriteria(DynamicAppVersion.class);
        }
    }

    /**
     * Tests that no validation is performed when saving a non-default version.
     */
    @Test
    public void testOnSaveNonDefaultVersionNoValidationNeeded() {
        try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
            obDalMock.when(OBDal::getInstance).thenReturn(obDal);

            validator.onSave(newEvent);

            verify(obDal, never()).createCriteria(DynamicAppVersion.class);
        }
    }

    /**
     * Tests that no validation is performed when checking for other records
     * with a non-default version.
     *
     * <p>This test ensures that no criteria are created or evaluated when
     * the version is not set as default.
     *
     * @throws Exception if there is an error accessing or invoking the method via reflection.
     */
    @Test
    public void testExistsOtherRecordNonDefaultVersionNoValidationNeeded() throws Exception {
        when(appVersion.isDefault()).thenReturn(false);

        try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
            obDalMock.when(OBDal::getInstance).thenReturn(obDal);

            Method method = ValidateDefaultValueInAppVersion.class
                .getDeclaredMethod(EXISTS_OTHER_RECORD_METHOD, DynamicAppVersion.class);
            method.setAccessible(true);
            method.invoke(validator, appVersion);

            verify(obDal, never()).createCriteria(DynamicAppVersion.class);
        }
    }

    /**
     * Tests the scenario where a default version is being validated,
     * and no other default version exists in the system.
     *
     * <p>This test verifies that the validation proceeds without throwing an
     * exception or triggering any messages when no other default version is found.
     *
     * @throws Exception if there is an error accessing or invoking the method via reflection.
     */
    @Test
    public void testExistsOtherRecordDefaultVersionNoOtherDefaultExists() throws Exception {
        when(appVersion.isDefault()).thenReturn(true);

        try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
             MockedStatic<OBMessageUtils> messageUtilsMock = mockStatic(OBMessageUtils.class)) {

            obDalMock.when(OBDal::getInstance).thenReturn(obDal);
            when(obDal.createCriteria(DynamicAppVersion.class)).thenReturn(criteria);
            when(criteria.add(any())).thenReturn(criteria);
            when(criteria.setMaxResults(anyInt())).thenReturn(criteria);
            when(criteria.uniqueResult()).thenReturn(null);

            Method method = ValidateDefaultValueInAppVersion.class
                .getDeclaredMethod(EXISTS_OTHER_RECORD_METHOD, DynamicAppVersion.class);
            method.setAccessible(true);
            method.invoke(validator, appVersion);

            verify(obDal).createCriteria(DynamicAppVersion.class);
            verify(criteria, times(3)).add(any());
            verify(criteria).setMaxResults(1);
            verify(criteria).uniqueResult();
            messageUtilsMock.verify(() -> OBMessageUtils.messageBD(anyString()), never());
        }
    }

    /**
     * Tests the scenario where a default version is being validated,
     * and another default version already exists.
     *
     * <p>This test verifies that an {@link OBException} is thrown with the correct
     * message when another default version already exists in the system.
     *
     * @throws Exception if there is an error accessing or invoking the method via reflection.
     */
    @Test
    public void testExistsOtherRecordDefaultVersionOtherDefaultExists() throws Exception {
        when(appVersion.isDefault()).thenReturn(true);
        DynamicAppVersion existingDefault = mock(DynamicAppVersion.class);

        try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
             MockedStatic<OBMessageUtils> messageUtilsMock = mockStatic(OBMessageUtils.class)) {

            obDalMock.when(OBDal::getInstance).thenReturn(obDal);
            when(obDal.createCriteria(DynamicAppVersion.class)).thenReturn(criteria);
            when(criteria.add(any())).thenReturn(criteria);
            when(criteria.setMaxResults(anyInt())).thenReturn(criteria);
            when(criteria.uniqueResult()).thenReturn(existingDefault);
            messageUtilsMock.when(() -> OBMessageUtils.messageBD(MESSAGE_BD_KEY))
                .thenReturn(DEFAULT_VERSION_EXISTS_MESSAGE);

            Method method = ValidateDefaultValueInAppVersion.class
                .getDeclaredMethod(EXISTS_OTHER_RECORD_METHOD, DynamicAppVersion.class);
            method.setAccessible(true);

            InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(validator, appVersion)
            );

            assertTrue(exception.getCause() instanceof OBException);
            assertEquals(DEFAULT_VERSION_EXISTS_MESSAGE, exception.getCause().getMessage());

            verify(obDal).createCriteria(DynamicAppVersion.class);
            verify(criteria, times(3)).add(any());
            verify(criteria).setMaxResults(1);
            verify(criteria).uniqueResult();
            messageUtilsMock.verify(
                () -> OBMessageUtils.messageBD(MESSAGE_BD_KEY),
                times(1)
            );
        }
    }

    /**
     * Tests that the correct restrictions are added when validating
     * a default version.
     *
     * @throws Exception if there is an error accessing or invoking the method via reflection.
     */
    @Test
    public void testExistsOtherRecordDefaultVersionVerifyRestrictions() throws Exception {
        when(appVersion.isDefault()).thenReturn(true);
        when(appVersion.getId()).thenReturn("testId");
        when(appVersion.getEtdappApp()).thenReturn(app);

        try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
            obDalMock.when(OBDal::getInstance).thenReturn(obDal);
            when(obDal.createCriteria(DynamicAppVersion.class)).thenReturn(criteria);
            when(criteria.add(any())).thenReturn(criteria);
            when(criteria.setMaxResults(anyInt())).thenReturn(criteria);
            when(criteria.uniqueResult()).thenReturn(null);

            Method method = ValidateDefaultValueInAppVersion.class
                .getDeclaredMethod(EXISTS_OTHER_RECORD_METHOD, DynamicAppVersion.class);
            method.setAccessible(true);
            method.invoke(validator, appVersion);

            ArgumentCaptor<Criterion> criterionCaptor = ArgumentCaptor.forClass(Criterion.class);
            verify(criteria, times(3)).add(criterionCaptor.capture());

            List<Criterion> capturedCriteria = criterionCaptor.getAllValues();
            assertEquals(3, capturedCriteria.size());
        }
    }

    /**
     * Tests onSave with invalid event using Mockito inline
     */
    @Test
    public void testOnSaveInvalidEvent() {
        ValidateDefaultValueInAppVersion testValidator = new ValidateDefaultValueInAppVersion() {
            @Override
            protected boolean isValidEvent(EntityPersistenceEvent event) {
                return false;
            }
        };

        testValidator.onSave(newEvent);

        verify(newEvent, never()).getTargetInstance();
    }

    /**
     * Verifies that {@code onUpdate} does not process an invalid event.
     */
    @Test
    public void testOnUpdateInvalidEvent() {
        ValidateDefaultValueInAppVersion testValidator = new ValidateDefaultValueInAppVersion() {
            @Override
            protected boolean isValidEvent(EntityPersistenceEvent event) {
                return false;
            }
        };

        testValidator.onUpdate(updateEvent);

        verify(updateEvent, never()).getTargetInstance();
    }

    /**
     * Tests that the onSave method does not proceed if the event is invalid.
     * Ensures that getTargetInstance is never called.
     */
    @Test
    public void testGetObservedEntities() {
        Field entitiesField;
        try {
            entitiesField = ValidateDefaultValueInAppVersion.class.getDeclaredField("entities");
            entitiesField.setAccessible(true);
            Entity[] entities = (Entity[]) entitiesField.get(validator);

            assertNotNull("Entities should not be null", entities);
            assertEquals("Should have one entity", 1, entities.length);
            assertEquals("Entity name should match",
                DynamicAppVersion.ENTITY_NAME,
                entities[0].getName());

        } catch (Exception e) {
            fail("Failed to access entities field: " + e.getMessage());
        }
    }

    /**
     * Tests that the onUpdate method does not proceed if the event is invalid.
     * Ensures that getTargetInstance is never called.
     */
    @Test
    public void testOnSaveValidEventExistingDefault() {
        when(appVersion.isDefault()).thenReturn(true);
        DynamicAppVersion existingDefault = mock(DynamicAppVersion.class);

        try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
             MockedStatic<OBMessageUtils> messageUtilsMock = mockStatic(OBMessageUtils.class)) {

            obDalMock.when(OBDal::getInstance).thenReturn(obDal);
            when(obDal.createCriteria(DynamicAppVersion.class)).thenReturn(criteria);
            when(criteria.add(any())).thenReturn(criteria);
            when(criteria.setMaxResults(anyInt())).thenReturn(criteria);
            when(criteria.uniqueResult()).thenReturn(existingDefault);
            messageUtilsMock.when(() -> OBMessageUtils.messageBD(MESSAGE_BD_KEY))
                .thenReturn(DEFAULT_VERSION_EXISTS_MESSAGE);

            validator = new ValidateDefaultValueInAppVersion() {
                @Override
                protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
                    return true;
                }
            };

            Exception exception = assertThrows(OBException.class, () -> validator.onSave(newEvent));
            assertEquals(DEFAULT_VERSION_EXISTS_MESSAGE, exception.getMessage());

            verify(criteria, times(3)).add(any());
            verify(criteria).setMaxResults(1);
            verify(criteria).uniqueResult();
        }
    }

    /**
     * Tests the retrieval of observed entities.
     * Uses reflection to access the private 'entities' field and
     * verifies that it contains the expected entity name.
     */
    @Test
    public void testOnUpdateValidEventExistingDefault() {
        when(appVersion.isDefault()).thenReturn(true);
        DynamicAppVersion existingDefault = mock(DynamicAppVersion.class);

        try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
             MockedStatic<OBMessageUtils> messageUtilsMock = mockStatic(OBMessageUtils.class)) {

            obDalMock.when(OBDal::getInstance).thenReturn(obDal);
            when(obDal.createCriteria(DynamicAppVersion.class)).thenReturn(criteria);
            when(criteria.add(any())).thenReturn(criteria);
            when(criteria.setMaxResults(anyInt())).thenReturn(criteria);
            when(criteria.uniqueResult()).thenReturn(existingDefault);
            messageUtilsMock.when(() -> OBMessageUtils.messageBD(MESSAGE_BD_KEY))
                .thenReturn(DEFAULT_VERSION_EXISTS_MESSAGE);

            validator = new ValidateDefaultValueInAppVersion() {
                @Override
                protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
                    return true;
                }
            };

            Exception exception = assertThrows(OBException.class, () -> validator.onUpdate(updateEvent));
            assertEquals(DEFAULT_VERSION_EXISTS_MESSAGE, exception.getMessage());

            verify(criteria, times(3)).add(any());
            verify(criteria).setMaxResults(1);
            verify(criteria).uniqueResult();
        }
    }

    /**
     * Tests that the onSave method successfully validates a default version
     * when no other default version exists.
     * Ensures that no exceptions are thrown and the criteria is queried correctly.
     */
    @Test
    public void testOnSaveValidEventNoExistingDefault() {
        when(appVersion.isDefault()).thenReturn(true);

        try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
            obDalMock.when(OBDal::getInstance).thenReturn(obDal);
            when(obDal.createCriteria(DynamicAppVersion.class)).thenReturn(criteria);
            when(criteria.add(any())).thenReturn(criteria);
            when(criteria.setMaxResults(anyInt())).thenReturn(criteria);
            when(criteria.uniqueResult()).thenReturn(null);

            validator = new ValidateDefaultValueInAppVersion() {
                @Override
                protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
                    return true;
                }
            };

            validator.onSave(newEvent);

            verify(criteria, times(3)).add(any());
            verify(criteria).setMaxResults(1);
            verify(criteria).uniqueResult();
        }
    }
}