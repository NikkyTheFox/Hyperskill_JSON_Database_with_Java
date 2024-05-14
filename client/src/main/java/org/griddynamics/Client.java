package org.griddynamics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * A class used to run Client Application
 */
public class Client {

    /**
     * Run parameters for Client Application.
     */
    private static ClientInputArguments inputArguments;

    /**
     * A getter for Client Application run parameters object.
     *
     * @return ClientInputArgument object.
     */
    public static ClientInputArguments getInputArguments() {
        return inputArguments;
    }

    /**
     * A setter for Client Application run parameters object.
     *
     * @param newClientInputArguments New object to be set as Client Application's run parameters.
     */
    public static void setInputArguments(ClientInputArguments newClientInputArguments) {
        inputArguments = newClientInputArguments;
    }

    /**
     * Main method for Client Application to run.
     *
     * @param args Run parameters for Client Application.
     */
    public static void main(String[] args) {
        inputArguments = initialise(args, ClientConstants.PATH_TO_DATA);
        if (!Objects.equals(null, inputArguments)) {
            runClient(ClientConstants.ADDRESS, ClientConstants.PORT, inputArguments);
        }
    }

    /**
     * Initialises run parameters for Client Application.
     * Creates directories and files.
     *
     * @param args Run parameters for Client Application.
     * @param pathToData Path to input data file.
     * @return {@code ClientInputArguments} containing initialised parameters or {@code null} if initialisation was unsuccessful.
     */
    private static ClientInputArguments initialise(String[] args, String pathToData) {
        ClientInputArguments clientInputArguments = new ClientInputArguments(args);
        Path filePath = Paths.get(pathToData);
        try {
            Files.createDirectories(filePath);
        } catch (IOException e) {
            System.out.println("Unable to create directory: " + e.getMessage());
            return null;
        }
        System.out.println();
        return clientInputArguments;
    }

    /**
     * Tries to establish a new connection to the Server Application and sends a JSON request String.
     * Awaits response from the server and presents it to the user.
     *
     * @param ipAddress IP Address that Client Application will try to connect to.
     * @param port Port number that Client Application will try to connect to.
     * @param clientInputArguments Run parameters that will be parsed into a Client Application request and sent to Server Application.
     */
    private static void runClient(String ipAddress, int port, ClientInputArguments clientInputArguments) {
        try (
                Socket socket = new Socket(InetAddress.getByName(ipAddress), port);
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())
        ) {
            System.out.println("Client started!");
            String message = clientInputArguments.parseIntoJson();
            if (Objects.equals(message, null)) {
                socket.close();
                return;
            }
            dataOutputStream.writeUTF(message);
            System.out.printf("Sent: %s\n", message);
            String input = dataInputStream.readUTF();
            System.out.printf("Received: %s\n", input);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

