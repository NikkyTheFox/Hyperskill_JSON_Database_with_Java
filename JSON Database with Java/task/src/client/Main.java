package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

class Constants {

    final static String ADDRESS = "127.0.0.1";

    final static int PORT = 22222;

    final static String PATH_TO_DATA = "./src/client/data/";
}

public class Main {

    private static InputArguments inputArguments;

    public static void main(String[] args) {
        initialise(args);
        runClient();
    }

    private static void initialise(String[] args) {
        inputArguments = new InputArguments(args);
        File file = new File(Constants.PATH_TO_DATA);
        file.getParentFile().mkdirs();
    }

    private static void runClient() {
        try (
                Socket socket = new Socket(InetAddress.getByName(Constants.ADDRESS), Constants.PORT);
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                ) {
            System.out.println("Client started!");
            String message = inputArguments.parseIntoJson();
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

class InputArguments {

    @Parameter(names = {"-t"}, description = "Type of a request")
    private String type;

    @Parameter(names = {"-k"}, description = "Key to be accessed")
    private String key;

    @Parameter(names = {"-v"}, description = "Value to be set")
    private String value;

    @Parameter(names = {"-in"}, description = "Name of the request input file")
    private String  fileName = null;

    public InputArguments(String[] args) {
        JCommander.newBuilder()
                .addObject(this)
                .build()
                .parse(args);
    }

    public String parseIntoJson() {
        if (!Objects.equals(fileName, null)) {
            try {
                return new String(Files.readAllBytes(Paths.get(Constants.PATH_TO_DATA + fileName)));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Map<String, String> map = new LinkedHashMap<>();
            Gson gson = new Gson();
            return switch (type) {
                case "set" -> {
                    map.put("type", type);
                    map.put("key", key);
                    map.put("value", value);
                    yield gson.toJson(map);
                }
                case "get", "delete" -> {
                    map.put("type", type);
                    map.put("key", key);
                    yield gson.toJson(map);
                }
                case "exit" -> {
                    map.put("type", type);
                    yield gson.toJson(map);
                }
                default -> null;
            };
        }
    }
}