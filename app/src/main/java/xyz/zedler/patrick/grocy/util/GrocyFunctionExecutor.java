/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.app.Application;
import android.util.Log;

import com.android.volley.VolleyError;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.Tool;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;

/**
 * Executes Grocy API function calls from Gemini.
 * Uses DownloadHelper to make actual HTTP requests.
 */
public class GrocyFunctionExecutor {

    private static final String TAG = GrocyFunctionExecutor.class.getSimpleName();
    private static final int REQUEST_TIMEOUT_SECONDS = 10;
    private final GrocyApi grocyApi;
    private final DownloadHelper downloadHelper;
    private final Client client;
    private final OpenAPIHelper helper;

    public GrocyFunctionExecutor(GrocyApi grocyApi, Application application, Client client, OpenAPIHelper helper) {
        this.grocyApi = grocyApi;
        this.downloadHelper = new DownloadHelper(application, "GrocyFunctionExecutor");
        this.client = client;
        this.helper = helper;
    }

    /**
     * Executes a function call from Gemini.
     * @param functionName Name of the function to execute
     * @param arguments JSON object containing function arguments
     * @return CompletableFuture with JSON string result
     */
    public CompletableFuture<String> executeFunction(String functionName, JSONObject arguments) {
        Log.d(TAG, "Executing function: " + functionName + " with args: " + arguments);

        try {
            if(Objects.equals(functionName, "google_search")) {
                return executeGoogleSearch(arguments)
                        .thenApply(GenerateContentResponse::text);
            }

            OpenAPIHelper.OperationInfo operation = helper.getGeminiFunctionOperation(functionName);
            if(operation == null) {
                return CompletableFuture.completedFuture(
                        createErrorResponse("Function '" + functionName + "' not recognized")
                );
            }

            return executeRest(functionName, operation, arguments);
        } catch (Exception e) {
            Log.e(TAG, "Error executing function: " + functionName, e);
            return CompletableFuture.completedFuture(
                    createErrorResponse("Error executing " + functionName + ": " + e.getMessage())
            );
        }
    }

    private CompletableFuture<String> executeRest(String functionName,
                               OpenAPIHelper.OperationInfo operation,
                               JSONObject arguments) {
        try {
            String path = operation.path;
            Map<String, String> queryParams = new HashMap<>();
            Map<String, String> headerParams = new HashMap<>();
            JSONObject requestBody = null;

            // Get all parameters from the operation
            List<io.swagger.v3.oas.models.parameters.Parameter> operationParams =
                    operation.operation.getParameters();

            if (operationParams != null) {
                for (io.swagger.v3.oas.models.parameters.Parameter param : operationParams) {
                    String paramName = param.getName();
                    String paramLocation = param.getIn(); // "path", "query", "header", "cookie"

                    if (!arguments.has(paramName)) {
                        // Check if parameter is required
                        if (param.getRequired() != null && param.getRequired()) {
                            return CompletableFuture.completedFuture(
                                    createErrorResponse("Missing required " + paramLocation + " parameter: " + paramName)
                            );
                        }
                        continue; // Skip optional parameters that aren't provided
                    }

                    String paramValue = convertArgumentToString(arguments, paramName);

                    switch (paramLocation) {
                        case "path":
                            // Replace path parameters
                            path = path.replace("{" + paramName + "}", paramValue);
                            break;
                        case "query":
                            queryParams.put(paramName, paramValue);
                            break;
                        case "header":
                            headerParams.put(paramName, paramValue);
                            break;
                        // "cookie" is rare and not supported by DownloadHelper
                    }
                }
            }

            // Handle request body if present
            if (arguments.has("body")) {
                requestBody = arguments.getJSONObject("body");
            }

            // Execute the appropriate HTTP method
            String httpMethod = operation.httpMethod.toLowerCase();
            switch (httpMethod) {
                case "get":
                    return getRequest(grocyApi.getUrlWithParams(path, queryParams), headerParams);

                case "post":
                    return postRequest(grocyApi.getUrlWithParams(path, queryParams), requestBody, headerParams);

                case "put":
                    return putRequest(grocyApi.getUrlWithParams(path, queryParams), requestBody, headerParams);

                case "delete":
                    return deleteRequest(grocyApi.getUrlWithParams(path, queryParams), headerParams);

                case "patch":
                    return patchRequest(grocyApi.getUrlWithParams(path, queryParams), requestBody, headerParams);

                default:
                    return CompletableFuture.completedFuture(
                            createErrorResponse("Unsupported HTTP method: " + operation.httpMethod)
                    );
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing arguments for " + functionName, e);
            return CompletableFuture.completedFuture(
                    createErrorResponse("Error parsing arguments: " + e.getMessage())
            );
        }
    }

    private CompletableFuture<GenerateContentResponse> executeGoogleSearch(JSONObject arguments) {
        String query = getStringArg(arguments, "query", "");
        if (query.isEmpty()) { return null; }

        Content systemInstruction = Content.fromParts(
                Part.fromText(
                        "You are a helpful assistant that can perform web searches to find up-to-date information."
                )
        );

        Tool googleSearchTool = Tool.builder().googleSearch(GoogleSearch.builder()).build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0))
                .systemInstruction(systemInstruction)
                .tools(googleSearchTool)
                .build();

        return client.async.models.generateContent("gemini-2.5-flash", "Perform a web search for: " + query, config);

        /*if (generateContentResponse != null && generateContentResponse.text() != null) {
            return "{ \"response\": " + JSONObject.quote(generateContentResponse.text()) + " }";
        }*/
    }

    // Helper methods

    /**
     * Makes a async GET request and returns the response.
     */
    private CompletableFuture<String> getRequest(String url, Map<String, String> headers) {
        CompletableFuture<String> future = new CompletableFuture<>();

        downloadHelper.get(
                url,
                future::complete,
                volleyError -> future.complete(createErrorResponse(getErrorMessage(volleyError)))
        );

        return future;
    }

    /**
     * Makes an async POST request and returns the response.
     */
    private CompletableFuture<String> postRequest(String url, JSONObject body, Map<String, String> headers) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (body == null) {
            body = new JSONObject();
        }

        downloadHelper.post(
                url,
                body,
                response -> future.complete(response.toString()),
                volleyError -> future.complete(createErrorResponse(getErrorMessage(volleyError)))
        );

        return future;
    }

    /**
     * Makes an async PUT request and returns the response.
     */
    private CompletableFuture<String> putRequest(String url, JSONObject body, Map<String, String> headers) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (body == null) {
            body = new JSONObject();
        }

        downloadHelper.put(
                url,
                body,
                response -> future.complete(response.toString()),
                volleyError -> future.complete(createErrorResponse(getErrorMessage(volleyError)))
        );

        return future;
    }

    /**
     * Makes an async DELETE request and returns the response.
     */
    private CompletableFuture<String> deleteRequest(String url, Map<String, String> headers) {
        CompletableFuture<String> future = new CompletableFuture<>();

        downloadHelper.delete(
                url,
                future::complete,
                volleyError -> future.complete(createErrorResponse(getErrorMessage(volleyError)))
        );

        return future;
    }

    /**
     * Makes an async PATCH request and returns the response.
     */
    private CompletableFuture<String> patchRequest(String url, JSONObject body, Map<String, String> headers) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (body == null) {
            body = new JSONObject();
        }

        downloadHelper.patch(
                url,
                body,
                response -> future.complete(response.toString()),
                volleyError -> future.complete(createErrorResponse(getErrorMessage(volleyError)))
        );

        return future;
    }

    private String getErrorMessage(VolleyError error) {
        if (error.networkResponse != null) {
            return "HTTP " + error.networkResponse.statusCode + ": " +
                   new String(error.networkResponse.data);
        }
        return error.getMessage() != null ? error.getMessage() : "Network error";
    }

    private int getIntArg(JSONObject args, String key, int defaultValue) {
        return args.optInt(key, defaultValue);
    }

    private double getDoubleArg(JSONObject args, String key, double defaultValue) {
        return args.optDouble(key, defaultValue);
    }

    private String getStringArg(JSONObject args, String key, String defaultValue) {
        return args.optString(key, defaultValue);
    }

    private boolean getBooleanArg(JSONObject args, String key, boolean defaultValue) {
        return args.optBoolean(key, defaultValue);
    }

    private JSONArray getArrayArg(JSONObject args, String key) {
        return args.optJSONArray(key);
    }

    // Helper methods for creating responses

    private String createErrorResponse(String error) {
        try {
            JSONObject response = new JSONObject();
            response.put("error", error);
            response.put("status", "error");
            return response.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + error + "\"}";
        }
    }

    /**
     * Convert a JSON argument to a string, handling different types
     */
    private String convertArgumentToString(JSONObject arguments, String key) throws JSONException {
        Object value = arguments.get(key);

        if (value == null || value == JSONObject.NULL) {
            return "";
        }

        // Handle arrays
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                if (i > 0) sb.append(",");
                sb.append(array.get(i).toString());
            }
            return sb.toString();
        }

        return value.toString();
    }
}
