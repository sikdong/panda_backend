# 엔티티 생성 규칙

앞으로 JPA Entity 생성 시 아래 규칙을 준수한다.

1. `@Setter`(Lombok) 사용 금지
2. `src/main/java/panda/listing/Listing.java`를 기준으로 Entity 어노테이션 구성
   - 기본 예시: `@Getter`, `@Entity`, `@Table`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
3. Entity 생성/초기화 시 Setter 방식 금지
4. `src/main/java/panda/listing/ListingService.java`의 `create` 함수처럼 Builder 패턴으로 생성

즉, Entity는 불변에 가깝게 설계하고 값 주입은 Builder로만 수행한다.

# 응답 규칙

앞으로 코드 수정 작업을 수행했으면 답변 마지막에 이번 작업에서 수정한 파일 목록을 반드시 제시한다.

# Git 추적 규칙

앞으로 생성되는 모든 파일은 git이 추적 가능해야 한다.
- 새 파일을 `.gitignore` 대상 경로에 만들지 않는다.
- 새 파일 생성 후 커밋 가능한 변경셋에 포함되도록 관리한다.

# 인코딩 규칙

앞으로 파일 저장/수정 시 UTF-8 BOM(바이트 순서 표시)을 절대 포함하지 않는다.
- `\ufeff`가 들어가면 즉시 제거한다.
- 가능하면 `apply_patch` 기반으로 수정해 BOM 유입을 방지한다.
