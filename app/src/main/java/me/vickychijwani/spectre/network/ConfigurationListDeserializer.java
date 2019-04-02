package me.vickychijwani.spectre.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Map;

import me.vickychijwani.spectre.model.entity.ConfigurationParam;
import me.vickychijwani.spectre.network.entity.ConfigurationList;
import me.vickychijwani.spectre.util.log.Log;

/* package */ class ConfigurationListDeserializer implements JsonDeserializer<ConfigurationList> {

    @Override
    public ConfigurationList deserialize(JsonElement element, Type type,
                                         JsonDeserializationContext context)
            throws JsonParseException {
        try {
            JsonArray configJsons = element.getAsJsonObject().getAsJsonArray("configuration");
            // { "configuration": [{
            //     "blogTitle": "My Blog",
            //     "routeKeywords": { ... },
            //     ...
            // }]}
            return parseConfig(configJsons.get(0).getAsJsonObject());
        } catch (Exception e) {
            Log.e("ParseException", "Exception thrown while trying to parse JSON: %s",
                    element.toString());
            throw e;
        }
    }

    private ConfigurationList parseConfig(JsonObject configJson) {
        ConfigurationList config = new ConfigurationList();
        for (Map.Entry<String, JsonElement> entryJson : configJson.entrySet()) {
            String key = entryJson.getKey();
            JsonElement value = entryJson.getValue();
            config.configuration.add(makeConfigParam(key, value));
        }
        return config;
    }

    private ConfigurationParam makeConfigParam(String key, JsonElement value) {
        ConfigurationParam param = new ConfigurationParam();
        param.setKey(key);
        String valueStr;
        if (value.isJsonNull()) {
            valueStr = "";
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            // toString would return the string with quotes around it which we don't want
            valueStr = value.getAsString();
        } else {
            valueStr = value.toString();
        }
        param.setValue(valueStr);
        return param;
    }

}
