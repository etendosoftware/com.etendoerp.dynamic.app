package com.etendoerp.dynamic.app.event;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.hibernate.criterion.Criterion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.openbravo.model.ad.access.Role;

import com.etendoerp.dynamic.app.data.DynamicApp;
import com.etendoerp.dynamic.app.data.DynamicRoleApp;

import java.lang.reflect.Field;

/**
 * Unit tests for the {@link ValidateAppConfigurationByRole} class.
 *
 * This class verifies that the role-based app configuration is correctly validated
 * in scenarios where entity update or creation events are triggered.
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidateAppConfigurationByRoleTest {

  private ValidateAppConfigurationByRole validator;

  @Mock
  private EntityUpdateEvent updateEvent;

  @Mock
  private EntityNewEvent newEvent;

  @Mock
  private DynamicRoleApp roleApp;

  @Mock
  private DynamicApp app;

  @Mock
  private Role role;

  @Mock
  private OBDal obDal;

  @Mock
  private OBCriteria<DynamicRoleApp> criteria;

  /**
   * Sets up the test environment before each test execution.
   *
   * <p>Initializes the {@code validator} instance, configures mock return values
   * for key methods, and prepares mock objects to simulate the environment needed
   * for testing role-based app configuration validations.
   */
  @Before
  public void setUp() {
    validator = new ValidateAppConfigurationByRole();

    when(roleApp.getId()).thenReturn("testId");
    when(roleApp.getEtdappApp()).thenReturn(app);
    when(roleApp.getRole()).thenReturn(role);
    when(newEvent.getTargetInstance()).thenReturn(roleApp);

  }

  /**
   * Tests that the observed entities are correctly retrieved.
   * Verifies that the entities array is not null and contains
   * the expected entity name.
   */
  @Test
  public void testGetObservedEntities() {
    try {
      Field entitiesField = ValidateAppConfigurationByRole.class.getDeclaredField("entities");
      entitiesField.setAccessible(true);
      Entity[] entities = (Entity[]) entitiesField.get(validator);

      assertNotNull("Entities should not be null", entities);
      assertEquals("Should have one entity", 1, entities.length);
      assertEquals("Entity name should match",
          DynamicRoleApp.ENTITY_NAME,
          entities[0].getName());
    } catch (ReflectiveOperationException e) {
      fail("Failed to access entities field: " + e.getMessage());
    }
  }

  /**
   * Tests that the onUpdate method does not proceed if the event is invalid.
   * Mocks the event to always return false for isValidEvent, then verifies that
   * getTargetInstance is never called.
   */
  @Test
  public void testOnUpdateInvalidEvent() {
    ValidateAppConfigurationByRole testValidator = new ValidateAppConfigurationByRole() {
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
   * Mocks the event to always return false for isValidEvent, then verifies that
   * getTargetInstance is never called.
   */
  @Test
  public void testOnSaveInvalidEvent() {
    ValidateAppConfigurationByRole testValidator = new ValidateAppConfigurationByRole() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return false;
      }
    };

    testValidator.onSave(newEvent);
    verify(newEvent, never()).getTargetInstance();
  }

  /**
   * Tests that an exception is thrown if an existing record is found during the onSave event.
   * Mocks the OBDal and OBMessageUtils, and verifies that an OBException is thrown
   * with the expected message.
   */
  @Test
  public void testOnSaveWithExistingRecord() {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBMessageUtils> messageUtilsMock = mockStatic(OBMessageUtils.class)) {

      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(DynamicRoleApp.class)).thenReturn(criteria);
      when(criteria.add(any())).thenReturn(criteria);
      when(criteria.setMaxResults(anyInt())).thenReturn(criteria);
      when(criteria.uniqueResult()).thenReturn(mock(DynamicRoleApp.class));

      messageUtilsMock.when(() -> OBMessageUtils.messageBD("ETDAPP_ExistsAppForRole"))
          .thenReturn("App already exists for this role");

      validator = new ValidateAppConfigurationByRole() {
        @Override
        protected boolean isValidEvent(EntityPersistenceEvent event) {
          return true;
        }
      };

      Exception exception = assertThrows(OBException.class, () ->
          validator.onSave(newEvent));

      assertEquals("App already exists for this role", exception.getMessage());
    }
  }
  /**
   * Tests that the onUpdate method handles cases where no existing record is found.
   * Mocks the behavior of OBDal to return null when checking for existing records,
   * and verifies that the criteria is correctly queried without throwing exceptions.
   */
  @Test
  public void testOnUpdateWithNoExistingRecord() {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      when(updateEvent.getTargetInstance()).thenReturn(roleApp);
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(DynamicRoleApp.class)).thenReturn(criteria);
      when(criteria.add(any(Criterion.class))).thenReturn(criteria);
      when(criteria.setMaxResults(anyInt())).thenReturn(criteria);
      when(criteria.uniqueResult()).thenReturn(null);

      validator = new ValidateAppConfigurationByRole() {
        @Override
        protected boolean isValidEvent(EntityPersistenceEvent event) {
          return true;
        }
      };

      validator.onUpdate(updateEvent);

      verify(criteria).uniqueResult();
    }
  }
  /**
   * Tests that the onSave method handles cases where no existing record is found.
   * Mocks the behavior of OBDal to return null when checking for existing records,
   * and verifies that the criteria is correctly queried without throwing exceptions.
   */
  @Test
  public void testOnSaveWithNoExistingRecord() {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      when(newEvent.getTargetInstance()).thenReturn(roleApp);
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(DynamicRoleApp.class)).thenReturn(criteria);
      when(criteria.add(any(Criterion.class))).thenReturn(criteria);
      when(criteria.setMaxResults(anyInt())).thenReturn(criteria);
      when(criteria.uniqueResult()).thenReturn(null);

      validator = new ValidateAppConfigurationByRole() {
        @Override
        protected boolean isValidEvent(EntityPersistenceEvent event) {
          return true;
        }
      };

      validator.onSave(newEvent);

      verify(criteria).uniqueResult();
    }
  }
}
