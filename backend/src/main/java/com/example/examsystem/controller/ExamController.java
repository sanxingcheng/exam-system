package com.example.examsystem.controller;

import com.example.examsystem.common.ApiResponse;
import com.example.examsystem.domain.AppUser;
import com.example.examsystem.service.ExamService;
import com.example.examsystem.service.ExamService.AttemptDto;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** APIs for generating papers, submitting answers and viewing scores. */
@RestController
@RequestMapping("/api")
public class ExamController {

  private final ExamService examService;

  public ExamController(ExamService examService) {
    this.examService = examService;
  }

  @PostMapping("/question-banks/{bankId}/exam-attempts")
  public ApiResponse<AttemptDto> createAttempt(
      @AuthenticationPrincipal AppUser user,
      @PathVariable Long bankId,
      @RequestBody CreateAttemptRequest request) {
    return ApiResponse.ok(examService.createAttempt(user, bankId, request.questionCount()));
  }

  @PostMapping("/exam-attempts/{attemptId}/submit")
  public ApiResponse<AttemptDto> submit(
      @AuthenticationPrincipal AppUser user,
      @PathVariable Long attemptId,
      @RequestBody SubmitAttemptRequest request) {
    return ApiResponse.ok(examService.submit(user, attemptId, request.answers()));
  }

  @GetMapping("/question-banks/{bankId}/exam-attempts")
  public ApiResponse<List<AttemptDto>> attempts(
      @AuthenticationPrincipal AppUser user, @PathVariable Long bankId) {
    return ApiResponse.ok(examService.listAttempts(user, bankId));
  }

  public record CreateAttemptRequest(int questionCount) {}

  public record SubmitAttemptRequest(Map<Long, String> answers) {}
}
