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

import android.content.Context;
import android.util.Log;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.Operation;
import xyz.zedler.patrick.grocy.R;

/**
 * Provides Grocy API function declarations for Google Generative AI SDK.
 * Based on https://github.com/saya6k/mcp-grocy-api
 */
public class GrocyFunctionDeclarations {

    private static OpenAPIHelper apiHelper;
    /**
     * Creates all Grocy function declarations for use with Gemini.
     * @return List of FunctionDeclaration objects
     */
    public static List<FunctionDeclaration> createFunctionDeclarations(Context context) {
        try(InputStream io = context.getResources().openRawResource(R.raw.openapi)) {
            apiHelper = new OpenAPIHelper(io);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<OpenAPIHelper.ParameterInfo> paramDetails =
                apiHelper.getOperationParameterDetails("/objects/{entity}", "GET");

        for (OpenAPIHelper.ParameterInfo info : paramDetails) {
            System.out.println("Parameter: " + info.name);
            System.out.println("  Type: " + info.type);
            System.out.println("  Description: " + info.description);
            System.out.println("  Required: " + info.required);
            System.out.println("  Location: " + info.location);
            if (!info.enumValues.isEmpty()) {
                System.out.println("  Enum values: " + info.enumValues);
            }
        }


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

        // System Functions
        functions.add(createSystemInfoFunction());
        functions.add(createSystemConfigFunction());

        // Object Entity Functions
        functions.add(createQueryProductsFunction());
        functions.add(createQueryProductBarcodesFunction());
        functions.add(createQueryStockFunction());
        functions.add(createQueryShoppingListFunction());
        functions.add(createQueryChoresFunction());
        functions.add(createQueryBatteriesFunction());
        functions.add(createQueryLocationsFunction());
        functions.add(createQueryQuantityUnitsFunction());

        return functions;
    }

    private static FunctionDeclaration createSystemConfigFunction() {
        return FunctionDeclaration.builder()
                .name("get_system_config")
                .description("Get Grocy backend settings such as currency, default locale, and enabled features.")
                .build();
    }

    private static FunctionDeclaration createSystemInfoFunction() {
        return FunctionDeclaration.builder()
                .name("get_system_info")
                .description("Get backend system information from the Grocy instance such as version and release date.")
                .build();
    }

    /**
     * Creates a Tool containing all Grocy function declarations.
     * @return Tool object for registration with GenerativeModel
     */
    public static Tool createGrocyTool(Context c) {
        return Tool.builder()
                .functionDeclarations(createFunctionDeclarations(c))
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

        //TODO: this is not working yet because api is different

        properties.put("entityType", Schema.builder()
                .type("STRING")
                .format("enum")
                        .enum_(enumValues)
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

    private static FunctionDeclaration.Builder createQueryObjectFunctionBuilder(List<String> filterFields) {
        Map<String, Schema> properties = new HashMap<>();

        List<String> operators = new ArrayList<>();
        operators.add("=");
        operators.add("!=");
        operators.add("<");
        operators.add(">");
        operators.add("<=");
        operators.add(">=");
        operators.add("~");  // LIKE operator

        Map<String, Schema> filterObjectProperties = new HashMap<>();
        filterObjectProperties.put("field", Schema.builder()
                .type("STRING")
                .format("enum")
                .enum_(filterFields)
                .description("Field to filter by")
                .build());
        filterObjectProperties.put("operator", Schema.builder()
                .type("STRING")
                .format("enum")
                .enum_(operators)
                .description("Comparison operator")
                .build());
        filterObjectProperties.put("value", Schema.builder()
                .type("OBJECT")
                .description("Value to filter by")
                .build());

        properties.put("filter", Schema.builder()
                .type("ARRAY")
                .items(Schema.builder()
                        .type("OBJECT")
                        .properties(filterObjectProperties)
                        .required("field", "operator", "value")
                        .build())
                .description("Fields to filter by")
                .build());

        List<String> orderByFields = new ArrayList<>(filterFields);

        properties.put("orderBy", Schema.builder()
                .type("STRING")
                .format("enum")
                .enum_(orderByFields)
                .description("Field to order results by")
                .build());

        properties.put("sortOrder", Schema.builder()
                .type("STRING")
                .format("enum")
                .enum_("asc", "desc")
                .description("Sort order (default: asc)")
                .build());

        properties.put("limit", Schema.builder()
                .type("INTEGER")
                .description("Maximum number of results to return")
                .build());

        properties.put("offset", Schema.builder()
                .type("INTEGER")
                .description("Number of results to skip")
                .build());

        return FunctionDeclaration.builder()
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(properties)
                        .build());
    }
    // Products Query Function
    private static FunctionDeclaration createQueryProductsFunction() {
        List<String> filterFields = new ArrayList<>();
        filterFields.add("id");
        filterFields.add("name");
        filterFields.add("description");
        filterFields.add("location_id");
        filterFields.add("product_group_id");
        filterFields.add("qu_id_purchase");
        filterFields.add("qu_id_stock");
        filterFields.add("min_stock_amount");
        filterFields.add("default_best_before_days");
        filterFields.add("active");
        filterFields.add("parent_product_id");
        filterFields.add("calories");
        filterFields.add("cumulate_min_stock_amount_of_sub_products");
        filterFields.add("due_type");
        filterFields.add("quick_consume_amount");
        filterFields.add("enable_tare_weight_handling");
        filterFields.add("tare_weight");
        filterFields.add("not_check_stock_fulfillment_for_recipes");
        filterFields.add("picture_file_name");
        filterFields.add("default_print_stock_label");
        filterFields.add("allow_label_per_unit");

        return createQueryObjectFunctionBuilder(filterFields)
                .name("query_products")
                .description("Query products with optional filtering, ordering, and pagination.")
                .build();
    }

    // Product Barcodes Query Function
    private static FunctionDeclaration createQueryProductBarcodesFunction() {
        List<String> filterFields = new ArrayList<>();
        filterFields.add("id");
        filterFields.add("product_id");
        filterFields.add("barcode");
        filterFields.add("amount");
        filterFields.add("qu_id");
        filterFields.add("shopping_location_id");
        filterFields.add("last_price");
        filterFields.add("note");

        return createQueryObjectFunctionBuilder(filterFields)
                .name("query_product_barcodes")
                .description("Query product barcodes with optional filtering, ordering, and pagination.")
                .build();
    }

    // Stock Query Function
    private static FunctionDeclaration createQueryStockFunction() {
        List<String> filterFields = new ArrayList<>();
        filterFields.add("product_id");
        filterFields.add("amount");
        filterFields.add("best_before_date");
        filterFields.add("purchased_date");
        filterFields.add("stock_id");
        filterFields.add("price");
        filterFields.add("open");
        filterFields.add("opened_date");
        filterFields.add("location_id");
        filterFields.add("shopping_location_id");
        filterFields.add("note");

        return createQueryObjectFunctionBuilder(filterFields)
                .name("query_stock")
                .description("Query stock entries with optional filtering, ordering, and pagination.")
                .build();
    }

    // Shopping List Query Function
    private static FunctionDeclaration createQueryShoppingListFunction() {
        List<String> filterFields = new ArrayList<>();
        filterFields.add("id");
        filterFields.add("product_id");
        filterFields.add("note");
        filterFields.add("amount");
        filterFields.add("shopping_list_id");
        filterFields.add("done");
        filterFields.add("qu_id");
        filterFields.add("shopping_location_id");

        return createQueryObjectFunctionBuilder(filterFields)
                .name("query_shopping_list")
                .description("Query shopping list items with optional filtering, ordering, and pagination.")
                .build();
    }

    // Chores Query Function
    private static FunctionDeclaration createQueryChoresFunction() {
        List<String> filterFields = new ArrayList<>();
        filterFields.add("id");
        filterFields.add("name");
        filterFields.add("description");
        filterFields.add("period_type");
        filterFields.add("period_config");
        filterFields.add("period_days");
        filterFields.add("active");
        filterFields.add("track_date_only");
        filterFields.add("rollover");
        filterFields.add("assignment_type");
        filterFields.add("assignment_config");
        filterFields.add("next_execution_assigned_to_user_id");
        filterFields.add("consume_product_on_execution");
        filterFields.add("product_id");
        filterFields.add("product_amount");

        return createQueryObjectFunctionBuilder(filterFields)
                .name("query_chores")
                .description("Query chores with optional filtering, ordering, and pagination.")
                .build();
    }

    // Batteries Query Function
    private static FunctionDeclaration createQueryBatteriesFunction() {
        List<String> filterFields = new ArrayList<>();
        filterFields.add("id");
        filterFields.add("name");
        filterFields.add("description");
        filterFields.add("used_in");
        filterFields.add("charge_interval_days");
        filterFields.add("active");

        return createQueryObjectFunctionBuilder(filterFields)
                .name("query_batteries")
                .description("Query batteries with optional filtering, ordering, and pagination.")
                .build();
    }

    // Locations Query Function
    private static FunctionDeclaration createQueryLocationsFunction() {
        List<String> filterFields = new ArrayList<>();
        filterFields.add("id");
        filterFields.add("name");
        filterFields.add("description");
        filterFields.add("is_freezer");
        filterFields.add("active");

        return createQueryObjectFunctionBuilder(filterFields)
                .name("query_locations")
                .description("Query storage locations with optional filtering, ordering, and pagination.")
                .build();
    }

    // Quantity Units Query Function
    private static FunctionDeclaration createQueryQuantityUnitsFunction() {
        List<String> filterFields = new ArrayList<>();
        filterFields.add("id");
        filterFields.add("name");
        filterFields.add("name_plural");
        filterFields.add("description");
        filterFields.add("active");

        return createQueryObjectFunctionBuilder(filterFields)
                .name("query_quantity_units")
                .description("Query quantity units with optional filtering, ordering, and pagination.")
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

    public static String getSystemInstructions() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Make sure to always include the corresponding unit for quantities, e.g., '2 pieces of bread' or '1.5 kg of apples'. Functions like get_stock only return an 'amount' without the unit, but you can retrieve the actual unit by looking at the 'product'.'qu_id_stock' and then querying the existing units to match the id using 'get_quantity_units'.\n");
        stringBuilder.append("When displaying dates, use the format that is appropriate for the user's locale, e.g., 'DD.MM.YYYY' for German or 'MM/DD/YYYY' for US English. The same holds true for floating point values, e.g. if the function call returns a value of 1.5 then this would be displayed as 1,5 if the locale is German.\n");
        stringBuilder.append("Dates in the JSON requests and responses for function calls are always in the format YYYY-MM-DD or YYYY-MM-DD HH:MM:SS.\n\n");

        stringBuilder.append("# Grocy API Data Relationships Guide\n" +
                "\n" +
                "This document describes how different API responses relate to each other, enabling intelligent navigation and data enrichment.\n" +
                "\n" +
                "## Entity Relationships Overview\n" +
                "\n" +
                "### Core Product & Stock Relationships\n" +
                "\n" +
                "#### Product → Quantity Unit\n" +
                "- **Relationship**: Products reference quantity units via `qu_id_purchase` and `qu_id_stock`\n" +
                "- **How to resolve**: \n" +
                "  1. Get product from `get_products` or from stock data\n" +
                "  2. Extract `qu_id_purchase` or `qu_id_stock`\n" +
                "  3. Query `get_quantity_units` and find the unit where `id` matches\n" +
                "  4. Use the `name` field for the unit name\n" +
                "\n" +
                "```json\n" +
                "// Product has:\n" +
                "{\n" +
                "  \"qu_id_purchase\": 3,\n" +
                "  \"qu_id_stock\": 2\n" +
                "}\n" +
                "// Resolve by finding in quantity_units where id = 3 or id = 2\n" +
                "```\n" +
                "\n" +
                "#### Product → Location\n" +
                "- **Relationship**: Products have a default location via `location_id`\n" +
                "- **How to resolve**:\n" +
                "  1. Get product data\n" +
                "  2. Extract `location_id`\n" +
                "  3. Query `get_locations` and find location where `id` matches\n" +
                "  4. Use the `name` field for location name\n" +
                "\n" +
                "#### Product → Product Group\n" +
                "- **Relationship**: Products belong to groups via `product_group_id`\n" +
                "- **How to resolve**:\n" +
                "  1. Get product data\n" +
                "  2. Extract `product_group_id`\n" +
                "  3. Query `get_product_groups` and find group where `id` matches\n" +
                "  4. Use the `name` field for group name\n" +
                "\n" +
                "#### Stock Entry → Product\n" +
                "- **Relationship**: Stock entries reference products via `product_id`\n" +
                "- **How to resolve**:\n" +
                "  1. Get stock data from `get_stock` or `get_product_entries`\n" +
                "  2. Extract `product_id`\n" +
                "  3. Query `get_products` and find product where `id` matches\n" +
                "  4. Use product details for enrichment\n" +
                "\n" +
                "#### Stock Entry → Location\n" +
                "- **Relationship**: Stock entries are stored at locations via `location_id`\n" +
                "- **How to resolve**:\n" +
                "  1. Get stock entry from `get_stock_by_location` or `get_product_entries`\n" +
                "  2. Extract `location_id`\n" +
                "  3. Query `get_locations` and find location where `id` matches\n" +
                "\n" +
                "### Shopping & Recipes\n" +
                "\n" +
                "#### Shopping List Item → Product\n" +
                "- **Relationship**: Shopping list items reference products via `product_id`\n" +
                "- **How to resolve**:\n" +
                "  1. Get shopping list from `get_shopping_list`\n" +
                "  2. Extract `product_id` from each item\n" +
                "  3. Query `get_products` to get product details\n" +
                "\n" +
                "#### Shopping List Item → Shopping Location\n" +
                "- **Relationship**: Shopping list items reference where to buy via `shopping_location_id`\n" +
                "- **How to resolve**:\n" +
                "  1. Get shopping list item\n" +
                "  2. Extract `shopping_location_id`\n" +
                "  3. Query `get_shopping_locations` and find location where `id` matches\n" +
                "\n" +
                "#### Recipe → Products (via recipe_pos)\n" +
                "- **Relationship**: Recipes contain products as ingredients\n" +
                "- **How to resolve**:\n" +
                "  1. Get recipe from `get_recipes`\n" +
                "  2. Recipe contains nested ingredient information\n" +
                "  3. Each ingredient references a `product_id`\n" +
                "  4. Query `get_products` to enrich ingredient data\n" +
                "\n" +
                "#### Recipe Fulfillment → Stock\n" +
                "- **Relationship**: Recipe fulfillment shows if enough stock exists\n" +
                "- **How to resolve**:\n" +
                "  1. Use `get_recipe_fulfillment` with recipe ID\n" +
                "  2. Response shows which products are missing/sufficient\n" +
                "  3. Cross-reference with `get_stock` for detailed stock info\n" +
                "\n" +
                "#### Meal Plan → Recipe\n" +
                "- **Relationship**: Meal plan entries reference recipes via `recipe_id`\n" +
                "- **How to resolve**:\n" +
                "  1. Get meal plan from `get_meal_plan`\n" +
                "  2. Extract `recipe_id` from each entry\n" +
                "  3. Query `get_recipes` to get recipe details\n" +
                "\n" +
                "### Chores & Batteries\n" +
                "\n" +
                "#### Chore → User\n" +
                "- **Relationship**: Chores can be assigned to users via `assignment_config` or execution tracking\n" +
                "- **How to resolve**:\n" +
                "  1. Get chore from `get_chores`\n" +
                "  2. When tracking execution, user context may be involved\n" +
                "  3. Query `get_users` for user details\n" +
                "\n" +
                "#### Battery → User (via tracking)\n" +
                "- **Relationship**: Battery charge tracking may reference user who performed the action\n" +
                "- **How to resolve**:\n" +
                "  1. Get battery from `get_batteries`\n" +
                "  2. Historical data may include user information\n" +
                "  3. Query `get_users` for user details\n" +
                "\n" +
                "### User & Permissions\n" +
                "\n" +
                "#### Any Entity → User (Created/Modified)\n" +
                "- **Relationship**: Most entities track `row_created_timestamp` and may track user\n" +
                "- **How to resolve**:\n" +
                "  1. Many entities have audit fields\n" +
                "  2. Query `get_users` to resolve user information\n" +
                "\n" +
                "## Common Patterns for the AI\n" +
                "\n" +
                "### Pattern 1: Display Product Name with Unit\n" +
                "**Question**: \"What's in stock?\"\n" +
                "\n" +
                "**Resolution Steps**:\n" +
                "1. Call `get_stock`\n" +
                "2. For each item, extract `product_id`\n" +
                "3. Call `get_products`, find matching product\n" +
                "4. Extract `qu_id_stock` from product\n" +
                "5. Call `get_quantity_units`, find matching unit\n" +
                "6. Display: \"{product.name}: {amount} {unit.name}\"\n" +
                "\n" +
                "### Pattern 2: Show Stock by Location Name\n" +
                "**Question**: \"What's in the fridge?\"\n" +
                "\n" +
                "**Resolution Steps**:\n" +
                "1. Call `get_locations`, find location where name contains \"fridge\"\n" +
                "2. Extract location `id`\n" +
                "3. Call `get_stock_by_location` with that `location_id`\n" +
                "4. Enrich with product names using Pattern 1\n" +
                "\n" +
                "### Pattern 3: Check Recipe Availability\n" +
                "**Question**: \"Can I make pizza?\"\n" +
                "\n" +
                "**Resolution Steps**:\n" +
                "1. Call `get_recipes`, find recipe where name contains \"pizza\"\n" +
                "2. Extract recipe `id`\n" +
                "3. Call `get_recipe_fulfillment` with recipe ID\n" +
                "4. Response shows ingredient availability\n" +
                "5. Optionally call `get_stock` to show current amounts\n" +
                "\n" +
                "### Pattern 4: Add Missing Recipe Ingredients to Shopping List\n" +
                "**Question**: \"Add missing ingredients for lasagna to shopping list\"\n" +
                "\n" +
                "**Resolution Steps**:\n" +
                "1. Call `get_recipes`, find recipe for lasagna\n" +
                "2. Extract recipe `id`\n" +
                "3. Call `get_recipe_fulfillment` with recipe ID\n" +
                "4. Identify missing ingredients\n" +
                "5. Call `add_recipe_products_to_shopping_list` with recipe ID\n" +
                "   - This automatically adds only missing items\n" +
                "\n" +
                "### Pattern 5: Product Search with Full Context\n" +
                "**Question**: \"Tell me about milk\"\n" +
                "\n" +
                "**Resolution Steps**:\n" +
                "1. Call `get_products`, filter for name containing \"milk\"\n" +
                "2. Extract `location_id`, `product_group_id`, `qu_id_stock`\n" +
                "3. Call `get_locations` to resolve location name\n" +
                "4. Call `get_product_groups` to resolve group name\n" +
                "5. Call `get_quantity_units` to resolve unit name\n" +
                "6. Call `get_stock` filtered by product to show current stock\n" +
                "7. Provide comprehensive answer with all context\n" +
                "\n" +
                "### Pattern 6: Shopping List with Locations\n" +
                "**Question**: \"What do I need to buy at the grocery store?\"\n" +
                "\n" +
                "**Resolution Steps**:\n" +
                "1. Call `get_shopping_locations`, find location matching \"grocery\"\n" +
                "2. Extract location `id`\n" +
                "3. Call `get_shopping_list`\n" +
                "4. Filter items where `shopping_location_id` matches\n" +
                "5. Enrich with product names from `get_products`\n" +
                "\n" +
                "## ID Field Reference Table\n" +
                "\n" +
                "| Entity | Primary Key | Common Foreign Keys |\n" +
                "|--------|-------------|---------------------|\n" +
                "| Products | `id` | `location_id`, `product_group_id`, `qu_id_purchase`, `qu_id_stock`, `parent_product_id` |\n" +
                "| Stock Entries | `stock_id` | `product_id`, `location_id` |\n" +
                "| Shopping List Items | `id` | `product_id`, `shopping_location_id` |\n" +
                "| Recipes | `id` | None direct (uses recipe_pos table) |\n" +
                "| Recipe Positions | `id` | `recipe_id`, `product_id` |\n" +
                "| Chores | `id` | `assignment_config` (may reference users) |\n" +
                "| Batteries | `id` | None direct |\n" +
                "| Tasks | `id` | None direct |\n" +
                "| Locations | `id` | None |\n" +
                "| Shopping Locations | `id` | None |\n" +
                "| Product Groups | `id` | None |\n" +
                "| Quantity Units | `id` | None |\n" +
                "| Users | `id` | None |\n" +
                "| Meal Plan | `id` | `recipe_id` |\n" +
                "| Equipment | `id` | None |\n" +
                "\n" +
                "## Special Considerations\n" +
                "\n" +
                "### Volatile Stock Data\n" +
                "- Use `get_stock_volatile` for products that expire soon\n" +
                "- References same product IDs as regular stock\n" +
                "- Useful for \"what expires soon?\" queries\n" +
                "\n" +
                "### Price History\n" +
                "- Use `get_price_history` with `product_id` to see pricing trends\n" +
                "- Relates to purchase transactions\n" +
                "\n" +
                "### Barcode Support\n" +
                "- Products can have multiple barcodes via `product_barcodes` entity\n" +
                "- Use this for scanning operations\n" +
                "\n" +
                "### Transaction History & Undo\n" +
                "- Most actions (consume, purchase, etc.) can be undone\n" +
                "- `undo_action` requires the transaction ID returned from operations\n" +
                "- Store transaction IDs if user might want to undo\n" +
                "\n" +
                "## Example Conversation Flows\n" +
                "\n" +
                "### Flow 1: \"What milk do I have?\"\n" +
                "```\n" +
                "1. get_products → filter name contains \"milk\" → extract product IDs\n" +
                "2. get_stock → filter by product IDs → get amounts and location IDs\n" +
                "3. get_locations → resolve location names\n" +
                "4. get_quantity_units → resolve unit names\n" +
                "Response: \"You have 2 liters of whole milk in the fridge and 500ml of almond milk in the pantry\"\n" +
                "```\n" +
                "\n" +
                "### Flow 2: \"I want to make dinner with what I have\"\n" +
                "```\n" +
                "1. get_recipes_fulfillment → see which recipes can be made\n" +
                "2. get_recipes → get details of fulfilled recipes\n" +
                "3. Present options to user\n" +
                "Response: \"You can make: Spaghetti Carbonara, Caesar Salad, or Chicken Stir Fry\"\n" +
                "```\n" +
                "\n" +
                "### Flow 3: \"I bought 2 dozen eggs\"\n" +
                "```\n" +
                "1. get_products → find \"eggs\" → extract product ID and qu_id_purchase\n" +
                "2. get_quantity_units → resolve unit (probably \"piece\" or \"dozen\")\n" +
                "3. purchase_product → with product_id, amount=24 (or 2 if unit is dozen), price\n" +
                "4. get_stock → confirm new stock level\n" +
                "Response: \"Added 24 eggs to stock. You now have 30 eggs total\"\n" +
                "```");

        return stringBuilder.toString();
    }
}
