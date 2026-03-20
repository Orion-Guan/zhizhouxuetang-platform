package com.tianji.learning.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tianji.common.enums.BaseEnum;
import lombok.Getter;

@Getter
public enum QuestionStatus implements BaseEnum {
    UN_CHECK(0, "未查看"),
    CHECKED(1, "已查看"),
    ;
    @JsonValue
    @EnumValue
    int value;
    String desc;

    QuestionStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }


    /**
     * 根据整数值转换为对应的 QuestionStatus 枚举实例
     * 该方法被标注为 JsonCreator，用于 Jackson 反序列化时将整数转换为枚举对象
     *
     * @param value 枚举对应的整数值
     * @return 匹配的 QuestionStatus 枚举实例，如果 value 为 null 或未找到匹配值则返回 null
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static QuestionStatus of(Integer value) {
        // 空值校验
        if (value == null) {
            return null;
        }
        // 遍历所有枚举值进行匹配
        for (QuestionStatus status : values()) {
            if (status.equalsValue(value)) {
                return status;
            }
        }
        return null;
    }

}
