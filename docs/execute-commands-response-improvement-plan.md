# execute_commands 응답 개선 상세 수정 계획

## 1. 배경과 목표
현재 `execute_commands` 응답은 다음 문제가 있다.
1. `results[].success`가 실제 게임 적용 성공을 의미하지 못한다.
2. `chatMessages`가 전역 배열이라 명령별 매핑이 불가능하다.
3. `message`가 대부분 `Command executed`로 고정되어 진단성과 자동화 활용성이 낮다.

목표는 응답 스키마를 확장해 실제 적용 결과와 진단 정보를 명령 단위로 제공하는 것이다.

## 2. 범위
대상 범위는 `execute_commands` 도구 응답에 한정한다.
1. 입력 스키마: `validate_safety` 등 기존 입력은 유지.
2. 출력 스키마: 명령 단위 적용 결과와 진단 정보 중심으로 확장.
3. 안전 검증 실패 응답 형식은 기존 구조를 유지하되 메타 정보 일관성만 개선.

## 3. 설계 원칙
1. 의미 분리: 전송 성공과 게임 적용 성공을 별도 필드로 분리한다.
2. 기계 친화성: 숫자/상태를 구조화 필드로 제공한다.
3. 명령 단위 진단성: 각 명령의 요약과 메시지를 독립적으로 제공한다.

## 4. 목표 응답 스키마 (제안)

```json
{
  "totalCommands": 2,
  "acceptedCount": 2,
  "appliedCount": 1,
  "failedCount": 1,
  "results": [
    {
      "index": 0,
      "command": "fill -40 200 -40 -39 200 -39 minecraft:glass",
      "status": "applied",
      "accepted": true,
      "applied": true,
      "executionTimeMs": 51,
      "summary": "Filled 4 blocks",
      "chatMessages": [
        "Successfully filled 4 block(s)"
      ]
    },
    {
      "index": 1,
      "command": "enchant @s minecraft:unbreaking 1",
      "status": "rejected_by_game",
      "accepted": true,
      "applied": false,
      "executionTimeMs": 50,
      "summary": "Carrot cannot support that enchantment",
      "chatMessages": [
        "Carrot cannot support that enchantment"
      ]
    }
  ],
  "chatMessages": [
    "Successfully filled 4 block(s)",
    "Carrot cannot support that enchantment"
  ],
  "hint": "Use get_blocks_in_area to verify the built structure and fix any issues."
}
```

상태값(`status`) 권장 집합은 `applied`, `rejected_by_game`, `execution_error`, `timed_out`, `rejected_by_safety`로 정의한다.

## 5. 구현 상세 계획

### 5.1 CommandResult 모델 확장
파일: `src/client/java/cuspymd/mcp/mod/command/CommandResult.java`

1. `transportAccepted` 필드 추가.
2. `applied` 필드 추가.
3. `status` 필드 추가.
4. `summary` 필드 추가.
5. `chatMessages` 필드 추가.
6. 기존 `success`/`message` 의존 코드를 제거하고 `status`/`applied`/`summary`를 단일 기준으로 사용.

## 5.2 executeOneCommand 결과 수집 개선
파일: `src/client/java/cuspymd/mcp/mod/command/CommandExecutor.java`

1. `sendChatCommand` 호출 성공 여부를 `transportAccepted`에 반영.
2. 명령 실행 직후 글로벌 수집 대신 명령 단위 수집 윈도우를 적용.
3. 명령별 메시지 파싱기를 도입해 `applied`와 `summary`를 결정.
4. 파싱 실패 시 안전한 기본값을 적용한다.
5. 기본 규칙은 다음과 같다.

| 조건 | applied | status |
|---|---|---|
| 실패 표현 포함 (`cannot`, `failed`, `no entity was found`, `unknown`) | false | rejected_by_game |
| 성공 표현 포함 (`successfully`, `teleported`, `summoned`, `given`, `set the weather`) | true | applied |
| 메시지 없음 + 전송 성공 | null 또는 true(정책 선택) | applied 또는 unknown |
| 예외 발생 | false | execution_error |

정책 결정안: 초기 구현은 메시지 없음이면 `applied`를 `null`로 두고 상태를 `unknown`으로 둔다.

## 5.3 응답 조립 로직 개선
파일: `src/client/java/cuspymd/mcp/mod/command/CommandExecutor.java`

1. `successCount`를 `acceptedCount`와 `appliedCount`로 분리.
2. `results[]`에 `status`, `accepted`, `applied`, `summary`, `chatMessages`를 추가.
3. `results[]`의 핵심 판단 기준을 `status`, `applied`, `summary`로 통일한다.
4. 전역 `chatMessages`는 디버깅/요약 용도로만 제공한다.
5. 응답 구조는 핵심 필드(`status`, `accepted`, `applied`, `summary`)를 기준으로 단순하게 유지한다.

## 5.4 MCP 도구 설명 동기화
파일: `src/main/java/cuspymd/mcp/mod/server/MCPProtocol.java`

1. `execute_commands` description에 응답 구조 확장 항목을 반영.
2. `commands` description의 `per-command results` 문구를 상태 분리 기준으로 구체화.

## 5.5 문서 갱신
파일: `README.md`, 필요 시 `design.md`

1. `execute_commands` 응답 예시를 확장 스키마 기준으로 교체.
2. `status`, `applied`, `summary` 중심의 파싱 방법을 문서화.
3. 전역 `chatMessages`와 명령별 `chatMessages`의 용도 차이를 문서화.

## 6. 테스트 계획

### 6.1 단위 테스트 추가
파일(신규): `src/test/java/cuspymd/mcp/mod/command/CommandExecutorResponseSchemaTest.java`

1. 명령 성공 메시지 입력 시 `applied=true` 판정 검증.
2. 명령 실패 메시지 입력 시 `applied=false` 판정 검증.
3. `results[].chatMessages` 귀속 검증.
4. 집계 필드 `acceptedCount`, `appliedCount`, `failedCount` 계산 검증.

구현 편의를 위해 메시지 판정/요약 로직은 별도 클래스로 분리 권장.
파일(신규): `src/client/java/cuspymd/mcp/mod/command/CommandOutcomeAnalyzer.java`

### 6.2 기존 테스트 보강
파일: `src/test/java/cuspymd/mcp/mod/server/MCPProtocolTest.java`

1. `execute_commands` description에 신규 응답 안내 문구 포함 여부 검증.
2. 허용 명령 필터 테스트는 그대로 유지.

## 7. 릴리즈 전략
1. 1차 릴리즈: 신규 스키마(`status`, `accepted`, `applied`, `summary`, `chatMessages`)를 기본 응답으로 적용.
2. 2차 릴리즈: README와 릴리즈 노트에 신규 필드 기반 파싱 예시를 제공.
3. 3차 릴리즈: 메시지 판정 규칙 튜닝과 상태값 집합 안정화.

## 8. 리스크와 대응
1. 메시지 파싱 오판 가능성.
대응: `applied=null`, `status=unknown` 경로를 제공해 오탐보다 보수적으로 처리.

2. 서버/클라이언트 메시지 로케일 차이.
대응: 문자열 규칙을 최소화하고, 규칙 기반 + 명령 종류별 보조 규칙으로 개선.

3. 성능 저하.
대응: 명령별 수집 대기 시간을 제한하고 전체 타임아웃을 기존 값 이내로 유지.

## 9. 작업 순서 제안
1. `CommandOutcomeAnalyzer` 추가.
2. `CommandResult` 확장.
3. `CommandExecutor` 결과 수집/응답 조립 교체.
4. `MCPProtocol` 설명 갱신.
5. `README.md`/`design.md` 갱신.
6. 테스트 추가 및 회귀 확인.
