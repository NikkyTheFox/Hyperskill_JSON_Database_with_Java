package org.griddynamics;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Class representing run parameters for Server Application.
 */
public class ServerInputArguments {

    /**
     * Debugging mode flag.
     */
    @Parameter(names = {"-d"}, description = "Debugging mode flag", arity = 1)
    private boolean debugFlag = false;

    @Parameter(names = {"-p"}, description = "Indicates whether database should be persisted upon initiation of a server", arity = 1)
    private boolean persistenceFlag = false;

    /**
     * A getter for debugging flag.
     *
     * @return {@code boolean} value representing debugging mode flag.
     */
    public boolean isDebugFlag() {
        return debugFlag;
    }

    /**
     * A setter for debugging flag.
     *
     * @param newDebugFlag {@code boolean} value to be set as new debugging mode flag.
     */
    public void setDebugFlag(boolean newDebugFlag) {
        debugFlag = newDebugFlag;
    }

    /**
     * A getter for database persistence flag.
     *
     * @return {@code boolean} value representing persistence mode flag.
     */
    public boolean isPersistenceFlag() {
        return persistenceFlag;
    }

    /**
     * A setter for database persistence flag.
     *
     * @param newPersistenceFlag {@code boolean} value to be set as new persistence mode flag.
     */
    public void setPersistenceFlag(boolean newPersistenceFlag) {
        persistenceFlag = newPersistenceFlag;
    }

    /**
     * Constructor for JCommander run parameters.
     *
     * @param args {@code String[]} run parameters of a Server Application.
     */
    public ServerInputArguments(String[] args) {
        JCommander.newBuilder()
                .addObject(this)
                .build()
                .parse(args);
    }
}
