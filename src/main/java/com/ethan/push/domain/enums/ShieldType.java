package com.ethan.push.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum ShieldType {
    NIGHT_NO_SHIELD(10, "夜间不屏蔽"),
    NIGHT_SHIELD(20, "夜间屏蔽"),
    NIGHT_SHIELD_BUT_NEXT_DAY(30, "夜间屏蔽(次日早上发送)"),
    ;

    private final Integer code;
    private final String description;
}
