package xyz.zedler.patrick.grocy.util;

import android.util.Log;
import android.util.Pair;

import com.google.genai.types.FunctionDeclaration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OpenAPIHelper {

    private final OpenAPI openAPI;

    private final Map<String, OperationInfo> geminiFunctionMap = new HashMap<>();

    /**
     * Load OpenAPI spec from a file in assets folder or resources
     * @param inputStream InputStream from assets or resources
     */
    public OpenAPIHelper(InputStream inputStream) {
        try {
            // Read the input stream to string
            String spec = convertStreamToString(inputStream);

            // Parse the OpenAPI spec
            ParseOptions options = new ParseOptions();
            options.setResolve(true); // Resolve $ref references
            options.setFlatten(true); // Flatten nested schemas
            options.setResolveFully(true); // Fully resolve all references

            this.openAPI = new OpenAPIV3Parser().readContents(spec, null, options).getOpenAPI();
        } catch (Exception e) {
            Log.e("OpenAPIHelper", "Failed to load OpenAPI specification", e);
            throw new RuntimeException("Failed to load OpenAPI specification", e);
        }
    }

    // ========== SCHEMA METHODS ==========

    /**
     * Get a schema by name from components/schemas
     */
    public Schema<?> getSchema(String schemaName) {
        if (openAPI == null || openAPI.getComponents() == null) {
            return null;
        }

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        return schemas != null ? schemas.get(schemaName) : null;
    }

    /**
     * Get property type from a schema
     */
    public String getPropertyType(Schema<?> schema, String propertyName) {
        Schema<?> resolved = resolveSchema(schema);
        if (resolved == null || resolved.getProperties() == null) {
            return null;
        }

        Schema<?> property = (Schema<?>) resolved.getProperties().get(propertyName);
        if (property != null) {
            // If the property itself has a $ref, resolve it
            if (property.get$ref() != null) {
                Schema<?> resolvedProp = resolveSchemaReference(property.get$ref());
                return resolvedProp != null ? resolvedProp.getType() : null;
            }
            if(property.getTypes() != null && !property.getTypes().isEmpty())
                return property.getTypes().iterator().next();
            else
                return property.getType();
        }
        return null;
    }

    /**
     * Get property description from a schema
     */
    public String getPropertyDescription(Schema<?> schema, String propertyName) {
        Schema<?> resolved = resolveSchema(schema);
        if (resolved == null || resolved.getProperties() == null) {
            return null;
        }

        Schema<?> property = (Schema<?>) resolved.getProperties().get(propertyName);
        return property != null ? property.getDescription() : null;
    }

    /**
     * Get required properties from a schema
     */
    public List<String> getRequiredProperties(String schemaName) {
        Schema<?> schema = getSchema(schemaName);
        if (schema == null) {
            return new ArrayList<>();
        }

        List<String> required = schema.getRequired();
        return required != null ? required : new ArrayList<>();
    }

    /**
     * Get enum values for a property (if it's an enum)
     */
    public List<String> getPropertyEnumValues(Schema<?> schema, String propertyName) {
        Schema<?> resolved = resolveSchema(schema);
        if (resolved == null || resolved.getProperties() == null) {
            return new ArrayList<>();
        }

        Schema<?> property = (Schema<?>) resolved.getProperties().get(propertyName);
        if (property == null || property.getEnum() == null) {
            return new ArrayList<>();
        }

        List<String> enumValues = new ArrayList<>();
        for (Object enumValue : property.getEnum()) {
            enumValues.add(String.valueOf(enumValue));
        }
        return enumValues;
    }

    /**
     * Check if a property is required
     */
    public boolean isPropertyRequired(String schemaName, String propertyName) {
        List<String> required = getRequiredProperties(schemaName);
        return required.contains(propertyName);
    }

    /**
     * Get all available schema names
     */
    public List<String> getAllSchemaNames() {
        if (openAPI == null || openAPI.getComponents() == null ||
                openAPI.getComponents().getSchemas() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(openAPI.getComponents().getSchemas().keySet());
    }

    // ========== PARAMETER RESOLUTION METHODS ==========

    /**
     * Resolve a parameter reference from components/parameters
     * @param refName The reference name (e.g., "query", "order", "limit")
     */
    public Parameter resolveParameterReference(String refName) {
        if (openAPI == null || openAPI.getComponents() == null ||
                openAPI.getComponents().getParameters() == null) {
            return null;
        }

        return openAPI.getComponents().getParameters().get(refName);
    }

    /**
     * Get all parameter references defined in components
     */
    public List<String> getAllParameterReferenceNames() {
        if (openAPI == null || openAPI.getComponents() == null ||
                openAPI.getComponents().getParameters() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(openAPI.getComponents().getParameters().keySet());
    }

    /**
     * Get the type of a parameter (resolves references if needed)
     */
    public String getParameterType(Parameter parameter) {
        if (parameter == null) {
            return null;
        }

        // Check if parameter has a schema
        if (parameter.getSchema() != null) {
            if(parameter.getSchema().getTypes() != null && !parameter.getSchema().getTypes().isEmpty())
                return parameter.getSchema().getTypes().iterator().next().toString();
            else
                return parameter.getSchema().getType();
        }

        return null;
    }

    /**
     * Get parameter type by reference name
     */
    public String getParameterTypeByRef(String refName) {
        Parameter param = resolveParameterReference(refName);
        return getParameterType(param);
    }

    /**
     * Get parameter description
     */
    public String getParameterDescription(Parameter parameter) {
        return parameter != null ? parameter.getDescription() : null;
    }

    /**
     * Get parameter enum values if it has any
     */
    public List<String> getParameterEnumValues(Parameter parameter) {
        if (parameter == null || parameter.getSchema() == null ||
                parameter.getSchema().getEnum() == null) {
            return new ArrayList<>();
        }

        List<String> enumValues = new ArrayList<>();
        for (Object enumValue : parameter.getSchema().getEnum()) {
            enumValues.add(String.valueOf(enumValue));
        }
        return enumValues;
    }

    /**
     * Check if parameter is required
     */
    public boolean isParameterRequired(Parameter parameter) {
        return parameter != null && parameter.getRequired() != null && parameter.getRequired();
    }

    /**
     * Get parameter location (query, path, header, cookie)
     */
    public String getParameterLocation(Parameter parameter) {
        return parameter != null ? parameter.getIn() : null;
    }

    // ========== PATH METHODS ==========

    /**
     * Get all paths defined in the OpenAPI spec
     */
    public List<String> getAllPaths() {
        if (openAPI == null || openAPI.getPaths() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(openAPI.getPaths().keySet());
    }

    /**
     * Get a PathItem by path string
     */
    public PathItem getPath(String path) {
        if (openAPI == null || openAPI.getPaths() == null) {
            return null;
        }

        return openAPI.getPaths().get(path);
    }

    /**
     * Get an operation from a path
     * @param path The path (e.g., "/objects/{entity}")
     * @param method HTTP method (GET, POST, PUT, DELETE, PATCH)
     */
    public Operation getOperation(String path, String method) {
        PathItem pathItem = getPath(path);
        if (pathItem == null) {
            return null;
        }

        switch (method.toUpperCase()) {
            case "GET":
                return pathItem.getGet();
            case "POST":
                return pathItem.getPost();
            case "PUT":
                return pathItem.getPut();
            case "DELETE":
                return pathItem.getDelete();
            case "PATCH":
                return pathItem.getPatch();
            case "HEAD":
                return pathItem.getHead();
            case "OPTIONS":
                return pathItem.getOptions();
            case "TRACE":
                return pathItem.getTrace();
            default:
                return null;
        }
    }

    /**
     * Get operation summary
     */
    public String getOperationSummary(String path, String method) {
        Operation operation = getOperation(path, method);
        return operation != null ? operation.getSummary() : null;
    }

    /**
     * Get operation description
     */
    public String getOperationDescription(String path, String method) {
        Operation operation = getOperation(path, method);
        return operation != null ? operation.getDescription() : null;
    }

    /**
     * Get operation tags
     */
    public List<String> getOperationTags(String path, String method) {
        Operation operation = getOperation(path, method);
        if (operation == null || operation.getTags() == null) {
            return new ArrayList<>();
        }
        return operation.getTags();
    }

    /**
     * Get parameters for an operation (already resolved by the parser)
     */
    public List<Parameter> getOperationParameters(String path, String method) {
        Operation operation = getOperation(path, method);
        if (operation == null || operation.getParameters() == null) {
            return new ArrayList<>();
        }
        return operation.getParameters();
    }

    /**
     * Get parameter names for an operation
     */
    public List<String> getOperationParameterNames(String path, String method) {
        List<Parameter> parameters = getOperationParameters(path, method);
        List<String> names = new ArrayList<>();
        for (Parameter param : parameters) {
            names.add(param.getName());
        }
        return names;
    }

    /**
     * Get detailed parameter information for an operation
     */
    public List<ParameterInfo> getOperationParameterDetails(String path, String method) {
        List<Parameter> parameters = getOperationParameters(path, method);
        List<ParameterInfo> details = new ArrayList<>();

        for (Parameter param : parameters) {
            ParameterInfo info = new ParameterInfo();
            info.name = param.getName();
            info.type = getParameterType(param);
            info.description = getParameterDescription(param);
            info.required = isParameterRequired(param);
            info.location = getParameterLocation(param);
            info.enumValues = getParameterEnumValues(param);

            details.add(info);
        }

        return details;
    }

    /**
     * Get required parameters for an operation
     */
    public List<String> getRequiredParameters(String path, String method) {
        List<Parameter> parameters = getOperationParameters(path, method);
        List<String> required = new ArrayList<>();
        for (Parameter param : parameters) {
            if (param.getRequired() != null && param.getRequired()) {
                required.add(param.getName());
            }
        }
        return required;
    }

    /**
     * Get a specific parameter by name
     */
    public Parameter getParameter(String path, String method, String paramName) {
        List<Parameter> parameters = getOperationParameters(path, method);
        for (Parameter param : parameters) {
            if (param.getName().equals(paramName)) {
                return param;
            }
        }
        return null;
    }

    /**
     * Get paths by tag
     */
    public List<String> getPathsByTag(String tag) {
        List<String> paths = new ArrayList<>();

        if (openAPI == null || openAPI.getPaths() == null) {
            return paths;
        }

        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            PathItem pathItem = entry.getValue();

            // Check all operations in this path
            List<Operation> operations = new ArrayList<>();
            if (pathItem.getGet() != null) operations.add(pathItem.getGet());
            if (pathItem.getPost() != null) operations.add(pathItem.getPost());
            if (pathItem.getPut() != null) operations.add(pathItem.getPut());
            if (pathItem.getDelete() != null) operations.add(pathItem.getDelete());
            if (pathItem.getPatch() != null) operations.add(pathItem.getPatch());

            for (Operation op : operations) {
                if (op.getTags() != null && op.getTags().contains(tag)) {
                    paths.add(entry.getKey());
                    break; // Only add the path once
                }
            }
        }

        return paths;
    }

    /**
     * Get all tags defined in the spec
     */
    public List<String> getAllTags() {
        List<String> tags = new ArrayList<>();

        if (openAPI == null || openAPI.getTags() == null) {
            return tags;
        }

        for (io.swagger.v3.oas.models.tags.Tag tag : openAPI.getTags()) {
            tags.add(tag.getName());
        }

        return tags;
    }

    /**
     * Resolve a schema reference
     * @param ref The reference string (e.g., "#/components/schemas/users_body")
     * @return The resolved schema, or null if not found
     */
    public Schema<?> resolveSchemaReference(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }

        // Extract schema name from reference
        // "#/components/schemas/users_body" -> "users_body"
        String schemaName = ref.substring("#/components/schemas/".length());

        return getSchema(schemaName);
    }

    /**
     * Resolve a schema, following $ref if present
     * @param schema The schema that might contain a $ref
     * @return The resolved schema
     */
    public Schema<?> resolveSchema(Schema<?> schema) {
        if (schema == null) {
            return null;
        }

        // If the schema has a $ref, resolve it
        if (schema.get$ref() != null) {
            return resolveSchemaReference(schema.get$ref());
        }

        return schema;
    }

    /**
     * Get request body schema for an operation (with reference resolution)
     */
    public Schema<?> getRequestBodySchema(String path, String method) {
        Operation operation = getOperation(path, method);
        if (operation == null || operation.getRequestBody() == null ||
                operation.getRequestBody().getContent() == null) {
            return null;
        }

        // Usually JSON request
        io.swagger.v3.oas.models.media.MediaType mediaType =
                operation.getRequestBody().getContent().get("application/json");

        if (mediaType == null || mediaType.getSchema() == null) {
            return null;
        }

        // Resolve the schema if it has a $ref
        return resolveSchema(mediaType.getSchema());
    }

    /**
     * Get response schema for an operation (with reference resolution)
     * @param path The path
     * @param method HTTP method
     * @param statusCode Status code (e.g., "200", "400")
     */
    public Schema<?> getResponseSchema(String path, String method, String statusCode) {
        Operation operation = getOperation(path, method);
        if (operation == null || operation.getResponses() == null) {
            return null;
        }

        ApiResponse response = operation.getResponses().get(statusCode);
        if (response == null || response.getContent() == null) {
            return null;
        }

        // Usually JSON response
        io.swagger.v3.oas.models.media.MediaType mediaType =
                response.getContent().get("application/json");

        if (mediaType == null || mediaType.getSchema() == null) {
            return null;
        }

        // Resolve the schema if it has a $ref
        return resolveSchema(mediaType.getSchema());
    }

    /**
     * Get property names from a schema, resolving references if needed
     */
    public List<String> getSchemaPropertyNames(Schema<?> schema) {
        Schema<?> resolved = resolveSchema(schema);
        if (resolved == null || resolved.getProperties() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(resolved.getProperties().keySet());
    }

    /**
     * Get property names from a schema by name (overload)
     */
    public List<String> getSchemaPropertyNames(String schemaName) {
        Schema<?> schema = getSchema(schemaName);
        return getSchemaPropertyNames(schema);
    }

    /**
     * Get all schema references used in the specification
     */
    public List<String> getAllSchemaReferences() {
        List<String> refs = new ArrayList<>();

        if (openAPI == null || openAPI.getPaths() == null) {
            return refs;
        }

        // Scan all operations for schema references
        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            PathItem pathItem = pathEntry.getValue();

            List<Operation> operations = new ArrayList<>();
            if (pathItem.getGet() != null) operations.add(pathItem.getGet());
            if (pathItem.getPost() != null) operations.add(pathItem.getPost());
            if (pathItem.getPut() != null) operations.add(pathItem.getPut());
            if (pathItem.getDelete() != null) operations.add(pathItem.getDelete());
            if (pathItem.getPatch() != null) operations.add(pathItem.getPatch());

            for (Operation op : operations) {
                // Check request body
                if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
                    io.swagger.v3.oas.models.media.MediaType mediaType =
                            op.getRequestBody().getContent().get("application/json");
                    if (mediaType != null && mediaType.getSchema() != null &&
                            mediaType.getSchema().get$ref() != null) {
                        refs.add(mediaType.getSchema().get$ref());
                    }
                }

                // Check responses
                if (op.getResponses() != null) {
                    for (ApiResponse response : op.getResponses().values()) {
                        if (response.getContent() != null) {
                            io.swagger.v3.oas.models.media.MediaType mediaType =
                                    response.getContent().get("application/json");
                            if (mediaType != null && mediaType.getSchema() != null &&
                                    mediaType.getSchema().get$ref() != null) {
                                refs.add(mediaType.getSchema().get$ref());
                            }
                        }
                    }
                }
            }
        }

        return refs;
    }

    // ========== UTILITY METHODS ==========

    /**
     * Get API info
     */
    public String getApiTitle() {
        return openAPI != null && openAPI.getInfo() != null ?
                openAPI.getInfo().getTitle() : null;
    }

    /**
     * Get API version
     */
    public String getApiVersion() {
        return openAPI != null && openAPI.getInfo() != null ?
                openAPI.getInfo().getVersion() : null;
    }

    /**
     * Get API description
     */
    public String getApiDescription() {
        return openAPI != null && openAPI.getInfo() != null ?
                openAPI.getInfo().getDescription() : null;
    }

    /**
     * Get server URLs
     */
    public List<String> getServerUrls() {
        List<String> urls = new ArrayList<>();

        if (openAPI == null || openAPI.getServers() == null) {
            return urls;
        }

        for (io.swagger.v3.oas.models.servers.Server server : openAPI.getServers()) {
            urls.add(server.getUrl());
        }

        return urls;
    }

    private String convertStreamToString(InputStream is) throws Exception {
        java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public String makeFunctionDeclarationName(String path, String type) {
        return type + path.replaceAll("[{}]", "").replace("/", "_").replace("-", "_");
    }

    public FunctionDeclaration createFunctionDeclaration(String path, String type, Operation op) {
        String functionName = makeFunctionDeclarationName(path, type);
        String description = getOperationDescription(path, type);
        String summary = getOperationSummary(path, type);

        Map<String, com.google.genai.types.Schema> paramsMap = new HashMap<>();
        List<String> requiredParams = new ArrayList<>();

        // Process URL/query parameters
        if (op.getParameters() != null) {
            for (Parameter param : op.getParameters()) {
                processParameter(param, paramsMap, requiredParams);
            }
        }

        // Process request body
        if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
            Schema<?> requestBodySchema = getRequestBodySchema(path, type);
            if (requestBodySchema != null) {
                processRequestBody(requestBodySchema, paramsMap, requiredParams, op.getRequestBody().getRequired());
            }
        }

        com.google.genai.types.Schema params = com.google.genai.types.Schema.builder()
                .properties(paramsMap)
                .required(requiredParams)
                .type("object")
                .build();

        geminiFunctionMap.put(functionName, new OperationInfo(type, path, op));

        return FunctionDeclaration.builder()
                .name(functionName)
                .description((path + ' ' + buildDescription(summary, description)).trim())
                .parameters(params)
                .build();
    }

    public OperationInfo getGeminiFunctionOperation(String functionName) {
        return geminiFunctionMap.getOrDefault(functionName, null);
    }

    /**
     * Process a single parameter and add it to the params map
     */
    private void processParameter(Parameter param,
                                  Map<String, com.google.genai.types.Schema> paramsMap,
                                  List<String> requiredParams) {
        String parameterName = param.getName();
        String parameterType = getParameterType(param);
        String parameterDescription = getParameterDescription(param);

        com.google.genai.types.Schema.Builder paramSchema = com.google.genai.types.Schema.builder()
                .type(Objects.requireNonNullElse(parameterType, "UNKNOWN"))
                .description(Objects.requireNonNullElse(parameterDescription, ""));

        // Add example if available
        if (param.getExample() != null) {
            paramSchema.example(param.getExample());
        }

        // Mark as required if needed
        if (isParameterRequired(param)) {
            requiredParams.add(parameterName);
        }

        // Handle enum values
        List<String> enumValues = getParameterEnumValues(param);
        if (!enumValues.isEmpty()) {
            paramSchema.format("enum").enum_(enumValues);
        }

        // Handle schema-specific properties
        if (param.getSchema() != null) {
            Schema<?> schema = param.getSchema();
            if (schema.getTitle() != null) {
                paramSchema.title(schema.getTitle());
            }
            if (schema.getDefault() != null) {
                paramSchema.default_(schema.getDefault().toString());
            }
            if(schema.getItems() != null) {
                paramSchema.items(convertSchema(schema.getItems()));
            }
        }

        paramsMap.put(parameterName, paramSchema.build());
    }

    /**
     * Process request body and add it to the params map
     */
    private void processRequestBody(Schema<?> requestBodySchema,
                                    Map<String, com.google.genai.types.Schema> paramsMap,
                                    List<String> requiredParams, boolean isRequired) {
        if (isRequired) {
            requiredParams.add("body");
        }

        Object example = Objects.requireNonNullElse(requestBodySchema.getExample(), "");

        // Handle oneOf/anyOf schemas
        if (requestBodySchema.getOneOf() != null) {
            List<com.google.genai.types.Schema> possibleSchemas = new ArrayList<>();
            for (Schema<?> schema : requestBodySchema.getOneOf()) {
                possibleSchemas.add(convertSchema(schema));
            }

            paramsMap.put("body", com.google.genai.types.Schema.builder()
                    .type("object")
                    .description("Request body (one of the possible schemas)")
                            .example(example)
                    .anyOf(possibleSchemas)
                    .build());
            return;
        }

        if (requestBodySchema.getAnyOf() != null) {
            List<com.google.genai.types.Schema> possibleSchemas = new ArrayList<>();
            for (Schema<?> schema : requestBodySchema.getAnyOf()) {
                possibleSchemas.add(convertSchema(schema));
            }
            paramsMap.put("body", com.google.genai.types.Schema.builder()
                    .type("object")
                    .description("Request body (any of the possible schemas)")
                            .example(example)
                    .anyOf(possibleSchemas)
                    .build());
            return;
        }

        // Handle allOf schemas
        if (requestBodySchema.getAllOf() != null) {
            // Merge all schemas in allOf
            Schema<?> mergedSchema = mergeAllOfSchemas(requestBodySchema.getAllOf());
            com.google.genai.types.Schema bodySchema = convertSchema(mergedSchema);

            paramsMap.put("body", bodySchema.toBuilder().example(example).build());
            return;
        }

        // Handle regular object schema
        com.google.genai.types.Schema bodySchema = convertSchema(requestBodySchema);
        paramsMap.put("body", bodySchema.toBuilder().example(example).build());
    }

    /**
     * Recursively convert an OpenAPI schema to a Gemini schema
     */
    private com.google.genai.types.Schema convertSchema(Schema<?> openApiSchema) {
        if (openApiSchema == null) {
            return com.google.genai.types.Schema.builder()
                    .type("UNKNOWN")
                    .build();
        }

        // Resolve references
        Schema<?> resolved = resolveSchema(openApiSchema);
        if (resolved == null) {
            resolved = openApiSchema;
        }

        com.google.genai.types.Schema.Builder builder = com.google.genai.types.Schema.builder();

        // Set basic properties
        String type = getSchemaType(resolved);
        builder.type(type);

        if (resolved.getDescription() != null) {
            builder.description(resolved.getDescription());
        }

        if (resolved.getTitle() != null) {
            builder.title(resolved.getTitle());
        }

        if (resolved.getDefault() != null) {
            builder.default_(resolved.getDefault().toString());
        }

        // Handle enum
        if (resolved.getEnum() != null && !resolved.getEnum().isEmpty()) {
            List<String> enumValues = new ArrayList<>();
            for (Object enumValue : resolved.getEnum()) {
                enumValues.add(String.valueOf(enumValue));
            }
            builder.format("enum").enum_(enumValues);
        }

        // Handle array type
        if ("array".equalsIgnoreCase(type) && resolved.getItems() != null) {
            com.google.genai.types.Schema itemSchema = convertSchema(resolved.getItems());
            builder.items(itemSchema);
        }

        // Handle object type with properties
        if ("object".equalsIgnoreCase(type) && resolved.getProperties() != null) {
            Map<String, com.google.genai.types.Schema> properties = new HashMap<>();

            for (Map.Entry<String, Schema> entry : resolved.getProperties().entrySet()) {
                String propName = entry.getKey();
                Schema<?> propSchema = entry.getValue();

                // Recursive call to handle nested objects
                properties.put(propName, convertSchema(propSchema));
            }

            builder.properties(properties);

            // Set required fields for this object
            if (resolved.getRequired() != null && !resolved.getRequired().isEmpty()) {
                builder.required(resolved.getRequired());
            }
        }

        // Handle oneOf
        if (resolved.getOneOf() != null) {
            List<com.google.genai.types.Schema> oneOfSchemas = new ArrayList<>();
            for (Schema<?> schema : resolved.getOneOf()) {
                oneOfSchemas.add(convertSchema(schema));
            }
            builder.anyOf(oneOfSchemas); // Gemini uses anyOf for oneOf
        }

        // Handle anyOf
        if (resolved.getAnyOf() != null) {
            List<com.google.genai.types.Schema> anyOfSchemas = new ArrayList<>();
            for (Schema<?> schema : resolved.getAnyOf()) {
                anyOfSchemas.add(convertSchema(schema));
            }
            builder.anyOf(anyOfSchemas);
        }

        // Handle format
        if (resolved.getFormat() != null) {
            builder.format(resolved.getFormat());
        }

        return builder.build();
    }

    /**
     * Get the type of a schema, handling both old and new OpenAPI versions
     */
    private String getSchemaType(Schema<?> schema) {
        if (schema == null) {
            return "UNKNOWN";
        }

        // Try new API first (OpenAPI 3.1+)
        if (schema.getTypes() != null && !schema.getTypes().isEmpty()) {
            return schema.getTypes().iterator().next();
        }

        // Fall back to old API (OpenAPI 3.0)
        if (schema.getType() != null) {
            return schema.getType();
        }

        return "object"; // Default to object if type is not specified
    }

    /**
     * Merge multiple schemas from allOf into a single schema
     */
    private Schema<?> mergeAllOfSchemas(List<Schema> schemas) {
        Schema<?> merged = new Schema<>();
        Map<String, Schema> allProperties = new HashMap<>();
        List<String> allRequired = new ArrayList<>();

        for (Schema<?> schema : schemas) {
            Schema<?> resolved = resolveSchema(schema);
            if (resolved == null) {
                resolved = schema;
            }

            // Merge properties
            if (resolved.getProperties() != null) {
                allProperties.putAll(resolved.getProperties());
            }

            // Merge required fields
            if (resolved.getRequired() != null) {
                allRequired.addAll(resolved.getRequired());
            }

            // Use description from first schema that has one
            if (merged.getDescription() == null && resolved.getDescription() != null) {
                merged.setDescription(resolved.getDescription());
            }

            // Use title from first schema that has one
            if (merged.getTitle() == null && resolved.getTitle() != null) {
                merged.setTitle(resolved.getTitle());
            }
        }

        merged.setProperties(allProperties);
        merged.setRequired(allRequired);
        merged.setType("object");

        return merged;
    }

    /**
     * Build a clean description from summary and description
     */
    private String buildDescription(String summary, String description) {
        String summaryPart = Objects.requireNonNullElse(summary, "").trim();
        String descriptionPart = Objects.requireNonNullElse(description, "").trim();

        if (summaryPart.isEmpty()) {
            return descriptionPart;
        }
        if (descriptionPart.isEmpty()) {
            return summaryPart;
        }

        // Combine them with proper spacing
        return summaryPart + ". " + descriptionPart;
    }

    // ========== INNER CLASSES ==========

    /**
     * Helper class to hold parameter information
     */
    public static class ParameterInfo {
        public String name;
        public String type;
        public String description;
        public boolean required;
        public String location; // query, path, header, cookie
        public List<String> enumValues;

        @Override
        public String toString() {
            return "ParameterInfo{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", description='" + description + '\'' +
                    ", required=" + required +
                    ", location='" + location + '\'' +
                    ", enumValues=" + enumValues +
                    '}';
        }
    }

    public static class OperationInfo {
        public final String httpMethod;
        public final String path;
        public final Operation operation;

        public OperationInfo(String httpMethod, String path, Operation operation) {
            this.httpMethod = httpMethod;
            this.path = path;
            this.operation = operation;
        }
    }
}
