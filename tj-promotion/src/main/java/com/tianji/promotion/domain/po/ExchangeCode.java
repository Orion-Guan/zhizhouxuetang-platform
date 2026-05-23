package com.tianji.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 兑换码
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-05-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exchange_code")
@ApiModel(value="ExchangeCode对象", description="兑换码")
public class ExchangeCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "兑换码id")
    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    @ApiModelProperty(value = "兑换码")
    @TableField("code")
    private String code;

    @ApiModelProperty(value = "兑换码状态， 1：待兑换，2：已兑换，3：兑换活动已结束")
    @TableField("status")
    private ExchangeCodeStatus status;

    @ApiModelProperty(value = "兑换人")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "兑换类型，1：优惠券，以后再添加其它类型")
    @TableField("type")
    private Integer type;

    @ApiModelProperty(value = "兑换码目标id，例如兑换优惠券，该id则是优惠券的配置id")
    @TableField("exchange_target_id")
    private Long exchangeTargetId;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "兑换码过期时间")
    @TableField("expired_time")
    private LocalDateTime expiredTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;


}
