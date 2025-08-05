package tech.catheu.jeamlit.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public interface JsonUtils {
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static String toJson(final List<?> objs) {
        try {
            return OBJECT_MAPPER.writeValueAsString(objs);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
