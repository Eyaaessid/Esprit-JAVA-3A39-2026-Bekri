package tn.esprit.pijavafx.service;

import tn.esprit.pijavafx.model.QuestionEvaluationDto;

import java.util.List;

public interface IQuestionService {
    List<QuestionEvaluationDto> getAll() throws Exception;
    QuestionEvaluationDto create(QuestionEvaluationDto dto) throws Exception;
    QuestionEvaluationDto update(Integer id, QuestionEvaluationDto dto) throws Exception;
    void delete(Integer id) throws Exception;
}