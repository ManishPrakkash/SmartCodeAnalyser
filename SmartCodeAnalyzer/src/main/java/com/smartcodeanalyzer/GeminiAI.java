package com.smartcodeanalyzer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Connects to the Gemini API to analyze code (explain, debug, refactor)
 */
public class GeminiAI {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final String apiKey;
    // Do NOT hardcode API keys. The API key must be provided via constructor or GEMINI_API_KEY env var.
    // Computed per request using endpointTemplate
    // Model configuration
    private final String primaryModel = "gemini-1.5-pro";
    private final String fallbackModel = "gemini-1.5-flash";
    private final String[] defaultModelSequence = new String[] {
        // Prefer widely-available 1.5 models first to reduce 404s
        "gemini-1.5-flash-8b",
        "gemini-1.5-flash-8b-latest",
        fallbackModel,
        fallbackModel + "-latest",
        primaryModel,
        primaryModel + "-latest",
        // Then try newer 2.5 family if available
        "gemini-2.5-flash",
        "gemini-2.5-flash-latest",
        "gemini-2.5-pro",
        "gemini-2.5-pro-latest",
        // Finally, 1.0 as last resort
        "gemini-1.0-pro",
        "gemini-1.0-pro-latest"
    };
    private final String endpointTemplate = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private final Gson gson;
    private final String[] modelCandidates;
    // Optional provider override via env: AI_PROVIDER=gemini|ollama (default: gemini)
    private final String provider;

    /**
     * Constructor initializes with API key
     * @param apiKey Google API key for Gemini
     */
    public GeminiAI(String apiKey) {
        // Prefer explicit constructor key, then environment variable. Fail fast if not present.
        String envKey = System.getenv("GEMINI_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            this.apiKey = apiKey;
        } else if (envKey != null && !envKey.isBlank()) {
            this.apiKey = envKey;
        } else {
            throw new IllegalArgumentException("GEMINI_API_KEY must be provided via constructor or GEMINI_API_KEY environment variable");
        }
    // Use a valid v1beta model (gemini-1.5-pro) as primary
        
        this.client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
            
        this.gson = new Gson();

        // Always use gemini provider
        this.provider = "gemini";

        // Compute candidate models (env override takes precedence if provided)
        String envModel = System.getenv("GEMINI_MODEL");
        if (envModel != null && !envModel.trim().isEmpty()) {
            // Try env model first, then defaults
            this.modelCandidates = new String[] { envModel.trim() , defaultModelSequence[0], defaultModelSequence[1], defaultModelSequence[2], defaultModelSequence[3] };
        } else {
            this.modelCandidates = defaultModelSequence;
        }
    }

    /**
     * Explains the code - general code explanation
     * @param code Java code to explain
     * @return String containing the explanation from Gemini
     */
    public String explainCode(String code) {
        String codeBlock = prepareCode(code);
    String prompt = "EXPLAIN (terminal-friendly, plain text):\n" +
            "Provide exactly 5 short numbered points (1.-5.) about the code.\n" +
            "Each point should be one short sentence (max 1-2 lines).\n" +
            "Do NOT use Markdown, bullets (* or -), bold/italic (** or _), or code fences (``` or `).\n" +
            "Avoid extra commentary; just output the 5 numbered points.\n\n" + codeBlock;
        String raw = sendRequest(prompt);
        return sanitizeExplainOutput(raw);
    }

    /**
     * Clean up EXPLAIN output: strip markdown/code fences and ensure plain numbered points.
     */
    private String sanitizeExplainOutput(String raw) {
        if (raw == null) return "";
        // Remove triple backticks and inline backticks
        String cleaned = raw.replace("```", "").replace("`", "");
        // Remove bold/italic markers
        cleaned = cleaned.replace("**", "").replace("__", "").replace("*", "");
            // Normalize bullets (•, -, +, *, +) at line starts
            // Use a simple, safe pattern and include the bullet character directly
            cleaned = cleaned.replaceAll("(?m)^\\s*[•*+-]\\s*", "");
        // Split into lines and collect non-empty lines
        String[] lines = cleaned.split("\\r?\\n");
        java.util.List<String> points = new java.util.ArrayList<>();
        for (String ln : lines) {
            String t = ln.trim();
            if (t.isEmpty()) continue;
            // Remove leading numbering like '1)', '1.' etc
                t = t.replaceFirst("^\\s*\\d+\\s*[\\)\\.]?\\s*", "");
            // Skip lines that are clearly meta
            if (t.toLowerCase().startsWith("note:") || t.toLowerCase().startsWith("explain")) continue;
            points.add(t);
            if (points.size() >= 6) break; // allow up to 6 but prefer 5
        }
        // If we found none, return original trimmed
        if (points.isEmpty()) return cleaned.trim();
        // Ensure we output exactly up to 6 numbered points, prefer first 5
        int take = Math.min(points.size(), 6);
        if (take > 5) take = 5; // prefer 5 points
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < take; i++) {
            out.append(i + 1).append(". ").append(points.get(i)).append(System.lineSeparator());
        }
        return out.toString().trim();
    }

    /**
     * Debugs the code - finds potential issues
     * @param code Java code to debug
     * @return String containing the debug analysis from Gemini
     */
    public String debugCode(String code) {
        String codeBlock = prepareCode(code);
        String prompt = "DEBUG (short):\n" +
                        "- If no errors, return: 'No errors found.'\n" +
                        "- Otherwise list issues as: [line]: brief reason -> concise fix\n\n" + codeBlock;
        
        return sendRequest(prompt);
    }

    /**
     * Refactors the code - suggests optimizations
     * @param code Java code to refactor
     * @return String containing refactoring suggestions from Gemini
     */
    public String refactorCode(String code) {
        String codeBlock = prepareCode(code);
        String prompt = "REFACTOR (optimize):\n" +
                        "1) TIME and SPACE complexity (current)\n" +
                        "2) Is optimization possible? (yes/no) and short rationale\n" +
                        "3) If yes: provide optimized code and new TIME/SPACE complexities\n\n" + codeBlock;
        
        return sendRequest(prompt);
    }

    /**
     * Sends request to Gemini API
     * @param prompt The prompt to send to Gemini API
     * @return String containing the AI response
     */
    private String sendRequest(String prompt) {
        // Always use Gemini only - Ollama fallback disabled
        return sendViaGemini(prompt);
    }

    // Original Gemini flow extracted here
    private String sendViaGemini(String prompt) {
        try {
            // Build request body
            
            // Create the content part with the prompt
            JsonObject contentObject = new JsonObject();
            contentObject.addProperty("role", "user");
            JsonObject partObject = new JsonObject();
            partObject.addProperty("text", prompt);
            
            JsonArray partsArray = new JsonArray();
            partsArray.add(partObject);
            
            contentObject.add("parts", partsArray);
            
            JsonArray contentsArray = new JsonArray();
            contentsArray.add(contentObject);
            
            // Create the request body with proper generation parameters
            JsonObject requestBody = new JsonObject();
            requestBody.add("contents", contentsArray);
            
            // Add generation configuration
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.2);
            generationConfig.addProperty("maxOutputTokens", 2048);
            generationConfig.addProperty("topP", 0.8);
            generationConfig.addProperty("topK", 40);
            requestBody.add("generationConfig", generationConfig);
            
            // Convert the request body to a string for debugging
            String requestBodyStr = requestBody.toString();
            
            // Helper to perform a single request against a URL
            java.util.function.Function<String, String> doRequest = (String url) -> {
                try {
                    RequestBody body = RequestBody.create(requestBodyStr, JSON);
                    Request request = new Request.Builder()
                        .url(url + "?key=" + apiKey)
                        .header("Content-Type", "application/json")
                        .post(body)
                        .build();

                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "Empty response";
                        if (!response.isSuccessful()) {
                            // Return a marker so caller can decide on fallback
                            return "__HTTP_" + response.code() + "__" + responseBody;
                        }
                        // Parse the response
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        String extractedText = extractTextFromResponse(jsonResponse);
                        if (extractedText == null || extractedText.isEmpty()) {
                            return "AI couldn't generate a response. Please try again with a simpler code sample.";
                        }
                        return extractedText;
                    }
                } catch (IOException io) {
                    return "__IO__" + io.getMessage();
                } catch (Exception ex) {
                    return "__EX__" + ex.getMessage();
                }
            };
            
            // Iterate through model candidates until success
            String lastHttpError = null;
            for (String model : modelCandidates) {
                String url = String.format(endpointTemplate, model);
                String result = doRequest.apply(url);
                if (result.startsWith("__HTTP_")) {
                    lastHttpError = result;
                    // If 404 for this model, try next one
                    String withoutPrefix = result.substring("__HTTP_".length());
                    int sep = withoutPrefix.indexOf("__");
                    String codeStr = sep > -1 ? withoutPrefix.substring(0, sep) : withoutPrefix;
                    int code = 0;
                    try { code = Integer.parseInt(codeStr); } catch (NumberFormatException ignore) {}
                    if (code == 404) {
                        continue; // try next model
                    }
                    // Non-404 http error: map and stop
                    if (code == 401 || code == 403) return "Invalid API key or insufficient permissions. Please check your API key.";
                    if (code == 400) return "The API request format is incorrect. Please report this issue.";
                    if (code == 429) return "The AI service rate limit was hit. Please slow down and try again.";
                    if (code >= 500) return "The AI service is currently experiencing issues. Please try again later.";
                    return "AI analysis unavailable. Please try again later.";
                } else if (result.startsWith("__IO__")) {
                    return "Could not connect to AI service. Please check your internet connection.";
                } else if (result.startsWith("__EX__")) {
                    return "An unexpected error occurred while communicating with the AI service.";
                } else {
                    // Success
                    return result;
                }
            }

            // If we exhausted candidates with 404s
            if (lastHttpError != null && lastHttpError.startsWith("__HTTP_404")) {
                return "No available AI model for this API key/project. Set GEMINI_MODEL to a permitted model or enable access to Gemini 1.5 in Google Cloud console.";
            }
            return "AI model not available right now. Please try again later.";
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return "An unexpected error occurred while communicating with the AI service.";
        }
    }

    // Limit code length to avoid oversized requests; include head and tail if truncated
    private String prepareCode(String code) {
        if (code == null) return "";
        final int max = 16000; // chars
        if (code.length() <= max) {
            return "---CODE START (java)---\n" + code + "\n---CODE END---";
        }
        int head = 8000;
        int tail = 8000;
        String truncated = code.substring(0, head) + "\n...\n[truncated for length]\n...\n" + code.substring(code.length() - tail);
        return "---CODE START (java)---\n" + truncated + "\n---CODE END---\n(Note: input was truncated for length)";
    }

    // Ollama local fallback (http://localhost:11434 by default)
    private String sendViaOllama(String prompt) {
        try {
            String ollamaHost = System.getenv("OLLAMA_HOST");
            if (ollamaHost == null || ollamaHost.isBlank()) {
                ollamaHost = "http://localhost:11434";
            }
            String ollamaModel = System.getenv("OLLAMA_MODEL");
            if (ollamaModel == null || ollamaModel.isBlank()) {
                // A sensible default that many installs provide
                ollamaModel = "llama3.1";
            }

            // Build request JSON
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(msg);

            JsonObject req = new JsonObject();
            req.addProperty("model", ollamaModel);
            req.add("messages", messages);
            req.addProperty("stream", false);

            String url = ollamaHost.endsWith("/") ? (ollamaHost + "api/chat") : (ollamaHost + "/api/chat");
            RequestBody body = RequestBody.create(req.toString(), JSON);
            Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ""; // allow caller to continue other fallbacks
                }
                String responseBody = response.body() != null ? response.body().string() : "";
                if (responseBody.isEmpty()) return "";
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                if (json == null) return "";
                // Try standard chat response structure
                if (json.has("message")) {
                    JsonObject message = json.getAsJsonObject("message");
                    if (message.has("content")) {
                        return message.get("content").getAsString();
                    }
                }
                // Some Ollama versions return an array of messages or different shapes; try a generic extract
                if (json.has("response")) {
                    return json.get("response").getAsString();
                }
                return "";
            }
        } catch (Exception e) {
            // Ollama not available/reachable
            return "";
        }
    }

    /**
     * Helper method to extract text from the API response
     * @param jsonResponse The JSON response from Gemini API
     * @return String containing the extracted text
     */
    private String extractTextFromResponse(JsonObject jsonResponse) {
        StringBuilder result = new StringBuilder();
        
        try {
            
            // Check for error field first
            if (jsonResponse.has("error")) {
                JsonObject error = jsonResponse.getAsJsonObject("error");
                return "API Error: " + error.get("message").getAsString();
            }
            
            // Try to extract the content
            if (!jsonResponse.has("candidates")) {
                System.err.println("No 'candidates' field in response");
                return "";
            }
            
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates.size() == 0) {
                System.err.println("Empty candidates array");
                return "";
            }
            
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            
            if (!candidate.has("content")) {
                System.err.println("No 'content' field in candidate");
                return "";
            }
            
            JsonObject content = candidate.getAsJsonObject("content");
            
            if (!content.has("parts")) {
                System.err.println("No 'parts' field in content");
                return "";
            }
            
            JsonArray parts = content.getAsJsonArray("parts");
            for (int i = 0; i < parts.size(); i++) {
                JsonObject part = parts.get(i).getAsJsonObject();
                if (part.has("text")) {
                    result.append(part.get("text").getAsString());
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting text from response: " + e.getMessage());
        }
        
        return result.toString();
    }
}