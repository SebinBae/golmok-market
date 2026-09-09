# 0008. FK는 JPA 연관관계로 매핑한다

- 상태: 채택
- 날짜: 2026-09-09

## 맥락

`UserCredential`이 `User`를 참조하는 방식을 정해야 했다.
`user_credential`이 FK를 가진 첫 테이블이라, 여기서 정한 방식이
`Product → Region`, `Product → User(seller)`, `ProductImage → Product`까지 그대로 간다.

`spec/domain.md`의 필드 표는 `userId | Long | FK → User`로 적혀 있다.
이것이 ERD 표기인지 JPA 모델링 의도인지 문서만으로는 갈리지 않는다.

- (A) `@ManyToOne(fetch = LAZY) private User user;`
- (B) `private Long userId;`

## 결정

(A)를 택한다. `spec/domain.md`의 `xxxId` 표기는 **DB 컬럼 기준 표기**로 읽고,
엔티티에서는 연관관계로 매핑한다.

## 근거

**결정적인 이유는 M2의 측정 실험이 (B)에서 성립하지 않는다는 것이다.**

`decisions/0003`과 `spec/domain.md`는 M1의 목록 조회를 일부러 느리게 두고,
M2에서 N+1을 측정한 뒤 개선하기로 했다.

> 목록 조회에서 대표 이미지 1장만 필요한데, 그대로 짜면 **N+1이 발생한다.**
> 이게 사다리 2단계의 실험 재료다. — `spec/domain.md`

N+1은 지연 로딩된 연관관계를 순회할 때 생긴다. (B)로 가면 연관관계가 없어
N+1이 아예 발생하지 않고, 따라서 `fetch join`·`@EntityGraph`·`@BatchSize`로
개선하는 실험도 성립하지 않는다. **측정 대상 자체가 사라진다.**

(B)의 장점(쿼리가 전부 명시적, 지연로딩 함정 없음)은 실제로 있지만,
그건 이 프로젝트가 M2에서 직접 겪어보기로 한 문제다.

## 결과

- `@ManyToOne`의 기본값은 `EAGER`다. **모든 `@ManyToOne`에 `fetch = LAZY`를 명시한다.**
  명시하지 않으면 조회마다 조인이 붙어, 이번에는 N+1이 아니라 다른 방향으로 측정이 왜곡된다
- 연관관계 편의 메서드나 양방향 매핑은 필요해질 때 추가한다. 지금은 단방향만 둔다
- M2 측정이 끝나고 개선안을 고른 뒤, 이 결정이 여전히 맞는지 다시 본다
