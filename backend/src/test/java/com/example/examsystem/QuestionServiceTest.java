package com.example.examsystem;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.examsystem.domain.Enums.AnswerSource;
import com.example.examsystem.domain.Enums.QuestionType;
import com.example.examsystem.domain.Question;
import com.example.examsystem.domain.QuestionAnswer;
import com.example.examsystem.domain.QuestionBank;
import com.example.examsystem.repository.QuestionAnswerRepository;
import com.example.examsystem.repository.QuestionBankRepository;
import com.example.examsystem.repository.QuestionRepository;
import com.example.examsystem.service.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class QuestionServiceTest {

  @Autowired private QuestionService questionService;
  @Autowired private QuestionBankRepository bankRepository;
  @Autowired private QuestionRepository questionRepository;
  @Autowired private QuestionAnswerRepository answerRepository;

  @Test
  void importedMockAnswerCanBeCorrectedByAdmin() {
    QuestionBank bank = new QuestionBank();
    bank.setName("locked-test-bank");
    bank.setBankType("TEST");
    bank = bankRepository.save(bank);

    Question question = new Question();
    question.setQuestionBank(bank);
    question.setType(QuestionType.SINGLE_CHOICE);
    question.setContent("锁定答案题");
    question.setHasAnswer(true);
    question.setAnswerLocked(true);
    question = questionRepository.save(question);

    QuestionAnswer answer = new QuestionAnswer();
    answer.setQuestion(question);
    answer.setAnswerText("A");
    answer.setSource(AnswerSource.IMPORTED_LOCKED);
    answer.setLocked(true);
    answerRepository.save(answer);

    Long questionId = question.getId();
    var updated = questionService.fillAnswer(questionId, "B", "解析");

    assertThat(updated.answer()).isEqualTo("B");
  }

  @Test
  void listQuestionsReturnsPagedResult() {
    QuestionBank bank = new QuestionBank();
    bank.setName("page-test-bank");
    bank.setBankType("TEST");
    bank = bankRepository.save(bank);

    for (int index = 0; index < 3; index++) {
      Question question = new Question();
      question.setQuestionBank(bank);
      question.setType(QuestionType.SINGLE_CHOICE);
      question.setContent("分页题目 " + index);
      questionRepository.save(question);
    }

    var page = questionService.listQuestions(bank.getId(), 0, 2, null, null, null);

    assertThat(page.items()).hasSize(2);
    assertThat(page.total()).isEqualTo(3);
  }

  @Test
  void listQuestionsFiltersByTypeAndKeyword() {
    QuestionBank bank = new QuestionBank();
    bank.setName("filter-test-bank");
    bank.setBankType("TEST");
    bank = bankRepository.save(bank);

    Question trueFalse = new Question();
    trueFalse.setQuestionBank(bank);
    trueFalse.setType(QuestionType.TRUE_FALSE);
    trueFalse.setContent("区块链数据不可篡改，判断对错");
    questionRepository.save(trueFalse);

    Question singleChoice = new Question();
    singleChoice.setQuestionBank(bank);
    singleChoice.setType(QuestionType.SINGLE_CHOICE);
    singleChoice.setContent("数据库索引是什么");
    questionRepository.save(singleChoice);

    var page = questionService.listQuestions(bank.getId(), 0, 20, "TRUE_FALSE", null, "不可篡改");

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).type()).isEqualTo("TRUE_FALSE");
  }

  @Test
  void listQuestionsFiltersBySmartCategory() {
    QuestionBank bank = new QuestionBank();
    bank.setName("category-test-bank");
    bank.setBankType("TEST");
    bank = bankRepository.save(bank);

    Question opsQuestion = new Question();
    opsQuestion.setQuestionBank(bank);
    opsQuestion.setType(QuestionType.TRUE_FALSE);
    opsQuestion.setContent("智能合约部署完成后地址由哈希计算得出");
    opsQuestion.setKnowledgeArea("区块链应用操作");
    questionRepository.save(opsQuestion);

    Question testQuestion = new Question();
    testQuestion.setQuestionBank(bank);
    testQuestion.setType(QuestionType.TRUE_FALSE);
    testQuestion.setContent("设计测试用例时需要覆盖核心链路");
    testQuestion.setKnowledgeArea("区块链测试");
    questionRepository.save(testQuestion);

    var page =
        questionService.listQuestions(
            bank.getId(), 0, 20, "TRUE_FALSE", "区块链应用操作", null);

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).knowledgeArea()).isEqualTo("区块链应用操作");
  }
}
