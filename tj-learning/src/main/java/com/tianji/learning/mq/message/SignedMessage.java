package com.tianji.learning.mq.message;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class SignedMessage {

    private Long userId;

    private Integer points;

}
