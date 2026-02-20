package panda.listing.enums;

import lombok.Getter;

@Getter
public enum MoveInType {
    NEGOTIABLE("협의필요"),
    FIXED("지정날짜"),
    IMMEDIATE("공실(즉시입주)");

    private final String label;

    MoveInType(String label) {
        this.label = label;
    }
}
