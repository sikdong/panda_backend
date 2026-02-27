package panda.listing.dto;

import java.util.List;

public record BuildingLedgerTitleResponse(Data data) {
    public record Data(Items items) {}
    public record Items(List<TitleItem> item) {}
    public record TitleItem(
        String mgmBldrgstPk,
        String dongNm,
        String grndFlrCnt,
        String useAprDay,
        String parkingCount,
        boolean parkingAvailable
    ) {}
}
