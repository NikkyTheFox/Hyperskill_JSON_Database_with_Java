package org.griddynamics;

import com.google.gson.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class allowing to handle each incoming connection from Client Application concurrently.
 */
class ClientConnectionHandler implements Runnable {

    /**
     * A socket used by Client Application connection.
     */
    private Socket socket;

    /**
     * Ordered map of Output commands send back to the client.
     */
    private Map<String, String> outputMap = new LinkedHashMap<>();

    /**
     * Lock used by threads to concurrently write to database file.
     */
    private Lock writerLock;

    /**
     * Lock used by threads to concurrently read from database file.
     */
    private Lock readerLock;

    /**
     * Path to the database file.
     */
    private String filePath;

    /**
     * Flag indicating debugging mode.
     */
    private boolean debugFlag;

    /**
     * A getter for socket representing accepted Client Application connection.
     *
     * @return {@code Socket} object that stores data about accepted connection.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * A setter for socket representing accepted Client Application connection.
     *
     * @param newSocket {@code Socket} object to be set as new connection socket.
     */
    public void setSocket(Socket newSocket) {
        socket = newSocket;
    }

    /**
     * A getter for output map used to store Server Application responses sent back to Client Application.
     *
     * @return {@code Map<String, String>} object containing current response structure of a Server Application from the current thread.
     */
    public Map<String, String> getOutputMap() {
        return outputMap;
    }

    /**
     * A setter for output map used to store Server Application response sent back to Client Application.
     *
     * @param newOutputMap {@code Map<String, String>} object to be set as new output map of a Server Application in a given thread.
     */
    public void setOutputMap(Map<String, String> newOutputMap) {
        outputMap = newOutputMap;
    }

    /**
     * A getter for writer lock used to concurrently modify database file.
     *
     * @return {@code Lock} object used to lock a file for concurrent modification.
     */
    public Lock getWriterLock() {
        return writerLock;
    }

    /**
     * A setter for writer lock used to concurrently modify database file.
     *
     * @param newWriterLock {@code Lock} object to be set as new writing lock.
     */
    public void setWriterLock(Lock newWriterLock) {
        writerLock = newWriterLock;
    }

    /**
     * A getter for reader lock used to concurrently access database file.
     *
     * @return {@code Lock} object used to lock a file for concurrent access.
     */
    public Lock getReaderLock() {
        return readerLock;
    }

    /**
     * A setter for reader lock used to concurrently access database file.
     *
     * @param newReaderLock {@code Lock} object to be set as new reading lock.
     */
    public void setReaderLock(Lock newReaderLock) {
        readerLock = newReaderLock;
    }

    /**
     * A getter for database file path.
     *
     * @return {@code String} representing path to the database file.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * A setter for database file path.
     *
     * @param newFilePath {@code String} to be set as new database file path.
     */
    public void setFilePath(String newFilePath) {
        filePath = newFilePath;
    }

    /**
     * A getter for debugging flag.
     *
     * @return {@code boolean} value informing whether debugging mode is on or off.
     */
    public boolean isDebugFlag() {
        return debugFlag;
    }

    /**
     * A setter for debugging mode flag.
     *
     * @param newDebugFlag {@code boolean} value to be set as new debugging mode flag.
     */
    public void setDebugFlag(boolean newDebugFlag) {
        debugFlag = newDebugFlag;
    }

    /**
     * Constructs a new MyClass object with the specified socket, output map, writer lock,
     * reader lock, and file path.
     *
     * @param socket A socket used by Client Application connection.
     * @param writerLock A lock used to concurrently write to the database.
     * @param readerLock A lock used to concurrently read from the database.
     * @param filePath A path to database file.
     */
    public ClientConnectionHandler(Socket socket, Lock writerLock, Lock readerLock, String filePath, boolean debugFlag) {
        this.socket = socket;
        this.writerLock = writerLock;
        this.readerLock = readerLock;
        this.filePath = filePath;
        this.debugFlag = debugFlag;
    }

    /**
     * Overrode default {@code run()} method of a thread.
     * <p>
     * Waits for input sent by Client Application to the server via socket.
     * <p>
     * Internally calls methods to handle request according to it's content.
     * <p>
     * Sends back response of the server and closes the connection socket.
     */
    @Override
    public void run() {
        try (
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())
        ) {
            String input = dataInputStream.readUTF();
            if (Server.getInputArguments().isDebugFlag()) {
                System.out.printf("Received: %s\n", input);
            }
            JsonObject jsonObject = parseFromJson(input);
            handleRequest(filePath, writerLock, readerLock, jsonObject);
            String output = advancedParseToJson(outputMap);
            dataOutputStream.writeUTF(output);
            if (Server.getInputArguments().isDebugFlag()) {
                System.out.printf("Sent: %s\n", output);
            }
            clearOutputMap(outputMap);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Parses input {@code String} into a {@code JsonObject} object.
     *
     * @param input {@code String} to be parsed.
     * @return {@code JsonObject} object created from input {@code String}.
     */
    private JsonObject parseFromJson(String input) {
        return JsonParser.parseString(input).getAsJsonObject();
    }

    /**
     * Parses a key-value map into single JSON structured String.
     *
     * @param map {@code Map<String, String>} input map to be parsed into a single JSON-like String.
     * @return {@code String} consisting of JSON structured output based on input map.
     */
    private String parseToJson(Map<String, String> map) {
        return new Gson().toJson(map);
    }

    /**
     * Auxiliary method that fixes JSON structure if needed.
     * <p>
     * It deletes unnecessary backslashes ({@code \}) and double-quotes ({@code "}) characters created during parsing.
     * <p>
     * Such correction is not always necessary.
     *
     * @param s {@code String} consisting of JSON structure to be fixed.
     * @return {@code String} consisting of fixed JSON structure.
     */
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

    /**
     * Combines parsing key-value map via {@code ParseIntoJson} method with fixing the JSON-like String via {@code fixJsonOutput} method.
     *
     * @param map {@code Map<String, String>} to be both parsed and fixed into a proper JSON structured String.
     * @return {@code String} consisting of properly formatted JSON structure build on key-value map.
     */
    private String advancedParseToJson(Map<String, String> map) {
        return fixJsonOutput(parseToJson(map));
    }

    /**
     * Calls proper request handling method based on request type.
     *
     * @param pathToFile Path to database file.
     * @param writerLock Lock used for concurrent writing into database.
     * @param readerLock Lock used for concurrent retrieval of data from database.
     * @param jsonObjectInput {@code JsonObject} object built of input request.
     */
    private void handleRequest(String pathToFile, Lock writerLock, Lock readerLock,  JsonObject jsonObjectInput) {
        if (checkIfNotNull(jsonObjectInput) && checkIfNotNull(jsonObjectInput.get("type"))) {
            switch (jsonObjectInput.get("type").getAsString()) {
                case "get":
                    handleGetRequest(pathToFile, readerLock, jsonObjectInput.get("key"));
                    break;
                case "set":
                    handleSetRequest(pathToFile, writerLock, jsonObjectInput.get("key"), jsonObjectInput.get("value"));
                    break;
                case "delete":
                    handleDeleteRequest(pathToFile, writerLock, readerLock, jsonObjectInput.get("key"));
                    break;
                case "exit":
                    handleExitRequest();
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * Reads data from the database file and parses it into a {@code JsonObject} object.
     *
     * @param pathToFile Path to database file.
     * @param readerLock Lock used for concurrent retrieval of data from database.
     * @return
     * <ul>
     *     <li>Nonempty {@code JsonObject} object in case data file consisted data and parsing process was successful.</li>
     *     <li>Empty {@code JsonObject} object in case data file consisted no data. </li>
     *     <li>{@code null} in case errors during either file access or data parsing.a</li>
     * </ul>
     */
    private JsonObject readFromJson(String pathToFile, Lock readerLock) {
        try (FileReader fileReader = new FileReader(pathToFile)) {
            readerLock.lock();
            JsonObject map = new Gson().fromJson(fileReader, JsonObject.class);
            if (checkIfNotNull(map)) {
                readerLock.unlock();
                return map;
            }
            readerLock.unlock();
            return new JsonObject();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            readerLock.unlock();
            return null;
        }
    }

    /**
     * Creates intermediate {@code JsonObjet} out of provided parameters and calls {@code writeToJsonDatabase} method in order to save new data in the database file.
     *
     * @param pathToFile Path to database file.
     * @param writerLock Lock used for concurrent writing into the database.
     * @param key {@code JsonElement} object consisting of key that identifies key-value pair to be inserted into the database.
     * @param value {@code JsonElement} object consisting of value part of key-value pair to be inserted into the database.
     * @return {@code int} code according to the success of operation:
     * <ul>
     *      <li>{@code 1} operation terminated successfully - new data was inserted into the database.</li>
     *      <li>{@code -1} operation terminated with an error - could not insert data into the database.</li>
     *      <li>{@code -2} operation terminated with an error - at least one of the provided parameters is a {@code null} value.</li>
     * </ul>
     */
    private int setInJsonDatabase(String pathToFile, Lock writerLock, JsonElement key, JsonElement value) {
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
        if (writeToJsonDatabase(pathToFile, writerLock, newData)) {
            return ServerConstants.POSITIVE;
        } else {
            return ServerConstants.ERROR;
        }
    }

    /**
     * Writes new data into the database file.
     *
     * @param pathToFile Path to database file.
     * @param writerLock Lock used for concurrent writing into the database.
     * @param newData {@code JsonObject} object containing new data to be added to the database file.
     * @return {@code boolean} value informing whether the operation was successful.
     */
    private boolean writeToJsonDatabase(String pathToFile, Lock writerLock, JsonObject newData) {
        // TESTing: compare content of pre-made file and one created here
        try (FileReader fileReader = new FileReader(pathToFile)) {
            Gson gson = new Gson();
            writerLock.lock();
            JsonObject map = new Gson().fromJson(fileReader, JsonObject.class);
            if (checkIfNotNull(map)) {
                mergeJsonObjects(map, newData);
            } else {
                map = newData;
            }
            try (FileWriter fileWriter = new FileWriter(pathToFile)) {
                gson.toJson(map, fileWriter);
            }
            writerLock.unlock();
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            writerLock.unlock();
            return false;
        }
    }

    /**
     * Merges 2 JSON objects into one making sure that JSON structure and data hierarchy is maintained and no duplicates are created.
     *
     * @param jsonObject1 {@code JsonObject} base object to merge data into.
     * @param jsonObject2 {@code JsonObject} object that would be merged into the base.
     */
    private void mergeJsonObjects(JsonObject jsonObject1, JsonObject jsonObject2) {
        for (String key : jsonObject2.keySet()) {
            if (jsonObject2.get(key).isJsonObject() && jsonObject1.has(key) && jsonObject1.get(key).isJsonObject()) {
                mergeJsonObjects(jsonObject1.getAsJsonObject(key), jsonObject2.getAsJsonObject(key));
            } else {
                jsonObject1.add(key, jsonObject2.get(key));
            }
        }
    }

    /**
     * Deletes a given key entry from database file.
     *
     * @param pathToFile Path to database file.
     * @param writerLock Lock used for concurrent writing into the database.
     * @param readerLock Lock used for concurrent retrieval of data from the database.
     * @param key {@code JsonObject} object representing key that identifies key-value pair be deleted from the database.
     * @return {@code int} code according to success of the operation:
     * <ul>
     *     <li>{@code 1} operation terminated successfully - value content was deleted from the database.</li>
     *     <li>{@code 0} operation terminated unsuccessfully - value content was not present int the database.</li>
     *     <li>{@code -1} operation terminated with an error - database was not found.</li>
     * </ul>
     */
    private int deleteFromJsonDatabase(String pathToFile, Lock writerLock, Lock readerLock, JsonElement key) {
        JsonObject database = readFromJson(pathToFile, readerLock);
        if (checkIfNotNull(database)) {
            if (key.isJsonArray()) {
                return deleteKeyArrayFromJsonDatabase(pathToFile, writerLock, database, key.getAsJsonArray());
            } else {
                if (checkIfNotNull(database.remove(key.getAsString()))) {
                    try {
                        writerLock.lock();
                        Files.write(Path.of(pathToFile), new byte[0]);
                        writerLock.unlock();
                        writeToJsonDatabase(pathToFile, writerLock, database);
                        return ServerConstants.POSITIVE;
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
                return ServerConstants.NEGATIVE;
            }
        }
        return ServerConstants.ERROR;
    }

    /**
     * Searches for a value identified by provided key array and deletes it.
     *
     * @param pathToFile Path to database file.
     * @param writerLock Lock used for concurrent writing into database.
     * @param database {@code JsonObject} containing database data.
     * @param keyArray Array of keys to identify data to be deleted.
     * @return {@code int} code according to success of the operation:
     *      * <ul>
     *      *     <li>{@code 1} operation terminated successfully - value content was deleted from the database.</li>
     *      *     <li>{@code 0} operation terminated unsuccessfully - value content was not present int the database.</li>
     *      * </ul>
     */
    private int deleteKeyArrayFromJsonDatabase(String pathToFile, Lock writerLock, JsonObject database, JsonArray keyArray) {
        int lastIndex = keyArray.size() - 1;
        JsonObject temp = database;
        for (int i = 0; i < keyArray.size(); i++) {
            JsonElement k = keyArray.get(i);
            JsonElement valuePlaceholder = temp.get(k.getAsString());
            if (checkIfNotNull(valuePlaceholder)) {
                if (valuePlaceholder.isJsonPrimitive()) {
                    if (i == lastIndex) {
                        if (checkIfNotNull(temp.remove(k.getAsString()))) {
                            try {
                                writerLock.lock();
                                Files.write(Path.of(pathToFile), new byte[0]);
                                writerLock.unlock();
                                writeToJsonDatabase(pathToFile, writerLock, database);
                                return ServerConstants.POSITIVE;
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                        return ServerConstants.NEGATIVE;
                    }
                } else {
                    temp = temp.getAsJsonObject(k.getAsString());
                }
            }
        }
        return ServerConstants.NEGATIVE;
    }

    /**
     * Handles retrieval of value (or values) for provided key.
     * <p>
     * Adds proper response message to the output map based on the success of the process.
     *
     * @param pathToFile Path to database file.
     * @param readerLock Lock used for concurrent reading from database file.
     * @param key {@code JsonElement} object representing key (or key array) to be used during retrieving process.
     */
    private void handleGetRequest(String pathToFile, Lock readerLock, JsonElement key) {
        if (checkIfNotNull(key)) {
            JsonObject database = readFromJson(pathToFile, readerLock);
            if (checkIfNotNull(database)) {
                if (key.isJsonArray()) {
                    handleGetRequestWithKeyArray(key.getAsJsonArray(), database);
                } else {
                    JsonElement value = database.get(key.getAsString());
                    if (checkIfNotNull(value)) {
                        writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.SUCCESS_MESSAGE);
                        writeTotMap(outputMap, ServerConstants.RESPONSE_VALUE, value.toString());
                    } else {
                        writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
                        writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_REASON_NO_KEY);
                    }
                }
            } else {
                writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
                writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_REASON_NO_KEY);
            }
            return;
        }
        writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
        writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_REASON_ILLEGAL);
    }

    /**
     * Retrieves data from database according to JSON structure and provided array of keys.
     *
     * @param keyArray {@code JsonArray} containing keys to be accessed.
     * @param database {@code JsonObject} to retrieve data from.
     */
    private void handleGetRequestWithKeyArray(JsonArray keyArray, JsonObject database) {
        JsonObject temp = database;
        int lastIndex = keyArray.size();
        for (int i = 0; i < keyArray.size(); i++) {
            JsonElement k = keyArray.get(i);
            JsonElement valuePlaceholder = temp.get(k.getAsString());
            if (checkIfNotNull(valuePlaceholder)) {
                if (temp.get(k.getAsString()).isJsonPrimitive()) {
                    if (i == lastIndex) {
                        writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.SUCCESS_MESSAGE);
                        writeTotMap(outputMap, ServerConstants.RESPONSE_VALUE, temp.get(k.getAsString()).getAsJsonPrimitive().getAsString());
                        return;
                    }
                } else {
                    temp = temp.getAsJsonObject(k.getAsString());
                }
            } else {
                writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
                writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_REASON_NO_KEY);
                return;
            }
        }
        if (!Objects.equals(temp, database)) {
            writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.SUCCESS_MESSAGE);
            writeTotMap(outputMap, ServerConstants.RESPONSE_VALUE, temp.toString());
        } else {
            writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
            writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_REASON_NO_KEY);
        }
    }

    /**
     * Calls {@code setInJsonDatabase} method responsible for setting of values connected to provided keys from JSON database.
     * <p>
     * Adds proper response message to the output map based on the return value of the {@code setInJsonDatabase} method.
     *
     * @param pathToFile Path to database file.
     * @param writerLock Lock used for concurrent writing into database file.
     * @param key {@code JsonElement} object representing key (or key array) to be used during setting key-value pair process.
     * @param value {@code JsonElement} object representing value (or value array) to be used during setting a key-value pair process.
     */
    private void handleSetRequest(String pathToFile, Lock writerLock, JsonElement key, JsonElement value) {
        if (checkIfNotNull(key) && checkIfNotNull(value)) {
            switch (setInJsonDatabase(pathToFile, writerLock, key, value)) {
                case ServerConstants.POSITIVE:
                    writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.SUCCESS_MESSAGE);
                    break;
                case ServerConstants.ERROR:
                    writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
                    writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_DATABASE_ERROR);
                    break;
            }
        } else {
            writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
            writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_REASON_ILLEGAL);
        }

    }

    /**
     * Calls {@code deleteFromJsonDatabase} method responsible for deletion of values connected to provided keys from JSON database.
     * <p>
     * Adds proper response message to the output map based on the return value of the {@code deleteFromJsonDatabase} method.
     *
     * @param pathToFile Path to database file.
     * @param writerLock Lock used for concurrent writing into database file.
     * @param readerLock Lock used for concurrent reading from database file.
     * @param key {@code JsonElement} object representing key (or key array) to be used during deletion process.
     */
    private void handleDeleteRequest(String pathToFile, Lock writerLock, Lock readerLock, JsonElement key) {
        if (checkIfNotNull(key)) {
            switch (deleteFromJsonDatabase(pathToFile, writerLock, readerLock, key)) {
                case ServerConstants.POSITIVE:
                    writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.SUCCESS_MESSAGE);
                    break;
                case ServerConstants.NEGATIVE:
                    writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
                    writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_REASON_NO_KEY);
                    break;
                case ServerConstants.ERROR:
                    writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
                    writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_DATABASE_ERROR);
                    break;
            }
        } else {
            writeTotMap(outputMap, ServerConstants.RESPONSE, ServerConstants.ERROR_MESSAGE);
            writeTotMap(outputMap, ServerConstants.RESPONSE_REASON, ServerConstants.RESPONSE_REASON_ILLEGAL);
        }
    }

    /**
     * Calls {@code shutdownServer} method of the Server Application.
     */
    private void handleExitRequest() {
        Server.shutdownServer();
        outputMap.put(ServerConstants.RESPONSE, ServerConstants.SUCCESS_MESSAGE);
    }

    /**
     * Checks whether the object is not null.
     *
     * @param input {@code Object} to be checked.
     * @return {@code boolean} value informing whether the {@code Object} is null or not.
     */
    private boolean checkIfNotNull(Object input) {
        return !Objects.equals(input, null);
    }

    /**
     * Adds given data into the output map used to create Server Application responses.
     *
     * @param map {@code Map<String, String>} to insert key-value pair into.
     * @param key {@code String} representing output key part.
     * @param value {@code String} representing output value part.
     */
    private void writeTotMap(Map<String, String> map, String key, String value) {
        map.put(key, value);
    }

    /**
     * Clears content of output map used to create Server Application responses.
     *
     * @param map {@code Map<String, String>} to be cleared out.
     */
    private void clearOutputMap(Map<String, String> map) {
        map.clear();
    }

}
