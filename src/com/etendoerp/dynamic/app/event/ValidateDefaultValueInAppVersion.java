package com.etendoerp.dynamic.app.event;


import javax.enterprise.event.Observes;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import com.etendoerp.dynamic.app.data.DynamicAppVersion;

public class ValidateDefaultValueInAppVersion extends EntityPersistenceEventObserver {
  private static final Entity[] entities = { ModelProvider.getInstance().getEntity(DynamicAppVersion.ENTITY_NAME) };
  private static final Logger logger = LogManager.getLogger();

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final DynamicAppVersion bpDocument = (DynamicAppVersion) event.getTargetInstance();
    existsOtherRecord(bpDocument);
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final DynamicAppVersion bpDocument = (DynamicAppVersion) event.getTargetInstance();
    existsOtherRecord(bpDocument);
  }

  private void existsOtherRecord(DynamicAppVersion appVersion) {
    if (appVersion.isDefault()) {
      OBCriteria<DynamicAppVersion> cAppVersion = OBDal.getInstance().createCriteria(DynamicAppVersion.class);
      cAppVersion.add(Restrictions.eq(DynamicAppVersion.PROPERTY_ETDAPPAPP, appVersion.getEtdappApp()));
      cAppVersion.add(Restrictions.eq(DynamicAppVersion.PROPERTY_DEFAULT, true));
      cAppVersion.add(Restrictions.ne(DynamicAppVersion.PROPERTY_ID, appVersion.getId()));
      cAppVersion.setMaxResults(1);
      if (cAppVersion.uniqueResult() != null) {
        throw new OBException(OBMessageUtils.messageBD("ETINTER_Exists_Other_Record"));
      }
    }
  }

}
