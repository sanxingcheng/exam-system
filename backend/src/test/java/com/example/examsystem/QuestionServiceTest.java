package com.example.examsystem;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
  void lockedImportedAnswerCannotBeChanged() {
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
    assertThatThrownBy(() -> questionService.fillAnswer(questionId, "B", "解析"))
        .hasMessageContaining("禁止修改");
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

    var page = questionService.listQuestions(bank.getId(), 0, 2);

    assertThat(page.items()).hasSize(2);
    assertThat(page.total()).isEqualTo(3);
  }
}
