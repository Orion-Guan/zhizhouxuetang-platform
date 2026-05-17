package com.tianji.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.DiscountType;
import com.tianji.promotion.enums.ObtainType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 优惠券的规则信息
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-05-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("coupon")
@ApiModel(value="Coupon对象", description="优惠券的规则信息")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "优惠券id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "优惠券名称，可以和活动名称保持一致")
    @TableField("name")
    private String name;

    @ApiModelProperty(value = "优惠券类型，1：普通券。目前就一种，保留字段")
    @TableField("type")
    private Integer type;

    @ApiModelProperty(value = "折扣类型，1：满减，2：每满减，3：折扣，4：无门槛")
    @TableField("discount_type")
    private DiscountType discountType;

    @ApiModelProperty(value = "是否限定作用范围，false：不限定，true：限定。默认false")
    @TableField("specific")
    private Boolean specific;

    @ApiModelProperty(value = "折扣值，如果是满减则存满减金额，如果是折扣，则存折扣率，8折就是存80")
    @TableField("discount_value")
    private Integer discountValue;

    @ApiModelProperty(value = "使用门槛，0：表示无门槛，其他值：最低消费金额")
    @TableField("threshold_amount")
    private Integer thresholdAmount;

    @ApiModelProperty(value = "最高优惠金额，满减最大，0：表示没有限制，不为0，则表示该券有金额的限制")
    @TableField("max_discount_amount")
    private Integer maxDiscountAmount;

    @ApiModelProperty(value = "获取方式：1：手动领取，2：兑换码")
    @TableField("obtain_way")
    private ObtainType obtainWay;

    @ApiModelProperty(value = "开始发放时间")
    @TableField("issue_begin_time")
    private LocalDateTime issueBeginTime;

    @ApiModelProperty(value = "结束发放时间")
    @TableField("issue_end_time")
    private LocalDateTime issueEndTime;

    @ApiModelProperty(value = "优惠券有效期天数，0：表示有效期是指定有效期的")
    @TableField("term_days")
    private Integer termDays;

    @ApiModelProperty(value = "优惠券有效期开始时间")
    @TableField("term_begin_time")
    private LocalDateTime termBeginTime;

    @ApiModelProperty(value = "优惠券有效期结束时间")
    @TableField("term_end_time")
    private LocalDateTime termEndTime;

    @ApiModelProperty(value = "优惠券配置状态，1：待发放，2：未开始   3：进行中，4：已结束，5：暂停")
    @TableField("status")
    private CouponStatus status;

    @ApiModelProperty(value = "总数量，不超过5000")
    @TableField("total_num")
    private Integer totalNum;

    @ApiModelProperty(value = "已发行数量，用于判断是否超发")
    @TableField("issue_num")
    private Integer issueNum;

    @ApiModelProperty(value = "已使用数量")
    @TableField("used_num")
    private Integer usedNum;

    @ApiModelProperty(value = "每个人限领的数量，默认1")
    @TableField("user_limit")
    private Integer userLimit;

    @ApiModelProperty(value = "拓展参数字段，保留字段")
    @TableField("ext_param")
    private String extParam;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "创建人")
    @TableField("creater")
    private Long creater;

    @ApiModelProperty(value = "更新人")
    @TableField("updater")
    private Long updater;


}
