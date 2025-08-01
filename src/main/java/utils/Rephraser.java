package utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Optionale Klasse für die KI-gestützte Neuformulierung von Fragen.
 * Implementierung ist abhängig von der gewählten API oder Methode.
 */
public class Rephraser {

    private static final String API_KEY = loadApiKey();
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private static String loadApiKey() {
        Properties prop = new Properties();
        try (InputStream input = Rephraser.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return null;
            }
            prop.load(input);
            return prop.getProperty("api.key");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String rephrase(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return originalText; // Retourne le texte original s'il est vide ou null
        }

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Construct the request body for the Gemini API
            ObjectNode rootNode = objectMapper.createObjectNode();
            ArrayNode contentsNode = rootNode.putArray("contents");
            ObjectNode partNode = contentsNode.addObject().putObject("parts");
            // Prompt modifié pour obtenir seulement le texte reformulé dans la langue d'origine
            partNode.put("text", "Rephrase the following text, keeping the original language and providing only the rephrased version: " + originalText);

            String requestBody = objectMapper.writeValueAsString(rootNode);

            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check for successful response
            if (response.statusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                // Extract the rephrased text from the response
                JsonNode textNode = responseJson.at("/candidates/0/content/parts/0/text");
                if (textNode.isTextual()) {
                    return textNode.asText();
                } else {
                    System.err.println("Error: Could not find rephrased text in Gemini API response. Response: " + response.body());
                    return originalText; // Retourne le texte original en cas d'échec de la reformulation
                }
            } else {
                System.err.println("Error calling Gemini API. Status Code: " + response.statusCode() + ", Response: " + response.body());
                return originalText; // Retourne le texte original en cas d'échec de l'appel API
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Exception during rephrasing: " + e.getMessage());
            return originalText; // Retourne le texte original en cas d'exception
        }
    }
}
