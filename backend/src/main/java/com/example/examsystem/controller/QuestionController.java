package com.example.examsystem.controller;

import com.example.examsystem.common.ApiResponse;
import com.example.examsystem.domain.AppUser;
import com.example.examsystem.domain.QuestionBank;
import com.example.examsystem.service.BlockchainPdfImportService;
import com.example.examsystem.service.BlockchainPdfImportService.ImportResult;
import com.example.examsystem.service.QuestionService;
import com.example.examsystem.service.QuestionService.QuestionDto;
import com.example.examsystem.service.QuestionService.QuestionPage;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Question bank browsing and administrator maintenance APIs. */
@RestController
@RequestMapping("/api")
public class QuestionController {

  private final QuestionService questionService;
  private final BlockchainPdfImportService importService;

  public QuestionController(QuestionService questionService, BlockchainPdfImportService importService) {
    this.questionService = questionService;
    this.importService = importService;
  }

  @GetMapping("/question-banks")
  public ApiResponse<List<QuestionBank>> banks() {
    return ApiResponse.ok(questionService.listBanks());
  }

  @GetMapping("/question-banks/{bankId}/questions")
  public ApiResponse<QuestionPage> questions(
      @PathVariable Long bankId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(questionService.listQuestions(bankId, page, size));
  }

  @GetMapping("/questions/{questionId}")
  public ApiResponse<QuestionDto> question(@PathVariable Long questionId) {
    return ApiResponse.ok(questionService.getQuestion(questionId));
  }

  @PutMapping("/admin/questions/{questionId}/answer")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<QuestionDto> fillAnswer(
      @PathVariable Long questionId, @RequestBody AnswerRequest request) {
    return ApiResponse.ok(
        questionService.fillAnswer(questionId, request.answerText(), request.explanation()));
  }

  @PutMapping("/admin/questions/{questionId}/explanation")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<QuestionDto> updateExplanation(
      @PathVariable Long questionId, @RequestBody ExplanationRequest request) {
    return ApiResponse.ok(questionService.updateExplanation(questionId, request.explanation()));
  }

  @PostMapping("/admin/import/blockchain-pdf")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<ImportResult> importPdf() {
    return ApiResponse.ok(importService.importDefaultPdf());
  }

  @PostMapping("/questions/{questionId}/hard")
  public ApiResponse<Void> markHard(
      @AuthenticationPrincipal AppUser user,
      @PathVariable Long questionId,
      @RequestBody HardMarkRequest request) {
    questionService.markHard(user, questionId, request.hard());
    return ApiResponse.ok(null);
  }

  public record AnswerRequest(String answerText, String explanation) {}

  public record ExplanationRequest(String explanation) {}

  public record HardMarkRequest(boolean hard) {}
}
