package com.example.examsystem;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.examsystem.domain.AppUser;
import com.example.examsystem.domain.Enums.AnswerSource;
import com.example.examsystem.domain.Enums.QuestionType;
import com.example.examsystem.domain.Enums.UserRole;
import com.example.examsystem.domain.Question;
import com.example.examsystem.domain.QuestionAnswer;
import com.example.examsystem.domain.QuestionBank;
import com.example.examsystem.repository.AppUserRepository;
import com.example.examsystem.repository.QuestionAnswerRepository;
import com.example.examsystem.repository.QuestionBankRepository;
import com.example.examsystem.repository.QuestionRepository;
import com.example.examsystem.repository.WrongQuestionRepository;
import com.example.examsystem.service.ExamService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class ExamServiceTest {

  @Autowired private ExamService examService;
  @Autowired private QuestionBankRepository bankRepository;
  @Autowired private QuestionRepository questionRepository;
  @Autowired private QuestionAnswerRepository answerRepository;
  @Autowired private AppUserRepository userRepository;
  @Autowired private WrongQuestionRepository wrongQuestionRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void submitAttemptRecordsScoreAndWrongQuestion() {
    AppUser user = new AppUser();
    user.setUsername("exam-user");
    user.setRole(UserRole.USER);
    user.setPasswordHash(passwordEncoder.encode("Password@123"));
    user = userRepository.save(user);

    QuestionBank bank = new QuestionBank();
    bank.setName("exam-test-bank");
    bank.setBankType("TEST");
    bank = bankRepository.save(bank);

    Question question = new Question();
    question.setQuestionBank(bank);
    question.setType(QuestionType.SINGLE_CHOICE);
    question.setContent("1+1=?");
    question.setKnowledgeArea("数学");
    question.setHasAnswer(true);
    question = questionRepository.save(question);

    QuestionAnswer answer = new QuestionAnswer();
    answer.setQuestion(question);
    answer.setAnswerText("A");
    answer.setSource(AnswerSource.ADMIN_FILLED);
    answer.setLocked(false);
    answerRepository.save(answer);

    var attempt = examService.createAttempt(user, bank.getId(), 1);
    var submitted = examService.submit(user, attempt.id(), Map.of(attempt.questions().get(0).id(), "B"));

    assertThat(submitted.score()).isZero();
    assertThat(wrongQuestionRepository.findByQuestionBankIdAndQuestionIdAndUserId(
            bank.getId(), question.getId(), user.getId()))
        .isPresent();
  }
}
