package tn.esprit.pijava.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tn.esprit.pijava.entity.ObjectifBienEtre;
import tn.esprit.pijava.service.ObjectifBienEtreService;
import tn.esprit.pijava.validation.ValidationException;
import tn.esprit.pijava.web.HttpResponseHelper;
import tn.esprit.pijava.web.JsonSupport;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ObjectifBienEtreController implements HttpHandler {

    private final ObjectifBienEtreService service;

    public ObjectifBienEtreController(ObjectifBienEtreService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String idPart = extractId(path, "/api/objectifs");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (idPart == null) {
                if ("GET".equalsIgnoreCase(method)) {
                    List<ObjectifBienEtre> list = service.findByUtilisateurId(1);
                    HttpResponseHelper.sendJson(exchange, 200, list);
                    return;
                }
                if ("POST".equalsIgnoreCase(method)) {
                    ObjectifBienEtre o = JsonSupport.MAPPER.readValue(HttpResponseHelper.readBody(exchange), ObjectifBienEtre.class);
                    o.setId(null);
                    o.setUtilisateurId(1);
                    ObjectifBienEtre saved = service.save(o);
                    exchange.getResponseHeaders().set("Location", "/api/objectifs/" + saved.getId());
                    HttpResponseHelper.sendJson(exchange, 201, saved);
                    return;
                }
            } else {
                Integer id = Integer.valueOf(idPart);
                if ("GET".equalsIgnoreCase(method)) {
                    Optional<ObjectifBienEtre> found = service.findById(id);
                    if (found.isPresent()) {
                        HttpResponseHelper.sendJson(exchange, 200, found.get());
                    } else {
                        HttpResponseHelper.sendError(exchange, 404, "Not found");
                    }
                    return;
                }
                if ("PUT".equalsIgnoreCase(method)) {
                    Optional<ObjectifBienEtre> existing = service.findById(id);
                    if (existing.isEmpty()) {
                        HttpResponseHelper.sendError(exchange, 404, "Not found");
                        return;
                    }
                    ObjectifBienEtre body = JsonSupport.MAPPER.readValue(HttpResponseHelper.readBody(exchange), ObjectifBienEtre.class);
                    ObjectifBienEtre o = existing.get();
                    o.setTitre(body.getTitre());
                    o.setDescription(body.getDescription());
                    o.setType(body.getType());
                    o.setValeurCible(body.getValeurCible());
                    o.setValeurActuelle(body.getValeurActuelle());
                    o.setDateDebut(body.getDateDebut());
                    o.setDateFin(body.getDateFin());
                    o.setStatut(body.getStatut());
                    o.setSlug(body.getSlug());
                    o.setUtilisateurId(1);
                    HttpResponseHelper.sendJson(exchange, 200, service.save(o));
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
