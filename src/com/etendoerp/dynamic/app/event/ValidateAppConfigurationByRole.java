package com.etendoerp.dynamic.app.event;


import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.dynamic.app.data.DynamicRoleApp;

public class ValidateAppConfigurationByRole extends EntityPersistenceEventObserver {
  private static final Entity[] entities = { ModelProvider.getInstance().getEntity(DynamicRoleApp.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final DynamicRoleApp roleApp = (DynamicRoleApp) event.getTargetInstance();
    existsOtherRecord(roleApp);
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final DynamicRoleApp roleApp = (DynamicRoleApp) event.getTargetInstance();
    existsOtherRecord(roleApp);
  }

  private void existsOtherRecord(DynamicRoleApp roleApp) {
    OBCriteria<DynamicRoleApp> cRoleApp = OBDal.getInstance().createCriteria(DynamicRoleApp.class);
    cRoleApp.add(Restrictions.eq(DynamicRoleApp.PROPERTY_ETDAPPAPP, roleApp.getEtdappApp()));
    cRoleApp.add(Restrictions.ne(DynamicRoleApp.PROPERTY_ID, roleApp.getId()));
    cRoleApp.add(Restrictions.eq(DynamicRoleApp.PROPERTY_ROLE, roleApp.getRole()));
    cRoleApp.setMaxResults(1);
    if (cRoleApp.uniqueResult() != null) {
      throw new OBException(OBMessageUtils.messageBD("ETDAPP_ExistsAppForRole"));
    }
  }

}
