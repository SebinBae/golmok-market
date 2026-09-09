# golmok

동네 기반 중고거래 서비스. **학습 목적 프로젝트.**

같은 도메인 위에 REST와 SSR 두 어댑터를 올려 비교하고,
가벼운 구현에서 시작해 측정으로 한계를 확인한 뒤 개선하는 것이 목표다.

---

## 먼저 읽을 것

작업 전에 관련 문서를 읽는다. 추측으로 채우지 않는다.

| 문서 | 내용 |
|---|---|
| `docs/spec/glossary.md` | **용어 사전. 여기 있는 말만 쓴다** |
| `docs/spec/domain.md` | 엔티티, 필드, 제약, 상태 전이 |
| `docs/spec/erd.md` | 관계도, 인덱스 |
| `docs/spec/screens.md` | 화면별 표시 항목, 이동 경로 |
| `docs/spec/requirements.md` | 요구사항 전체 (마일스톤 무관) |
| `docs/milestones/M1.md` | **지금 작업 중인 범위와 완료 조건** |
| `docs/decisions/` | 설계 결정 기록 (ADR) |

---

## 상위 지침 예외

아래는 `~/.claude/CLAUDE.md`의 규칙 중 **이 프로젝트에 적용하지 않는 것**이다.

- **N+1 점검**: M1에서는 하지 않는다. 의도적으로 발생시켜 측정한 뒤 개선한다.
  `fetch join`, `@EntityGraph`, `@BatchSize`를 **먼저 제안하지 말 것.**
  → `docs/decisions/0003-m1-allows-n-plus-one.md`
  → M2에서 측정이 끝나면 이 예외를 해제한다

---

## 스택

- Java 21, Spring Boot 4.0
- Spring AI 2.0.1 — **LangGraph, FastAPI를 쓰지 않는다** (`decisions/0002`)
- PostgreSQL, Flyway
- Spring Data JPA — **QueryDSL을 쓰지 않는다.** 동적 쿼리가 실제로 필요해지면 그때 도입
- Thymeleaf (`web` 모듈)
- 프론트엔드 React는 별도 레포(`golmok-web`), M3부터

---

## 모듈 구조

```
core/   도메인 + 애플리케이션 서비스. HTTP를 모름. jar만 생성
api/    REST 어댑터. JSON, JWT. bootJar (8080)
web/    SSR 어댑터. Thymeleaf, 세션, 폼. bootJar (8081)
```

- `api`와 `web`은 **서로를 모른다.** 둘 다 `core`만 의존한다
- 패키지는 `com.golmok`으로 통일 (컴포넌트 스캔)
- 의존 방향은 ArchUnit으로 강제한다
- **모듈 경계가 계층형 아키텍처 규칙보다 우선한다**

실행:
```bash
./gradlew :api:bootRun     # REST
./gradlew :web:bootRun     # SSR
```

---

## 응답 스타일 (학습 프로젝트)

- 코드를 작성하거나 수정하면 **왜 그렇게 했는지** 설명한다.
  코드에 그대로 보이는 내용은 다시 읽어주지 않는다.
  (❌ "findById로 조회하고 orElseThrow로 예외를 던집니다")
- 설명은 코드 주석이 아니라 **응답 본문**에 쓴다. 학습용 주석이 코드에 남으면 지저분해진다
- **한 번에 파일 1~2개, 100줄 안쪽.** 더 필요하면 쪼개서 확인받는다
- 추상화(인터페이스, 추상 클래스, 제네릭)를 도입하려면 **먼저 이유를 말하고 확인받는다.**
  구현체가 하나뿐이면 만들지 않는다
- 지금 필요 없는 확장 지점을 미리 만들지 않는다

---

## 문서 갱신 규칙

작업 후 해당 문서를 갱신한다.

| 변경 | 갱신할 문서 |
|---|---|
| 엔티티·필드·관계 | `spec/domain.md`, `spec/erd.md` |
| 새 용어 등장 | `spec/glossary.md` |
| 화면 표시 항목 | `spec/screens.md` |
| 마일스톤 항목 완료 | `milestones/M1.md` 체크박스 |
| 설계 선택이 갈린 지점 | `decisions/`에 **새 ADR**. 기존 파일 수정 금지 |
| 실행 방법·스택 변경 | `README.md` |
| 성능 측정 결과 | `measurements/` |

**코드와 문서가 어긋나면 멈추고 물어본다.** 코드에 맞춰 문서를 조용히 무시하지 않는다.

---

## 설계 원칙

**가벼운 대안 먼저 → 한계 측정 → 교체**

Kafka, Elasticsearch, Redis를 미리 넣지 않는다.
단순한 버전을 만들고, 측정으로 한계를 확인하고, 근거를 남기고 교체한다.
**측정과 판단 과정 자체가 이 프로젝트의 산출물이다.**

- 외부 유료 서비스(SMS, 메일, 이미지 저장)는 인터페이스 뒤에 둔다.
  M1 구현체는 콘솔 출력 / 로컬 디스크
- 성능 개선을 주장할 때는 `measurements/`에 before/after 수치를 남긴다

---

## 주의

- **M1 범위 밖의 것을 만들지 않는다.** `milestones/M1.md`의 "이번에 하지 않는 것"을 확인할 것
- `@Enumerated`는 반드시 `EnumType.STRING`. 기본값 ORDINAL이면 enum 값 추가 시 데이터가 밀린다
- Bean Validation과 DB 컬럼 길이를 일치시킨다
- soft delete: M1에서는 `@SQLRestriction`을 쓰지 않고 Repository에 조건을 명시한다
- Flyway 명명은 `V20260909_01__사유.sql` (`decisions/0006`)
