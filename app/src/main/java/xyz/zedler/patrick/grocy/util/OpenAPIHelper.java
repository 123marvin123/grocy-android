package xyz.zedler.patrick.grocy.util;

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
import java.util.List;
import java.util.Map;

public class OpenAPIHelper {

    private OpenAPI openAPI;

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
            e.printStackTrace();
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
     * Get all property names from a schema
     */
    public List<String> getSchemaPropertyNames(String schemaName) {
        Schema<?> schema = getSchema(schemaName);
        if (schema == null || schema.getProperties() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(schema.getProperties().keySet());
    }

    /**
     * Get property type from a schema
     */
    public String getPropertyType(String schemaName, String propertyName) {
        Schema<?> schema = getSchema(schemaName);
        if (schema == null || schema.getProperties() == null) {
            return null;
        }

        Schema<?> property = (Schema<?>) schema.getProperties().get(propertyName);
        return property != null ? property.getType() : null;
    }

    /**
     * Get property description from a schema
     */
    public String getPropertyDescription(String schemaName, String propertyName) {
        Schema<?> schema = getSchema(schemaName);
        if (schema == null || schema.getProperties() == null) {
            return null;
        }

        Schema<?> property = (Schema<?>) schema.getProperties().get(propertyName);
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
    public List<String> getPropertyEnumValues(String schemaName, String propertyName) {
        Schema<?> schema = getSchema(schemaName);
        if (schema == null || schema.getProperties() == null) {
            return new ArrayList<>();
        }

        Schema<?> property = (Schema<?>) schema.getProperties().get(propertyName);
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
     * Get response schema for an operation
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

        return mediaType.getSchema();
    }

    /**
     * Get request body schema for an operation
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

        return mediaType.getSchema();
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
}