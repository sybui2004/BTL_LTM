package com.ltm.memorygame.tcp;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .serializeNulls()
            // Serialize java.time.Instant as epoch millis to avoid Gson runtime issues
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>)
                    (src, typeOfSrc, context) -> src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toEpochMilli()))
            .create();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }
}
