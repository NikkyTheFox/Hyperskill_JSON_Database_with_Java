package org.griddynamics;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientInputArgumentsTest {

    private static String[] params;

    private ClientInputArguments mockedClientInputArguments;

    private ClientInputArguments testSubject;

    @BeforeAll
    static void setUpEnvironment(){
        params = new String[6];
        params[0] = "-t";
        params[1] = "testCaseType";
        params[2] = "-k";
        params[3] = "testCaseKey";
        params[4] = "-v";
        params[5] = "testCaseValue";
    }

    @AfterAll
    static void tearDownEnvironment() {
        params = null;
    }

    @BeforeEach
    void setUp() {
        mockedClientInputArguments = mock(ClientInputArguments.class);
        testSubject = new ClientInputArguments(params);
    }

    @AfterEach
    void tearDown() {
        mockedClientInputArguments = null;
        testSubject = null;
    }

    @Test
    @DisplayName("Test constructor with request Type parameter.")
    void constructorTypeParameterTest() {
        when(mockedClientInputArguments.getType()).thenReturn("testCaseType");
        assertEquals(mockedClientInputArguments.getType(), testSubject.getType());
        assertNotNull(testSubject.getType());
    }

    @Test
    @DisplayName("Test constructor with request Type parameter not null.")
    void constructorTypeParameterNotNullTest() {
        assertNotNull(testSubject.getType());
    }

    @Test
    @DisplayName("Test constructor with request Key parameter.")
    void constructorKeyParameterTest() {
        when(mockedClientInputArguments.getKey()).thenReturn("testCaseKey");
        assertEquals(mockedClientInputArguments.getKey(), testSubject.getKey());
    }

    @Test
    @DisplayName("Test constructor with request Key parameter not null.")
    void constructorKeyParameterNotNullTest() {
        assertNotNull(testSubject.getKey());
    }

    @Test
    @DisplayName("Test constructor with request Key parameter.")
    void constructorValueParameterTest() {
        when(mockedClientInputArguments.getValue()).thenReturn("testCaseValue");
        assertEquals(mockedClientInputArguments.getValue(), testSubject.getValue());
    }

    @Test
    @DisplayName("Test constructor with request Type parameter not null.")
    void constructorValueParameterNotNullTest() {
        assertNotNull(testSubject.getValue());
    }

    @Test
    @DisplayName("Test constructor with request File Name parameter.")
    void constructorFileNameParameterTest() {
        when(mockedClientInputArguments.getFileName()).thenReturn("testCaseFileName");
        String[] fileParams = new String[2];
        fileParams[0] = "-in";
        fileParams[1] = "testCaseFileName";
        testSubject = new ClientInputArguments(fileParams);
        assertEquals(mockedClientInputArguments.getFileName(), testSubject.getFileName());
    }

    @Test
    @DisplayName("Test constructor with request File Name parameter not null.")
    void constructorFileNameParameterNotNullTest() {
        String[] fileParams = new String[2];
        fileParams[0] = "-in";
        fileParams[1] = "testCaseFileName";
        testSubject = new ClientInputArguments(fileParams);
        assertNotNull(testSubject.getFileName());
    }

    @Test
    @DisplayName("Test parsing Set Request into a JSON String.")
    void parseIntJsonSetRequestTest() {
        String expectedJSON = "{\"type\":\"set\",\"key\":\"key 1\",\"value\":\"value 1\"}";
        String[] inputParam = new String[6];
        inputParam[0] = "-t";
        inputParam[1] = "set";
        inputParam[2] = "-k";
        inputParam[3] = "key 1";
        inputParam[4] = "-v";
        inputParam[5] = "value 1";
        testSubject = new ClientInputArguments(inputParam);
        assertEquals(expectedJSON, testSubject.parseIntoJson());
    }

    @Test
    @DisplayName("Test parsing Get Request into a JSON String.")
    void parseIntJsonGetRequestTest() {
        String expectedJSON = "{\"type\":\"get\",\"key\":\"key 1\"}";
        String[] inputParam = new String[4];
        inputParam[0] = "-t";
        inputParam[1] = "get";
        inputParam[2] = "-k";
        inputParam[3] = "key 1";
        testSubject = new ClientInputArguments(inputParam);
        assertEquals(expectedJSON, testSubject.parseIntoJson());
    }

    @Test
    @DisplayName("Test parsing Delete Request into a JSON String.")
    void parseIntJsonDeleteRequestTest() {
        String expectedJSON = "{\"type\":\"delete\",\"key\":\"key 1\"}";
        String[] inputParam = new String[4];
        inputParam[0] = "-t";
        inputParam[1] = "delete";
        inputParam[2] = "-k";
        inputParam[3] = "key 1";
        testSubject = new ClientInputArguments(inputParam);
        assertEquals(expectedJSON, testSubject.parseIntoJson());
    }

    @Test
    @DisplayName("Test parsing Exit Request into a JSON String.")
    void parseIntJsonExitRequestTest() {
        String expectedJSON = "{\"type\":\"exit\"}";
        String[] inputParam = new String[2];
        inputParam[0] = "-t";
        inputParam[1] = "exit";
        testSubject = new ClientInputArguments(inputParam);
        assertEquals(expectedJSON, testSubject.parseIntoJson());
    }
}