# MCP 서버/클라이언트 양면 지원 검토 및 설계 메모

이 문서는 기존 `mcp-server-mod`가 클라이언트 모드 전용으로 구현된 상황에서 서버 모드 지원을 추가하기 위한 검토 내용과 설계안을 정리한 것이다. GitHub 이슈에서 서버에서도 모드를 활용하고 싶다는 의견이 제기되었으며, 이에 따라 현재 구조를 분석하고 서버 모드 지원의 필요성과 가능성을 평가하였다.

## 1. 이슈 배경

- 1번 이슈에서 사용자는 MCP 서버 모드를 싱글 플레이 환경 뿐 아니라 멀티 플레이 서버에서도 사용하고 싶다는 의견을 남겼다. 클라이언트 내에 HTTP 서버를 띄우는 기존 구조는 통합 서버에서는 문제없지만 전용 서버에서 사용하기 어렵다는 지적이다. 저장소 소유자는 해당 제안을 긍정적으로 검토하겠다고 답변했다.

## 2. 현재 모드의 구조와 제약

- **클라이언트 전용 구현**: `MCPServerModClient`는 `ClientModInitializer`를 통해 클라이언트 초기화 시 HTTP MCP 서버를 시작하도록 되어 있다. 이는 패브릭 클라이언트 환경(`MinecraftClient`)에만 의존한다. `design.md`에서도 모드가 "클라이언트 모드에서 MCP 서버를 구현"한다고 명시한다.
- **도구별 클라이언트 의존성**:
  - `CommandExecutor`는 `MinecraftClient.getInstance().player.getNetworkHandler().sendChatCommand()`를 호출해 채팅 명령을 실행한다.
  - `PlayerInfoProvider`와 `BlockScanner`는 `MinecraftClient`를 통해 월드와 플레이어 정보를 가져온다.
  - `ScreenshotUtils`는 클라이언트 렌더러(`ScreenshotRecorder`)를 사용해 화면을 캡처한다.
  이러한 클래스는 전용 서버(`MinecraftServer`)에서는 존재하지 않는 클라이언트 API에 의존하기 때문에 서버 환경에서 그대로 사용할 수 없다.

## 3. 서버 모드에서 가능한 기능과 의미

서버 전용 환경에서는 렌더링이 없지만 명령 실행과 월드/플레이어 데이터 접근은 가능하다. MCP 도구별 서버에서의 지원 가능 여부를 정리하면 다음과 같다.

| 도구명 | 서버에서 실행 가능성 | 서버에서의 활용 의미 |
| --- | --- | --- |
| **execute_commands** | 가능. `MinecraftServer#getCommandManager().executeWithPrefix(server.getCommandSource(), command)`를 사용하면 문자열 명령을 실행할 수 있다. | AI가 서버에서 직접 명령을 실행해 NPC 건축이나 월드 변경을 수행할 수 있다. |
| **get_player_info** | 가능. `ServerPlayerEntity`를 통해 특정 플레이어나 첫 번째 접속자의 위치, 체력, 인벤토리 등을 읽을 수 있다. | AI가 플레이어/봇의 상태를 알고 다음 작업을 계획하는 데 사용된다. |
| **get_blocks_in_area** | 가능. `ServerWorld`에서 지정한 영역을 순회하며 공기가 아닌 블록을 수집하면 된다. | 구조물 건설 상태 확인, 자원 조사 등에 활용된다. |
| **take_screenshot** | 불가능. 전용 서버는 화면을 렌더링하지 않으므로 스크린샷을 찍을 수 없다. | 서버 모드에서는 비활성화하거나 오류를 반환해야 한다. |

서버 모드 지원을 추가하면 Mindcraft와 같이 서버에서 LLM이 NPC를 제어하는 프로젝트에서 즉각적인 월드 조작과 플레이어 정보를 제공할 수 있어 의미가 크다.

## 4. 서버 모드 지원을 위한 구조 개선안

1. **공통 코드 분리**
   - 환경에 의존하지 않는 `MCPProtocol`, `MCPConfig` 등은 `common` 패키지로 이동하여 클라이언트와 서버에서 공유한다.

2. **서버 초기화 클래스 추가**
   - `ModInitializer` 대신 `DedicatedServerModInitializer`를 구현하는 `MCPServerModServer` 클래스를 추가한다. 서버 시작 시 HTTP MCP 서버를 인스턴스화하고 서버 종료 시 종료하도록 한다.
   - `fabric.mod.json`의 `entrypoints`에 `"server": ["...MCPServerModServer"]` 항목을 추가하고 `environment`를 `"*"`로 설정하여 양쪽 모두에서 로드되도록 한다.

3. **도구 구현체 분리와 추상화**
   - `CommandExecutor`, `PlayerInfoProvider`, `BlockScanner`, `ScreenshotUtils`에 대한 인터페이스를 정의하고, 클라이언트용과 서버용 구현을 별도로 만든다.
   - **ServerCommandExecutor**: `MinecraftServer`의 `CommandManager`를 사용해 명령을 실행하고 결과를 수집한다.
   - **ServerPlayerInfoProvider**: 플레이어 ID/UUID를 받아 `ServerPlayerEntity`를 찾고 위치, 시야, 체력/허기, 인벤토리 등을 JSON 형태로 반환한다.
   - **ServerBlockScanner**: `ServerWorld`를 순회해 공기가 아닌 블록을 수집하고 기존 압축 로직을 적용한다.
   - **ServerScreenshotUtils**: 구현하지 않고 호출 시 오류를 반환한다.

4. **HTTP 서버 리팩터링**
   - `HTTPMCPServer`는 생성자에서 `CommandExecutor`, `PlayerInfoProvider`, `BlockScanner` 인스턴스를 주입받도록 수정하고, 환경에 따라 클라이언트나 서버 구현을 전달한다.
   - `MCPProtocol.getToolsListResponse()`에서 서버 모드일 때는 `take_screenshot` 도구를 목록에서 제거한다.

5. **설정 파일 분리와 포트 구분**
   - `config/mcp-client.json`과 `config/mcp-server.json`을 별도로 두어 포트 및 허용 명령, 최대 영역 크기 등 설정을 분리한다.
   - 기본 포트를 클라이언트와 서버에서 다르게 지정해 싱글 플레이 환경에서도 두 MCP 서버를 구분할 수 있도록 한다. 예를 들어 클라이언트는 `8080`, 서버는 `8081` 등으로 설정할 수 있다.

6. **싱글 플레이(통합 서버) 처리**
   - 클라이언트 모드에서 `MinecraftClient.isIntegratedServerRunning()`을 체크하여 통합 서버가 실행 중인 경우 서버용 MCP 서버를 시작하지 않도록 한다. 통합 서버에서는 클라이언트 MCP 서버만 활성화되어 스크린샷 도구를 사용할 수 있다.

7. **안전성 및 권한 검사**
   - 명령 실행 시 서버 설정에 명시된 허용 명령만 실행하며, 서버의 권한 레벨도 고려한다.
   - 여러 플레이어가 접속한 서버에서 실행할 경우 API 호출에 플레이어 UUID를 명시하도록 요구하거나, LLM 제어용 NPC 플레이어를 기본 대상으로 삼도록 설계할 수 있다.

## 5. 결론과 향후 과제

서버 모드 지원은 기술적으로 가능하며 서버 기반 AI 제어 프로젝트에서 유용하다. 단, 스크린샷 기능은 서버에서 구현할 수 없으므로 비활성화해야 하고, 서버 환경 특성상 플레이어 지정과 권한 관리를 면밀히 설계해야 한다. 위 설계안을 바탕으로 클라이언트와 서버 코드를 분리·추상화하고 설정 체계를 개선하면 MCP 서버 모드를 양면 지원으로 확장할 수 있다.
