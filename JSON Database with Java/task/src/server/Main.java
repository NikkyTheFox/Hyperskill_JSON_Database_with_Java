package server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Constants {

    final static int POSITIVE = 1;

    final static int NEGATIVE = 0;

    final static int ERROR = -1;

    final static int ILLEGAL = -2;

    final static String SUCCESS_MESSAGE = "OK";

    final static String ERROR_MESSAGE = "ERROR";

    final static String RESPONSE = "response";

    final static String RESPONSE_VALUE = "value";

    final static String RESPONSE_REASON = "reason";

    final static String RESPONSE_REASON_NO_KEY = "No such key";

    final static String RESPONSE_REASON_ILLEGAL = "Invalid arguments";

    final static String RESPONSE_DATABASE_ERROR = "503 - something went wrong on server side";

    final static String ADDRESS = "127.0.0.1";

    final static int PORT = 22222;

    final static int BACKLOG = 50;

    final static String PATH_TO_DATA = ".src/server/data/db.json";

    final static int POOL_SIZE = 4;
}

public class Main {

    private static InputArguments inputArguments;

    private static volatile boolean exitFlag = false;

    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static ServerSocket serverSocket;

    static InputArguments getInputArguments() {
        return inputArguments;
    }

    public static Lock getWriteLock() {
        return lock.writeLock();
    }

    public static Lock getReadLock() {
        return lock.readLock();
    }

    public static void main(String[] args) {
        initialise(args);
        runServer();
    }

    private static void initialise(String[] args) {
        inputArguments = new InputArguments(args);
        File file = new File(Constants.PATH_TO_DATA);
        try {
            if(file.getParentFile().mkdirs()) {
                Files.write(Path.of(Constants.PATH_TO_DATA), new byte[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runServer() {
        try {
            serverSocket = new ServerSocket(Constants.PORT, Constants.BACKLOG, InetAddress.getByName(Constants.ADDRESS));
            System.out.println("Server started!");
            ExecutorService executor = Executors.newFixedThreadPool(Constants.POOL_SIZE);

            while (!exitFlag) {
                try {
                    Socket socket = serverSocket.accept();
                    executor.submit(new handleSocket(socket));
                } catch (Exception e) {
                    if (exitFlag) {
                        break;
                    } else {
                        e.printStackTrace();
                    }
                }
            }
            executor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
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

    public static void shutdownServer() {
        exitFlag = true;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error occurred while closing the server socket: " + e.getMessage());
        }
    }
}

class InputArguments {

    @Parameter(names = {"-d"}, description = "Debugging mode flag", arity = 1)
    private boolean debug = false;

    public InputArguments(String[] args) {
        JCommander.newBuilder()
                .addObject(this)
                .build()
                .parse(args);
    }

    public boolean getDebug() {
        return this.debug;
    }
}

class handleSocket implements Runnable {

    private final Socket socket;

    private final Map<String, String> outputMap = new LinkedHashMap<>();

    private final Lock writerLock;

    private final Lock readerLock;

    private final String filePath;

    public handleSocket(Socket socket) {
        this.socket = socket;
        this.writerLock = Main.getWriteLock();
        this.readerLock = Main.getReadLock();
        this.filePath = Constants.PATH_TO_DATA;
    }

    @Override
    public void run() {
        parseRequest();
    }

    private void parseRequest() {
        try (
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())
                )
        {
            String input = dataInputStream.readUTF();
            if (Main.getInputArguments().getDebug()) {
                System.out.printf("Received: %s\n", input);
            }
            JsonObject jsonObject = advancedParseFromJson(input);
            handleRequest(jsonObject);
            String output = advancedParseToJson(outputMap);
            dataOutputStream.writeUTF(output);
            if (Main.getInputArguments().getDebug()){
                System.out.printf("Sent: %s\n", output);
            }
            clearOutputMap();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private JsonObject advancedParseFromJson(String input) {
        return JsonParser.parseString(input).getAsJsonObject();
    }

    private String parseToJson(Map<String, String> map) {
        return new Gson().toJson(map);
    }

    private String fixJsonOutput(String s) {
        String regex1 = "\\\\";
        Pattern pattern1 = Pattern.compile(regex1);
        Matcher matcher = pattern1.matcher(s);
        String afterRegex1 = matcher.replaceAll("");

        String regex2 = "\"\\{";
        Pattern pattern2 = Pattern.compile(regex2);
        matcher = pattern2.matcher(afterRegex1);
        String afterRegex2 = matcher.replaceAll("{");

        String regex3 = "}\"";
        Pattern pattern3 = Pattern.compile(regex3);
        matcher = pattern3.matcher(afterRegex2);
        return matcher.replaceAll("}");
    }

    private String advancedParseToJson(Map<String, String> map) {
        return fixJsonOutput(parseToJson(map));
    }

    private void handleRequest(JsonObject jsonObject) {
        switch (jsonObject.get("type").getAsString()) {
            case "get":
                get(jsonObject.get("key"));
                break;
            case "set":
                set(jsonObject.get("key"), jsonObject.get("value"));
                break;
            case "delete":
                delete(jsonObject.get("key"));
                break;
            case "exit":
                exit();
                break;
            default:
                break;
        }
    }

    private JsonObject readFromJson(){
        try (FileReader fileReader = new FileReader(filePath)) {
            readerLock.lock();
            JsonObject map = new Gson().fromJson(fileReader, JsonObject.class);
            if(checkIfNotNull(map)) {
                readerLock.unlock();
                return map;
            }
            readerLock.unlock();
            return new JsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            readerLock.unlock();
            return null;
        }
    }

    private int setInJson(JsonElement key, JsonElement value) {
        if (checkIfNotNull(key) && checkIfNotNull(value)) {
            JsonObject newData = new JsonObject();
            if (key.isJsonArray()) {
                JsonObject placeholder = new JsonObject();
                JsonArray keyArray = key.getAsJsonArray();
                int lastIndex = keyArray.size() - 1;
                for (int i = lastIndex; i > 0; i--) {
                    JsonElement currentKey = keyArray.get(i);

                    if (i == lastIndex) {
                        placeholder.add(currentKey.getAsString(), value);
                    } else {
                        JsonObject tempObject = new JsonObject();
                        tempObject.add(currentKey.getAsString(), placeholder);
                        placeholder = tempObject;
                    }
                }
                newData.add(keyArray.get(0).getAsString(), placeholder);
            } else {
                newData.add(key.getAsString(), value);
            }
            if(writeToJson(newData)) {
                return Constants.POSITIVE;
            } else {
                return Constants.ERROR;
            }
        }
        return Constants.NEGATIVE;
    }

    private boolean writeToJson(JsonObject newData) {
        try (FileReader fileReader = new FileReader(filePath)) {
            Gson gson = new Gson();
            writerLock.lock();
            JsonObject map = new Gson().fromJson(fileReader, JsonObject.class);
            if (checkIfNotNull(map)) {
                mergeJsonObjects(map, newData);
            } else {
                map = newData;
            }
            try (FileWriter fileWriter = new FileWriter(filePath)) {
                gson.toJson(map, fileWriter);
            }
            writerLock.unlock();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            writerLock.unlock();
            return false;
        }
    }

    private void mergeJsonObjects(JsonObject jsonObject1, JsonObject jsonObject2) {
        for (String key : jsonObject2.keySet()) {
            if (jsonObject2.get(key).isJsonObject() && jsonObject1.has(key) && jsonObject1.get(key).isJsonObject()) {
                mergeJsonObjects(jsonObject1.getAsJsonObject(key), jsonObject2.getAsJsonObject(key));
            } else {
                jsonObject1.add(key, jsonObject2.get(key));
            }
        }
    }

    private int deleteFromJson(JsonElement key) {
        if (checkIfNotNull(key)) {
            JsonObject database = readFromJson();
            if (checkIfNotNull(database)){
                Path path = Path.of(filePath);
                if(key.isJsonArray()){
                    JsonObject temp = database;
                    JsonArray keyArray = key.getAsJsonArray();
                    int lastIndex = keyArray.size() - 1;
                    for (int i = 0; i < keyArray.size(); i++) {
                        JsonElement k = keyArray.get(i);
                        JsonElement valuePlaceholder = temp.get(k.getAsString());
                        if (checkIfNotNull(valuePlaceholder)) {
                            if (valuePlaceholder.isJsonPrimitive()) {
                                if (i == lastIndex) {
                                    if (checkIfNotNull(temp.remove(k.getAsString()))) {
                                        try {
                                            writerLock.lock();
                                            Files.write(path, new byte[0]);
                                            writerLock.unlock();
                                            writeToJson(database);
                                            return Constants.POSITIVE;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    return Constants.NEGATIVE;
                                }
                            } else {
                                temp = temp.getAsJsonObject(k.getAsString());
                            }
                        } else {
                            return Constants.NEGATIVE;
                        }
                    }
                } else {
                    if (checkIfNotNull(database.remove(key.getAsString()))) {
                        try {
                            writerLock.lock();
                            Files.write(path, new byte[0]);
                            writerLock.unlock();
                            writeToJson(database);
                            return Constants.POSITIVE;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return Constants.NEGATIVE;
                }
            }
            return Constants.ERROR;
        }
        return Constants.ILLEGAL;
    }

    private void get (JsonElement key) {
        if (checkIfNotNull(key)) {
           JsonObject database = readFromJson();
            if (checkIfNotNull(database)) {
                if (key.isJsonArray()) {
                    JsonObject temp = database;
                    JsonArray keyArray = key.getAsJsonArray();
                    int lastIndex = keyArray.size() - 1;
                    for (int i = 0; i < keyArray.size(); i++) {
                        JsonElement k = keyArray.get(i);
                        JsonElement valuePlaceholder = temp.get(k.getAsString());
                        if (checkIfNotNull(valuePlaceholder)) {
                            if (temp.get(k.getAsString()).isJsonPrimitive()) {
                                if (i == lastIndex) {
                                    writeToOutputMap(Constants.RESPONSE, Constants.SUCCESS_MESSAGE);
                                    writeToOutputMap(Constants.RESPONSE_VALUE, temp.get(k.getAsString()).getAsJsonPrimitive().getAsString());
                                    return;
                                }
                            } else {
                                temp = temp.getAsJsonObject(k.getAsString());
                            }
                        } else {
                            writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
                            writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_REASON_NO_KEY);
                            return;
                        }
                    }
                    if (!Objects.equals(temp, database)) {
                        writeToOutputMap(Constants.RESPONSE, Constants.SUCCESS_MESSAGE);
                        writeToOutputMap(Constants.RESPONSE_VALUE, temp.toString());
                    } else {
                        writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
                        writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_REASON_NO_KEY);
                    }
                } else {
                    JsonElement value = database.get(key.toString());
                    if (checkIfNotNull(value)) {
                        writeToOutputMap(Constants.RESPONSE, Constants.SUCCESS_MESSAGE);
                        writeToOutputMap(Constants.RESPONSE_VALUE, value.toString());
                    } else {
                        writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
                        writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_REASON_NO_KEY);
                    }
                }
            } else {
                writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
                writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_REASON_NO_KEY);
            }
            return;
        }
        writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
        writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_REASON_ILLEGAL);
    }

    private void set(JsonElement key, JsonElement value) {
        switch (setInJson(key, value)) {
            case Constants.POSITIVE:
                writeToOutputMap(Constants.RESPONSE, Constants.SUCCESS_MESSAGE);
                break;
            case Constants.NEGATIVE:
                writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
                writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_REASON_ILLEGAL);
                break;
            case Constants.ERROR:
                writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
                writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_DATABASE_ERROR);
                break;
        }
    }

    private void delete(JsonElement key) {
        switch (deleteFromJson(key)) {
            case 1:
                writeToOutputMap(Constants.RESPONSE, Constants.SUCCESS_MESSAGE);
                break;
            case 0:
                writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
                writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_REASON_NO_KEY);
                break;
            case -1:
                writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
                writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_DATABASE_ERROR);
                break;
            case -2:
                writeToOutputMap(Constants.RESPONSE, Constants.ERROR_MESSAGE);
                writeToOutputMap(Constants.RESPONSE_REASON, Constants.RESPONSE_REASON_ILLEGAL);
                break;
        }

    }

    private void exit() {
        Main.shutdownServer();
        outputMap.put(Constants.RESPONSE, Constants.SUCCESS_MESSAGE);
    }

    private boolean checkIfNotNull(Object input) {
        return !Objects.equals(input, null);
    }

    private void writeToOutputMap(String key, String value) {
        outputMap.put(key, value);
    }

    private void clearOutputMap() {
        this.outputMap.clear();
    }

}