package org.griddynamics;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerInputArgumentsTest {

    static String[] params;

    ServerInputArguments mockedServerInputArguments;

    ServerInputArguments testSubject;

    @BeforeAll
    static void setUpEnvironment() {
        params = new String[2];
        params[0] = "-d";
        params[1] = "true";
    }

    @AfterEach
    void tearDown() {
        mockedServerInputArguments = null;
        testSubject = null;
    }

    @Test
    @DisplayName("Test if default value of Debug Flag is False.")
    void debugFlagDefaultValueTest() {
        testSubject = new ServerInputArguments(new String[0]);
        assertFalse(testSubject.isDebugFlag());
    }

    @Test
    @DisplayName("Test constructor with Debug Flag parameter")
    void debugFlagTest() {
        mockedServerInputArguments = mock(ServerInputArguments.class);
        when(mockedServerInputArguments.isDebugFlag()).thenReturn(true);
        testSubject = new ServerInputArguments(params);
        assertEquals(mockedServerInputArguments.isDebugFlag(), testSubject.isDebugFlag());
    }
}