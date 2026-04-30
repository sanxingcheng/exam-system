package com.example.examsystem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.examsystem.domain.Enums.QuestionType;
import com.example.examsystem.domain.Question;
import com.example.examsystem.domain.QuestionAnswer;
import com.example.examsystem.repository.QuestionAnswerRepository;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BlockchainPdfImportServiceTest {

  @Test
  void parseQuestionsSwitchesTypeAtBoundaryHeading() {
    BlockchainPdfImportService service =
        new BlockchainPdfImportService(null, null, null, null, null, "/tmp/unused.pdf");
    String text =
        """
        第 4 部分
        理论知识复习题
        一、 判断题（将判断结果填入括号中。正确的填“√”，错误的填“×”）
        1. 在 Unix/Linux 系统中，使用 cd 命令可以切换当前工作目录。（ ）
        2. 区块链集群中，只要剩余节点足够多就可继续运行。（ ）
        二、 单项选择题（第 1 题~第 140 题。选择一个正确的答案）
        1. 区块链中的共识机制主要用于解决（ ）问题。
        （A）数据存储
        （B）数据传输
        （C）数据一致性
        （D）数据加密
        2. 在 Truffle 框架中，部署合约脚本通常放在（ ）目录。
        （A）scripts
        （B）migrations
        （C）contracts
        （D）build
        """;

    var parsed = service.parseQuestions(text).questions();

    assertThat(parsed).hasSize(4);
    assertThat(parsed.get(0).type()).isEqualTo(QuestionType.TRUE_FALSE);
    assertThat(parsed.get(1).type()).isEqualTo(QuestionType.TRUE_FALSE);
    assertThat(parsed.get(2).type()).isEqualTo(QuestionType.SINGLE_CHOICE);
    assertThat(parsed.get(3).type()).isEqualTo(QuestionType.SINGLE_CHOICE);
  }

  @Test
  @DisplayName("题号与章节标题在同一行时保留理论复习题章节")
  void parseQuestionsKeepsSectionForCompactPartHeading() {
    BlockchainPdfImportService service =
        new BlockchainPdfImportService(null, null, null, null, null, "/tmp/unused.pdf");
    String text =
        """
        第4 部分 理论知识复习题
        一、 判断题（将判断结果填入括号中。正确的填“√”，错误的填“×”）
        189. 在以太坊上部署合约时，合约地址是由开发者自行指定的。（   ）
        """;

    var parsed = service.parseQuestions(text).questions();

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0).type()).isEqualTo(QuestionType.TRUE_FALSE);
    assertThat(parsed.get(0).externalCode()).isEqualTo("189");
    assertThat(parsed.get(0).section()).isEqualTo("理论知识复习题");
  }

  @Test
  @DisplayName("页眉重复的章节标题不会重置当前题型")
  void parseQuestionsDoesNotResetTypeOnRepeatedPartHeader() {
    BlockchainPdfImportService service =
        new BlockchainPdfImportService(null, null, null, null, null, "/tmp/unused.pdf");
    String text =
        """
        第 4 部分
        理论知识复习题
        一、 判断题（将判断结果填入括号中。正确的填“√”，错误的填“×”）
        1. 第一题。（   ）
        第4 部分 理论知识复习题
        2. 第二题。（   ）
        """;

    var outcome = service.parseQuestions(text);
    var parsed = outcome.questions();

    assertThat(parsed).hasSize(2);
    assertThat(parsed.get(0).type()).isEqualTo(QuestionType.TRUE_FALSE);
    assertThat(parsed.get(1).type()).isEqualTo(QuestionType.TRUE_FALSE);
    assertThat(outcome.stats().skippedWithoutTypeHeading).isZero();
  }

  @Test
  @DisplayName("目录中的点线章节标题不会触发章节切换")
  void parseQuestionsIgnoresCatalogStylePartHeading() {
    BlockchainPdfImportService service =
        new BlockchainPdfImportService(null, null, null, null, null, "/tmp/unused.pdf");
    String text =
        """
        第 4 部分  理论知识复习题···················（17）
        第 5 部分  操作技能复习题·················（151）
        第 4 部分
        理论知识复习题
        一、 判断题（将判断结果填入括号中。正确的填“√”，错误的填“×”）
        1. 第一题。（   ）
        """;

    var outcome = service.parseQuestions(text);
    var parsed = outcome.questions();

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0).externalCode()).isEqualTo("1");
    assertThat(parsed.get(0).section()).isEqualTo("理论知识复习题");
    assertThat(outcome.stats().skippedWithoutTypeHeading).isZero();
  }

  @Test
  @DisplayName("同一题号在多份模拟答案中重复时，使用首次出现的判断题答案")
  void parseMockAnswersKeepsFirstTrueFalseAnswerWhenDuplicateNumberExists() {
    BlockchainPdfImportService service =
        new BlockchainPdfImportService(null, null, null, null, null, "/tmp/unused.pdf");
    String text =
        """
        第 6 部分 理论知识模拟试卷及答案
        一、 判断题（第 1 题～第 40 题）
        1. 示例题干一。（ ）
        2. 示例题干二。（ ）
        1 × 2 ✓
        一、 判断题（第 1 题～第 40 题）
        1. 另一份试卷题干一。（ ）
        2. 另一份试卷题干二。（ ）
        1 ✓ 2 ×
        """;

    Object outcome = ReflectionTestUtils.invokeMethod(service, "parseMockAnswers", text);
    @SuppressWarnings("unchecked")
    Map<QuestionType, Map<String, String>> theoryAnswers =
        (Map<QuestionType, Map<String, String>>)
            ReflectionTestUtils.invokeMethod(outcome, "theoryAnswers");
    String answer1 = theoryAnswers.getOrDefault(QuestionType.TRUE_FALSE, Map.of()).get("1");
    String answer2 = theoryAnswers.getOrDefault(QuestionType.TRUE_FALSE, Map.of()).get("2");

    assertThat(answer1).isEqualTo("FALSE");
    assertThat(answer2).isEqualTo("TRUE");
  }

  @Test
  @DisplayName("题号匹配题干不一致时回退到题干匹配并写入正确题目")
  void applyMockAnswersFallsBackToContentWhenNumberPointsToWrongQuestion() {
    QuestionAnswerRepository answerRepository = mock(QuestionAnswerRepository.class);
    when(answerRepository.findByQuestionId(any())).thenReturn(java.util.Optional.empty());
    when(answerRepository.save(any(QuestionAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));

    BlockchainPdfImportService service =
        new BlockchainPdfImportService(null, null, null, answerRepository, null, "/tmp/unused.pdf");

    Question wrongByNumber = new Question();
    ReflectionTestUtils.setField(wrongByNumber, "id", 1L);
    wrongByNumber.setContent("区块链是一种去中心化的分布式账本。（   ）");

    Question correctByContent = new Question();
    ReflectionTestUtils.setField(correctByContent, "id", 2L);
    correctByContent.setContent("在以太坊上部署合约时，合约地址是由开发者自行指定的。（   ）");

    BlockchainPdfImportService.MockAnswerOutcome outcome =
        new BlockchainPdfImportService.MockAnswerOutcome(
            Map.of(QuestionType.TRUE_FALSE, Map.of("35", "FALSE")),
            Map.of(QuestionType.TRUE_FALSE, Map.of("35", "在以太坊上部署合约时，合约地址是由开发者自行指定的。（   ）")),
            Map.of());

    Map<String, Question> theoryQuestionIndex = new java.util.HashMap<>();
    theoryQuestionIndex.put("TRUE_FALSE|35", wrongByNumber);

    String contentKey =
        "TRUE_FALSE|"
            + (String)
                ReflectionTestUtils.invokeMethod(
                    service, "normalizeQuestionContent", correctByContent.getContent());
    Map<String, Question> theoryContentIndex = new java.util.HashMap<>();
    theoryContentIndex.put(contentKey, correctByContent);

    int matched =
        (int)
            ReflectionTestUtils.invokeMethod(
                service,
                "applyMockAnswers",
                outcome,
                theoryQuestionIndex,
                theoryContentIndex,
                Map.of());

    assertThat(matched).isEqualTo(1);
    assertThat(correctByContent.isHasAnswer()).isTrue();
    assertThat(wrongByNumber.isHasAnswer()).isFalse();
  }
}
