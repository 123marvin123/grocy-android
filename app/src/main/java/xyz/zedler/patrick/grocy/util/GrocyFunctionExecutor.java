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
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.Tool;

import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private final Tool grocyTool;

    public GrocyFunctionExecutor(GrocyApi grocyApi, Application application, Client client, Tool grocyTool) {
        this.grocyApi = grocyApi;
        this.downloadHelper = new DownloadHelper(application, "GrocyFunctionExecutor");
        this.client = client;
        this.grocyTool = grocyTool;
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
                return Objects.requireNonNull(executeGoogleSearch(arguments))
                        .thenApply(GenerateContentResponse::text);
            }

            FunctionDeclaration fun = grocyTool.functionDeclarations()
                    .orElse(new ArrayList<>())
                    .stream()
                    .filter(f ->
                            f.name()
                                    .orElse("")
                                    .equals(functionName))
                    .findFirst()
                    .orElseThrow();

            return executeRest(functionName, fun, arguments);
        } catch (Exception e) {
            Log.e(TAG, "Error executing function: " + functionName, e);
            return CompletableFuture.completedFuture(
                    createErrorResponse("Error executing " + functionName + ": " + e.getMessage())
            );
        }
    }

    private CompletableFuture<String> executeRest(String functionName,
                                                  FunctionDeclaration fun,
                                                    JSONObject arguments) {
        Optional<String> optDesc = fun.description();
        if(optDesc.isEmpty()) {
            return CompletableFuture.completedFuture(
                createErrorResponse("Function " + functionName + " has no REST path")
            );
        }

        if (arguments == null) {
            return CompletableFuture.completedFuture(
                    createErrorResponse("Function " + functionName + " requires non-null arguments object")
            );
        }

        String path = optDesc.get().split(" ")[0];

        try {
            Map<String, String> queryParams = new HashMap<>();
            JSONObject requestBody = null;

                for (Iterator<String> it = arguments.keys(); it.hasNext(); ) {
                    String param = it.next();
                    String paramValue = convertArgumentToString(arguments, param);

                    if (path.contains("{" + param + "}")) {
                        // Path parameter
                        path = path.replace("{" + param + "}", paramValue);
                    } else {
                        queryParams.put(param, paramValue);
                    }
                }

            // Handle request body if present
            if (arguments.has("body")) {
                requestBody = arguments.getJSONObject("body");
            }

            switch (functionName.split("_")[0]) {
                case "get":
                    return getRequest(grocyApi.getUrlWithParams(path, queryParams));

                case "post":
                    return postRequest(grocyApi.getUrlWithParams(path, queryParams), requestBody);

                case "put":
                    return putRequest(grocyApi.getUrlWithParams(path, queryParams), requestBody);

                case "delete":
                    return deleteRequest(grocyApi.getUrlWithParams(path, queryParams));

                case "patch":
                    return patchRequest(grocyApi.getUrlWithParams(path, queryParams), requestBody);

                default:
                    return CompletableFuture.completedFuture(
                            createErrorResponse("Unsupported HTTP method")
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
    }

    // Helper methods

    /**
     * Makes a async GET request and returns the response.
     */
    private CompletableFuture<String> getRequest(String url) {
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
    private CompletableFuture<String> postRequest(String url, JSONObject body) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (body == null) {
            body = new JSONObject();
        }

        downloadHelper.postPlain(
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
    private CompletableFuture<String> putRequest(String url, JSONObject body) {
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
    private CompletableFuture<String> deleteRequest(String url) {
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
    private CompletableFuture<String> patchRequest(String url, JSONObject body) {
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
                sb.append(key).append("=[");
                sb.append(array.get(i).toString());
                sb.append("]");
                if (i < array.length() - 1) {
                    sb.append("&");
                }
            }
            return sb.toString();
        }

        return value.toString();
    }
}
