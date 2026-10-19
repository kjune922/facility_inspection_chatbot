package com.kjune922.waterapi.question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QuestionHistoryRepository extends JpaRepository<QuestionHistory, Long> {
    Page<QuestionHistory> findByFacility_IdOrderByCreatedAtDescIdDesc(Long facilityId, Pageable pageable);
    Optional<QuestionHistory> findByIdAndFacility_Id(Long id, Long facilityId);
    boolean existsByFacility_Id(Long facilityId);
}
