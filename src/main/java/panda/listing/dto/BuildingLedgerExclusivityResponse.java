package panda.listing.dto;

import java.util.List;

public record BuildingLedgerExclusivityResponse(Data data) {
    public record Data(Items items) {}
    public record Items(List<ExclusivityItem> item) {}
    public record ExclusivityItem(
        String bldNm,
        String dongNm,
        String hoNm,
        String flrNo,
        String area,
        String mainPurpsCdNm
    ) {}
}
