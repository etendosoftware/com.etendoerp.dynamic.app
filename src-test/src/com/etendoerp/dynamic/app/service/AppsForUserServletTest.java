package com.etendoerp.dynamic.app.service;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smf.securewebservices.rsql.OBRestUtils;
import com.smf.securewebservices.utils.SecureWebServicesUtils;
import com.smf.securewebservices.utils.WSResult;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import com.etendoerp.dynamic.app.data.DynamicApp;
import com.etendoerp.dynamic.app.data.DynamicAppVersion;
import com.etendoerp.dynamic.app.data.DynamicRoleApp;


@RunWith(MockitoJUnitRunner.class)
public class AppsForUserServletTest {

    @InjectMocks
    private AppsForUserServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private DecodedJWT decodedToken;

    @Mock
    private OBDal obDal;

    @Mock
    private OBContext obContext;

    @Mock
    private Role mockRole;

    @Mock
    private DynamicApp mockApp;

    @Mock
    private DynamicAppVersion mockVersion;

    @Mock
    private DynamicRoleApp mockRoleApp;

    @Mock
    private ServletContext servletContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        List<DynamicRoleApp> roleAppList = new ArrayList<>();
        roleAppList.add(mockRoleApp);

        when(mockRole.getETDAPPDynamicRoleAppList()).thenReturn(roleAppList);
        when(mockRoleApp.getEtdappApp()).thenReturn(mockApp);
        when(mockRoleApp.getEtdappAppVersion()).thenReturn(mockVersion);
        when(mockApp.getName()).thenReturn("TestApp");
        when(mockApp.getDirectoryLocation()).thenReturn("@basedesign@/testDir");
        when(mockVersion.getName()).thenReturn("1.0");
        when(mockVersion.getFileName()).thenReturn("test.js");
        when(servletContext.getRealPath("")).thenReturn("/test/path/");

        when(obContext.getRole()).thenReturn(mockRole);
    }

    @Test
    public void testGetDefaultVersion() throws Exception {
        OBCriteria<DynamicAppVersion> mockCriteria = mock(OBCriteria.class);
        when(obDal.createCriteria(DynamicAppVersion.class)).thenReturn(mockCriteria);
        when(mockCriteria.add(any())).thenReturn(mockCriteria);
        when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
        when(mockCriteria.uniqueResult()).thenReturn(mockVersion);

        try (MockedStatic<OBDal> obDalMock = Mockito.mockStatic(OBDal.class)) {
            obDalMock.when(OBDal::getInstance).thenReturn(obDal);

            java.lang.reflect.Method method = AppsForUserServlet.class.getDeclaredMethod("getVersionDefault", DynamicApp.class);
            method.setAccessible(true);
            DynamicAppVersion result = (DynamicAppVersion) method.invoke(servlet, mockApp);

            assertNotNull(result);
            assertEquals(mockVersion, result);
        }
    }
    @Test
    public void testGetWithNullAppVersion() throws Exception {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("token", "valid-token");
        when(mockRoleApp.getEtdappAppVersion()).thenReturn(null);

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<OBDal> obDalMock = Mockito.mockStatic(OBDal.class);
             MockedStatic<DalContextListener> dalContext = Mockito.mockStatic(DalContextListener.class);
             MockedStatic<OBContext> obContextMock = Mockito.mockStatic(OBContext.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            when(decodedToken.getClaim("role")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));

            obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
            when(obContext.getRole()).thenReturn(mockRole);

            obDalMock.when(OBDal::getInstance).thenReturn(obDal);
            OBCriteria<DynamicAppVersion> mockCriteria = mock(OBCriteria.class);
            when(obDal.createCriteria(DynamicAppVersion.class)).thenReturn(mockCriteria);
            when(mockCriteria.add(any())).thenReturn(mockCriteria);
            when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
            when(mockCriteria.uniqueResult()).thenReturn(mockVersion);

            dalContext.when(DalContextListener::getServletContext).thenReturn(servletContext);

            when(mockApp.getName()).thenReturn("TestApp");
            when(mockApp.getDirectoryLocation()).thenReturn("/test/path/");
            when(mockVersion.getName()).thenReturn("1.0");
            when(mockVersion.getFileName()).thenReturn("test.js");

            WSResult result = servlet.get("test-path", params);

            assertEquals(WSResult.Status.OK, result.getStatus());
        }
    }

    @Test
    public void testUnimplementedMethods() throws Exception {
        assertNull(servlet.post("test", new HashMap<>(), new JSONObject()));
        assertNull(servlet.put("test", new HashMap<>(), new JSONObject()));
        assertNull(servlet.delete("test", new HashMap<>(), new JSONObject()));
    }

    @Test
    public void testGetWithNoDefaultVersion() throws Exception {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("token", "valid-token");
        when(mockRoleApp.getEtdappAppVersion()).thenReturn(null);

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<OBDal> obDalMock = Mockito.mockStatic(OBDal.class);
             MockedStatic<OBContext> obContextMock = Mockito.mockStatic(OBContext.class);
             MockedStatic<OBMessageUtils> obMessageMock = Mockito.mockStatic(OBMessageUtils.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            when(decodedToken.getClaim("role")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));

            obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
            when(obContext.getRole()).thenReturn(mockRole);

            obDalMock.when(OBDal::getInstance).thenReturn(obDal);
            OBCriteria<DynamicAppVersion> mockCriteria = mock(OBCriteria.class);
            when(obDal.createCriteria(DynamicAppVersion.class)).thenReturn(mockCriteria);
            when(mockCriteria.add(any())).thenReturn(mockCriteria);
            when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
            when(mockCriteria.uniqueResult()).thenReturn(null);

            when(mockApp.getName()).thenReturn("TestApp");

            obMessageMock.when(() -> OBMessageUtils.messageBD("ETDAPP_NoDefaultVersion"))
                    .thenReturn("No default version found for app %s");

            WSResult result = servlet.get("test-path", params);

            assertEquals(WSResult.Status.NOT_FOUND, result.getStatus());
        }
    }

    @Test
    public void testGetWithDevelopmentVersion() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("token", "valid-token");
        when(mockRoleApp.getEtdappAppVersion()).thenReturn(mockVersion);
        when(mockVersion.isDevelopment()).thenReturn(true);

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<DalContextListener> dalContext = Mockito.mockStatic(DalContextListener.class);
             MockedStatic<OBContext> obContextMock = Mockito.mockStatic(OBContext.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            when(decodedToken.getClaim("role")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));

            obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
            when(obContext.getRole()).thenReturn(mockRole);

            dalContext.when(DalContextListener::getServletContext).thenReturn(servletContext);

            WSResult result = servlet.get("test-path", params);

            assertEquals(WSResult.Status.OK, result.getStatus());
        }
    }

    @Test
    public void testMultipleApps() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("token", "valid-token");

        List<DynamicRoleApp> roleAppList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            DynamicRoleApp mockRoleApp = mock(DynamicRoleApp.class);
            DynamicApp mockApp = mock(DynamicApp.class);
            DynamicAppVersion mockVersion = mock(DynamicAppVersion.class);

            when(mockApp.getName()).thenReturn("TestApp" + i);
            when(mockApp.getDirectoryLocation()).thenReturn("@basedesign@/testDir" + i);
            when(mockVersion.getName()).thenReturn("1." + i);
            when(mockVersion.getFileName()).thenReturn("test" + i + ".js");
            when(mockRoleApp.getEtdappApp()).thenReturn(mockApp);
            when(mockRoleApp.getEtdappAppVersion()).thenReturn(mockVersion);

            roleAppList.add(mockRoleApp);
        }

        when(mockRole.getETDAPPDynamicRoleAppList()).thenReturn(roleAppList);

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<DalContextListener> dalContext = Mockito.mockStatic(DalContextListener.class);
             MockedStatic<OBContext> obContextMock = Mockito.mockStatic(OBContext.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            when(decodedToken.getClaim("role")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
            obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
            when(obContext.getRole()).thenReturn(mockRole);
            dalContext.when(DalContextListener::getServletContext).thenReturn(servletContext);

            WSResult result = servlet.get("test-path", params);

            assertEquals(WSResult.Status.OK, result.getStatus());
        }
    }

    @Test
    public void testEmptyRoleAppList() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("token", "valid-token");

        when(mockRole.getETDAPPDynamicRoleAppList()).thenReturn(new ArrayList<>());

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<OBContext> obContextMock = Mockito.mockStatic(OBContext.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            when(decodedToken.getClaim("role")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
            obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
            when(obContext.getRole()).thenReturn(mockRole);

            WSResult result = servlet.get("test-path", params);

            assertEquals(WSResult.Status.OK, result.getStatus());
        }
    }

    @Test
    public void testNullRole() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("token", "valid-token");

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<OBContext> obContextMock = Mockito.mockStatic(OBContext.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            Claim mockClaim = mock(Claim.class);
            when(mockClaim.asString()).thenReturn("someRoleId");
            when(decodedToken.getClaim("role")).thenReturn(mockClaim);

            obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
            when(obContext.getRole()).thenReturn(null);

            WSResult result = servlet.get("test-path", params);

            assertEquals(WSResult.Status.BAD_REQUEST, result.getStatus());
        }
    }

    @Test
    public void testRoleNotFound() throws Exception {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("token", "valid-token");
        String nonExistentRoleId = "nonexistent-role-id";

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<OBDal> obDalMock = Mockito.mockStatic(OBDal.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            Claim mockClaim = mock(Claim.class);
            when(mockClaim.asString()).thenReturn(nonExistentRoleId);
            when(decodedToken.getClaim("role")).thenReturn(mockClaim);

            obDalMock.when(OBDal::getInstance).thenReturn(obDal);

            WSResult result = servlet.get("test-path", params);

            assertEquals(WSResult.Status.BAD_REQUEST, result.getStatus());
        }
    }
    @Test
    public void testMissingToken() throws Exception {
        Map<String, String> params = new HashMap<>();

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class)) {
            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(null))
                    .thenThrow(new IllegalArgumentException("The token cannot be null"));

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(""))
                    .thenThrow(new IllegalArgumentException("The token cannot be null"));

            WSResult result = servlet.get("test-path", params);
            assertEquals(WSResult.Status.BAD_REQUEST, result.getStatus());
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals("The token cannot be null", e.getMessage());
        }
    }

    @Test
    public void testInvalidToken() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("token", "invalid-token");

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class)) {
            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString()))
                    .thenThrow(new RuntimeException("Invalid token"));

            WSResult result = servlet.get("test-path", params);
            assertEquals(WSResult.Status.BAD_REQUEST, result.getStatus());
        } catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
            assertEquals("Invalid token", e.getMessage());
        }
    }

    @Test
    public void testDoGetWithValidBearerToken() throws Exception {
        String validPath = "test/path";
        String bearerToken = "Bearer valid-token-123";
        WSResult mockResult = mock(WSResult.class);

        when(request.getHeader("Authorization")).thenReturn(bearerToken);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            Map<String, String> params = new HashMap<>();
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request)).thenReturn(params);

            AppsForUserServlet spyServlet = spy(servlet);
            doReturn(mockResult).when(spyServlet).get(eq(validPath), argThat(map ->
                    map.containsKey("token") && map.get("token").equals("valid-token-123")
            ));

            spyServlet.doGet(validPath, request, response);

            obRestUtils.verify(() -> OBRestUtils.writeWSResponse(mockResult, response));
            verify(request).getHeader("Authorization");
        }
    }

    @Test
    public void testDoGetWithoutAuthorizationHeader() throws Exception {
        String validPath = "test/path";
        WSResult mockResult = mock(WSResult.class);

        when(request.getHeader("Authorization")).thenReturn(null);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            Map<String, String> params = new HashMap<>();
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request)).thenReturn(params);

            AppsForUserServlet spyServlet = spy(servlet);
            doReturn(mockResult).when(spyServlet).get(eq(validPath), argThat(map ->
                    map.containsKey("token") && map.get("token") == null
            ));

            spyServlet.doGet(validPath, request, response);

            obRestUtils.verify(() -> OBRestUtils.writeWSResponse(mockResult, response));
            verify(request).getHeader("Authorization");
        }
    }

    @Test
    public void testDoGetWithInvalidAuthorizationFormat() throws Exception {
        String validPath = "test/path";
        String invalidAuthHeader = "Basic invalid-auth";
        WSResult mockResult = mock(WSResult.class);

        when(request.getHeader("Authorization")).thenReturn(invalidAuthHeader);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            Map<String, String> params = new HashMap<>();
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request)).thenReturn(params);

            AppsForUserServlet spyServlet = spy(servlet);
            doReturn(mockResult).when(spyServlet).get(eq(validPath), argThat(map ->
                    map.containsKey("token") && map.get("token") == null
            ));

            spyServlet.doGet(validPath, request, response);

            obRestUtils.verify(() -> OBRestUtils.writeWSResponse(mockResult, response));
            verify(request).getHeader("Authorization");
        }
    }

    @Test
    public void testDoGetWithRequestParameters() throws Exception {
        String validPath = "test/path";
        String bearerToken = "Bearer valid-token-123";
        WSResult mockResult = mock(WSResult.class);

        when(request.getHeader("Authorization")).thenReturn(bearerToken);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            Map<String, String> params = new HashMap<>();
            params.put("param1", "value1");
            params.put("param2", "value2");
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request)).thenReturn(params);

            AppsForUserServlet spyServlet = spy(servlet);
            doReturn(mockResult).when(spyServlet).get(eq(validPath), argThat(map ->
                    map.containsKey("token") &&
                            map.get("token").equals("valid-token-123") &&
                            map.get("param1").equals("value1") &&
                            map.get("param2").equals("value2")
            ));

            spyServlet.doGet(validPath, request, response);

            obRestUtils.verify(() -> OBRestUtils.writeWSResponse(mockResult, response));
            verify(request).getHeader("Authorization");
        }
    }

    @Test
    public void testDoGetWithExceptionInRequestParamsToMap() throws Exception {
        String validPath = "test/path";
        String bearerToken = "Bearer valid-token-123";

        when(request.getHeader("Authorization")).thenReturn(bearerToken);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request))
                    .thenThrow(new RuntimeException("Error processing request parameters"));

            assertThrows(RuntimeException.class, () ->
                    servlet.doGet(validPath, request, response));
        }
    }


}
