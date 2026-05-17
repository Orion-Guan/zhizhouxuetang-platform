package com.tianji.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 优惠券作用范围信息
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-05-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("coupon_scope")
@ApiModel(value="CouponScope对象", description="优惠券作用范围信息")
public class CouponScope implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "范围限定类型：1-分类，2-课程，等等")
    @TableField("type")
    private Integer type;

    @ApiModelProperty(value = "优惠券id")
    @TableField("coupon_id")
    private Long couponId;

    @ApiModelProperty(value = "优惠券作用范围的业务id，例如分类id、课程id")
    @TableField("biz_id")
    private Long bizId;


}
