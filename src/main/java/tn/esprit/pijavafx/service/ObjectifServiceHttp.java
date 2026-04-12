package tn.esprit.pijavafx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import tn.esprit.pijavafx.model.ObjectifBienEtreDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

public class ObjectifServiceHttp implements IObjectifService {

    private static final String BASE = "http://127.0.0.1:8080/api/objectifs";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public ObjectifServiceHttp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public List<ObjectifBienEtreDto> getAll() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .GET()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(res);
        return Arrays.asList(mapper.readValue(res.body(), ObjectifBienEtreDto[].class));
    }

    @Override
    public ObjectifBienEtreDto create(ObjectifBienEtreDto dto) throws Exception {
        String json = mapper.writeValueAsString(dto);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(res);
        return mapper.readValue(res.body(), ObjectifBienEtreDto.class);
    }

    @Override
    public ObjectifBienEtreDto update(int id, ObjectifBienEtreDto dto) throws Exception {
        String json = mapper.writeValueAsString(dto);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(res);
        return mapper.readValue(res.body(), ObjectifBienEtreDto.class);
    }

    @Override
    public void delete(int id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/" + id))
                .DELETE()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(res);
    }

    /**
     * Throws an Exception if the HTTP response status is 4xx or 5xx.
     * This ensures callers (including tests) receive an exception on error responses.
     */
    private void checkStatus(HttpResponse<String> res) throws Exception {
        if (res.statusCode() >= 400) {
            throw new Exception("HTTP " + res.statusCode() + " — " + res.body());
        }
    }
}