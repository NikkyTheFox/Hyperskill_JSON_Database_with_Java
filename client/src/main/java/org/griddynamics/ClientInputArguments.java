package org.griddynamics;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class consisting of run parameters for Client Application.
 */
public class ClientInputArguments {

    /**
     * Type of request.
     */
    @Parameter(names = {"-t"}, description = "Type of a request")
    private String type;

    /**
     * Key to be accessed.
     */
    @Parameter(names = {"-k"}, description = "Key to be accessed")
    private String key;

    /**
     * Value to be set to the given key entry.
     */
    @Parameter(names = {"-v"}, description = "Value to be set")
    private String value;

    /**
     * Path of a file that stores a request to be sent to server.
     */
    @Parameter(names = {"-in"}, description = "Name of the request input file")
    private String fileName = null;

    /**
     * A getter for {@code type} parameter of a request sent by Client Application.
     *
     * @return {@code String} representing current request type parameter.
     */
    public String getType() {
        return type;
    }

    /**
     * A setter for {@code type} parameter of a request sent by Client Application.
     *
     * @param newType {@code String} to be set as new request type.
     */
    public void setType(String newType) {
        type = newType;
    }

    /**
     * A getter for {@code key} parameter of a request sent by Client Application.
     *
     * @return {@code String} representing current request key parameter.
     */
    public String getKey() {
        return key;
    }

    /**
     * A setter for {@code key} parameter of a request sent by Client Application.
     *
     * @param newKey {@code String} to be set as new request key parameter.
     */
    public void setKey(String newKey) {
        key = newKey;
    }

    /**
     * A getter for {@code value} parameter of a request sent by Client Application.
     *
     * @return {@code String} representing current request value parameter.
     */
    public String getValue() {
        return value;
    }

    /**
     * A setter for {@code value} parameter of a request sent by Client Application.
     *
     * @param newValue {@code String} to be set as new request value parameter.
     */
    public void setValue(String newValue) {
        value = newValue;
    }

    /**
     * A getter for a {@code file path} parameter of a request set by Client Application.
     *
     * @return {@code String} representing current request file path parameter.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * A setter for {@code file path} parameter of a request to be sent by Client Application.
     *
     * @param newFileName {@code String} to be set as new request file path parameter.
     */
    public void setFileName(String newFileName) {
        fileName = newFileName;
    }

    /**
     * A constructor for {@code ClientInputArguments} object that parses arguments with JCommander.
     *
     * @param args {@code String[]} Run arguments used by the Client Application.
     */
    public ClientInputArguments(String[] args) {
        JCommander.newBuilder()
                .addObject(this)
                .build()
                .parse(args);
    }

    /**
     * Parse all run parameters into a single JSON String.
     *
     * @return {@code String} containing parameters parsed into a JSON syntax.
     */
    public String parseIntoJson() {
        if (!Objects.equals(fileName, null)) {
            try {
                return new String(Files.readAllBytes(Paths.get(ClientConstants.PATH_TO_DATA + fileName)));
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
