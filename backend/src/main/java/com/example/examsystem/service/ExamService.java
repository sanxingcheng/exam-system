package com.example.examsystem.service;

import com.example.examsystem.common.BusinessException;
import com.example.examsystem.domain.AppUser;
import com.example.examsystem.domain.Enums.AttemptStatus;
import com.example.examsystem.domain.ExamAttempt;
import com.example.examsystem.domain.ExamAttemptAnswer;
import com.example.examsystem.domain.ExamPaper;
import com.example.examsystem.domain.ExamPaperQuestion;
import com.example.examsystem.domain.Question;
import com.example.examsystem.domain.QuestionAnswer;
import com.example.examsystem.domain.QuestionBank;
import com.example.examsystem.domain.QuestionMark;
import com.example.examsystem.domain.QuestionOption;
import com.example.examsystem.domain.WrongQuestion;
import com.example.examsystem.repository.ExamAttemptAnswerRepository;
import com.example.examsystem.repository.ExamAttemptRepository;
import com.example.examsystem.repository.ExamPaperQuestionRepository;
import com.example.examsystem.repository.ExamPaperRepository;
import com.example.examsystem.repository.QuestionAnswerRepository;
import com.example.examsystem.repository.QuestionBankRepository;
import com.example.examsystem.repository.QuestionMarkRepository;
import com.example.examsystem.repository.QuestionOptionRepository;
import com.example.examsystem.repository.QuestionRepository;
import com.example.examsystem.repository.WrongQuestionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Generates papers and records exam attempts within a selected question bank. */
@Service
public class ExamService {

  private final QuestionBankRepository bankRepository;
  private final QuestionRepository questionRepository;
  private final QuestionOptionRepository optionRepository;
  private final QuestionAnswerRepository answerRepository;
  private final QuestionMarkRepository markRepository;
  private final WrongQuestionRepository wrongQuestionRepository;
  private final ExamPaperRepository paperRepository;
  private final ExamPaperQuestionRepository paperQuestionRepository;
  private final ExamAttemptRepository attemptRepository;
  private final ExamAttemptAnswerRepository attemptAnswerRepository;

  public ExamService(
      QuestionBankRepository bankRepository,
      QuestionRepository questionRepository,
      QuestionOptionRepository optionRepository,
      QuestionAnswerRepository answerRepository,
      QuestionMarkRepository markRepository,
      WrongQuestionRepository wrongQuestionRepository,
      ExamPaperRepository paperRepository,
      ExamPaperQuestionRepository paperQuestionRepository,
      ExamAttemptRepository attemptRepository,
      ExamAttemptAnswerRepository attemptAnswerRepository) {
    this.bankRepository = bankRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.answerRepository = answerRepository;
    this.markRepository = markRepository;
    this.wrongQuestionRepository = wrongQuestionRepository;
    this.paperRepository = paperRepository;
    this.paperQuestionRepository = paperQuestionRepository;
    this.attemptRepository = attemptRepository;
    this.attemptAnswerRepository = attemptAnswerRepository;
  }

  @Transactional
  public AttemptDto createAttempt(AppUser user, Long bankId, int questionCount) {
    QuestionBank bank =
        bankRepository.findById(bankId).orElseThrow(() -> new BusinessException("题库不存在"));
    List<Question> selected = selectQuestions(user, bankId, Math.max(questionCount, 1));
    if (selected.isEmpty()) {
      throw new BusinessException("当前题库暂无有答案的题目，不能生成模拟试卷");
    }
    ExamPaper paper = new ExamPaper();
    paper.setQuestionBank(bank);
    paper.setTitle(bank.getName() + "模拟试卷");
    paper.setDurationMinutes(bank.getDefaultDurationMinutes());
    paperRepository.save(paper);

    int order = 1;
    for (Question question : selected) {
      QuestionAnswer answer =
          answerRepository
              .findByQuestionId(question.getId())
              .orElseThrow(() -> new BusinessException("题目答案缺失"));
      ExamPaperQuestion paperQuestion = new ExamPaperQuestion();
      paperQuestion.setPaper(paper);
      paperQuestion.setQuestion(question);
      paperQuestion.setQuestionOrder(order++);
      paperQuestion.setType(question.getType());
      paperQuestion.setContentSnapshot(question.getContent());
      paperQuestion.setOptionsSnapshot(optionsSnapshot(question.getId()));
      paperQuestion.setAnswerSnapshot(answer.getAnswerText());
      paperQuestionRepository.save(paperQuestion);
    }

    ExamAttempt attempt = new ExamAttempt();
    attempt.setQuestionBank(bank);
    attempt.setPaper(paper);
    attempt.setUser(user);
    attempt.setTotalQuestions(selected.size());
    attemptRepository.save(attempt);
    return toAttemptDto(attempt);
  }

  @Transactional
  public AttemptDto submit(AppUser user, Long attemptId, Map<Long, String> answers) {
    ExamAttempt attempt =
        attemptRepository.findById(attemptId).orElseThrow(() -> new BusinessException("考试记录不存在"));
    if (!attempt.getUser().getId().equals(user.getId())) {
      throw new BusinessException("不能提交他人的考试记录");
    }
    if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
      throw new BusinessException("考试已提交");
    }
    List<ExamPaperQuestion> questions =
        paperQuestionRepository.findByPaperIdOrderByQuestionOrderAsc(attempt.getPaper().getId());
    int correct = 0;
    for (ExamPaperQuestion paperQuestion : questions) {
      String userAnswer = normalize(answers.getOrDefault(paperQuestion.getId(), ""));
      boolean isCorrect = normalize(paperQuestion.getAnswerSnapshot()).equals(userAnswer);
      ExamAttemptAnswer attemptAnswer = new ExamAttemptAnswer();
      attemptAnswer.setAttempt(attempt);
      attemptAnswer.setPaperQuestion(paperQuestion);
      attemptAnswer.setUserAnswer(userAnswer);
      attemptAnswer.setCorrect(isCorrect);
      attemptAnswerRepository.save(attemptAnswer);
      if (isCorrect) {
        correct++;
      } else {
        recordWrong(user, attempt.getQuestionBank(), paperQuestion.getQuestion());
      }
    }
    attempt.setCorrectQuestions(correct);
    attempt.setScore((int) Math.round(correct * 100.0 / Math.max(questions.size(), 1)));
    attempt.setSubmittedAt(Instant.now());
    attempt.setStatus(AttemptStatus.SUBMITTED);
    return toAttemptDto(attempt);
  }

  @Transactional(readOnly = true)
  public List<AttemptDto> listAttempts(AppUser user, Long bankId) {
    return attemptRepository.findByUserIdAndQuestionBankIdOrderByStartedAtDesc(user.getId(), bankId).stream()
        .map(this::toAttemptDto)
        .toList();
  }

  private List<Question> selectQuestions(AppUser user, Long bankId, int questionCount) {
    List<Question> answered = questionRepository.findByQuestionBankIdAndHasAnswerTrue(bankId);
    Map<Long, Question> selected = new LinkedHashMap<>();
    wrongQuestionRepository.findByQuestionBankIdAndUserIdAndMasteredFalseOrderByWrongCountDesc(bankId, user.getId())
        .stream()
        .map(WrongQuestion::getQuestion)
        .filter(answered::contains)
        .forEach(question -> addIfRoom(selected, question, questionCount));
    markRepository.findByQuestionBankIdAndUserIdAndHardTrue(bankId, user.getId()).stream()
        .map(QuestionMark::getQuestion)
        .filter(answered::contains)
        .forEach(question -> addIfRoom(selected, question, questionCount));

    Set<String> coveredTypes = new LinkedHashSet<>();
    Set<String> coveredAreas = new LinkedHashSet<>();
    selected.values().forEach(q -> {
      coveredTypes.add(q.getType().name());
      coveredAreas.add(q.getKnowledgeArea());
    });
    for (Question question : answered) {
      if (!coveredTypes.contains(question.getType().name())) {
        addIfRoom(selected, question, questionCount);
        coveredTypes.add(question.getType().name());
      }
    }
    for (Question question : answered) {
      if (!coveredAreas.contains(question.getKnowledgeArea())) {
        addIfRoom(selected, question, questionCount);
        coveredAreas.add(question.getKnowledgeArea());
      }
    }
    answered.stream()
        .sorted(Comparator.comparing(Question::getId))
        .forEach(question -> addIfRoom(selected, question, questionCount));
    return new ArrayList<>(selected.values());
  }

  private void addIfRoom(Map<Long, Question> selected, Question question, int questionCount) {
    if (selected.size() < questionCount) {
      selected.putIfAbsent(question.getId(), question);
    }
  }

  private void recordWrong(AppUser user, QuestionBank bank, Question question) {
    WrongQuestion wrong =
        wrongQuestionRepository
            .findByQuestionBankIdAndQuestionIdAndUserId(bank.getId(), question.getId(), user.getId())
            .orElseGet(
                () -> {
                  WrongQuestion created = new WrongQuestion();
                  created.setQuestionBank(bank);
                  created.setQuestion(question);
                  created.setUser(user);
                  return created;
                });
    wrong.markWrong();
    wrongQuestionRepository.save(wrong);
  }

  private String optionsSnapshot(Long questionId) {
    List<QuestionOption> options = optionRepository.findByQuestionIdOrderByOptionKeyAsc(questionId);
    StringBuilder builder = new StringBuilder();
    for (QuestionOption option : options) {
      if (!builder.isEmpty()) {
        builder.append('\n');
      }
      builder.append(option.getOptionKey()).append(". ").append(option.getContent());
    }
    return builder.toString();
  }

  private String normalize(String value) {
    return value == null ? "" : value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
  }

  private AttemptDto toAttemptDto(ExamAttempt attempt) {
    List<PaperQuestionDto> questions =
        paperQuestionRepository.findByPaperIdOrderByQuestionOrderAsc(attempt.getPaper().getId()).stream()
            .map(
                question ->
                    new PaperQuestionDto(
                        question.getId(),
                        question.getQuestionOrder(),
                        question.getType().name(),
                        question.getContentSnapshot(),
                        question.getOptionsSnapshot()))
            .toList();
    return new AttemptDto(
        attempt.getId(),
        attempt.getQuestionBank().getId(),
        attempt.getPaper().getId(),
        attempt.getPaper().getTitle(),
        attempt.getPaper().getDurationMinutes(),
        attempt.getStatus().name(),
        attempt.getScore(),
        attempt.getTotalQuestions(),
        attempt.getCorrectQuestions(),
        questions);
  }

  public record AttemptDto(
      Long id,
      Long questionBankId,
      Long paperId,
      String title,
      int durationMinutes,
      String status,
      int score,
      int totalQuestions,
      int correctQuestions,
      List<PaperQuestionDto> questions) {}

  public record PaperQuestionDto(
      Long id, int order, String type, String content, String optionsSnapshot) {}
}
