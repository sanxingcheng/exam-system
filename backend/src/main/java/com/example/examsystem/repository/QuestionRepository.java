package com.example.examsystem.repository;

import com.example.examsystem.domain.Enums.QuestionType;
import com.example.examsystem.domain.Question;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {
  long countByQuestionBankId(Long questionBankId);

  List<Question> findByQuestionBankIdOrderByIdAsc(Long questionBankId);

  Page<Question> findByQuestionBankId(Long questionBankId, Pageable pageable);

  @Query(
      """
      select q from Question q
      where q.questionBank.id = :bankId
        and (:type is null or q.type = :type)
        and (:category is null or q.knowledgeArea = :category)
        and (:keyword is null or lower(q.content) like lower(concat('%', :keyword, '%')))
      """)
  Page<Question> searchByBank(
      @Param("bankId") Long bankId,
      @Param("type") QuestionType type,
      @Param("category") String category,
      @Param("keyword") String keyword,
      Pageable pageable);

  List<Question> findByQuestionBankIdAndHasAnswerTrue(Long questionBankId);

  @Query(
      """
      select q from Question q
      where q.questionBank.id = :bankId
        and q.hasAnswer = true
        and (:type is null or q.type = :type)
        and (:area is null or q.knowledgeArea = :area)
      order by q.id asc
      """)
  List<Question> searchAnswered(
      @Param("bankId") Long bankId,
      @Param("type") QuestionType type,
      @Param("area") String area);

  @Query("select distinct q.type from Question q where q.questionBank.id = :bankId and q.hasAnswer = true")
  List<QuestionType> findAnsweredTypes(@Param("bankId") Long bankId);

  @Query("select distinct q.knowledgeArea from Question q where q.questionBank.id = :bankId and q.hasAnswer = true and q.knowledgeArea is not null")
  List<String> findAnsweredAreas(@Param("bankId") Long bankId);
}
