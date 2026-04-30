package com.example.examsystem.service;

import com.example.examsystem.common.BusinessException;
import com.example.examsystem.domain.AppUser;
import com.example.examsystem.domain.Enums.AnswerSource;
import com.example.examsystem.domain.Enums.QuestionType;
import com.example.examsystem.domain.Question;
import com.example.examsystem.domain.QuestionAnswer;
import com.example.examsystem.domain.QuestionBank;
import com.example.examsystem.domain.QuestionMark;
import com.example.examsystem.domain.QuestionOption;
import com.example.examsystem.repository.QuestionAnswerRepository;
import com.example.examsystem.repository.QuestionBankRepository;
import com.example.examsystem.repository.QuestionMarkRepository;
import com.example.examsystem.repository.QuestionOptionRepository;
import com.example.examsystem.repository.QuestionRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Question bank queries and administrator answer maintenance. */
@Service
public class QuestionService {

  private final QuestionBankRepository bankRepository;
  private final QuestionRepository questionRepository;
  private final QuestionOptionRepository optionRepository;
  private final QuestionAnswerRepository answerRepository;
  private final QuestionMarkRepository markRepository;

  public QuestionService(
      QuestionBankRepository bankRepository,
      QuestionRepository questionRepository,
      QuestionOptionRepository optionRepository,
      QuestionAnswerRepository answerRepository,
      QuestionMarkRepository markRepository) {
    this.bankRepository = bankRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.answerRepository = answerRepository;
    this.markRepository = markRepository;
  }

  @Transactional(readOnly = true)
  public List<QuestionBank> listBanks() {
    return bankRepository.findAll();
  }

  @Transactional(readOnly = true)
  public QuestionPage listQuestions(
      Long bankId, int page, int size, String type, String category, String keyword) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    QuestionType questionType = parseType(type);
    String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
    String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    var questions =
        questionRepository.searchByBank(
            bankId,
            questionType,
            normalizedCategory,
            normalizedKeyword,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "id")));
    List<QuestionDto> items = questions.stream().map(this::toDto).toList();
    return new QuestionPage(items, questions.getTotalElements(), questions.getNumber(), questions.getSize());
  }

  @Transactional(readOnly = true)
  public QuestionDto getQuestion(Long questionId) {
    return toDto(loadQuestion(questionId));
  }

  @Transactional
  public QuestionDto fillAnswer(Long questionId, String answerText, String explanation) {
    Question question = loadQuestion(questionId);
    QuestionAnswer answer = answerRepository.findByQuestionId(questionId).orElse(null);
    if (answer == null) {
      answer = new QuestionAnswer();
      answer.setQuestion(question);
      answer.setSource(AnswerSource.ADMIN_FILLED);
      answer.setLocked(false);
    }
    answer.setAnswerText(answerText);
    answer.setExplanation(explanation);
    answerRepository.save(answer);
    question.setHasAnswer(true);
    question.setAnswerLocked(false);
    return toDto(question);
  }

  @Transactional
  public QuestionDto updateExplanation(Long questionId, String explanation) {
    QuestionAnswer answer =
        answerRepository.findByQuestionId(questionId).orElseThrow(() -> new BusinessException("题目暂无答案"));
    answer.setExplanation(explanation);
    return toDto(answer.getQuestion());
  }

  @Transactional
  public void markHard(AppUser user, Long questionId, boolean hard) {
    Question question = loadQuestion(questionId);
    QuestionMark mark =
        markRepository
            .findByQuestionBankIdAndQuestionIdAndUserId(
                question.getQuestionBank().getId(), questionId, user.getId())
            .orElseGet(
                () -> {
                  QuestionMark created = new QuestionMark();
                  created.setQuestionBank(question.getQuestionBank());
                  created.setQuestion(question);
                  created.setUser(user);
                  return created;
                });
    mark.setHard(hard);
    markRepository.save(mark);
  }

  private Question loadQuestion(Long questionId) {
    return questionRepository.findById(questionId).orElseThrow(() -> new BusinessException("题目不存在"));
  }

  private QuestionType parseType(String type) {
    if (type == null || type.isBlank()) {
      return null;
    }
    try {
      return QuestionType.valueOf(type.trim());
    } catch (IllegalArgumentException exception) {
      throw new BusinessException("不支持的题型: " + type);
    }
  }

  private QuestionDto toDto(Question question) {
    List<OptionDto> options =
        optionRepository.findByQuestionIdOrderByOptionKeyAsc(question.getId()).stream()
            .map(option -> new OptionDto(option.getOptionKey(), option.getContent()))
            .toList();
    QuestionAnswer answer = answerRepository.findByQuestionId(question.getId()).orElse(null);
    return new QuestionDto(
        question.getId(),
        question.getQuestionBank().getId(),
        question.getType().name(),
        question.getContent(),
        question.getSourceSection(),
        classifyForReview(question),
        question.isHasAnswer(),
        question.isAnswerLocked(),
        options,
        answer == null ? null : answer.getAnswerText(),
        answer == null ? null : answer.getExplanation(),
        answer == null ? null : normalizeAnswerSource(answer.getSource()));
  }

  private String normalizeAnswerSource(AnswerSource source) {
    if (source == AnswerSource.IMPORTED_LOCKED) {
      return AnswerSource.IMPORTED_FROM_MOCK.name();
    }
    return source.name();
  }

  private String classifyForReview(Question question) {
    if (question.getKnowledgeArea() != null && !question.getKnowledgeArea().isBlank()) {
      return question.getKnowledgeArea();
    }
    String content = question.getContent() == null ? "" : question.getContent();
    if (content.contains("合约") || content.contains("部署")) {
      return "区块链应用操作";
    }
    if (content.contains("测试") || content.contains("用例")) {
      return "区块链测试";
    }
    if (content.contains("监控") || content.contains("日志")) {
      return "区块链运维";
    }
    if (content.contains("需求") || content.contains("设计")) {
      return "区块链应用设计";
    }
    return "综合";
  }

  public record QuestionDto(
      Long id,
      Long questionBankId,
      String type,
      String content,
      String sourceSection,
      String knowledgeArea,
      boolean hasAnswer,
      boolean answerLocked,
      List<OptionDto> options,
      String answer,
      String explanation,
      String answerSource) {}

  public record OptionDto(String optionKey, String content) {}

  public record QuestionPage(List<QuestionDto> items, long total, int page, int size) {}
}
