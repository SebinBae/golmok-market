# 도메인 모델

> 용어는 [glossary.md](./glossary.md)를 따른다.
> ERD 다이어그램은 [erd.md](./erd.md) 참고.
> 엔티티/필드/관계가 바뀌면 이 문서와 erd.md를 함께 갱신한다.

---

## 전체 그림

```
User ──< UserRegion >── Region
 │                        │
 │ 1                      │ 1
 │                        │
 └──< UserCredential      │
       │                  │
       └──< PasswordResetToken
                          │
User(seller) 1 ──< Product ──┘ (등록 시점 고정)
                    │
                    ├──< ProductImage
                    │
                    └──< ChatRoom ──0..1── Trade    [M1 제외]
```

**핵심 관계 두 개**

1. `Product`는 `Region`을 **직접** 참조한다. 판매자를 통해 따라가지 않는다.
2. `Trade`는 `ChatRoom`을 참조하고 `Product`를 **직접 참조하지 않는다.** 채팅방 없는 거래를 구조적으로 불가능하게 만든다.

---

## M1 구현 범위

| 엔티티 | M1 |
|---|---|
| User | ✅ 구현 |
| UserCredential | ✅ 구현 (LOCAL만) |
| PasswordResetToken | ✅ 구현 |
| VerificationCode | ✅ 구현 |
| Region | ✅ 구현 |
| UserRegion | ✅ 구현 |
| Product | ✅ 구현 |
| ProductImage | ✅ 구현 (URL만, 파일 업로드 없음) |
| ChatRoom | ❌ 설계만 |
| Trade | ❌ 설계만 |

---

## User

사용자 프로필. **인증 정보를 갖지 않는다.**

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| nickname | String | NOT NULL, unique, 2~20자 | |
| phoneNumber | String | NOT NULL, unique, 11자 | 숫자만 저장 (하이픈 제거) |
| createdAt | Instant | NOT NULL | |
| deletedAt | Instant | nullable | 탈퇴 시각 |

**컬럼 길이**

`nickname` → `varchar(20)`, `phone_number` → `varchar(11)`.
하한 2자는 DB로 걸 수 없으므로 앱에서 검증한다.
`varchar(11)`이 하이픈 섞인 값(`010-1111-1111`, 13자)을 DB에서 거부한다 —
"숫자만 저장" 규칙의 마지막 방어선이다.

**시각 컬럼**

`Instant` ↔ `timestamptz`. Hibernate 기본 매핑이 그대로 맞아서 `@Column` 타입 지정이 필요 없다.
`created_at`에 `DEFAULT now()`는 걸지 않는다. 값은 애플리케이션이 채운다.

**설계 메모**

- `phoneNumber`는 아이디 찾기의 입력값이라 unique여야 한다
- 저장 포맷을 한 가지로 고정한다. `010-1234-5678`과 `01012345678`이 섞이면 unique가 무의미해진다
- 탈퇴도 soft delete다. 탈퇴한 사용자의 상품과 채팅 이력이 남아야 한다
- **닉네임 unique는 부분 인덱스로 건다.** 살아있는 사용자끼리만 유일하고, 탈퇴한 사용자의 닉네임은 재사용 가능하다

```sql
CREATE UNIQUE INDEX uk_user_nickname
  ON users (nickname) WHERE deleted_at IS NULL;
```

  단순 unique를 걸면 탈퇴해도 닉네임이 영구히 점유된다. PostgreSQL 부분 인덱스로 한 줄이면 해결되므로 처음부터 이렇게 간다.
  단, 탈퇴한 A의 닉네임을 B가 가져가면 예전 채팅에서 혼동이 생기므로 **탈퇴 사용자는 화면에 "알 수 없음"으로 표시**한다.
- `phoneNumber`의 unique도 같은 문제를 갖는다. 동일하게 부분 인덱스로 건다

---

## UserCredential

인증수단 하나. 한 사용자가 여러 개를 가질 수 있다.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| userId | Long | FK → User, NOT NULL | |
| provider | Enum | NOT NULL | `LOCAL`, `KAKAO`, `GOOGLE` |
| providerId | String | nullable | 소셜 제공자의 사용자 식별자 |
| email | String | nullable | |
| emailVerified | boolean | NOT NULL, default false | |
| passwordHash | String | nullable | `LOCAL`일 때만 |
| createdAt | Instant | NOT NULL | |

**제약**

- `unique(provider, providerId)` — 같은 소셜 계정으로 두 번 가입 불가
- `unique(provider, email)` where `provider = 'LOCAL'` — 이메일 중복 가입 방지
- `provider = 'LOCAL'`이면 `email`과 `passwordHash`가 반드시 있어야 한다 (애플리케이션에서 검증)

**컬럼 길이**

`provider` → `varchar(20)`, `provider_id`/`email` → `varchar(255)`, `password_hash` → `varchar(72)`.

`password_hash`가 60이 아닌 이유: Spring Security의 `DelegatingPasswordEncoder`는
`{bcrypt}$2a$10$...`처럼 **알고리즘 접두사를 붙여 저장한다.** 8 + 60 = 68자라 60으로 잡으면
가입 시점에 `value too long`으로 터진다. 72는 그 68에 여유를 둔 값이다.

**`unique(provider, provider_id)`가 LOCAL을 제약하지 않는다**

PostgreSQL은 유니크 제약에서 NULL을 서로 다른 값으로 본다.
`LOCAL` 행은 `provider_id`가 NULL이므로 이 제약에 걸리지 않고 여러 개 공존한다. 의도한 동작이다 —
`LOCAL`의 유일성은 `uk_user_credential_local_email` 부분 인덱스가 담당한다.

**설계 메모**

- 이메일을 User가 아닌 여기에 둔 이유는 **검증 여부를 인증수단별로 관리해야 하기 때문**이다. 소셜에서 받아온 이메일은 우리가 검증한 것이 아니므로 `emailVerified = false`이고, 비밀번호 재설정 메일을 그리로 보내면 계정 탈취 경로가 된다
- 비밀번호 재설정은 **`LOCAL` + `emailVerified = true`인 credential에만** 허용한다
- 해싱은 BCrypt. `passwordHash` 컬럼 길이는 60자 이상 확보

---

## PasswordResetToken

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| credentialId | Long | FK → UserCredential, NOT NULL | |
| tokenHash | String | NOT NULL, unique | **평문 저장 금지** |
| expiresAt | Instant | NOT NULL | 발급 후 30분 |
| usedAt | Instant | nullable | 사용 시각. 일회용 보장 |
| createdAt | Instant | NOT NULL | |

**설계 메모**

- 사용자에게는 평문 토큰을 링크로 보내고, DB에는 해시만 저장한다. DB가 유출되어도 토큰을 복원할 수 없다
- 검증 조건: 해시 일치 + `expiresAt > now` + `usedAt IS NULL`
- 새 토큰 발급 시 기존 미사용 토큰을 무효화할지 결정 필요 → 무효화하는 쪽을 권장
- M1에서 메일 발송은 **인터페이스만 정의하고 구현은 콘솔 출력**

---

## VerificationCode

휴대폰 번호 확인용 단기 코드. 아이디 찾기에 사용한다.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| phoneNumber | String | NOT NULL | |
| codeHash | String | NOT NULL | 평문 저장 금지 |
| purpose | Enum | NOT NULL | `FIND_EMAIL` (확장 여지) |
| expiresAt | Instant | NOT NULL | 발급 후 5분 |
| usedAt | Instant | nullable | |
| attemptCount | int | NOT NULL, default 0 | 무차별 대입 방어 |
| createdAt | Instant | NOT NULL | |

**설계 메모**

- 코드가 6자리 숫자면 경우의 수가 100만이라 재시도를 막지 않으면 뚫린다. `attemptCount` 5회 초과 시 폐기
- 발급 자체에도 제한이 필요하다 (같은 번호로 1분 내 재발급 불가)
- **아이디 찾기 결과는 반드시 마스킹**한다: `hy****@gmail.com`
- M1에서 SMS 발송은 콘솔 출력

---

## Region

행정동 단위 동네.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| code | String | NOT NULL, unique, 10자 | 행정동 코드 |
| sido | String | NOT NULL, 최대 20자 | 서울특별시 |
| sigungu | String | NOT NULL, 최대 20자 | 강북구 |
| dong | String | NOT NULL, 최대 20자 | 수유1동 |
| latitude | double | NOT NULL | 중심 위도 |
| longitude | double | NOT NULL | 중심 경도 |

**컬럼 길이**

`code` → `varchar(10)`, `sido`/`sigungu`/`dong` → `varchar(20)`.
엔티티는 `@Column(length = ...)`로만 맞춘다. **Bean Validation은 붙이지 않는다** —
Region은 Flyway로만 들어오는 기준 데이터라 M1에 생성·수정 경로가 없고,
검증기가 호출될 자리가 없는 `@Size`는 죽은 애노테이션이 된다.

**설계 메모**

- 초기 데이터는 **서울 강북구 행정동만** 넣는다 (13개). 코드는 `V20260909_02__insert_gangbuk_regions.sql`에 있다.
  ⚠️ 행정안전부 원본과 대조하지 않았다. **동결(M1 종료) 전에 한 번 확인할 것**
- ⚠️ 좌표는 현재 전부 `0`이다. `(0, 0)`은 문법적으로 유효한 좌표라 `NOT NULL`로도 걸러지지 않는다.
  **범위 설정 기능을 붙이기 전에 반드시 실제 좌표를 채울 것**
- 좌표는 M1에서 쓰지 않는다. 범위 설정을 나중에 붙일 때를 위해 미리 넣어둔다
- 이 데이터는 **Flyway 마이그레이션**에 넣는다 (mock이 아니라 기준 데이터)
- 인접 관계 테이블은 만들지 않는다. 범위 설정은 좌표 기반으로 시작해 필요하면 사전 계산 방식으로 교체

---

## UserRegion

사용자와 동네의 다대다 연결. **최대 2개.**

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| userId | Long | FK → User, NOT NULL | |
| regionId | Long | FK → Region, NOT NULL | |
| createdAt | Instant | NOT NULL | |

**제약**

- `unique(userId, regionId)`
- 사용자당 최대 2개 — **DB로 강제할 수 없다.** 애플리케이션 서비스에서 검증한다

**설계 메모**

- "최대 2개"를 DB 제약으로 표현하려면 트리거나 체크 제약이 필요한데 과하다. 서비스 계층 검증 + 테스트로 보장한다
- 동시에 두 요청이 들어오면 3개가 될 수 있다. M1에서는 무시하고, 나중에 동시성 실험 대상으로 삼는다

---

## Product

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| sellerId | Long | FK → User, NOT NULL | |
| regionId | Long | FK → Region, NOT NULL | **등록 시점 고정** |
| title | String | NOT NULL, 1~100자 | |
| description | String | NOT NULL, 1~2000자 | |
| price | int | NOT NULL, >= 0 | 0 = 나눔 |
| category | Enum | NOT NULL, **STRING 저장** | |
| status | Enum | NOT NULL, default `ON_SALE` | |
| createdAt | Instant | NOT NULL | |
| updatedAt | Instant | NOT NULL | |
| deletedAt | Instant | nullable | soft delete |

**검증 규칙**

- Bean Validation과 DB 컬럼 길이를 **반드시 일치**시킨다. `@Size(max=100)`인데 `varchar(50)`이면 검증은 통과하고 DB에서 터진다
- ⚠️ **이 일치를 `ddl-auto: validate`가 잡아주지 않는다.** 실측 확인: 엔티티 `@Column(length = 5)` ↔ DB `varchar(20)`으로 어긋나게 두어도 기동에 성공한다.
  validate는 테이블·컬럼의 **존재와 타입**만 본다(없는 컬럼은 `missing column`으로 즉시 실패). 길이는 사람이 맞춰야 한다
- `title` → `varchar(100)`, `@Size(min=1, max=100)`
- `description` → `varchar(2000)`, `@Size(min=1, max=2000)`
- `price` → `@PositiveOrZero` + `@Max(100_000_000)` — 상한 1억원. 걸지 않으면 `int` 범위인 21억까지 들어간다

**중요: enum 저장 방식**

```java
@Enumerated(EnumType.STRING)  // 반드시 명시
private Category category;
```

JPA 기본값이 `ORDINAL`이라 명시하지 않으면 DB에 0, 1, 2로 저장된다.
나중에 enum 중간에 값을 하나 추가하면 **기존 데이터의 의미가 통째로 밀린다.**

다행히 이건 `ddl-auto: validate`가 잡아준다. 실측 확인: `@Enumerated`를 빼면
`ORDINAL`이 정수 컬럼을 기대하는데 DB는 `varchar`라
`wrong column type encountered in column [provider]`로 기동이 막힌다.
(같은 validate가 varchar **길이** 불일치는 못 잡는다 — 위 User 절 참고)

**상태 전이**

```
ON_SALE ──(예약)──> RESERVED ──(거래완료)──> SOLD
   ^                    │
   └───(예약취소)────────┘
```

M1에서는 `RESERVED`로 가는 경로가 없다. enum에 자리만 만들어둔다.

**Soft delete 처리**

- M1에서는 `@SQLRestriction`을 쓰지 않는다. Repository 메서드에 `deletedAt IS NULL`을 **명시적으로** 쓴다
- 이유: 자동 필터는 편하지만 어디서 걸러지는지 안 보인다. 학습 단계에서는 눈에 보이는 게 낫다. 빠뜨린 곳이 생기면 그때 자동 필터로 전환하면서 필요성을 체감한다

**수정 · 삭제 정책**

| 상품 상태 | 수정 | 삭제 |
|---|---|---|
| `ON_SALE` (판매중) | 가능 | 가능 |
| `RESERVED` (예약중) | 가능 | **불가** — 예약을 먼저 취소해야 함 |
| `SOLD` (거래완료) | **불가** | 가능 |

- 모든 동작은 **판매자 본인만** 가능하다. 타인이 시도하면 403
- 예약중 삭제를 막는 이유: 구매자가 약속한 물건이 말없이 사라지는 것을 방지한다. 판매자가 예약을 취소하면 구매자가 상황을 인지하게 된다
- M1에는 `RESERVED` 상태로 가는 경로가 없으므로 이 정책은 채팅/거래 단계에서 실제로 작동한다. **정책만 먼저 정해둔다**

**목록 조회 인덱스**

```sql
CREATE INDEX idx_product_list
  ON product (region_id, status, deleted_at, created_at DESC);
```

이 인덱스가 성능 실험의 주인공이다. mock 20만 건에서 인덱스 없이 측정 → 생성 후 재측정.

---

## Category (enum)

당근 카테고리 참고. 15개 내외 고정.

```
DIGITAL, APPLIANCE, FURNITURE, LIVING, KITCHEN,
CLOTHING, BEAUTY, SPORTS, HOBBY, BOOK,
PET, PLANT, BABY, TICKET, ETC
```

**테이블이 아니라 enum인 이유**: 거의 바뀌지 않고, 테이블로 빼면 조회마다 조인이 붙는데 얻는 게 없다. 나중에 AI가 카테고리를 분류할 때도 enum이면 반환값 검증이 깔끔하다.

---

## ProductImage

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| productId | Long | FK → Product, NOT NULL | |
| url | String | NOT NULL | |
| sortOrder | int | NOT NULL | 0부터 |

**제약**

- `unique(productId, sortOrder)`
- 상품당 최대 장수 제한 (예: 10장) — 서비스 계층 검증

**설계 메모**

- M1에서 **로컬 디스크 저장만** 구현한다. S3는 나중 단계
- 저장소는 인터페이스 뒤에 둔다 (`ImageStorage`). SMS와 같은 구조로, 나중에 S3 구현체만 추가하면 된다
- mock 데이터는 `https://picsum.photos/seed/{productId}/400/300` 형태의 외부 플레이스홀더를 쓴다. 20만 건에 실제 파일을 만들 수 없기 때문
- 따라서 `url` 컬럼에는 **외부 URL(mock)과 로컬 경로(업로드) 두 종류가 섞인다.** 화면에서 분기 없이 쓰려면 로컬 파일도 `/images/{...}` 형태의 URL로 내려주는 매핑이 필요하다
- 업로드 시 검증할 것: 파일 크기 상한, 확장자 허용 목록, **실제 이미지인지 확인**(확장자만 바꾼 실행 파일 차단), 파일명 충돌 방지(UUID 등)
- 목록 조회에서 대표 이미지 1장만 필요한데, 그대로 짜면 **N+1이 발생한다.** 이게 사다리 2단계의 실험 재료다

---

## ChatRoom (M1 제외 — 설계만)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| productId | Long | FK → Product, NOT NULL | |
| buyerId | Long | FK → User, NOT NULL | |
| status | Enum | NOT NULL, default `OPEN` | `OPEN`, `CLOSED` |
| createdAt | Instant | NOT NULL | |
| lastMessageAt | Instant | nullable | 목록 정렬용 |

**제약**

- `unique(productId, buyerId)` — 같은 상품 + 같은 구매자는 채팅방 하나

**설계 메모**

- 판매자는 `product.sellerId`로 따라가므로 별도 컬럼을 두지 않는다
- `status`는 파생값이 아니다. 예약이 여러 번 취소되는 동안 계속 `OPEN`이어야 하고, 거래 완료 시에만 `CLOSED`가 된다

---

## Trade (M1 제외 — 설계만)

채팅방에서 파생된 거래.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK | |
| chatRoomId | Long | FK → ChatRoom, NOT NULL | |
| status | Enum | NOT NULL | `RESERVED`, `COMPLETED`, `CANCELLED` |
| reservedAt | Instant | NOT NULL | |
| completedAt | Instant | nullable | |
| cancelledAt | Instant | nullable | |

**설계 메모**

- **`Product`를 직접 참조하지 않는다.** 상품은 `chatRoom.product`로 따라간다. 채팅방 없는 거래가 구조적으로 만들어질 수 없다
- 상품 하나에 Trade가 여러 번 생긴다 (예약 → 취소 → 재예약)

**의도적 연기: 동시 예약 경합**

"상품당 활성 Trade는 하나"라는 제약을 어떻게 강제할지 **일부러 지금 정하지 않는다.**
Trade가 Product를 모르기 때문에 DB 유니크 인덱스를 바로 걸 수 없다.

선택지:
- (A) `productId`를 Trade에 비정규화하고 부분 유니크 인덱스 — DB가 막아준다. 안전하고 단순하지만 데이터 중복
- (B) 정규화 유지 + 비관적 락 또는 분산락 — 깨끗하지만 락 설계를 직접 해야 함

**왜 미루는가**: 지금 (A)를 고르면 DB가 막아주니 안전하지만, 보호 장치 없이 만들었을 때 실제로 어떻게 깨지는지 볼 기회가 사라진다.
이 프로젝트의 원칙("가벼운 대안 먼저 → 한계 측정 → 교체")이 적용될 자리다.

**진행 방법**: 채팅/거래 구현 단계에서 아무 제약 없이 만들고 → 동시 요청 테스트로 활성 Trade가 2개 생기는 것을 재현하고 → 그 결과를 근거로 (A)/(B)를 고른다.
→ 결정 시 `docs/decisions/`에 ADR로 기록한다.

---

## 미해결 항목

없음. 아래는 **의도적으로 연기한** 결정이며, 정하지 못한 것이 아니다.

| 항목 | 언제 정하나 | 왜 미루나 |
|---|---|---|
| 동시 예약 경합 제어 방식 | 채팅/거래 구현 단계 | 락 없이 깨지는 것을 먼저 관찰하기 위해 (위 참고) |
| 알림용 대표 이메일 (`User.contactEmail`) | 알림 기능 도입 시 | 지금은 필요한 곳이 없다. 인증용 이메일은 UserCredential에서 이미 해결됨 |
