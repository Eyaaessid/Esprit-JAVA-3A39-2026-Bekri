package tn.esprit.pijava.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tn.esprit.pijava.entity.QuestionEvaluation;
import tn.esprit.pijava.service.QuestionEvaluationService;
import tn.esprit.pijava.validation.ValidationException;
import tn.esprit.pijava.web.HttpResponseHelper;
import tn.esprit.pijava.web.JsonSupport;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class QuestionEvaluationController implements HttpHandler {

    private final QuestionEvaluationService service;

    public QuestionEvaluationController(QuestionEvaluationService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String idPart = extractId(path, "/api/questions");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (idPart == null) {
                if ("GET".equalsIgnoreCase(method)) {
                    List<QuestionEvaluation> list = service.findAll();
                    HttpResponseHelper.sendJson(exchange, 200, list);
                    return;
                }
                if ("POST".equalsIgnoreCase(method)) {
                    QuestionEvaluation q = JsonSupport.MAPPER.readValue(HttpResponseHelper.readBody(exchange), QuestionEvaluation.class);
                    q.setId(null);
                    QuestionEvaluation saved = service.save(q);
                    exchange.getResponseHeaders().set("Location", "/api/questions/" + saved.getId());
                    HttpResponseHelper.sendJson(exchange, 201, saved);
                    return;
                }
            } else {
                Integer id = Integer.valueOf(idPart);
                if ("GET".equalsIgnoreCase(method)) {
                    Optional<QuestionEvaluation> found = service.findById(id);
                    if (found.isPresent()) {
                        HttpResponseHelper.sendJson(exchange, 200, found.get());
                    } else {
                        HttpResponseHelper.sendError(exchange, 404, "Not found");
                    }
                    return;
                }
                if ("PUT".equalsIgnoreCase(method)) {
                    Optional<QuestionEvaluation> existing = service.findById(id);
                    if (existing.isEmpty()) {
                        HttpResponseHelper.sendError(exchange, 404, "Not found");
                        return;
                    }
                    QuestionEvaluation body = JsonSupport.MAPPER.readValue(HttpResponseHelper.readBody(exchange), QuestionEvaluation.class);
                    QuestionEvaluation q = existing.get();
                    q.setTexte(body.getTexte());
                    q.setCategory(body.getCategory());
                    q.setTypeReponse(body.getTypeReponse());
                    q.setOption1(body.getOption1());
                    q.setOption2(body.getOption2());
                    q.setOption3(body.getOption3());
                    q.setMinValue(body.getMinValue());
                    q.setMaxValue(body.getMaxValue());
                    HttpResponseHelper.sendJson(exchange, 200, service.save(q));
                    return;
                }
                if ("DELETE".equalsIgnoreCase(method)) {
                    if (!service.existsById(id)) {
                        HttpResponseHelper.sendError(exchange, 404, "Not found");
                        return;
                    }
                    service.deleteById(id);
                    HttpResponseHelper.sendNoContent(exchange);
                    return;
                }
            }

            HttpResponseHelper.sendError(exchange, 405, "Method Not Allowed");
        } catch (NumberFormatException e) {
            HttpResponseHelper.sendError(exchange, 400, "Invalid id");
        } catch (ValidationException e) {
            HttpResponseHelper.sendError(exchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            HttpResponseHelper.sendError(exchange, 500, e.getMessage());
        } finally {
            exchange.close();
        }
    }

    private String extractId(String path, String basePath) {
        if (path.equals(basePath) || path.equals(basePath + "/")) {
            return null;
        }
        String prefix = basePath + "/";
        if (!path.startsWith(prefix)) {
            return null;
        }
        String tail = path.substring(prefix.length());
        if (tail.contains("/")) {
            return null;
        }
        return tail;
    }
}
