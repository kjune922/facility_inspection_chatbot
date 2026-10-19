# WaterAPI Local Chat

WaterAPI main `f26e6f9`에서 파생한 시설 점검 프로젝트입니다.
수기로 등록한 점검일지를 로컬 DB에 저장하고, 시설별 챗봇에서 기록을 조회해 하나의 로컬 Ollama 모델로 요약·질문·상태 검토를 수행합니다.
이 패키지는 코드 구현본이며, 이 제작 환경에서는 네트워크 제한으로 Gradle/Spring 실행과 실제 모델 추론 검증을 완료하지 못했습니다.

## 실행 (Windows, JDK 17)

1. https://ollama.com/download/windows 에서 Ollama를 설치합니다.
2. PowerShell에서 모델을 한 번 내려받습니다.

```powershell
ollama pull qwen2.5:3b
```

이 모델만 사용합니다. 별도 임베딩 모델, 벡터 DB, 파인튜닝은 없습니다.
GPU가 없는 PC에서는 CPU로 실행할 수 있으나 응답 시간은 PC 성능과 입력 길이에 따라 다릅니다. 모델 파일은 ZIP에 포함하지 않았습니다.

3. 로컬 전용으로 실행하려면 Ollama를 트레이에서 종료한 뒤 PowerShell에서 실행합니다.

```powershell
$env:OLLAMA_NO_CLOUD="1"
ollama serve
```

이미 다른 Ollama 프로세스가 실행 중이면 포트 충돌이 발생합니다. 기존 프로세스를 종료하고 하나만 실행하세요.
모델 다운로드에는 인터넷이 필요하며, 설치·다운로드를 마친 뒤 점검 기록을 처리하는 호출은 로컬 `127.0.0.1:11434`로 전송됩니다.

4. 새 PowerShell을 열고 `WaterAPI-chat` 폴더에서 실행합니다.

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
```

처음 Gradle과 Java 라이브러리 다운로드에도 인터넷이 필요합니다.
브라우저 접속: http://localhost:8080
챗봇 바로 접속: http://localhost:8080/chat
예제 데이터가 필요한 새 DB라면 아래 명령으로 실행합니다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

## 사용하는 순서

1. 시설 관리에서 시설을 등록합니다.
2. 점검일지에서 시설, 날짜, 점검 내용과 조치 내역을 입력합니다.
3. 공통 메뉴의 AI 챗봇을 열고 시설을 선택합니다.
4. 예를들면 “최근 기록을 요약해줘”, “기록상 주의할 점은?”, “누수에 어떤 조치를 했어?”처럼 질문합니다.
5. 스프링이 해당 시설의 최근 기록을 조회한 뒤 로컬 모델에 전달합니다. 개별 점검 분석 버튼을 누를 필요가 없습니다.
6. 답변 밑의 참고 기록을 펼쳐 원문 복사본을 확인합니다.
7. TXT / CSV / HTML 버튼으로 해당 질문 및 답변 근거를 내려받습니다.
8. 대시보드로 이동해 전체 시설 수, 점검 수와 처리 단계별 현황을 확인합니다.

시설 단위의 대화 이력을 공유하는 개인용 로컬 앱입니다. 별도 사용자 계정이나 독립 채팅방은 없습니다.
대화 목록은 페이지당 10개이며 화면에서는 오래된 대화부터 보여줍니다. 이전 페이지도 조회할 수 있습니다.
같은 시설의 이전 질문·답변 4개를 맥락으로 전달하며 답변은 각 500자로 제한합니다.
다른 시설에 대한 질문은 먼저 시설 선택을 변경하세요. 질문 문장에서 시설 ID나 기간을 자동 해석해 검색하지는 않습니다.

## 조회 범위와 답변 의미

- 시설 ID로 엄격하게 조회합니다. 기본은 해당 시설의 최신 최대 10건입니다.
- 기간과 키워드 입력은 선택 사항입니다. 키워드는 점검 내용의 부분 일치 검색입니다.
- 자료가 길면 전체 점검 내용 6,000자 이내에 들어가는 최신 기록만 전달합니다. 개별 기록 중간은 자르지 않습니다.
- 화면에 조건 일치 건수와 실제 제공 건수를 표시합니다. 일부 기록을 바탕으로 전체 기간의 반복·발생 통계를 보장하지 않습니다.
- AI는 상태 질문에 안전/주의/위험/판단불가의 참고 의견과 근거 점검 번호를 작성하도록 요청받습니다. 모델 응답은 확정 판정이나 현재 실측 상태가 아닙니다.
- 업무의 처리 대기/진행/완료와 AI가 제시하는 위험 의견은 별개입니다. 대시보드의 처리 현황은 DB 값입니다.
- 근거가 없으면 모델을 호출하지 않고 “등록된 기록에서 확인할 수 없습니다”라고 저장합니다.
- Ollama 연결 실패·시간 초과·빈 답변은 오류로 안내하고 질문 입력을 유지합니다. 실패를 정상 답변으로 저장하지 않습니다.
- 답변에는 모델명, 생성 시각, 조회 범위, 실제 참고한 점검 원문 복사본을 저장합니다.
- 모델 답변의 근거 인용이 정확한지 자동으로 검증하는 기능은 없습니다. 펼친 근거 기록으로 확인하세요.
- AI가 DB에 직접 접속하거나 SQL을 생성하지 않습니다. Spring/JPA만 조회 및 저장을 담당합니다.

## DB 유지하면서 적용하기

이전 UI 업데이트와 달리 이번 변경은 Java와 Gradle 의존성을 포함합니다. CSS, HTML만 교체하면 동작하지 않습니다.

1. 기존 서버를 종료하고 기존 프로젝트 및 DB를 백업합니다.
2. 기존 프로젝트에서 `src/main/java`, `src/main/resources/templates`, `src/main/resources/static`, `build.gradle`을 이 프로젝트의 같은 경로 파일로 교체합니다.
3. 기존 `application.properties`의 DB 설정은 유지하고 아래 AI 설정만 추가합니다. MySQL 프로필 파일과 접속 계정도 기존 값을 유지하세요.

```properties
water.ai.base-url=http://127.0.0.1:11434
water.ai.model=${OLLAMA_MODEL:qwen2.5:3b}
```

4. 기존 Gradle 의존성에 별도 추가한 내용이 있다면 유지하면서 Lombok의 compileOnly/annotationProcessor 설정을 반영합니다.
5. 테스트 파일도 새 패키지를 추가해 확인한 후 서버를 재시작하고 브라우저에서 Ctrl+F5를 누릅니다.

기존 question_history 테이블에 nullable answer_model 열이 추가됩니다. 기존 update 설정에서 추가되며, 기존 질문은 “기존 기록 조회 · AI 미연결”로 표시됩니다. 분석 테이블과 기존 레코드는 삭제하지 않습니다.
H2 기본 파일은 실행 폴더 기준 `data/water-local`입니다. 새 폴더에서 실행하면 기존 폴더의 DB를 자동으로 찾아오지 않습니다.
MySQL을 사용했다면 동일한 DB URL·계정과 `mysql` 프로필을 사용하세요. 비밀번호는 파일에 공유하지 말고 환경변수로 전달할 수 있습니다.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/water_local?serverTimezone=Asia/Seoul"
$env:DB_USERNAME="water_local"
$env:DB_PASSWORD="본인의 비밀번호"
.\gradlew.bat bootRun --args="--spring.profiles.active=mysql"
```

## 주요 코드

- chat/ChatController: 대화 화면, 질문 접수, TXT·CSV·HTML 다운로드
- chat/ChatDataService: 시설·점검 조회, 대화 맥락 구성, 답변·근거 저장 (짧은 DB 트랜잭션)
- chat/ChatService: 데이터 조회 → 로컬 AI 호출 → 결과 저장 (추론 중 DB 트랜잭션 없음)
- chat/OllamaChatClient: 하나의 로컬 모델, `/api/chat`, 비스트리밍 응답, 연결·읽기 제한
- chat/ChatExportService: UTF-8 TXT, BOM 포함 CSV, HTML 이스케이프
- question/QuestionHistory, RecordEvidence: 기존 저장 구조에 모델 정보 추가, 근거 복사본 유지

## 참고 문서

- https://docs.ollama.com/api/chat
- https://docs.ollama.com/faq
- https://ollama.com/library/qwen2.5:3b
