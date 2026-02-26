package panda.listing.dto;

import java.util.List;

public record ExclusivePublicAreaResponse(
    String resultCode,
    String resultMsg,
    List<ExclusivePublicAreaItem> items
) {
    public record ExclusivePublicAreaItem(
        String bldNm,            // 건물명
        String dongNm,           // 동명칭
        String hoNm,             // 호명칭
        String flrGbCdNm,        // 층구분코드명
        String flrNo,            // 층번호
        String exmprprSeCdNm,    // 전유공용구분코드명
        String mainPurpsCdNm,    // 주요용도코드명
        String area,             // 면적(㎡)
        String etcPurps          // 기타용도
    ) {}
}
