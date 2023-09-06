package com.etendoerp.dynamic.app.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.etendoerp.dynamic.app.data.DynamicApp;
import com.etendoerp.dynamic.app.data.DynamicAppVersion;
import com.etendoerp.dynamic.app.data.DynamicRoleApp;
import com.smf.securewebservices.rsql.OBRestUtils;
import com.smf.securewebservices.service.BaseWebService;
import com.smf.securewebservices.utils.SecureWebServicesUtils;
import com.smf.securewebservices.utils.WSResult;

public class AppsForUserServlet extends BaseWebService {

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
    String authStr = request.getHeader("Authorization");
    String token = null;
    if (authStr != null && authStr.startsWith("Bearer ")) {
      token = authStr.substring(7);
    }

    Map<String, String> requestParams = OBRestUtils.requestParamsToMap(request);
    requestParams.put("token", token);

    WSResult result = get(path, requestParams);
    OBRestUtils.writeWSResponse(result, response);
  }

  @Override
  public WSResult get(String path, Map<String, String> requestParams) throws Exception {
    String token = requestParams.get("token");
    DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);
    String roleId = decodedToken.getClaim("role").asString();

    JSONArray appsArray = new JSONArray();
    WSResult wsResult = new WSResult();
    try {
      OBContext.setAdminMode(true);
      Role role = OBContext.getOBContext().getRole();
      if (!StringUtils.isEmpty(roleId)) {
        role = OBDal.getInstance().get(Role.class, roleId);
      }

      for (DynamicRoleApp roleApp : role.getETDAPPDynamicRoleAppList()) {
        JSONObject roleAppJson = new JSONObject();

        roleAppJson.put("id", roleApp.getId());
        roleAppJson.put("etdappApp", roleApp.getEtdappApp().getId());
        roleAppJson.put("etdappAppName", roleApp.getEtdappApp().getName());
        DynamicAppVersion version = roleApp.getEtdappAppVersion() != null ? roleApp.getEtdappAppVersion() : getVersionDefault(
            roleApp.getEtdappApp());
        roleAppJson.put("etdappAppVersion", version.getId());
        roleAppJson.put("etdappAppVersionName", version.getName());
        if (version.isDevelopment()) {
          roleAppJson.put("etdappAppVersionIsDev", version.isDevelopment());
        }
        String strBaseDesign = DalContextListener.getServletContext().getRealPath("") + "src-loc/design/";
        String strDirectory = roleApp.getEtdappApp().getDirectoryLocation();
        String strFileName = version.getFileName();

        strDirectory = strDirectory.replace("@basedesign@", strBaseDesign);
        String strFinalPath = strDirectory + strFileName;
        strFinalPath = strFinalPath.replace("//", "/");

        roleAppJson.put("path", strFinalPath);

        appsArray.put(roleAppJson);
      }

      wsResult.setStatus(WSResult.Status.OK);
      wsResult.setData(appsArray);

    } catch (OBException e) {
      wsResult.setStatus(WSResult.Status.NOT_FOUND);
      JSONObject error = new JSONObject();
      error.put("Error", e.getMessage());
      appsArray.put(error);
      wsResult.setData(appsArray);
      return wsResult;
    } catch (Exception e) {
      wsResult.setStatus(WSResult.Status.BAD_REQUEST);
      JSONObject error = new JSONObject();
      error.put("Error", e.getMessage());
      appsArray.put(error);
      wsResult.setData(appsArray);
      return wsResult;
    } finally {
      OBContext.restorePreviousMode();
    }

    return wsResult;
  }

  @Override
  public WSResult post(String path, Map<String, String> parameters, JSONObject body) throws Exception {
    return null;
  }

  @Override
  public WSResult put(String path, Map<String, String> parameters, JSONObject body) throws Exception {
    return null;
  }

  @Override
  public WSResult delete(String path, Map<String, String> parameters, JSONObject body) throws Exception {
    return null;
  }

  private DynamicAppVersion getVersionDefault(DynamicApp etdappApp) {
    OBCriteria<DynamicAppVersion> cAppVersion = OBDal.getInstance().createCriteria(DynamicAppVersion.class);
    cAppVersion.add(Restrictions.eq(DynamicAppVersion.PROPERTY_ETDAPPAPP, etdappApp));
    cAppVersion.add(Restrictions.eq(DynamicAppVersion.PROPERTY_DEFAULT, true));
    cAppVersion.setMaxResults(1);
    DynamicAppVersion version = (DynamicAppVersion) cAppVersion.uniqueResult();
    if (version == null) {
      throw new OBException(String.format(OBMessageUtils.messageBD("ETDAPP_NoDefaultVersion"), etdappApp.getName()));
    }
    return version;
  }
}
