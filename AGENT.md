# Entity 생성 규칙

앞으로 JPA Entity 클래스 생성 시 아래 규칙을 준수한다.

1. `@Setter`(Lombok) 사용 금지
2. `src/main/java/panda/listing/Listing.java`를 기준으로 Entity 어노테이션 구성
   - 기본 예시: `@Getter`, `@Entity`, `@Table`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
3. Entity 생성/초기화 시 Setter 방식 금지
4. `src/main/java/panda/listing/ListingService.java`의 `create` 함수처럼 Builder 패턴으로 생성

즉, Entity는 불변에 가깝게 설계하고 값 주입은 Builder로만 수행한다.

# 응답 규칙

앞으로 코드 수정 작업을 수행한 뒤, 답변 마지막에 이번 작업에서 수정한 클래스/파일 목록을 반드시 표시한다.
