package org.motechproject.mds.docs.swagger.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a parameter passed to a REST endpoint when executing.
 * Based on the {@code in} value, this can be either a parameter passed in the query, path, body, etc.
 * Inputs for parameters will be generated by the Swagger UI in the block for the appropriate
 * {@link org.motechproject.mds.docs.swagger.model.PathEntry}.
 */
public class Parameter implements Serializable {

    private static final long serialVersionUID = 2770559337188359837L;

    private String name;
    private ParameterType in;
    private String description;
    private boolean required;
    private String type;
    private String format;
    private Property items;
    private Map<String, String> schema;
    @SerializedName("enum")
    private List<String> enumValues;

    /**
     * The name of the parameter. If this is a form or query parameter for example, this name will be used
     * when sending the request. It's ignored by the body or path parameters by example for obvious reasons,
     * however this name will be always displayed to the user on the UI.
     * @return the name of the parameter
     */
    public String getName() {
        return name;
    }

    /**
     * The name of the parameter. If this is a form or query parameter for example, this name will be used
     * when sending the request. It's ignored by the body or path parameters by example for obvious reasons,
     * however this name will be always displayed to the user on the UI.
     * @param name the name of the parameter
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return how this parameter will be passed
     * @see ParameterType
     */
    public ParameterType getIn() {
        return in;
    }

    /**
     * @param in how this parameter will be passed
     * @see ParameterType
     */
    public void setIn(ParameterType in) {
        this.in = in;
    }

    /**
     * @return a human readable description of the parameter
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description a human readable description of the parameter
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return true if this parameter is required on the UI, false otherwise
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @param required whether this parameter will be required on the UI
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * @return the type of this parameter
     * @see <a href="https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#data-types for more info">Swagger data types</a>
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type of this parameter
     * @see <a href="https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#data-types for more info">Swagger data types</a>
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the property for the items of this parameter, only used if this is a collection parameter
     */
    public Property getItems() {
        return items;
    }

    /**
     * @param items the property for the items of this parameter, only used if this is a collection parameter
     */
    public void setItems(Property items) {
        this.items = items;
    }

    /**
     * @return the format of this parameter, depends on the type
     * @see <a href="https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#data-types for more info">Swagger data types</a>
     */
    public String getFormat() {
        return format;
    }

    /**
     * @param format  the format of this parameter, depends on the type
     * @see <a href="https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#data-types for more info">Swagger data types</a>
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @return the schema element of this parameter, used primarily for referencing definitions.
     * @see org.motechproject.mds.docs.swagger.model.Definition
     */
    public Map<String, String> getSchema() {
        return schema;
    }

    /**
     * @param schema the schema element of this parameter, used primarily for referencing definitions.
     * @see org.motechproject.mds.docs.swagger.model.Definition
     */
    public void setSchema(Map<String, String> schema) {
        this.schema = schema;
    }

    /**
     * @return list of allowed values for this parameter
     */
    public List<String> getEnumValues() {
        return enumValues;
    }

    /**
     * @param enumValues list of allowed values for this parameter
     */
    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    /**
     * Adds a key-value pair to the schema element of this parameter.
     * @param key the key in the schema
     * @param value the value in the schema
     */
    public void addSchema(String key, String value) {
        if (schema == null) {
            schema = new HashMap<>();
        }
        schema.put(key, value);
    }
}
