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

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides Grocy API function declarations for Google Generative AI SDK.
 * Based on https://github.com/saya6k/mcp-grocy-api
 */
public class GrocyFunctionDeclarations {

    /**
     * Creates all Grocy function declarations for use with Gemini.
     * @return List of FunctionDeclaration objects
     */
    public static List<FunctionDeclaration> createFunctionDeclarations() {
        List<FunctionDeclaration> functions = new ArrayList<>();

        // Stock Management Functions
        functions.add(createGetStockFunction());
        functions.add(createGetStockVolatileFunction());
        functions.add(createGetProductEntriesFunction());
        functions.add(createGetStockByLocationFunction());
        functions.add(createPurchaseProductFunction());
        functions.add(createConsumeProductFunction());
        functions.add(createInventoryProductFunction());
        functions.add(createOpenProductFunction());
        functions.add(createTransferProductFunction());
        functions.add(createGetPriceHistoryFunction());

        // Shopping List Functions
        functions.add(createGetShoppingListFunction());
        functions.add(createAddShoppingListItemFunction());
        functions.add(createRemoveShoppingListItemFunction());

        // Recipe Functions
        functions.add(createGetRecipesFunction());
        functions.add(createGetRecipeFulfillmentFunction());
        functions.add(createGetRecipesFulfillmentFunction());
        functions.add(createAddRecipeProductsToShoppingListFunction());
        functions.add(createConsumeRecipeFunction());
        functions.add(createAddRecipeToMealPlanFunction());

        // Chore Functions
        functions.add(createGetChoresFunction());
        functions.add(createTrackChoreExecutionFunction());

        // Task Functions
        functions.add(createGetTasksFunction());
        functions.add(createCompleteTaskFunction());

        // Battery Functions
        functions.add(createGetBatteriesFunction());
        functions.add(createChargeBatteryFunction());

        // Location Functions
        functions.add(createGetLocationsFunction());
        functions.add(createGetShoppingLocationsFunction());

        // Product Information Functions
        functions.add(createGetProductsFunction());
        functions.add(createGetProductGroupsFunction());
        functions.add(createGetQuantityUnitsFunction());

        // Meal Plan Functions
        functions.add(createGetMealPlanFunction());

        // Equipment Functions
        functions.add(createGetEquipmentFunction());

        // User Functions
        functions.add(createGetUsersFunction());

        // Utility Functions
        functions.add(createUndoActionFunction());

        return functions;
    }

    /**
     * Creates a Tool containing all Grocy function declarations.
     * @return Tool object for registration with GenerativeModel
     */
    public static Tool createGrocyTool() {
        return Tool.builder()
                .functionDeclarations(createFunctionDeclarations())
                .build();
    }

    // Stock Management Functions

    private static FunctionDeclaration createGetStockFunction() {
        return FunctionDeclaration.builder()
                .name("get_stock")
                .description("Get current stock from your Grocy instance.")
                .build();
    }

    private static FunctionDeclaration createGetStockVolatileFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("includeDetails", Schema.builder()
                .type("BOOLEAN")
                .description("Whether to include additional details about each stock item")
                .build());

        return FunctionDeclaration.builder()
                .name("get_stock_volatile")
                .description("Get volatile stock information (due products, overdue products, expired products, missing products).")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .build())
                .build();
    }

    private static FunctionDeclaration createGetProductEntriesFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("productId", Schema.builder()
                .type("INTEGER")
                .description("ID of the product to get stock entries for")
                .build());

        List<String> required = new ArrayList<>();
        required.add("productId");

        return FunctionDeclaration.builder()
                .name("get_product_entries")
                .description("Get all stock entries for a specific product in your Grocy instance.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createGetStockByLocationFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("locationId", Schema.builder()
                .type("INTEGER")
                .description("ID of the location to get stock for")
                .build());

        List<String> required = new ArrayList<>();
        required.add("locationId");

        return FunctionDeclaration.builder()
                .name("get_stock_by_location")
                .description("Get all stock from a specific location in your Grocy instance.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createPurchaseProductFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("productId", Schema.builder()
                .type("INTEGER")
                .description("ID of the product to purchase")
                .build());
        properties.put("amount", Schema.builder()
                .type("NUMBER")
                .description("Amount to purchase (default: 1)")
                .build());
        properties.put("bestBeforeDate", Schema.builder()
                .type("STRING")
                .description("Best before date in YYYY-MM-DD format (default: one year from now)")
                .build());
        properties.put("price", Schema.builder()
                .type("NUMBER")
                .description("Price per unit (optional)")
                .build());
        properties.put("storeId", Schema.builder()
                .type("INTEGER")
                .description("ID of the store where purchased (optional)")
                .build());
        properties.put("locationId", Schema.builder()
                .type("INTEGER")
                .description("ID of the storage location (optional)")
                .build());
        properties.put("note", Schema.builder()
                .type("STRING")
                .description("Optional note")
                .build());

        List<String> required = new ArrayList<>();
        required.add("productId");

        return FunctionDeclaration.builder()
                .name("purchase_product")
                .description("Track purchase of a product in your Grocy instance.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createConsumeProductFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("productId", Schema.builder()
                .type("INTEGER")
                .description("ID of the product to consume")
                .build());
        properties.put("amount", Schema.builder()
                .type("NUMBER")
                .description("Amount to consume (default: 1)")
                .build());
        properties.put("spoiled", Schema.builder()
                .type("BOOLEAN")
                .description("Whether the product is spoiled (default: false)")
                .build());
        properties.put("recipeId", Schema.builder()
                .type("INTEGER")
                .description("ID of the recipe if consuming for a recipe (optional)")
                .build());
        properties.put("locationId", Schema.builder()
                .type("INTEGER")
                .description("ID of the location to consume from (optional)")
                .build());
        properties.put("note", Schema.builder()
                .type("STRING")
                .description("Optional note")
                .build());

        List<String> required = new ArrayList<>();
        required.add("productId");

        return FunctionDeclaration.builder()
                .name("consume_product")
                .description("Track consumption of a product in your Grocy instance.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createInventoryProductFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("productId", Schema.builder()
                .type("INTEGER")
                .description("ID of the product to inventory")
                .build());
        properties.put("newAmount", Schema.builder()
                .type("NUMBER")
                .description("The new total amount in stock")
                .build());
        properties.put("bestBeforeDate", Schema.builder()
                .type("STRING")
                .description("Best before date in YYYY-MM-DD format (optional)")
                .build());
        properties.put("locationId", Schema.builder()
                .type("INTEGER")
                .description("ID of the storage location (optional)")
                .build());
        properties.put("price", Schema.builder()
                .type("NUMBER")
                .description("Price per unit (optional)")
                .build());
        properties.put("note", Schema.builder()
                .type("STRING")
                .description("Optional note")
                .build());

        List<String> required = new ArrayList<>();
        required.add("productId");
        required.add("newAmount");

        return FunctionDeclaration.builder()
                .name("inventory_product")
                .description("Track a product inventory (set current stock amount).")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createOpenProductFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("productId", Schema.builder()
                .type("INTEGER")
                .description("ID of the product to mark as opened")
                .build());
        properties.put("amount", Schema.builder()
                .type("NUMBER")
                .description("Amount to mark as opened (default: 1)")
                .build());
        properties.put("note", Schema.builder()
                .type("STRING")
                .description("Optional note")
                .build());

        return FunctionDeclaration.builder()
                .name("open_product")
                .description("Mark a product as opened in your Grocy instance.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .build())
                .build();
    }

    private static FunctionDeclaration createTransferProductFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("productId", Schema.builder()
                .type("INTEGER")
                .description("ID of the product to transfer")
                .build());
        properties.put("amount", Schema.builder()
                .type("NUMBER")
                .description("Amount to transfer")
                .build());
        properties.put("locationIdFrom", Schema.builder()
                .type("INTEGER")
                .description("ID of the source location")
                .build());
        properties.put("locationIdTo", Schema.builder()
                .type("INTEGER")
                .description("ID of the destination location")
                .build());
        properties.put("note", Schema.builder()
                .type("STRING")
                .description("Optional note")
                .build());

        List<String> required = new ArrayList<>();
        required.add("productId");
        required.add("amount");
        required.add("locationIdFrom");
        required.add("locationIdTo");

        return FunctionDeclaration.builder()
                .name("transfer_product")
                .description("Transfer a product from one location to another in your Grocy instance.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createGetPriceHistoryFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("productId", Schema.builder()
                .type("INTEGER")
                .description("ID of the product to get price history for")
                .build());

        List<String> required = new ArrayList<>();
        required.add("productId");

        return FunctionDeclaration.builder()
                .name("get_price_history")
                .description("Get the price history of a product from your Grocy instance.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    // Shopping List Functions

    private static FunctionDeclaration createGetShoppingListFunction() {
        return FunctionDeclaration.builder()
                .name("get_shopping_list")
                .description("Get your current shopping list items.")
                .build();
    }

    private static FunctionDeclaration createAddShoppingListItemFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("productId", Schema.builder()
                .type("INTEGER")
                .description("ID of the product to add")
                .build());
        properties.put("amount", Schema.builder()
                .type("NUMBER")
                .description("Amount to add (default: 1)")
                .build());
        properties.put("shoppingListId", Schema.builder()
                .type("INTEGER")
                .description("ID of the shopping list to add to (default: 1)")
                .build());
        properties.put("note", Schema.builder()
                .type("STRING")
                .description("Optional note for the shopping list item")
                .build());

        List<String> required = new ArrayList<>();
        required.add("productId");

        return FunctionDeclaration.builder()
                .name("add_shopping_list_item")
                .description("Add an item to your shopping list.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createRemoveShoppingListItemFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("shoppingListItemId", Schema.builder()
                .type("INTEGER")
                .description("ID of the shopping list item to remove")
                .build());

        List<String> required = new ArrayList<>();
        required.add("shoppingListItemId");

        return FunctionDeclaration.builder()
                .name("remove_shopping_list_item")
                .description("Remove an item from your shopping list.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    // Recipe Functions

    private static FunctionDeclaration createGetRecipesFunction() {
        return FunctionDeclaration.builder()
                .name("get_recipes")
                .description("Get all recipes from your Grocy instance.")
                .build();
    }

    private static FunctionDeclaration createGetRecipeFulfillmentFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("recipeId", Schema.builder()
                .type("INTEGER")
                .description("ID of the recipe to check fulfillment for")
                .build());
        properties.put("servings", Schema.builder()
                .type("INTEGER")
                .description("Number of servings (default: 1)")
                .build());

        List<String> required = new ArrayList<>();
        required.add("recipeId");

        return FunctionDeclaration.builder()
                .name("get_recipe_fulfillment")
                .description("Get stock fulfillment information for a recipe.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createGetRecipesFulfillmentFunction() {
        return FunctionDeclaration.builder()
                .name("get_recipes_fulfillment")
                .description("Get fulfillment information for all recipes.")
                .build();
    }

    private static FunctionDeclaration createAddRecipeProductsToShoppingListFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("recipeId", Schema.builder()
                .type("INTEGER")
                .description("ID of the recipe")
                .build());

        List<String> required = new ArrayList<>();
        required.add("recipeId");

        return FunctionDeclaration.builder()
                .name("add_recipe_products_to_shopping_list")
                .description("Add not fulfilled products of a recipe to the shopping list.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createConsumeRecipeFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("recipeId", Schema.builder()
                .type("INTEGER")
                .description("ID of the recipe to consume")
                .build());
        properties.put("servings", Schema.builder()
                .type("INTEGER")
                .description("Number of servings to consume (default: 1)")
                .build());

        List<String> required = new ArrayList<>();
        required.add("recipeId");

        return FunctionDeclaration.builder()
                .name("consume_recipe")
                .description("Consume all ingredients needed for a recipe in your Grocy instance.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private static FunctionDeclaration createAddRecipeToMealPlanFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("recipeId", Schema.builder()
                .type("INTEGER")
                .description("ID of the recipe to add")
                .build());
        properties.put("day", Schema.builder()
                .type("STRING")
                .description("Day to add the recipe to in YYYY-MM-DD format (default: today)")
                .build());
        properties.put("servings", Schema.builder()
                .type("INTEGER")
                .description("Number of servings (default: 1)")
                .build());

        List<String> required = new ArrayList<>();
        required.add("recipeId");

        return FunctionDeclaration.builder()
                .name("add_recipe_to_meal_plan")
                .description("Add a recipe to the meal plan.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    // Chore Functions

    private static FunctionDeclaration createGetChoresFunction() {
        return FunctionDeclaration.builder()
                .name("get_chores")
                .description("Get all chores from your Grocy instance.")
                .build();
    }

    private static FunctionDeclaration createTrackChoreExecutionFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("choreId", Schema.builder()
                .type("INTEGER")
                .description("ID of the chore to track")
                .build());
        properties.put("executedBy", Schema.builder()
                .type("INTEGER")
                .description("ID of the user who executed the chore (optional)")
                .build());
        properties.put("trackedTime", Schema.builder()
                .type("STRING")
                .description("Time of execution in YYYY-MM-DD HH:MM:SS format (default: now)")
                .build());

        List<String> required = new ArrayList<>();
        required.add("choreId");

        return FunctionDeclaration.builder()
                .name("track_chore_execution")
                .description("Track an execution of a chore.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    // Task Functions

    private static FunctionDeclaration createGetTasksFunction() {
        return FunctionDeclaration.builder()
                .name("get_tasks")
                .description("Get all tasks from your Grocy instance.")
                .build();
    }

    private static FunctionDeclaration createCompleteTaskFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("taskId", Schema.builder()
                .type("INTEGER")
                .description("ID of the task to complete")
                .build());
        properties.put("note", Schema.builder()
                .type("STRING")
                .description("Optional note")
                .build());

        List<String> required = new ArrayList<>();
        required.add("taskId");

        return FunctionDeclaration.builder()
                .name("complete_task")
                .description("Mark a task as completed.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    // Battery Functions

    private static FunctionDeclaration createGetBatteriesFunction() {
        return FunctionDeclaration.builder()
                .name("get_batteries")
                .description("Get all batteries from your Grocy instance.")
                .build();
    }

    private static FunctionDeclaration createChargeBatteryFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("batteryId", Schema.builder()
                .type("INTEGER")
                .description("ID of the battery to charge")
                .build());
        properties.put("trackedTime", Schema.builder()
                .type("STRING")
                .description("Time of charge in YYYY-MM-DD HH:MM:SS format (default: now)")
                .build());

        List<String> required = new ArrayList<>();
        required.add("batteryId");

        return FunctionDeclaration.builder()
                .name("charge_battery")
                .description("Track a charge cycle of a battery.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    // Location Functions

    private static FunctionDeclaration createGetLocationsFunction() {
        return FunctionDeclaration.builder()
                .name("get_locations")
                .description("Get all storage locations from your Grocy instance.")
                .build();
    }

    private static FunctionDeclaration createGetShoppingLocationsFunction() {
        return FunctionDeclaration.builder()
                .name("get_shopping_locations")
                .description("Get all shopping locations (stores) from your Grocy instance.")
                .build();
    }

    // Product Information Functions

    private static FunctionDeclaration createGetProductsFunction() {
        return FunctionDeclaration.builder()
                .name("get_products")
                .description("Get all products from your Grocy instance.")
                .build();
    }

    private static FunctionDeclaration createGetProductGroupsFunction() {
        return FunctionDeclaration.builder()
                .name("get_product_groups")
                .description("Get all product groups from your Grocy instance.")
                .build();
    }

    private static FunctionDeclaration createGetQuantityUnitsFunction() {
        return FunctionDeclaration.builder()
                .name("get_quantity_units")
                .description("Get all quantity units from your Grocy instance.")
                .build();
    }

    // Meal Plan Functions

    private static FunctionDeclaration createGetMealPlanFunction() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("startDate", Schema.builder()
                .type("STRING")
                .description("Optional start date in YYYY-MM-DD format. Defaults to today.")
                .build());
        properties.put("days", Schema.builder()
                .type("INTEGER")
                .description("Optional number of days to retrieve. Defaults to 7.")
                .build());

        return FunctionDeclaration.builder()
                .name("get_meal_plan")
                .description("Get your meal plan data from Grocy instance.")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .build())
                .build();
    }

    // Equipment Functions

    private static FunctionDeclaration createGetEquipmentFunction() {
        return FunctionDeclaration.builder()
                .name("get_equipment")
                .description("Get all equipment from your Grocy instance.")
                .build();
    }

    // User Functions

    private static FunctionDeclaration createGetUsersFunction() {
        return FunctionDeclaration.builder()
                .name("get_users")
                .description("Get all users from your Grocy instance.")
                .build();
    }

    // Utility Functions

    private static FunctionDeclaration createUndoActionFunction() {
        Map<String, Schema> properties = new HashMap<>();
        
        List<String> enumValues = new ArrayList<>();
        enumValues.add("chores");
        enumValues.add("batteries");
        enumValues.add("tasks");
        
        properties.put("entityType", Schema.builder()
                .type("STRING")
                .description("Type of entity (chores, batteries, tasks)")
                .build());
        properties.put("id", Schema.builder()
                .type("STRING")
                .description("ID of the execution, charge cycle, or task")
                .build());

        List<String> required = new ArrayList<>();
        required.add("entityType");
        required.add("id");

        return FunctionDeclaration.builder()
                .name("undo_action")
                .description("Undo an action for different entity types (chores, batteries, tasks).")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    public static Tool createProxyGoogleSearchTool() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("query", Schema.builder()
                .type("STRING")
                .description("The search query")
                .build());

        List<String> required = new ArrayList<>();
        required.add("query");

        return Tool.builder()
                .functionDeclarations(
                        FunctionDeclaration.builder()
                                .name("google_search")
                                .description("Invokes an online Google Search with a query")
                                .parameters(Schema.builder().type("OBJECT").properties(properties).required(required).build()).build()
                )
                .build();

    }
}
