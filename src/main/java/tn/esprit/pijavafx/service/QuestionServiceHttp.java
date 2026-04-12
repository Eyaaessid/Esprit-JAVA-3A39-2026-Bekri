package tn.esprit.pijavafx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import tn.esprit.pijavafx.model.QuestionEvaluationDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

public class QuestionServiceHttp implements IQuestionService {

    private static final String BASE_URL = "http://127.0.0.1:8080/api/questions";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public QuestionServiceHttp() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public List<QuestionEvaluationDto> getAll() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);
        return Arrays.asList(mapper.readValue(response.body(), QuestionEvaluationDto[].class));
    }

    @Override
    public QuestionEvaluationDto create(QuestionEvaluationDto dto) throws Exception {
        String json = mapper.writeValueAsString(dto);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);
        return mapper.readValue(response.body(), QuestionEvaluationDto.class);
    }

    @Override
    public QuestionEvaluationDto update(Integer id, QuestionEvaluationDto dto) throws Exception {
        String json = mapper.writeValueAsString(dto);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);
        return mapper.readValue(response.body(), QuestionEvaluationDto.class);
    }

    @Override
    public void delete(Integer id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);
    }

    /**
     * Throws an Exception if the HTTP response status is 4xx or 5xx.
     */
    private void checkStatus(HttpResponse<String> response) throws Exception {
        if (response.statusCode() >= 400) {
            throw new Exception("HTTP " + response.statusCode() + " — " + response.body());
        }
    }
}