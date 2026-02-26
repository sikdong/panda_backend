package panda.listing.dto;

import java.util.List;

public record BuildingLedgerResponse(
    String resultCode,
    String resultMsg,
    List<BuildingLedgerItem> items
) {
    public record BuildingLedgerItem(
        String bldNm,            // 건물명
        String dongNm,           // 동명
        String platGbCd,         // 대지구분코드
        String sigunguCd,        // 시군구코드
        String bjdongCd,         // 법정동코드
        String bun,              // 번
        String ji,               // 지
        String platArea,         // 대지면적
        String archArea,         // 건축면적
        String bcRat,            // 건폐율
        String totArea,          // 연면적
        String vlRat,            // 용적률
        String strctCdNm,        // 구조코드명
        String mainPurpsCdNm,    // 주요용도코드명
        String etcPurps,         // 기타용도
        String hhldCnt,          // 세대수
        String fmlyCnt,          // 가구수
        String grndFlrCnt,       // 지상층수
        String ugndFlrCnt,       // 지하층수
        String useAprvDe,        // 사용승인일
        String pmsDe,            // 허가일
        String stcnsDe           // 착공일
    ) {}
}
