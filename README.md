# golmok (골목)

동네 기반 중고거래 서비스.

> **학습 목적 프로젝트입니다.** 같은 도메인 위에 REST와 SSR 두 가지 어댑터를 올려
> 비교하고, 가벼운 구현에서 시작해 측정으로 한계를 확인한 뒤 개선하는 과정을 기록합니다.

---

## 목적

이 프로젝트는 두 가지를 하려고 만들었습니다.

**1. 같은 도메인, 두 개의 표현 계층**

하나의 `core` 위에 REST API(`api`)와 서버 사이드 렌더링(`web`)을 각각 올립니다.
인증 방식(JWT vs 세션), 예외 번역, 조회 모델이 어떻게 달라지는지,
서비스 계층에 HTTP 관심사가 새어나가지 않는지를 두 어댑터를 통해 검증합니다.

**2. 측정 후 개선**

Kafka, Elasticsearch, Redis를 처음부터 넣지 않습니다.
단순한 구현을 먼저 만들고, mock 데이터 20만 건에서 실제로 느려지는 것을 측정한 뒤,
근거를 남기고 교체합니다. 측정 결과는 `docs/measurements/`에 before/after로 기록합니다.

---

## 스택

| 영역 | 기술 |
|---|---|
| 언어 · 런타임 | Java 21 |
| 프레임워크 | Spring Boot 4.0 |
| AI | Spring AI 2.0.1 (M5 예정) |
| DB | PostgreSQL |
| 마이그레이션 | Flyway |
| 영속성 | Spring Data JPA |
| 뷰 | Thymeleaf (`web` 모듈) |
| 프론트엔드 | React (별도 레포 `golmok-web`, M3 예정) |

---

## 모듈 구조

```
golmok/
├── core/    도메인 + 애플리케이션 서비스. HTTP를 모름. 실행되지 않음
├── api/     REST 어댑터. JSON, JWT
└── web/     SSR 어댑터. Thymeleaf, 세션, 폼 바인딩
```

`api`와 `web`은 서로를 모릅니다. 둘 다 `core`만 의존합니다.
의존 방향은 ArchUnit으로 강제합니다.

---

## 실행

### 1. DB 띄우기

```bash
docker compose up -d
```

### 2. 실행

**모듈별로 명령이 다릅니다.** `./gradlew bootRun`은 동작하지 않습니다.

```bash
./gradlew :api:bootRun     # REST API  → http://localhost:8080
./gradlew :web:bootRun     # SSR 화면  → http://localhost:8081
```

마이그레이션은 애플리케이션 기동 시 Flyway가 자동 실행합니다.

### 3. 개발 프로필

mock 데이터 규모를 프로필로 전환합니다.

| 프로필 | mock 상품 | 용도 |
|---|---|---|
| `dev` | 20건 | 평소 개발 (기동 빠름) |
| `dev-bulk` | 200,000건 | 성능 측정 |

```bash
./gradlew :web:bootRun --args='--spring.profiles.active=dev-bulk'
```

`dev-bulk`는 최초 기동 시 데이터 적재에 시간이 걸립니다.

### 4. 외부 연동 (M1)

M1에서는 실제로 발송하지 않고 **콘솔에 출력**합니다.

| 기능 | M1 구현 |
|---|---|
| SMS 인증번호 | 콘솔 출력 |
| 비밀번호 재설정 메일 | 콘솔 출력 |
| 이미지 저장 | 로컬 디스크 |

인증번호와 재설정 링크는 애플리케이션 로그에서 확인하세요.

---

## 문서

설계 결정과 명세는 `docs/`에 있습니다.

| 경로 | 내용 |
|---|---|
| `docs/spec/glossary.md` | 용어 사전 — **여기부터 읽으세요** |
| `docs/spec/domain.md` | 엔티티, 제약, 상태 전이 |
| `docs/spec/erd.md` | 관계도, 인덱스 |
| `docs/spec/screens.md` | 화면별 표시 항목 |
| `docs/spec/requirements.md` | 요구사항 전체 |
| `docs/milestones/` | 마일스톤별 범위와 완료 조건 |
| `docs/decisions/` | 설계 결정 기록 (ADR) |
| `docs/measurements/` | 성능 측정 before/after |

도메인 규칙은 이 README에 적지 않습니다. `docs/spec/`을 참조하세요.

---

## 진행 상황

| 마일스톤 | 내용 | 상태 |
|---|---|---|
| M1 | 기반 + 회원 + 상품 (REST + SSR) | 진행 중 |
| M2 | 성능 측정 → 개선 | 예정 |
| M3 | React SPA | 예정 |
| M4 | 채팅 · 거래 | 예정 |
| M5 | AI 상품 등록 | 예정 |

---

## AI 도구 사용

이 프로젝트는 개발 과정에서 AI 도구를 사용했으며, 그 사실을 명시합니다.

- **UI 디자인**: Claude Design으로 화면 디자인과 디자인 토큰을 만들었습니다.
  Thymeleaf 템플릿 이관과 이후 구현은 직접 했습니다.
- **코드 작성**: Claude Code와 함께 작업합니다.
  프로젝트 규칙은 `CLAUDE.md`에 정의되어 있습니다.
- **설계**: 도메인 모델과 마일스톤 범위는 대화를 통해 정리했고,
  결정 근거는 `docs/decisions/`에 남깁니다.

AI 제안을 그대로 채택하지 않고 검토·수정한 사례는 `docs/TROUBLE_SHOOTING.md`에 기록합니다.
