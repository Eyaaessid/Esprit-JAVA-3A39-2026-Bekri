package tn.esprit.pijava.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonSupport {
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private JsonSupport() {
    }
}
