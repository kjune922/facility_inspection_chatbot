package com.kjune922.waterapi.config;

import com.kjune922.waterapi.facility.*;
import com.kjune922.waterapi.inspection.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoData implements ApplicationRunner {
    private final FacilityRepository facilities;
    private final InspectionReportRepository inspections;
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (facilities.count() != 0) { return; }
        Facility pump = facilities.save(new Facility("[예시] 1번 펌프", FacilityType.PUMP, "부산 가상 정수장"));
        Facility other = facilities.save(new Facility("[예시] 2번 펌프", FacilityType.PUMP, "부산 가상 정수장"));
        for (int i = 1; i <= 12; i++) {
            String content = "정상 작동 확인. 특이사항 없음.";
            if (i % 3 == 0) { content = "연결부 누수 확인. 패킹 상태 재점검 필요. 점검 회차 " + i; }
            InspectionReport report = new InspectionReport(pump, content, LocalDate.of(2026, 10, i));
            if (i == 3) { report.changeProcessingStatus(ProcessingStatus.COMPLETED); }
            inspections.save(report);
        }
        inspections.save(new InspectionReport(other, "2번 시설에서 누수 확인", LocalDate.of(2026, 10, 12)));
    }
}
