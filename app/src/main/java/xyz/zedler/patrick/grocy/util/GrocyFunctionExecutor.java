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
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    public GrocyFunctionExecutor(GrocyApi grocyApi, Application application, Client client) {
        this.grocyApi = grocyApi;
        this.downloadHelper = new DownloadHelper(application, "GrocyFunctionExecutor");
        this.client = client;
    }

    /**
     * Executes a function call from Gemini.
     * @param functionName Name of the function to execute
     * @param arguments JSON object containing function arguments
     * @return JSON string with the result
     */
    public String executeFunction(String functionName, JSONObject arguments) {
        Log.d(TAG, "Executing function: " + functionName + " with args: " + arguments);

        try {
            switch (functionName) {
                // Stock Management Functions
                case "get_stock":
                    return executeGetStock();
                case "get_stock_volatile":
                    return executeGetStockVolatile(arguments);
                case "get_product_entries":
                    return executeGetProductEntries(arguments);
                case "get_stock_by_location":
                    return executeGetStockByLocation(arguments);
                case "purchase_product":
                    return executePurchaseProduct(arguments);
                case "consume_product":
                    return executeConsumeProduct(arguments);
                case "inventory_product":
                    return executeInventoryProduct(arguments);
                case "open_product":
                    return executeOpenProduct(arguments);
                case "transfer_product":
                    return executeTransferProduct(arguments);
                case "get_price_history":
                    return executeGetPriceHistory(arguments);

                // Shopping List Functions
                case "get_shopping_list":
                    return executeGetShoppingList();
                case "add_shopping_list_item":
                    return executeAddShoppingListItem(arguments);
                case "remove_shopping_list_item":
                    return executeRemoveShoppingListItem(arguments);

                // Recipe Functions
                case "get_recipes":
                    return executeGetRecipes();
                case "get_recipe_fulfillment":
                    return executeGetRecipeFulfillment(arguments);
                case "get_recipes_fulfillment":
                    return executeGetRecipesFulfillment();
                case "add_recipe_products_to_shopping_list":
                    return executeAddRecipeProductsToShoppingList(arguments);
                case "consume_recipe":
                    return executeConsumeRecipe(arguments);
                case "add_recipe_to_meal_plan":
                    return executeAddRecipeToMealPlan(arguments);

                // Chore Functions
                case "get_chores":
                    return executeGetChores();
                case "track_chore_execution":
                    return executeTrackChoreExecution(arguments);

                // Task Functions
                case "get_tasks":
                    return executeGetTasks();
                case "complete_task":
                    return executeCompleteTask(arguments);

                // Battery Functions
                case "get_batteries":
                    return executeGetBatteries();
                case "charge_battery":
                    return executeChargeBattery(arguments);

                // Location Functions
                case "get_locations":
                    return executeGetLocations();
                case "get_shopping_locations":
                    return executeGetShoppingLocations();

                // Product Information Functions
                case "get_products":
                    return executeGetProducts();
                case "get_product_groups":
                    return executeGetProductGroups();
                case "get_quantity_units":
                    return executeGetQuantityUnits();

                // Meal Plan Functions
                case "get_meal_plan":
                    return executeGetMealPlan(arguments);

                // Equipment Functions
                case "get_equipment":
                    return executeGetEquipment();

                // User Functions
                case "get_users":
                    return executeGetUsers();

                // Utility Functions
                case "undo_action":
                    return executeUndoAction(arguments);

                // System Functions
                case "get_system_config":
                    return executeSystemConfig();

                case "get_system_info":
                    return executeSystemInfo();

                case "google_search":
                    return executeGoogleSearch(arguments).join().text();
                default:
                    return createErrorResponse("Unknown function: " + functionName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing function: " + functionName, e);
            return createErrorResponse("Error executing " + functionName + ": " + e.getMessage());
        }
    }

    private String executeSystemInfo() {
        return getRequest(grocyApi.getSystemInfo());
    }

    private String executeSystemConfig() {
        return getRequest(grocyApi.getSystemConfig());
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
     * Makes a synchronous GET request and returns the response.
     */
    private String getRequest(String url) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        downloadHelper.get(
                url,
                response -> {
                    result.set(response);
                    latch.countDown();
                },
                volleyError -> {
                    error.set(getErrorMessage(volleyError));
                    latch.countDown();
                }
        );

        try {
            latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return createErrorResponse("Request interrupted: " + e.getMessage());
        }

        if (error.get() != null) {
            return createErrorResponse(error.get());
        }

        return result.get() != null ? result.get() : createErrorResponse("Empty response");
    }

    /**
     * Makes a synchronous POST request and returns the response.
     */
    private String postRequest(String url, JSONObject body) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        downloadHelper.post(
                url,
                body,
                response -> {
                    result.set(response.toString());
                    latch.countDown();
                },
                volleyError -> {
                    error.set(getErrorMessage(volleyError));
                    latch.countDown();
                }
        );

        try {
            latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return createErrorResponse("Request interrupted: " + e.getMessage());
        }

        if (error.get() != null) {
            return createErrorResponse(error.get());
        }

        return result.get() != null ? result.get() : createErrorResponse("Empty response");
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

    // Stock Management Function Implementations

    private String executeGetStock() {
        return getRequest(grocyApi.getStock());
    }

    private String executeGetStockVolatile(JSONObject args) {
        return getRequest(grocyApi.getStockVolatile());
    }

    private String executeGetProductEntries(JSONObject args) {
        int productId = getIntArg(args, "productId", 0);
        return getRequest(grocyApi.getStockEntriesFromProduct(productId));
    }

    private String executeGetStockByLocation(JSONObject args) {
        int locationId = getIntArg(args, "locationId", 0);
        return getRequest(grocyApi.getStockLocationsFromProduct(locationId));
    }

    private String executePurchaseProduct(JSONObject args) {
        try {
            int productId = getIntArg(args, "productId", 0);
            double amount = getDoubleArg(args, "amount", 1.0);
            String bestBeforeDate = getStringArg(args, "bestBeforeDate", "");
            double price = getDoubleArg(args, "price", 0.0);
            
            JSONObject body = new JSONObject();
            body.put("amount", amount);
            if (!bestBeforeDate.isEmpty()) {
                body.put("best_before_date", bestBeforeDate);
            }
            if (price > 0) {
                body.put("price", price);
            }
            if (args.has("storeId")) {
                body.put("shopping_location_id", getIntArg(args, "storeId", 0));
            }
            if (args.has("locationId")) {
                body.put("location_id", getIntArg(args, "locationId", 0));
            }
            if (args.has("note")) {
                body.put("note", getStringArg(args, "note", ""));
            }
            
            String url = grocyApi.getUrl("/stock/products/" + productId + "/add");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error creating purchase request: " + e.getMessage());
        }
    }

    private String executeConsumeProduct(JSONObject args) {
        try {
            int productId = getIntArg(args, "productId", 0);
            double amount = getDoubleArg(args, "amount", 1.0);
            boolean spoiled = getBooleanArg(args, "spoiled", false);
            
            JSONObject body = new JSONObject();
            body.put("amount", amount);
            body.put("spoiled", spoiled);
            if (args.has("recipeId")) {
                body.put("recipe_id", getIntArg(args, "recipeId", 0));
            }
            if (args.has("locationId")) {
                body.put("location_id", getIntArg(args, "locationId", 0));
            }
            if (args.has("note")) {
                body.put("note", getStringArg(args, "note", ""));
            }
            
            String url = grocyApi.getUrl("/stock/products/" + productId + "/consume");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error creating consume request: " + e.getMessage());
        }
    }

    private String executeInventoryProduct(JSONObject args) {
        try {
            int productId = getIntArg(args, "productId", 0);
            double newAmount = getDoubleArg(args, "newAmount", 0.0);
            
            JSONObject body = new JSONObject();
            body.put("new_amount", newAmount);
            if (args.has("bestBeforeDate")) {
                body.put("best_before_date", getStringArg(args, "bestBeforeDate", ""));
            }
            if (args.has("locationId")) {
                body.put("location_id", getIntArg(args, "locationId", 0));
            }
            if (args.has("price")) {
                body.put("price", getDoubleArg(args, "price", 0.0));
            }
            if (args.has("note")) {
                body.put("note", getStringArg(args, "note", ""));
            }
            
            String url = grocyApi.getUrl("/stock/products/" + productId + "/inventory");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error creating inventory request: " + e.getMessage());
        }
    }

    private String executeOpenProduct(JSONObject args) {
        try {
            int productId = getIntArg(args, "productId", 0);
            double amount = getDoubleArg(args, "amount", 1.0);
            
            JSONObject body = new JSONObject();
            body.put("amount", amount);
            if (args.has("note")) {
                body.put("note", getStringArg(args, "note", ""));
            }
            
            String url = grocyApi.getUrl("/stock/products/" + productId + "/open");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error creating open product request: " + e.getMessage());
        }
    }

    private String executeTransferProduct(JSONObject args) {
        try {
            int productId = getIntArg(args, "productId", 0);
            double amount = getDoubleArg(args, "amount", 1.0);
            int locationIdFrom = getIntArg(args, "locationIdFrom", 0);
            int locationIdTo = getIntArg(args, "locationIdTo", 0);
            
            JSONObject body = new JSONObject();
            body.put("amount", amount);
            body.put("location_id_from", locationIdFrom);
            body.put("location_id_to", locationIdTo);
            if (args.has("note")) {
                body.put("note", getStringArg(args, "note", ""));
            }
            
            String url = grocyApi.getUrl("/stock/products/" + productId + "/transfer");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error creating transfer request: " + e.getMessage());
        }
    }

    private String executeGetPriceHistory(JSONObject args) {
        int productId = getIntArg(args, "productId", 0);
        return getRequest(grocyApi.getPriceHistory(productId));
    }

    // Shopping List Function Implementations

    private String executeGetShoppingList() {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST));
    }

    private String executeAddShoppingListItem(JSONObject args) {
        try {
            int productId = getIntArg(args, "productId", 0);
            double amount = getDoubleArg(args, "amount", 1.0);
            int shoppingListId = getIntArg(args, "shoppingListId", 1);
            
            JSONObject body = new JSONObject();
            body.put("product_id", productId);
            body.put("amount", amount);
            body.put("shopping_list_id", shoppingListId);
            if (args.has("note")) {
                body.put("note", getStringArg(args, "note", ""));
            }
            
            String url = grocyApi.getUrl("/stock/shoppinglist/add-product");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error adding shopping list item: " + e.getMessage());
        }
    }

    private String executeRemoveShoppingListItem(JSONObject args) {
        int itemId = getIntArg(args, "shoppingListItemId", 0);
        String url = grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, itemId);
        // Note: Would need DELETE request support in DownloadHelper
        return createErrorResponse("DELETE not yet supported in DownloadHelper");
    }

    // Recipe Function Implementations

    private String executeGetRecipes() {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.RECIPES));
    }

    private String executeGetRecipeFulfillment(JSONObject args) {
        int recipeId = getIntArg(args, "recipeId", 0);
        String url = grocyApi.getUrl("/recipes/" + recipeId + "/fulfillment");
        return getRequest(url);
    }

    private String executeGetRecipesFulfillment() {
        String url = grocyApi.getUrl("/recipes/fulfillment");
        return getRequest(url);
    }

    private String executeAddRecipeProductsToShoppingList(JSONObject args) {
        int recipeId = getIntArg(args, "recipeId", 0);
        String url = grocyApi.getUrl("/recipes/" + recipeId + "/add-not-fulfilled-products-to-shoppinglist");
        return postRequest(url, new JSONObject());
    }

    private String executeConsumeRecipe(JSONObject args) {
        try {
            int recipeId = getIntArg(args, "recipeId", 0);
            int servings = getIntArg(args, "servings", 1);
            
            JSONObject body = new JSONObject();
            body.put("servings", servings);
            
            String url = grocyApi.getUrl("/recipes/" + recipeId + "/consume");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error consuming recipe: " + e.getMessage());
        }
    }

    private String executeAddRecipeToMealPlan(JSONObject args) {
        try {
            int recipeId = getIntArg(args, "recipeId", 0);
            String day = getStringArg(args, "day", "");
            int servings = getIntArg(args, "servings", 1);
            
            JSONObject body = new JSONObject();
            body.put("recipe_id", recipeId);
            body.put("servings", servings);
            if (!day.isEmpty()) {
                body.put("day", day);
            }
            
            String url = grocyApi.getUrl("/objects/" + GrocyApi.ENTITY.MEAL_PLAN);
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error adding recipe to meal plan: " + e.getMessage());
        }
    }

    // Chore Function Implementations

    private String executeGetChores() {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.CHORES));
    }

    private String executeTrackChoreExecution(JSONObject args) {
        try {
            int choreId = getIntArg(args, "choreId", 0);
            
            JSONObject body = new JSONObject();
            if (args.has("executedBy")) {
                body.put("done_by", getIntArg(args, "executedBy", 0));
            }
            if (args.has("trackedTime")) {
                body.put("tracked_time", getStringArg(args, "trackedTime", ""));
            }
            
            String url = grocyApi.getUrl("/chores/" + choreId + "/execute");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error tracking chore: " + e.getMessage());
        }
    }

    // Task Function Implementations

    private String executeGetTasks() {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.TASKS));
    }

    private String executeCompleteTask(JSONObject args) {
        try {
            int taskId = getIntArg(args, "taskId", 0);
            
            JSONObject body = new JSONObject();
            if (args.has("note")) {
                body.put("note", getStringArg(args, "note", ""));
            }
            
            String url = grocyApi.getUrl("/tasks/" + taskId + "/complete");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error completing task: " + e.getMessage());
        }
    }

    // Battery Function Implementations

    private String executeGetBatteries() {
        String url = grocyApi.getUrl("/batteries");
        return getRequest(url);
    }

    private String executeChargeBattery(JSONObject args) {
        try {
            int batteryId = getIntArg(args, "batteryId", 0);
            
            JSONObject body = new JSONObject();
            if (args.has("trackedTime")) {
                body.put("tracked_time", getStringArg(args, "trackedTime", ""));
            }
            
            String url = grocyApi.getUrl("/batteries/" + batteryId + "/charge");
            return postRequest(url, body);
        } catch (Exception e) {
            return createErrorResponse("Error charging battery: " + e.getMessage());
        }
    }

    // Location Function Implementations

    private String executeGetLocations() {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS));
    }

    private String executeGetShoppingLocations() {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.STORES));
    }

    // Product Information Function Implementations

    private String executeGetProducts() {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS));
    }

    private String executeGetProductGroups() {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS));
    }

    private String executeGetQuantityUnits() {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS));
    }

    // Meal Plan Function Implementations

    private String executeGetMealPlan(JSONObject args) {
        return getRequest(grocyApi.getObjects(GrocyApi.ENTITY.MEAL_PLAN));
    }

    // Equipment Function Implementations

    private String executeGetEquipment() {
        String url = grocyApi.getUrl("/objects/equipment");
        return getRequest(url);
    }

    // User Function Implementations

    private String executeGetUsers() {
        return getRequest(grocyApi.getUsers());
    }

    // Utility Function Implementations

    private String executeUndoAction(JSONObject args) {
        try {
            String entityType = getStringArg(args, "entityType", "");
            String id = getStringArg(args, "id", "");
            
            String endpoint;
            switch (entityType.toLowerCase()) {
                case "chores":
                    endpoint = "/chores/executions/" + id + "/undo";
                    break;
                case "batteries":
                    endpoint = "/batteries/charge-cycles/" + id + "/undo";
                    break;
                case "tasks":
                    endpoint = "/tasks/" + id + "/undo";
                    break;
                default:
                    return createErrorResponse("Unknown entity type: " + entityType);
            }
            
            String url = grocyApi.getUrl(endpoint);
            return postRequest(url, new JSONObject());
        } catch (Exception e) {
            return createErrorResponse("Error undoing action: " + e.getMessage());
        }
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
}
