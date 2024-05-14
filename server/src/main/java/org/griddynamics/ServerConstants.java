package org.griddynamics;

/**
 * A class containing constant values used by Server application
 */
class ServerConstants {

    /**
     * Value used as a returning confirmation of some method's work.
     */
    final static int POSITIVE = 1;

    /**
     * Value used as a returning disproof of some method's work.
     */
    final static int NEGATIVE = 0;

    /**
     * Value used as a simple error indicator of some method's work.
     */
    final static int ERROR = -1;

    /**
     * Value used as a simple indicator of illegal action of some method's work.
     */
    final static int ILLEGAL = -2;

    /**
     * Value used during response creation, confirming action's positive result.
     */
    final static String SUCCESS_MESSAGE = "OK";

    /**
     * Value used during response creation, confirming action's negative result.
     */
    final static String ERROR_MESSAGE = "ERROR";

    /**
     * Value reused during server response creation process.
     */
    final static String RESPONSE = "response";

    /**
     * Value reused during server response creation process.
     */
    final static String RESPONSE_VALUE = "value";

    /**
     * Value reused during server response creation process.
     */
    final static String RESPONSE_REASON = "reason";

    /**
     * Value used as a response type during server response creation process equivalent to "Key not found".
     */
    final static String RESPONSE_REASON_NO_KEY = "No such key";

    /**
     * Value used as a response type during server response creation process equivalent to "Invalid request".
     */
    final static String RESPONSE_REASON_ILLEGAL = "Invalid arguments";

    /**
     * Value used as a response type during server response creation process equivalent to "Server side error".
     */
    final static String RESPONSE_DATABASE_ERROR = "503 - something went wrong on server side";

    /**
     * IP Address used by a server.
     */
    final static String ADDRESS = "127.0.0.1";

    /**
     * Port used by a server.
     */
    final static int PORT = 22222;

    /**
     * Maximum size of queue of incoming connections.
     */
    final static int BACKLOG = 50;

    /**
     * Path to a database file.
     */
    final static String PATH_TO_DATA = "./src/main/data/db.json";

    /**
     * Executor pool size for handling Client Application connections concurrently.
     */
    final static int POOL_SIZE = 4;
}
