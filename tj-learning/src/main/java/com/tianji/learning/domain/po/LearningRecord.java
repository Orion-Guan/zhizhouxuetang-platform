package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 学习记录表
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="LearningRecord对象", description="学习记录表")
public class LearningRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "学习记录的id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "对应课表的id")
    @TableField("lesson_id")
    private Long lessonId;

    @ApiModelProperty(value = "对应小节的id")
    @TableField("section_id")
    private Long sectionId;

    @ApiModelProperty(value = "用户id")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "视频的当前观看时间点，单位秒")
    @TableField("moment")
    private Integer moment;

    @ApiModelProperty(value = "是否完成学习，默认false")
    @TableField("finished")
    private Boolean finished;

    @ApiModelProperty(value = "第一次观看时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "完成学习的时间")
    @TableField("finish_time")
    private LocalDateTime finishTime;

    @ApiModelProperty(value = "更新时间（最近一次观看时间）")
    @TableField("update_time")
    private LocalDateTime updateTime;


}
