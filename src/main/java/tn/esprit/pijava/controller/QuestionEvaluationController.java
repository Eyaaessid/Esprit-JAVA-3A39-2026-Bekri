package tn.esprit.pijava.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pijava.entity.QuestionEvaluation;
import tn.esprit.pijava.repository.QuestionEvaluationRepository;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionEvaluationController {

    private final QuestionEvaluationRepository repo;

    public QuestionEvaluationController(QuestionEvaluationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<QuestionEvaluation> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionEvaluation> getOne(@PathVariable Integer id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // @Valid triggers @NotBlank / @Size constraints → returns HTTP 400 if invalid
    @PostMapping
    public ResponseEntity<QuestionEvaluation> create(@Valid @RequestBody QuestionEvaluation q) {
        q.setId(null);
        QuestionEvaluation saved = repo.save(q);
        return ResponseEntity.created(URI.create("/api/questions/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionEvaluation> update(@PathVariable Integer id,
                                                     @Valid @RequestBody QuestionEvaluation body) {
        return repo.findById(id).map(existing -> {
            existing.setTexte(body.getTexte());
            existing.setCategory(body.getCategory());
            existing.setTypeReponse(body.getTypeReponse());
            existing.setOption1(body.getOption1());
            existing.setOption2(body.getOption2());
            existing.setOption3(body.getOption3());
            existing.setMinValue(body.getMinValue());
            existing.setMaxValue(body.getMaxValue());
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