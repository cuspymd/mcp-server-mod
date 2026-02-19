# Minecraft MCP Client 모드 설계 문서

## 프로젝트 개요

**프로젝트명**: Minecraft MCP Client
**목표**: Fabric 1.21.4 기반 클라이언트 모드로 MCP 서버를 구현하여 LLM이 마인크래프트 명령어를 실행할 수 있도록 함
**버전**: Fabric 0.16.14, Minecraft 1.21.4

---

## 아키텍처 구조

```
[Claude/LLM] ←HTTP/JSON→ [MCP Server] ←→ [Fabric Mod] ←→ [Minecraft Client]
                              ↑              ↑              ↑
                          HTTP 8080      Mod Events    Game World
```

### 주요 컴포넌트

1. **MCP HTTP Server**: HTTP 서버로 LLM과 통신
2. **Command Executor**: 마인크래프트 명령어 실행 엔진
3. **Safety Manager**: 안전성 검증 시스템
4. **Response Handler**: 실행 결과 피드백 시스템

---

## 기술 스택

### 개발 환경
- **Java**: OpenJDK 21+
- **Fabric**: 0.16.14
- **Minecraft**: 1.21.4
- **Fabric API**: 0.110.5+1.21.4

### 주요 라이브러리
- **HTTP Server**: Java NIO (내장)
- **JSON 처리**: Gson (Minecraft 내장)
- **비동기 처리**: CompletableFuture
- **로깅**: SLF4J (Fabric 제공)

---

## 프로젝트 구조

```
minecraft-mcp-client/
├── src/main/java/com/mcpmods/client/
│   ├── MinecraftMCPClient.java              # 메인 모드 클래스
│   ├── server/
│   │   ├── MCPServer.java                   # HTTP MCP 서버
│   │   ├── MCPRequestHandler.java           # MCP 요청 처리
│   │   └── MCPProtocol.java                 # MCP 프로토콜 구현
│   ├── command/
│   │   ├── CommandExecutor.java             # 명령어 실행기
│   │   ├── SafetyValidator.java             # 안전성 검증
│   │   └── CommandResult.java               # 실행 결과 모델
│   ├── utils/
│   │   ├── CoordinateUtils.java             # 좌표 유틸리티
│   │   └── CommandParser.java               # 명령어 파싱
│   └── config/
│       └── MCPConfig.java                   # 설정 관리
├── src/main/resources/
│   ├── fabric.mod.json                      # 모드 메타데이터
│   └── mcp_client.mixins.json               # Mixin 설정
└── build.gradle                             # 빌드 설정
```

---

## MCP 프로토콜 사양

### 지원하는 MCP 메시지

**초기화**
- `initialize` - MCP 세션 초기화
- `ping` - 연결 상태 확인

**도구 관리**
- `tools/list` - 사용 가능한 도구 목록 반환
- `tools/call` - 도구 실행

**서버 정보**
- `server/info` - 서버 메타데이터 반환

### 제공 도구: execute_commands

**Tool 스펙:**
```json
{
  "name": "execute_commands",
  "description": "Execute one or more Minecraft commands sequentially",
  "inputSchema": {
    "type": "object",
    "properties": {
      "commands": {
        "type": "array",
        "items": {
          "type": "string"
        },
        "description": "Array of Minecraft commands to execute (without leading slash)",
        "minItems": 1
      },
      "validate_safety": {
        "type": "boolean", 
        "description": "Whether to validate command safety (default: true)",
        "default": true
      }
    },
    "required": ["commands"]
  }
}
```

**사용 예시:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "execute_commands",
    "arguments": {
      "commands": [
        "fill ~ ~ ~ ~10 ~5 ~8 oak_planks",
        "setblock ~5 ~6 ~4 oak_door",
        "summon villager ~5 ~1 ~4"
      ],
      "validate_safety": true
    }
  }
}
```

---

## 응답 형식 사양

### 성공 응답
```json
{
  "isError": false,
  "content": [
    {
      "type": "text",
      "text": "{\"totalCommands\":2,\"acceptedCount\":2,\"appliedCount\":1,\"failedCount\":1,\"results\":[{\"index\":0,\"command\":\"fill 0 64 0 1 64 1 stone\",\"status\":\"applied\",\"accepted\":true,\"applied\":true,\"summary\":\"Successfully filled 4 block(s)\",\"chatMessages\":[\"Successfully filled 4 block(s)\"]},{\"index\":1,\"command\":\"enchant @s minecraft:unbreaking 1\",\"status\":\"rejected_by_game\",\"accepted\":true,\"applied\":false,\"summary\":\"Carrot cannot support that enchantment\",\"chatMessages\":[\"Carrot cannot support that enchantment\"]}],\"chatMessages\":[\"Successfully filled 4 block(s)\",\"Carrot cannot support that enchantment\"]}"
    }
  ]
}
```

### 오류 응답
```json
{
  "isError": true,
  "content": [
    {
      "type": "text", 
      "text": "Command execution failed at command 2: Invalid block type 'invalid_block' in command 'setblock ~ ~ ~ invalid_block'"
    }
  ],
  "_meta": {
    "failed_command_index": 1,
    "failed_command": "setblock ~ ~ ~ invalid_block",
    "total_commands": 3,
    "executed_commands": 1
  }
}
```

### 안전성 검증 실패
```json
{
  "isError": true,
  "content": [
    {
      "type": "text",
      "text": "Command rejected by safety validator at command 3: Potentially destructive pattern detected in 'kill @a'"
    }
  ],
  "_meta": {
    "failed_command_index": 2,
    "failed_command": "kill @a",
    "total_commands": 5,
    "executed_commands": 0
  }
}
```

---

## 안전성 검증 사양

### 허용 명령어 목록
- `fill` - 블록 채우기
- `clone` - 구조물 복사
- `setblock` - 단일 블록 설정  
- `summon` - 엔티티 소환
- `tp` / `teleport` - 텔레포트
- `give` - 아이템 지급
- `gamemode` - 게임모드 변경
- `effect` - 효과 부여
- `enchant` - 인챈트 부여
- `weather` - 날씨 변경
- `time` - 시간 설정
- `say` / `tell` / `title` - 메시지 출력

### 거부 패턴
- `/kill @a` 또는 `/kill @e` - 대량 엔티티 제거
- 50x50x50 블록을 초과하는 `fill` 영역
- `{Count:100}` 이상의 대량 아이템/엔티티 생성
- `@a`를 대상으로 하는 `gamemode creative` 명령


---

## 명령어 실행 사양

### 실행 방식
1. **클라이언트 명령어**: 클라이언트에서 직접 실행 (`/gamemode`, `/effect @s` 등)
2. **서버 명령어**: 서버로 패킷 전송하여 실행 (`/fill`, `/clone` 등)
3. **채팅 명령어**: 채팅으로 전송하여 실행 (일반적인 방식)

### 실행 결과 수집
- 명령어 실행 성공/실패 여부
- 영향받은 블록 수 (해당하는 경우)
- 생성/제거된 엔티티 수 (해당하는 경우)  
- 게임에서 출력된 피드백 메시지
- 실행 시간 (성능 모니터링용)

### 비동기 처리
- 모든 명령어 실행은 비동기로 처리
- 메인 스레드 블로킹 방지
- 긴 작업의 경우 진행 상황 피드백

---

## HTTP 서버 사양

### 서버 설정
- **포트**: 8080 (설정 가능)
- **프로토콜**: HTTP/1.1
- **Content-Type**: application/json
- **인코딩**: UTF-8

### 엔드포인트

**POST /mcp/initialize**
- MCP 세션 초기화
- 서버 정보 및 capabilities 반환

**POST /mcp/ping**
- 연결 상태 확인
- 빠른 응답으로 연결 유지

**POST /mcp/tools/list**
- 사용 가능한 도구 목록 반환
- execute_commands 도구 정보 포함

**POST /mcp/tools/call**
- 도구 실행 요청 처리
- 명령어 검증 및 실행

### 요청/응답 헤더
```
Content-Type: application/json
Accept: application/json
User-Agent: MCP-Client/1.0
```

---

## 설정 파일 사양

### config/mcp-client.json
```json
{
  "server": {
    "port": 8080,
    "host": "localhost",
    "enable_safety": true,
    "max_area_size": 50,
    "allowed_commands": ["fill", "clone", "setblock", "summon", "tp", "give"],
    "request_timeout_ms": 30000
  },
  "client": {
    "auto_start": true,
    "show_notifications": true,
    "log_level": "INFO",
    "log_commands": false
  },
  "safety": {
    "max_entities_per_command": 10,
    "max_blocks_per_command": 125000,
    "block_creative_for_all": true,
    "require_op_for_admin_commands": true
  }
}
```
