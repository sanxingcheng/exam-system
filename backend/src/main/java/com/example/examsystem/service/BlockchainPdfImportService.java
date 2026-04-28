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
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  private final QuestionBankRepository bankRepository;
  private final QuestionRepository questionRepository;
  private final QuestionOptionRepository optionRepository;
  private final QuestionAnswerRepository answerRepository;
  private final ImportBatchRepository importBatchRepository;
  private final String defaultPdfPath;

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
    if (importBatchRepository.existsByQuestionBankIdAndImporterTypeAndFileSha256(
        bank.getId(), IMPORTER_TYPE, sha256)) {
      return new ImportResult(0, "文件已导入，跳过重复导入");
    }
    String text = readPdf(pdfPath);
    List<ParsedQuestion> parsedQuestions = parseQuestions(text);
    int saved = 0;
    for (ParsedQuestion parsed : parsedQuestions) {
      Question question = new Question();
      question.setQuestionBank(bank);
      question.setType(parsed.type());
      question.setContent(parsed.content());
      question.setSourceSection(parsed.section());
      question.setKnowledgeArea(parsed.area());
      question.setHasAnswer(parsed.answer() != null && !parsed.answer().isBlank());
      question.setAnswerLocked(parsed.locked());
      question.setExternalCode(parsed.externalCode());
      questionRepository.save(question);
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
        answer.setLocked(parsed.locked());
        answer.setSource(parsed.locked() ? AnswerSource.IMPORTED_LOCKED : AnswerSource.ADMIN_FILLED);
        answerRepository.save(answer);
      }
      saved++;
    }
    String report = "导入成功题数=" + saved + "，解析策略=规则解析，文件=" + pdfPath;
    ImportBatch batch = new ImportBatch();
    batch.setQuestionBank(bank);
    batch.setImporterType(IMPORTER_TYPE);
    batch.setFileSha256(sha256);
    batch.setQuestionCount(saved);
    batch.setReport(report);
    importBatchRepository.save(batch);
    log.info("pdf_import_completed bank={} count={} file={}", bankName, saved, pdfPath);
    return new ImportResult(saved, report);
  }

  List<ParsedQuestion> parseQuestions(String text) {
    String[] lines = text.split("\\R");
    List<ParsedQuestion> result = new ArrayList<>();
    ParsedQuestionBuilder current = null;
    String section = "";
    boolean lockedAnswerSection = false;
    for (String rawLine : lines) {
      String line = rawLine.trim();
      if (line.isBlank() || line.startsWith("--")) {
        continue;
      }
      if (line.contains("第 4 部分")) {
        section = "理论知识复习题";
        lockedAnswerSection = false;
        continue;
      }
      if (line.contains("第 5 部分")) {
        section = "操作技能复习题";
        lockedAnswerSection = false;
        continue;
      }
      if (line.contains("第 6 部分")) {
        section = "理论知识模拟试卷及答案";
        lockedAnswerSection = true;
        continue;
      }
      if (line.contains("第 7 部分")) {
        section = "操作技能模拟试卷及答案";
        lockedAnswerSection = true;
        continue;
      }
      Matcher questionMatcher = QUESTION_START.matcher(line);
      if (questionMatcher.matches()) {
        if (current != null) {
          result.add(current.build());
        }
        current = new ParsedQuestionBuilder();
        current.externalCode = questionMatcher.group(1);
        current.content.append(questionMatcher.group(2));
        current.section = section;
        current.locked = lockedAnswerSection;
        current.type = lockedAnswerSection ? QuestionType.SINGLE_CHOICE : QuestionType.MULTIPLE_CHOICE;
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
      result.add(current.build());
    }
    return result.stream().filter(item -> !item.content().isBlank()).toList();
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

  public record ImportResult(int importedCount, String report) {}

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
}
