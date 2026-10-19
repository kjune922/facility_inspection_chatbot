package com.kjune922.waterapi.inspection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionReportRepository extends JpaRepository<InspectionReport, Long> {

    Page<InspectionReport> findByProcessingStatus(ProcessingStatus processingStatus, Pageable pageable);

    long countByProcessingStatus(ProcessingStatus processingStatus);

    boolean existsByFacility_Id(Long facilityId);
    @org.springframework.data.jpa.repository.Query("""
            select i from InspectionReport i
            where i.facility.id = :facilityId
            and i.inspectionDate >= :fromDate and i.inspectionDate <= :toDate
            and lower(i.content) like lower(:pattern) escape '!'
            order by i.inspectionDate desc, i.id desc
            """)
    org.springframework.data.domain.Page<InspectionReport> searchRecords(
            @org.springframework.data.repository.query.Param("facilityId") Long facilityId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDate fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDate toDate,
            @org.springframework.data.repository.query.Param("pattern") String pattern,
            org.springframework.data.domain.Pageable pageable);
}

