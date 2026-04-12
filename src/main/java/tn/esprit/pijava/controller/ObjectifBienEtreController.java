package tn.esprit.pijava.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pijava.entity.ObjectifBienEtre;
import tn.esprit.pijava.repository.ObjectifBienEtreRepository;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/objectifs")
@CrossOrigin(origins = "*")
public class ObjectifBienEtreController {

    private final ObjectifBienEtreRepository repo;

    public ObjectifBienEtreController(ObjectifBienEtreRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ObjectifBienEtre> getAll() {
        return repo.findByUtilisateurId(1);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjectifBienEtre> getOne(@PathVariable Integer id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // @Valid triggers the @NotBlank / @NotNull / @Size constraints on the entity
    // If validation fails → Spring automatically returns HTTP 400 Bad Request
    @PostMapping
    public ResponseEntity<ObjectifBienEtre> create(@Valid @RequestBody ObjectifBienEtre o) {
        o.setId(null);
        o.setUtilisateurId(1);
        ObjectifBienEtre saved = repo.save(o);
        return ResponseEntity.created(URI.create("/api/objectifs/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObjectifBienEtre> update(@PathVariable Integer id,
                                                   @Valid @RequestBody ObjectifBienEtre body) {
        return repo.findById(id).map(existing -> {
            existing.setTitre(body.getTitre());
            existing.setDescription(body.getDescription());
            existing.setType(body.getType());
            existing.setValeurCible(body.getValeurCible());
            existing.setValeurActuelle(body.getValeurActuelle());
            existing.setDateDebut(body.getDateDebut());
            existing.setDateFin(body.getDateFin());
            existing.setStatut(body.getStatut());
            existing.setSlug(body.getSlug());
            existing.setUtilisateurId(1);
            return ResponseEntity.ok(repo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}