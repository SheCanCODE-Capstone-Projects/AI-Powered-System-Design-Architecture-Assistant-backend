package com.archpilot.repository;

import com.archpilot.model.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, Long> {

    List<ReviewFinding> findByProjectIdOrderByReviewedAtDesc(Long projectId);
}