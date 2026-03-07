package com.tianji.learning.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tianji.common.enums.BaseEnum;
import lombok.Getter;

@Getter
public enum SectionType implements BaseEnum {
    VIDEO(1, "视频"),
    EXAM(2, "考试"),
    ;
    @JsonValue   //将枚举实例序列化为对应的具体值
    @EnumValue   //枚举实例在保存到数据库时自动映射为具体的数值
    int value;
    String desc;

    SectionType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 将JSON请求参数自动反序列化为对应的枚举类型
     * DELEGATING（委托模式）：将整数值直接传递给方法，返回其对应的枚举类型
     * @param value
     * @return
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SectionType of(Integer value){
        if (value == null) {
            return null;
        }
        for (SectionType status : values()) {
            if (status.equalsValue(value)) {
                return status;
            }
        }
        return null;
    }
}
