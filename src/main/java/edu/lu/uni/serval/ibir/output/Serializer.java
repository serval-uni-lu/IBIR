package edu.lu.uni.serval.ibir.output;

import com.google.gson.Gson;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Serializer {

    private static Gson instance;


    private Serializer() {
        throw new IllegalAccessError("Utility class: No instance allowed, static access only.");
    }


    public static Gson getInstance() {
        if (instance == null) {
            instance = new Gson();
        }
        return instance;
    }

    public static String to(Serializable object) {
        return Base64.getEncoder().encodeToString(getInstance().toJson(object).getBytes(StandardCharsets.UTF_8));
    }

    public static <T extends Serializable> T from(String str, Class<T> classOfT) {
        return getInstance().fromJson(new String(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8))), classOfT);
    }

}
