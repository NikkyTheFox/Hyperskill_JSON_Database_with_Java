package org.griddynamics;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Improve your database in this stage.
 * It should be able to store not only strings but any JSON objects as values.
 * <p>
 * The key should not only be a string since the user needs to retrieve part of the JSON value.
 * <p>
 * If there are no root objects, the server should create them, too.
 * <p>
 * The deletion of objects should follow the same rules.
 * <p>
 * Enhance your database with the ability to store any JSON objects as values as portrayed at the description.
 */
public class Server {

    /**
     * Parameters provided while running the server.
     */
    private static ServerInputArguments inputArguments;

    /**
     * Flag indicating whether a server should be shut down.
     * It is exposed to all running threads.
     */
    private static volatile boolean exitFlag = false;

    /**
     * Concurrency lock for accessing and editing database file.
     */
    private static ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Main socket that server will run on.
     */
    private static ServerSocket serverSocket;

    /**
     * A getter for run parameters of Server Application.
     *
     * @return {@code ServerInputArguments} object that stores Server Application run parameters.
     */
    public static ServerInputArguments getInputArguments() {
        return inputArguments;
    }

    /**
     * A setter for run parameters of Server Application.
     *
     * @param newServerInputArguments {@code ServerInputArguments} object to be set as new run parameters for Server Application.
     */
    public static void SetInputArguments(ServerInputArguments newServerInputArguments) {
        inputArguments = newServerInputArguments;
    }

    /**
     * A getter for exitFlag boolean of Server Application.
     *
     * @return {@code boolean} value representing Server Application's exitFlag.
     */
    public static boolean getExitFlag() {
        return exitFlag;
    }

    /**
     * A setter for exitFlag boolean of Server Application.
     *
     * @param newExitFlag {@code boolean} value to be set as Server Application's new exitFlag.
     */
    public static void setExitFlag(boolean newExitFlag) {
        exitFlag = newExitFlag;
    }

    /**
     * A getter for {@code ReadWriteLock} lock object used by Server Application to concurrently work on a file.
     *
     * @return {@code ReadWriteLock} object used by Server Application.
     */
    public static ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    /**
     * A setter for {@code ReadWriteLock} lock object used by Server Application to concurrently work on a file.
     *
     * @param newReadWriteLock {@code ReadWriteLock} object to be set as new ReadWriteLock lock.
     */
    public static void setReadWriteLock(ReadWriteLock newReadWriteLock) {
        readWriteLock = newReadWriteLock;
    }

    /**
     * A getter for {@code ServerSocket} object used as a socket by Server Application.
     *
     * @return {@code ServerSocket} object used by Server Application.
     */
    public static ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * A setter for {@code ServerSocket} object used as a socket by Server Application.
     *
     * @param newServerSocket {@code ServerSocket} object to be set as new socket of Server Application.
     */
    public static void setServerSocket(ServerSocket newServerSocket) {
        serverSocket = newServerSocket;
    }

    /**
     * A getter for file writing specific lock.
     *
     * @return {@code Lock} object used to lock a file while writing to file.
     */
    public static Lock getWriteLock() {
        return readWriteLock.writeLock();
    }

    /**
     * A getter for file reading specific lock.
     *
     * @return {@code Lock} object used to lock a file while reading from file.
     */
    public static Lock getReadLock() {
        return readWriteLock.readLock();
    }

    /**
     * Main method for Server Application.
     *
     * @param args {@code String[]} run parameters for Server Application.
     */
    public static void main(String[] args) {
        inputArguments = initialise(args, ServerConstants.PATH_TO_DATA);
        if (!Objects.equals(null, inputArguments)) {
            runServer(ServerConstants.ADDRESS, ServerConstants.PORT, ServerConstants.BACKLOG, ServerConstants.POOL_SIZE);
        }
    }

    /**
     * Method for initialising Server Application.
     * Initialises run parameters, directory and database file.
     *
     * @param args {@code String[} run parameters for Server Application.
     */
    private static ServerInputArguments initialise(String[] args, String pathToData) {
        ServerInputArguments serverInputArguments = new ServerInputArguments(args);
        File file = new File(pathToData);
        try {
            if (file.getParentFile().mkdirs() || file.getParentFile().exists()) {
                if (!serverInputArguments.isPersistenceFlag() || !file.exists()) {
                    Files.write(Path.of(pathToData), new byte[0]);
                }
                return serverInputArguments;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Launches Server on a socket.
     * <p>
     * Awaits new Client connections and runs Executor from a pool for each connection.
     */
    private static void runServer(String ipAddress, int port, int backlogSize, int threadPoolSize) {
        try {
            serverSocket = new ServerSocket(port, backlogSize, InetAddress.getByName(ipAddress));
            System.out.println("Server started!");
            ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
            while (!exitFlag) {
                try {
                    Socket socket = serverSocket.accept();
                    executor.submit(new ClientConnectionHandler(socket, getWriteLock(), getReadLock(), ServerConstants.PATH_TO_DATA, inputArguments.isDebugFlag()));
                } catch (Exception e) {
                    if (exitFlag) {
                        break;
                    } else {
                        System.out.println(e.getMessage());
                    }
                }
            }
            executor.shutdown();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error occurred while closing the server socket: " + e.getMessage());
            }
        }
    }

    /**
     * Shutdowns socket and Server Application.
     */
    public static boolean shutdownServer() {
        exitFlag = true;
        try {
            if (serverSocket != null) {
                serverSocket.close();
                return true;
            }
        } catch (IOException e) {
            System.err.println("Error occurred while closing the server socket: " + e.getMessage());
        }
        return false;
    }
}

