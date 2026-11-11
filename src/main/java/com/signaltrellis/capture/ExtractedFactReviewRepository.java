package com.signaltrellis.capture;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExtractedFactReviewRepository extends JpaRepository<ExtractedFactReview, UUID> { }
