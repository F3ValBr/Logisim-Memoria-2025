package com.cburch.logisim.verilog.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * Helper class for loading and validating JSON synthesis files.
 */
public class JsonSynthFile {

    /**
     * Loads a JSON file and parses it into a JsonNode.
     *
     * @param file JSON file to read.
     * @return Parsed JsonNode.
     * @throws IOException If the file does not exist, is not readable, or if there
     * is an error parsing the JSON.
     */
    public static JsonNode load(File file) throws IOException {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new IOException(Strings.get("jsonNullFileError", file == null ? "null" : file.getAbsolutePath()));
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(file);
        } catch (IOException ex) {
            throw new IOException(Strings.get("jsonParseError", file.getAbsolutePath()), ex);
        }
    }

    /**
     * Loads a JSON file and validates that it contains a non-null "name" field.
     *
     * @param file JSON file to read.
     * @return Validated JsonNode.
     * @throws IOException If the file does not exist, is not readable, or if the JSON is invalid.
     */
    public static JsonNode loadAndValidate(File file) throws IOException {
        JsonNode content = load(file);

        // Validate that the "name" field is present and non-null
        JsonNode creatorNode = content.get("creator");
        if (creatorNode == null || !creatorNode.isTextual()) {
            throw new IOException(Strings.get("jsonContentError", "creator"));
        }

        // Check that the creator is Yosys
        String creatorText = creatorNode.asText();
        if (!creatorText.startsWith("Yosys")) {
            throw new IOException(Strings.get("nonYosysError"));
        }

        return content;
    }
}
