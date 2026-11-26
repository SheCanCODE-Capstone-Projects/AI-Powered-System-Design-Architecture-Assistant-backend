package com.archpilot.controller;

import com.archpilot.dto.ReviewFindingDto;
import com.archpilot.service.AiReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ReviewController {

    private final AiReviewService aiReviewService;

    public ReviewController(AiReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    @PostMapping("/{projectId}/reviews")
    public ResponseEntity<List<ReviewFindingDto>> runReview(@PathVariable Long projectId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiReviewService.runReview(projectId));
    }

    @GetMapping("/{projectId}/reviews")
    public List<ReviewFindingDto> getReviews(@PathVariable Long projectId) {
        return aiReviewService.getReviews(projectId);
    }
}