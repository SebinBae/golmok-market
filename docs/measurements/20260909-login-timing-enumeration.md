# 로그인 응답 시간으로 가입 여부가 새는지

- 날짜: 2026-09-09
- 대상: `LoginService.authenticate`

## 왜 쟀나

`screens.md` A1이 이렇게 정했다.

> 실패 메시지는 "이메일 또는 비밀번호가 올바르지 않습니다"로 통일한다.
> "없는 이메일입니다"라고 알려주면 가입 여부를 알아내는 열거 공격이 가능해진다.

메시지를 통일해도 **응답 시간이 다르면 같은 정보가 샌다.**
없는 이메일은 조회에서 끝나고, 있는 이메일은 BCrypt 대조까지 간다.
실제로 얼마나 벌어지는지 확인했다.

## 측정 조건

- `LoginTimingMeasurement` (core 테스트, 실제 PostgreSQL)
- 실패 경로만 측정. 각 11회 반복 후 **중앙값**, 워밍업 1회 선행
- BCrypt 기본 강도(10), WSL2 / Java 21

## Before — 없는 이메일이면 해싱을 건너뛴다

```java
if (credential.isEmpty()) throw ...;
if (!passwordEncoder.matches(rawPassword, credential.get().getPasswordHash())) throw ...;
```

| 입력 | 중앙값 |
|---|---|
| 가입된 이메일 + 틀린 비밀번호 | **62ms** |
| 없는 이메일 | **1ms** |

**62배 차이.** 응답 시간만 재면 가입 여부를 판별할 수 있다. 메시지 통일이 무의미해진다.

## 원인

BCrypt는 의도적으로 느리다(강도 10 기준 약 60ms). 그 비용을 한쪽 경로에서만 치르면
그 자체가 신호가 된다.

## 개선

없는 이메일일 때도 더미 해시로 `matches()`를 돌린다. 결과는 항상 false다.

```java
String hash = credential.map(UserCredential::getPasswordHash).orElse(dummyHash);
boolean matched = passwordEncoder.matches(rawPassword, hash);
if (credential.isEmpty() || !matched) throw ...;
```

더미 해시는 빈 생성 시점에 랜덤 UUID로 한 번 만든다.

## After

| 입력 | 중앙값 |
|---|---|
| 가입된 이메일 + 틀린 비밀번호 | **62ms** |
| 없는 이메일 | **62ms** |

## 판단

값어치가 있었다. 코드는 두 줄 늘었고 정상 로그인 성능은 그대로다
(성공 경로는 원래도 BCrypt를 거친다).

**남은 한계**: 이 방어는 서비스 계층까지만이다. 어댑터에서 "없는 이메일"과
"비밀번호 틀림"을 다른 상태코드나 다른 화면으로 처리하면 다시 샌다.
`api`/`web` 컨트롤러를 만들 때 이 문서를 다시 볼 것.
