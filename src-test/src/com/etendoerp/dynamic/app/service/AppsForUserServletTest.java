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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
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

/**
 * Test class for AppsForUserServlet.
 * This class tests the functionality of the AppsForUserServlet, including authentication,
 * authorization, and retrieval of dynamic applications for users.
 */
@RunWith(MockitoJUnitRunner.class)
public class AppsForUserServletTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String TOKEN_PARAM = "token";
    private static final String TEST_PATH = "test-path";
    private static final String TOKEN_NULL_MESSAGE = "The token cannot be null";
    private static final String VALID_PATH = "test/path";
    private static final String BEARER_TOKEN = "Bearer valid-token-123";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TEST_APP_NAME = "TestApp";

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

    /**
     * Sets up the test environment with mock objects and common configurations.
     *
     * @throws Exception if setup fails
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        List<DynamicRoleApp> roleAppList = new ArrayList<>();
        roleAppList.add(mockRoleApp);

        when(mockRole.getETDAPPDynamicRoleAppList()).thenReturn(roleAppList);
        when(mockRoleApp.getEtdappApp()).thenReturn(mockApp);
        when(mockRoleApp.getEtdappAppVersion()).thenReturn(mockVersion);
        when(mockApp.getName()).thenReturn(TEST_APP_NAME);
        when(mockApp.getDirectoryLocation()).thenReturn("@basedesign@/testDir");
        when(mockVersion.getName()).thenReturn("1.0");
        when(mockVersion.getFileName()).thenReturn("test.js");
        when(servletContext.getRealPath("")).thenReturn("/test/path/");

        when(obContext.getRole()).thenReturn(mockRole);
    }

    /**
     * Tests the retrieval of the default version for a dynamic application.
     * Verifies that when no specific version is set, the system correctly
     * fetches the default version.
     *
     * @throws Exception if test execution fails
     */
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

    /**
     * Tests the get functionality when the application version is null.
     * Verifies that the system handles null app versions gracefully by
     * attempting to fetch the default version.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testGetWithNullAppVersion() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(TOKEN_PARAM, VALID_TOKEN);
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

            when(mockApp.getName()).thenReturn(TEST_APP_NAME);
            when(mockApp.getDirectoryLocation()).thenReturn("/test/path/");
            when(mockVersion.getName()).thenReturn("1.0");
            when(mockVersion.getFileName()).thenReturn("test.js");

            WSResult result = servlet.get(TEST_PATH, params);

            assertEquals(WSResult.Status.OK, result.getStatus());
        }
    }

    /**
     * Tests that unimplemented HTTP methods (POST, PUT, DELETE) return null.
     * Verifies that these methods are properly stubbed out in the servlet.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testUnimplementedMethods() throws Exception {
        assertNull(servlet.post("test", new HashMap<>(), new JSONObject()));
        assertNull(servlet.put("test", new HashMap<>(), new JSONObject()));
        assertNull(servlet.delete("test", new HashMap<>(), new JSONObject()));
    }

    /**
     * Tests the get functionality when no default version exists for an app.
     * Verifies that the system returns an appropriate error response when
     * no default version can be found.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testGetWithNoDefaultVersion() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(TOKEN_PARAM, VALID_TOKEN);
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

            when(mockApp.getName()).thenReturn(TEST_APP_NAME);

            obMessageMock.when(() -> OBMessageUtils.messageBD("ETDAPP_NoDefaultVersion"))
                    .thenReturn("No default version found for app %s");

            WSResult result = servlet.get(TEST_PATH, params);

            assertEquals(WSResult.Status.NOT_FOUND, result.getStatus());
        }
    }

    /**
     * Tests the retrieval of an application with a development version.
     * Verifies that development versions are handled correctly and appropriate
     * paths are generated.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testGetWithDevelopmentVersion() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(TOKEN_PARAM, VALID_TOKEN);
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

            WSResult result = servlet.get(TEST_PATH, params);

            assertEquals(WSResult.Status.OK, result.getStatus());
        }
    }

    /**
     * Tests the creation of multiple applications.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testMultipleApps() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(TOKEN_PARAM, VALID_TOKEN);

        List<DynamicRoleApp> roleAppList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            DynamicRoleApp newRoleApp = mock(DynamicRoleApp.class);
            DynamicApp newApp = mock(DynamicApp.class);
            DynamicAppVersion newVersion = mock(DynamicAppVersion.class);

            when(newApp.getName()).thenReturn(TEST_APP_NAME + i);
            when(newApp.getDirectoryLocation()).thenReturn("@basedesign@/testDir" + i);
            when(newVersion.getName()).thenReturn("1." + i);
            when(newVersion.getFileName()).thenReturn("test" + i + ".js");
            when(newRoleApp.getEtdappApp()).thenReturn(newApp);
            when(newRoleApp.getEtdappAppVersion()).thenReturn(newVersion);

            roleAppList.add(newRoleApp);
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

            WSResult result = servlet.get(TEST_PATH, params);

            assertEquals(WSResult.Status.OK, result.getStatus());
        }
    }

    /**
     * Tests behavior when the role's application list is empty.
     * Verifies that the system handles empty application lists gracefully
     * and returns an appropriate response.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testEmptyRoleAppList() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(TOKEN_PARAM, VALID_TOKEN);

        when(mockRole.getETDAPPDynamicRoleAppList()).thenReturn(new ArrayList<>());

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<OBContext> obContextMock = Mockito.mockStatic(OBContext.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            when(decodedToken.getClaim("role")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
            obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
            when(obContext.getRole()).thenReturn(mockRole);

            WSResult result = servlet.get(TEST_PATH, params);

            assertEquals(WSResult.Status.OK, result.getStatus());
        }
    }

    /**
     * Tests behavior when the user role is null.
     * Verifies that the system properly handles null roles and returns
     * an appropriate error response.
     *
     * @throws Exception if test execution fails
     */

    @Test
    public void testNullRole() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(TOKEN_PARAM, VALID_TOKEN);

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<OBContext> obContextMock = Mockito.mockStatic(OBContext.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            Claim mockClaim = mock(Claim.class);
            when(mockClaim.asString()).thenReturn("someRoleId");
            when(decodedToken.getClaim("role")).thenReturn(mockClaim);

            obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
            when(obContext.getRole()).thenReturn(null);

            WSResult result = servlet.get(TEST_PATH, params);

            assertEquals(WSResult.Status.BAD_REQUEST, result.getStatus());
        }
    }

    /**
     * Tests behavior when attempting to access a non-existent role.
     * Verifies that the system returns an appropriate error response when
     * a role cannot be found.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testRoleNotFound() throws Exception {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put(TOKEN_PARAM, VALID_TOKEN);
        String nonExistentRoleId = "nonexistent-role-id";

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class);
             MockedStatic<OBDal> obDalMock = Mockito.mockStatic(OBDal.class)) {

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString())).thenReturn(decodedToken);
            Claim mockClaim = mock(Claim.class);
            when(mockClaim.asString()).thenReturn(nonExistentRoleId);
            when(decodedToken.getClaim("role")).thenReturn(mockClaim);

            obDalMock.when(OBDal::getInstance).thenReturn(obDal);

            WSResult result = servlet.get(TEST_PATH, params);

            assertEquals(WSResult.Status.BAD_REQUEST, result.getStatus());
        }
    }
    /**
     * Tests behavior when no authentication token is provided.
     * Verifies that the system properly handles missing tokens and returns
     * an appropriate error response.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testMissingToken() throws Exception {
        Map<String, String> params = new HashMap<>();

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class)) {
            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(null))
                    .thenThrow(new IllegalArgumentException(TOKEN_NULL_MESSAGE));

            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(""))
                    .thenThrow(new IllegalArgumentException(TOKEN_NULL_MESSAGE));

            WSResult result = servlet.get(TEST_PATH, params);
            assertEquals(WSResult.Status.BAD_REQUEST, result.getStatus());
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals(TOKEN_NULL_MESSAGE, e.getMessage());
        }
    }

    /**
     * Tests behavior when an invalid authentication token is provided.
     * Verifies that the system properly handles invalid tokens and returns
     * an appropriate error response.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testInvalidToken() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(TOKEN_PARAM, "invalid-token");

        try (MockedStatic<SecureWebServicesUtils> secureUtils = Mockito.mockStatic(SecureWebServicesUtils.class)) {
            secureUtils.when(() -> SecureWebServicesUtils.decodeToken(anyString()))
                    .thenThrow(new RuntimeException("Invalid token"));

            WSResult result = servlet.get(TEST_PATH, params);
            assertEquals(WSResult.Status.BAD_REQUEST, result.getStatus());
        } catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
            assertEquals("Invalid token", e.getMessage());
        }
    }

    /**
     * Tests the doGet method with valid bearer token.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testDoGetWithValidBearerToken() throws Exception {
        String validPath = VALID_PATH;
        String bearerToken = BEARER_TOKEN;
        WSResult mockResult = mock(WSResult.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(bearerToken);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            Map<String, String> params = new HashMap<>();
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request)).thenReturn(params);

            AppsForUserServlet spyServlet = spy(servlet);
            doReturn(mockResult).when(spyServlet).get(eq(validPath), argThat(map ->
                    map.containsKey(TOKEN_PARAM) && map.get(TOKEN_PARAM).equals("valid-token-123")
            ));

            spyServlet.doGet(validPath, request, response);

            obRestUtils.verify(() -> OBRestUtils.writeWSResponse(mockResult, response));
            verify(request).getHeader(AUTHORIZATION_HEADER);
        }
    }

    /**
     * Tests the doGet method when no authorization header is present.
     * Verifies that the system properly handles requests without authorization
     * headers and returns an appropriate response.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testDoGetWithoutAuthorizationHeader() throws Exception {
        String validPath = VALID_PATH;
        WSResult mockResult = mock(WSResult.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(null);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            Map<String, String> params = new HashMap<>();
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request)).thenReturn(params);

            AppsForUserServlet spyServlet = spy(servlet);
            doReturn(mockResult).when(spyServlet).get(eq(validPath), argThat(map ->
                    map.containsKey(TOKEN_PARAM) && map.get(TOKEN_PARAM) == null
            ));

            spyServlet.doGet(validPath, request, response);

            obRestUtils.verify(() -> OBRestUtils.writeWSResponse(mockResult, response));
            verify(request).getHeader(AUTHORIZATION_HEADER);
        }
    }

    /**
     * Tests the doGet method with an invalid authorization header format.
     * Verifies that the system properly handles malformed authorization headers
     * and returns an appropriate error response.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testDoGetWithInvalidAuthorizationFormat() throws Exception {
        String validPath = VALID_PATH;
        String invalidAuthHeader = "Basic invalid-auth";
        WSResult mockResult = mock(WSResult.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(invalidAuthHeader);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            Map<String, String> params = new HashMap<>();
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request)).thenReturn(params);

            AppsForUserServlet spyServlet = spy(servlet);
            doReturn(mockResult).when(spyServlet).get(eq(validPath), argThat(map ->
                    map.containsKey(TOKEN_PARAM) && map.get(TOKEN_PARAM) == null
            ));

            spyServlet.doGet(validPath, request, response);

            obRestUtils.verify(() -> OBRestUtils.writeWSResponse(mockResult, response));
            verify(request).getHeader(AUTHORIZATION_HEADER);
        }
    }

    /**
     * Tests the doGet method with additional request parameters.
     * Verifies that the system correctly processes and includes additional
     * request parameters in the response.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testDoGetWithRequestParameters() throws Exception {
        String validPath = VALID_PATH;
        String bearerToken = BEARER_TOKEN;
        WSResult mockResult = mock(WSResult.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(bearerToken);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            Map<String, String> params = new HashMap<>();
            params.put("param1", "value1");
            params.put("param2", "value2");
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request)).thenReturn(params);

            AppsForUserServlet spyServlet = spy(servlet);
            doReturn(mockResult).when(spyServlet).get(eq(validPath), argThat(map ->
                    map.containsKey(TOKEN_PARAM) &&
                            map.get(TOKEN_PARAM).equals("valid-token-123") &&
                            map.get("param1").equals("value1") &&
                            map.get("param2").equals("value2")
            ));

            spyServlet.doGet(validPath, request, response);

            obRestUtils.verify(() -> OBRestUtils.writeWSResponse(mockResult, response));
            verify(request).getHeader(AUTHORIZATION_HEADER);
        }
    }

    /**
     * Tests the doGet method when an exception occurs during parameter processing.
     * Verifies that the system properly handles exceptions during request parameter
     * processing and returns an appropriate error response.
     *
     * @throws Exception if test execution fails
     */
    @Test
    public void testDoGetWithExceptionInRequestParamsToMap() throws Exception {
        String validPath = VALID_PATH;
        String bearerToken = BEARER_TOKEN;

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(bearerToken);

        try (MockedStatic<OBRestUtils> obRestUtils = Mockito.mockStatic(OBRestUtils.class)) {
            obRestUtils.when(() -> OBRestUtils.requestParamsToMap(request))
                    .thenThrow(new RuntimeException("Error processing request parameters"));

            assertThrows(RuntimeException.class, () ->
                    servlet.doGet(validPath, request, response));
        }
    }


}
