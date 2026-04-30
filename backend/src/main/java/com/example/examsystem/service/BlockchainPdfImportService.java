package com.example.examsystem.service;

import com.example.examsystem.common.BusinessException;
import com.example.examsystem.domain.Enums.AnswerSource;
import com.example.examsystem.domain.Enums.QuestionType;
import com.example.examsystem.domain.ImportBatch;
import com.example.examsystem.domain.Question;
import com.example.examsystem.domain.QuestionAnswer;
import com.example.examsystem.domain.QuestionBank;
import com.example.examsystem.domain.QuestionOption;
import com.example.examsystem.repository.ImportBatchRepository;
import com.example.examsystem.repository.QuestionAnswerRepository;
import com.example.examsystem.repository.QuestionBankRepository;
import com.example.examsystem.repository.QuestionOptionRepository;
import com.example.examsystem.repository.QuestionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports the current blockchain operator PDF into its dedicated question bank. */
@Service
public class BlockchainPdfImportService {

  private static final Logger log = LoggerFactory.getLogger(BlockchainPdfImportService.class);
  private static final String IMPORTER_TYPE = "BLOCKCHAIN_OPERATOR_LEVEL3_PDF";
  private static final Pattern QUESTION_START = Pattern.compile("^(\\d+)\\.\\s*(.+)");
  private static final Pattern OPTION = Pattern.compile("^（([A-E])）\\s*(.+)");
  private static final Pattern ANSWER_LINE = Pattern.compile("^(\\d+)\\.?\\s*[:：]?\\s*([A-E]+)\\s*$");
  private static final Pattern ANSWER_PAIR = Pattern.compile("(\\d{1,3})\\s*([A-ExX√✓×])");

  private final QuestionBankRepository bankRepository;
  private final QuestionRepository questionRepository;
  private final QuestionOptionRepository optionRepository;
  private final QuestionAnswerRepository answerRepository;
  private final ImportBatchRepository importBatchRepository;
  private final String defaultPdfPath;

  @PersistenceContext private EntityManager entityManager;

  public BlockchainPdfImportService(
      QuestionBankRepository bankRepository,
      QuestionRepository questionRepository,
      QuestionOptionRepository optionRepository,
      QuestionAnswerRepository answerRepository,
      ImportBatchRepository importBatchRepository,
      @Value("${app.import.blockchain-pdf}") String defaultPdfPath) {
    this.bankRepository = bankRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.answerRepository = answerRepository;
    this.importBatchRepository = importBatchRepository;
    this.defaultPdfPath = defaultPdfPath;
  }

  @Transactional
  public ImportResult importDefaultPdf() {
    return importPdf(Path.of(defaultPdfPath), BootstrapService.BLOCKCHAIN_BANK_NAME);
  }

  @Transactional
  public ImportResult importPdf(Path pdfPath, String bankName) {
    if (!Files.exists(pdfPath)) {
      throw new BusinessException("PDF 文件不存在: " + pdfPath);
    }
    QuestionBank bank =
        bankRepository.findByName(bankName).orElseThrow(() -> new BusinessException("题库不存在"));
    String sha256 = sha256(pdfPath);
    if (questionRepository.countByQuestionBankId(bank.getId()) > 0) {
      resetImportedBankData(bank.getId());
    }
    String text = readPdf(pdfPath);
    ParseOutcome parseOutcome = parseQuestions(text);
    List<ParsedQuestion> parsedQuestions = parseOutcome.questions();
    MockAnswerOutcome answerOutcome = parseMockAnswers(text);
    if (parsedQuestions.isEmpty()) {
      throw new BusinessException("PDF 未解析出题目，请检查文件格式或解析规则");
    }
    int saved = 0;
    Map<String, Question> theoryQuestionIndex = new HashMap<>();
    Map<String, Question> theoryContentIndex = new HashMap<>();
    Map<String, Question> practicalQuestionIndex = new HashMap<>();
    for (ParsedQuestion parsed : parsedQuestions) {
      Question question = new Question();
      question.setQuestionBank(bank);
      question.setType(parsed.type());
      question.setContent(parsed.content());
      question.setSourceSection(parsed.section());
      question.setKnowledgeArea(parsed.area());
      question.setHasAnswer(parsed.answer() != null && !parsed.answer().isBlank());
      question.setAnswerLocked(false);
      question.setExternalCode(parsed.externalCode());
      questionRepository.save(question);
      indexQuestion(theoryQuestionIndex, theoryContentIndex, practicalQuestionIndex, question, parsed);
      for (ParsedOption parsedOption : parsed.options()) {
        QuestionOption option = new QuestionOption();
        option.setQuestion(question);
        option.setOptionKey(parsedOption.key());
        option.setContent(parsedOption.content());
        optionRepository.save(option);
      }
      if (question.isHasAnswer()) {
        QuestionAnswer answer = new QuestionAnswer();
        answer.setQuestion(question);
        answer.setAnswerText(parsed.answer());
        answer.setExplanation(parsed.explanation());
        answer.setLocked(false);
        answer.setSource(parsed.locked() ? AnswerSource.IMPORTED_FROM_MOCK : AnswerSource.ADMIN_FILLED);
        answerRepository.save(answer);
      }
      saved++;
    }
    int matchedAnswers =
        applyMockAnswers(answerOutcome, theoryQuestionIndex, theoryContentIndex, practicalQuestionIndex);
    ParseReportDetail detail =
        new ParseReportDetail(
            parseOutcome.stats().importedSingleChoice,
            parseOutcome.stats().importedMultipleChoice,
            parseOutcome.stats().importedTrueFalse,
            parseOutcome.stats().importedPractical,
            parseOutcome.stats().skippedWithoutTypeHeading,
            parseOutcome.stats().skippedChoiceWithoutOptions,
            answerOutcome.theoryAnswerCount(),
            answerOutcome.practicalAnswerCount(),
            matchedAnswers);
    String report =
        "重建导入成功题数="
            + saved
            + "，单选="
            + detail.singleChoiceCount()
            + "，多选="
            + detail.multipleChoiceCount()
            + "，判断="
            + detail.trueFalseCount()
            + "，实操="
            + detail.practicalCount()
            + "，提取答案="
            + detail.theoryAnswerCount()
            + "/"
            + detail.practicalAnswerCount()
            + "，匹配成功="
            + detail.matchedAnswerCount()
            + "，文件="
            + pdfPath;
    ImportBatch batch = new ImportBatch();
    batch.setQuestionBank(bank);
    batch.setImporterType(IMPORTER_TYPE);
    batch.setFileSha256(sha256);
    batch.setQuestionCount(saved);
    batch.setReport(report);
    importBatchRepository.save(batch);
    log.info("pdf_import_completed bank={} count={} file={}", bankName, saved, pdfPath);
    return new ImportResult(saved, report, detail);
  }

  private void resetImportedBankData(Long bankId) {
    executeDelete(
        "delete from exam_attempt_answers where attempt_id in "
            + "(select id from exam_attempts where question_bank_id = ?)",
        bankId);
    executeDelete("delete from exam_attempts where question_bank_id = ?", bankId);
    executeDelete(
        "delete from exam_paper_questions where paper_id in "
            + "(select id from exam_papers where question_bank_id = ?)",
        bankId);
    executeDelete("delete from exam_papers where question_bank_id = ?", bankId);
    executeDelete("delete from wrong_questions where question_bank_id = ?", bankId);
    executeDelete("delete from question_marks where question_bank_id = ?", bankId);
    executeDelete(
        "delete from question_answers where question_id in "
            + "(select id from questions where question_bank_id = ?)",
        bankId);
    executeDelete(
        "delete from question_options where question_id in "
            + "(select id from questions where question_bank_id = ?)",
        bankId);
    executeDelete("delete from questions where question_bank_id = ?", bankId);
    executeDelete("delete from import_batches where question_bank_id = ?", bankId);
  }

  private void executeDelete(String sql, Long bankId) {
    entityManager.createNativeQuery(sql).setParameter(1, bankId).executeUpdate();
  }

  ParseOutcome parseQuestions(String text) {
    String[] lines = text.split("\\R");
    List<ParsedQuestion> result = new ArrayList<>();
    ParseStats stats = new ParseStats();
    ParsedQuestionBuilder current = null;
    String section = "";
    Integer pendingPart = null;
    boolean lockedAnswerSection = false;
    QuestionType currentType = null;
    for (String rawLine : lines) {
      String line = rawLine.trim();
      if (line.isBlank() || line.startsWith("--")) {
        continue;
      }
      if (isPart4Heading(line)) {
        if ("理论知识复习题".equals(section)) {
          continue;
        }
        pendingPart = 4;
        if (line.replaceAll("\\s+", "").contains("理论知识复习题")) {
          if (current != null) {
            addIfValid(result, current, stats);
            current = null;
          }
          section = "理论知识复习题";
          lockedAnswerSection = false;
          currentType = null;
          pendingPart = null;
        }
        continue;
      }
      if (isPart5Heading(line)) {
        if ("操作技能复习题".equals(section)) {
          continue;
        }
        pendingPart = 5;
        if (line.replaceAll("\\s+", "").contains("操作技能复习题")) {
          if (current != null) {
            addIfValid(result, current, stats);
            current = null;
          }
          section = "操作技能复习题";
          lockedAnswerSection = false;
          currentType = null;
          pendingPart = null;
        }
        continue;
      }
      if (isPart6Heading(line) && !line.contains("·")) {
        pendingPart = 6;
        if (line.replaceAll("\\s+", "").contains("理论知识模拟试卷及答案")) {
          if (current != null) {
            addIfValid(result, current, stats);
            current = null;
          }
          section = "理论知识模拟试卷及答案";
          lockedAnswerSection = true;
          currentType = QuestionType.SINGLE_CHOICE;
          pendingPart = null;
        }
        continue;
      }
      if (isPart7Heading(line) && !line.contains("·")) {
        pendingPart = 7;
        if (line.replaceAll("\\s+", "").contains("操作技能模拟试卷及答案")) {
          if (current != null) {
            addIfValid(result, current, stats);
            current = null;
          }
          section = "操作技能模拟试卷及答案";
          lockedAnswerSection = true;
          currentType = null;
          pendingPart = null;
        }
        continue;
      }
      if (pendingPart != null && pendingPart == 4 && line.equals("理论知识复习题")) {
        section = "理论知识复习题";
        lockedAnswerSection = false;
        currentType = null;
        pendingPart = null;
        continue;
      }
      if (pendingPart != null && pendingPart == 5 && line.equals("操作技能复习题")) {
        section = "操作技能复习题";
        lockedAnswerSection = false;
        currentType = null;
        pendingPart = null;
        continue;
      }
      if (pendingPart != null && pendingPart == 6) {
        section = "理论知识模拟试卷及答案";
        lockedAnswerSection = true;
        currentType = QuestionType.SINGLE_CHOICE;
        pendingPart = null;
      }
      if (pendingPart != null && pendingPart == 7) {
        section = "操作技能模拟试卷及答案";
        lockedAnswerSection = true;
        currentType = null;
        pendingPart = null;
      }
      if (isPageNoiseLine(line)) {
        continue;
      }
      QuestionType headingType = detectQuestionTypeHeading(line);
      if (headingType != null) {
        if (current != null) {
          addIfValid(result, current, stats);
          current = null;
        }
        currentType = headingType;
        continue;
      }
      if (isPracticalSection(section) && !isMockSection(section) && line.startsWith("试题名称：")) {
        if (current != null) {
          addIfValid(result, current, stats);
        }
        current = new ParsedQuestionBuilder();
        current.externalCode = line.substring("试题名称：".length()).trim();
        current.content.append(line);
        current.section = section;
        current.locked = lockedAnswerSection;
        current.type = QuestionType.PRACTICAL;
        current.area = inferArea(section, line);
        continue;
      }
      if (current != null && current.type == QuestionType.PRACTICAL) {
        if (line.contains("试 题 评 分 表") || line.contains("参考答案")) {
          addIfValid(result, current, stats);
          current = null;
          continue;
        }
        current.content.append('\n').append(line);
        continue;
      }
      Matcher questionMatcher = QUESTION_START.matcher(line);
      if (questionMatcher.matches() && currentType == null) {
        stats.skippedWithoutTypeHeading++;
        continue;
      }
      if (currentType != null && questionMatcher.matches()) {
        if (current != null) {
          addIfValid(result, current, stats);
        }
        current = new ParsedQuestionBuilder();
        current.externalCode = questionMatcher.group(1);
        current.content.append(questionMatcher.group(2));
        current.section = section;
        current.locked = lockedAnswerSection;
        current.type = currentType;
        current.area = inferArea(section, questionMatcher.group(2));
        continue;
      }
      Matcher optionMatcher = OPTION.matcher(line);
      if (current != null && optionMatcher.matches()) {
        current.options.add(new ParsedOption(optionMatcher.group(1), optionMatcher.group(2)));
        continue;
      }
      Matcher answerMatcher = ANSWER_LINE.matcher(line);
      if (current != null && lockedAnswerSection && answerMatcher.matches()) {
        current.answer = answerMatcher.group(2);
        continue;
      }
      if (current != null) {
        current.content.append('\n').append(line);
      }
    }
    if (current != null) {
      addIfValid(result, current, stats);
    }
    return new ParseOutcome(result.stream().filter(item -> !item.content().isBlank()).toList(), stats);
  }

  MockAnswerOutcome parseMockAnswers(String text) {
    String[] lines = text.split("\\R");
    Map<QuestionType, Map<String, String>> theoryAnswers = new HashMap<>();
    Map<QuestionType, Map<String, String>> theoryQuestionContent = new HashMap<>();
    Map<String, String> practicalAnswers = new HashMap<>();
    boolean inPart6 = false;
    boolean inPart7 = false;
    QuestionType currentType = null;
    String currentPracticalTitle = null;
    boolean collectingPracticalAnswer = false;
    StringBuilder practicalAnswerBuffer = new StringBuilder();
    ParsedQuestionBuilder currentMockQuestion = null;
    String currentMockNumber = null;

    for (String rawLine : lines) {
      String line = rawLine.trim();
      if (line.isBlank()) {
        continue;
      }
      if (isPart6Heading(line)) {
        if (!inPart6) {
          inPart6 = true;
          inPart7 = false;
          currentType = null;
          currentMockQuestion = null;
          currentMockNumber = null;
        }
        continue;
      }
      if (isPart7Heading(line)) {
        if (currentMockQuestion != null && currentMockNumber != null && currentType != null) {
          theoryQuestionContent
              .computeIfAbsent(currentType, ignored -> new TreeMap<>())
              .putIfAbsent(currentMockNumber, currentMockQuestion.content.toString());
        }
        inPart6 = false;
        inPart7 = true;
        currentType = null;
        currentMockQuestion = null;
        currentMockNumber = null;
        continue;
      }
      if (isPageNoiseLine(line)) {
        continue;
      }
      if (inPart6) {
        QuestionType headingType = detectQuestionTypeHeading(line);
        if (headingType != null) {
          if (currentMockQuestion != null && currentMockNumber != null) {
            theoryQuestionContent
                .computeIfAbsent(Objects.requireNonNull(currentType), ignored -> new TreeMap<>())
                .putIfAbsent(currentMockNumber, currentMockQuestion.content.toString());
          }
          currentMockQuestion = null;
          currentMockNumber = null;
          currentType = headingType;
          continue;
        }
        if (currentType == null) {
          continue;
        }
        Matcher questionMatcher = QUESTION_START.matcher(line);
        if (questionMatcher.matches()) {
          if (currentMockQuestion != null && currentMockNumber != null) {
            theoryQuestionContent
                .computeIfAbsent(currentType, ignored -> new TreeMap<>())
                .putIfAbsent(currentMockNumber, currentMockQuestion.content.toString());
          }
          currentMockNumber = questionMatcher.group(1);
          currentMockQuestion = new ParsedQuestionBuilder();
          currentMockQuestion.content = new StringBuilder(questionMatcher.group(2));
          continue;
        }
        Matcher optionMatcher = OPTION.matcher(line);
        if (currentMockQuestion != null && optionMatcher.matches()) {
          currentMockQuestion.content.append('\n').append(line);
          continue;
        }
        Matcher pairMatcher = ANSWER_PAIR.matcher(line);
        while (pairMatcher.find()) {
          String number = pairMatcher.group(1);
          String answer = normalizeObjectiveAnswer(pairMatcher.group(2));
          theoryAnswers.computeIfAbsent(currentType, ignored -> new TreeMap<>()).putIfAbsent(number, answer);
        }
        if (currentMockQuestion != null) {
          currentMockQuestion.content.append('\n').append(line);
        }
        continue;
      }
      if (inPart7) {
        if (line.startsWith("试题名称：")) {
          if (currentPracticalTitle != null && practicalAnswerBuffer.length() > 0) {
            practicalAnswers.put(normalizeTitle(currentPracticalTitle), practicalAnswerBuffer.toString().trim());
          }
          currentPracticalTitle = line.substring("试题名称：".length()).trim();
          practicalAnswerBuffer = new StringBuilder();
          collectingPracticalAnswer = false;
          continue;
        }
        if (line.startsWith("参考答案")) {
          collectingPracticalAnswer = true;
          continue;
        }
        if (collectingPracticalAnswer && currentPracticalTitle != null) {
          if (line.startsWith("试 题 单") || line.startsWith("试 题 评 分 表")) {
            practicalAnswers.put(normalizeTitle(currentPracticalTitle), practicalAnswerBuffer.toString().trim());
            collectingPracticalAnswer = false;
            continue;
          }
          if (practicalAnswerBuffer.length() > 0) {
            practicalAnswerBuffer.append('\n');
          }
          practicalAnswerBuffer.append(line);
        }
      }
    }
    if (currentPracticalTitle != null && practicalAnswerBuffer.length() > 0) {
      practicalAnswers.put(normalizeTitle(currentPracticalTitle), practicalAnswerBuffer.toString().trim());
    }
    if (currentType != null && currentMockQuestion != null && currentMockNumber != null) {
      theoryQuestionContent
          .computeIfAbsent(currentType, ignored -> new TreeMap<>())
          .putIfAbsent(currentMockNumber, currentMockQuestion.content.toString());
    }
    return new MockAnswerOutcome(theoryAnswers, theoryQuestionContent, practicalAnswers);
  }

  private String normalizeObjectiveAnswer(String raw) {
    if ("√".equals(raw) || "✓".equals(raw)) {
      return "TRUE";
    }
    if ("×".equals(raw) || "x".equalsIgnoreCase(raw)) {
      return "FALSE";
    }
    return raw.trim();
  }

  private void indexQuestion(
      Map<String, Question> theoryQuestionIndex,
      Map<String, Question> theoryContentIndex,
      Map<String, Question> practicalQuestionIndex,
      Question question,
      ParsedQuestion parsed) {
    if (parsed.type() != QuestionType.PRACTICAL
        && !isMockSection(parsed.section())
        && parsed.externalCode() != null) {
      String key = parsed.type().name() + "|" + parsed.externalCode().trim();
      theoryQuestionIndex.put(key, question);
      theoryContentIndex.putIfAbsent(parsed.type().name() + "|" + normalizeQuestionContent(parsed.content()), question);
    }
    if ("操作技能复习题".equals(parsed.section()) && question.getType() == QuestionType.PRACTICAL) {
      String title = extractPracticalTitle(parsed.content());
      if (!title.isBlank()) {
        practicalQuestionIndex.put(normalizeTitle(title), question);
      }
    }
  }

  private int applyMockAnswers(
      MockAnswerOutcome answerOutcome,
      Map<String, Question> theoryQuestionIndex,
      Map<String, Question> theoryContentIndex,
      Map<String, Question> practicalQuestionIndex) {
    int matched = 0;
    for (Map.Entry<QuestionType, Map<String, String>> typeEntry : answerOutcome.theoryAnswers().entrySet()) {
      for (Map.Entry<String, String> answerEntry : typeEntry.getValue().entrySet()) {
        String key = typeEntry.getKey().name() + "|" + answerEntry.getKey();
        Question question = theoryQuestionIndex.get(key);
        String content = answerOutcome.theoryQuestionContent(typeEntry.getKey(), answerEntry.getKey());
        String normalizedContent = content == null ? null : normalizeQuestionContent(content);
        if (question != null && normalizedContent != null) {
          String normalizedIndexedQuestion = normalizeQuestionContent(question.getContent());
          // 模拟卷题号与复习题题号可能不一致，题干不一致时优先使用题干匹配。
          if (!normalizedIndexedQuestion.equals(normalizedContent)) {
            question = null;
          }
        }
        if (question == null && normalizedContent != null) {
          question = theoryContentIndex.get(typeEntry.getKey().name() + "|" + normalizedContent);
        }
        if (question == null) {
          continue;
        }
        saveOrUpdateAnswer(question, answerEntry.getValue(), null, AnswerSource.IMPORTED_FROM_MOCK);
        matched++;
      }
    }
    for (Map.Entry<String, String> entry : answerOutcome.practicalAnswers().entrySet()) {
      Question question = practicalQuestionIndex.get(entry.getKey());
      if (question == null) {
        continue;
      }
      saveOrUpdateAnswer(question, entry.getValue(), entry.getValue(), AnswerSource.IMPORTED_FROM_MOCK);
      matched++;
    }
    return matched;
  }

  private void saveOrUpdateAnswer(
      Question question, String answerText, String explanation, AnswerSource source) {
    QuestionAnswer answer =
        answerRepository
            .findByQuestionId(question.getId())
            .orElseGet(
                () -> {
                  QuestionAnswer created = new QuestionAnswer();
                  created.setQuestion(question);
                  return created;
                });
    answer.setAnswerText(answerText);
    answer.setExplanation(explanation);
    answer.setLocked(false);
    answer.setSource(source);
    answerRepository.save(answer);
    question.setHasAnswer(true);
    question.setAnswerLocked(false);
  }

  private boolean isMockSection(String section) {
    return "理论知识模拟试卷及答案".equals(section) || "操作技能模拟试卷及答案".equals(section);
  }

  private String normalizeQuestionContent(String content) {
    if (content == null) {
      return "";
    }
    String normalized =
        content
            .replaceAll("第[4567]部分(理论知识复习题|操作技能复习题|理论知识模拟试卷及答案|操作技能模拟试卷及答案)", "")
            .replaceAll("（\\s*\\）", "")
            .replaceAll("[\\p{Punct}，。；：、“”‘’（）()【】\\s]", "")
            .toLowerCase();
    return normalized.length() > 160 ? normalized.substring(0, 160) : normalized;
  }

  private String extractPracticalTitle(String content) {
    String marker = "试题名称：";
    int start = content.indexOf(marker);
    if (start < 0) {
      return "";
    }
    String tail = content.substring(start + marker.length()).trim();
    int newLine = tail.indexOf('\n');
    return newLine >= 0 ? tail.substring(0, newLine).trim() : tail;
  }

  private String normalizeTitle(String title) {
    return title == null ? "" : title.replaceAll("\\s+", "");
  }

  private QuestionType detectQuestionTypeHeading(String line) {
    if (!line.matches("^[一二三四五六七八九十]+、\\s*.*")) {
      return null;
    }
    if (line.contains("判断题")) {
      return QuestionType.TRUE_FALSE;
    }
    if (line.contains("单选题") || line.contains("单项选择题")) {
      return QuestionType.SINGLE_CHOICE;
    }
    if (line.contains("多选题") || line.contains("多项选择题")) {
      return QuestionType.MULTIPLE_CHOICE;
    }
    return null;
  }

  private boolean isPart4Heading(String line) {
    String compact = line.replaceAll("\\s+", "");
    if (line.contains("·")) {
      return false;
    }
    return compact.equals("第4部分") || compact.equals("第4部分理论知识复习题");
  }

  private boolean isPart5Heading(String line) {
    String compact = line.replaceAll("\\s+", "");
    if (line.contains("·")) {
      return false;
    }
    return compact.equals("第5部分") || compact.equals("第5部分操作技能复习题");
  }

  private boolean isPart6Heading(String line) {
    String compact = line.replaceAll("\\s+", "");
    return compact.equals("第6部分") || compact.contains("第6部分理论知识模拟试卷及答案");
  }

  private boolean isPart7Heading(String line) {
    String compact = line.replaceAll("\\s+", "");
    return compact.equals("第7部分") || compact.contains("第7部分操作技能模拟试卷及答案");
  }

  private boolean isPracticalSection(String section) {
    return section != null && section.contains("操作技能");
  }

  private boolean isPageNoiseLine(String line) {
    String compact = line.replaceAll("\\s+", "");
    if (compact.matches("^\\d{1,4}$")) {
      return true;
    }
    if (compact.matches("^第[4567]部分(理论知识复习题|操作技能复习题|理论知识模拟试卷及答案|操作技能模拟试卷及答案)$")) {
      return true;
    }
    if (compact.contains("区块链应用操作员（三级）")) {
      return true;
    }
    if (compact.equals("理论知识复习题") || compact.equals("操作技能复习题")) {
      return true;
    }
    return false;
  }

  private void addIfValid(List<ParsedQuestion> result, ParsedQuestionBuilder current, ParseStats stats) {
    ParsedQuestion question = current.build();
    if (isMockSection(question.section())) {
      return;
    }
    if (question.type() == QuestionType.PRACTICAL) {
      result.add(question);
      stats.importedPractical++;
      return;
    }
    if (question.type() == QuestionType.TRUE_FALSE && question.options().isEmpty()) {
      result.add(question);
      stats.importedTrueFalse++;
      return;
    }
    if ((question.type() == QuestionType.SINGLE_CHOICE || question.type() == QuestionType.MULTIPLE_CHOICE)
        && question.options().size() >= 2) {
      result.add(question);
      if (question.type() == QuestionType.SINGLE_CHOICE) {
        stats.importedSingleChoice++;
      } else {
        stats.importedMultipleChoice++;
      }
      return;
    }
    stats.skippedChoiceWithoutOptions++;
  }

  private String inferArea(String section, String content) {
    if (content.contains("测试")) {
      return "区块链测试";
    }
    if (content.contains("监控") || content.contains("日志") || content.contains("运维")) {
      return "区块链运维";
    }
    if (content.contains("需求") || content.contains("设计") || content.contains("调研")) {
      return "区块链应用设计";
    }
    if (content.contains("合约") || content.contains("部署") || content.contains("节点")) {
      return "区块链应用操作";
    }
    return section == null || section.isBlank() ? "综合" : section;
  }

  private String readPdf(Path path) {
    try (PDDocument document = Loader.loadPDF(path.toFile())) {
      return new PDFTextStripper().getText(document);
    } catch (IOException exception) {
      throw new BusinessException("读取 PDF 失败: " + exception.getMessage());
    }
  }

  private String sha256(Path path) {
    try (InputStream input = Files.newInputStream(path)) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) > 0) {
        digest.update(buffer, 0, read);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (IOException | NoSuchAlgorithmException exception) {
      throw new BusinessException("计算文件指纹失败: " + exception.getMessage());
    }
  }

  public record ImportResult(int importedCount, String report, ParseReportDetail detail) {}

  public record ParseReportDetail(
      int singleChoiceCount,
      int multipleChoiceCount,
      int trueFalseCount,
      int practicalCount,
      int skippedWithoutTypeHeading,
      int skippedChoiceWithoutOptions,
      int theoryAnswerCount,
      int practicalAnswerCount,
      int matchedAnswerCount) {}

  record ParseOutcome(List<ParsedQuestion> questions, ParseStats stats) {}

  record MockAnswerOutcome(
      Map<QuestionType, Map<String, String>> theoryAnswers,
      Map<QuestionType, Map<String, String>> theoryQuestionContent,
      Map<String, String> practicalAnswers) {
    int theoryAnswerCount() {
      return theoryAnswers.values().stream().mapToInt(Map::size).sum();
    }

    int practicalAnswerCount() {
      return practicalAnswers.size();
    }

    String theoryQuestionContent(QuestionType type, String number) {
      return theoryQuestionContent.getOrDefault(type, Map.of()).get(number);
    }
  }

  record ParsedOption(String key, String content) {}

  record ParsedQuestion(
      String externalCode,
      QuestionType type,
      String content,
      String section,
      String area,
      List<ParsedOption> options,
      String answer,
      String explanation,
      boolean locked) {}

  static class ParsedQuestionBuilder {
    String externalCode;
    QuestionType type;
    StringBuilder content = new StringBuilder();
    String section;
    String area;
    List<ParsedOption> options = new ArrayList<>();
    String answer;
    String explanation;
    boolean locked;

    ParsedQuestion build() {
      return new ParsedQuestion(
          externalCode, type, content.toString(), section, area, options, answer, explanation, locked);
    }
  }

  static class ParseStats {
    int importedSingleChoice;
    int importedMultipleChoice;
    int importedTrueFalse;
    int importedPractical;
    int skippedWithoutTypeHeading;
    int skippedChoiceWithoutOptions;
  }
}
