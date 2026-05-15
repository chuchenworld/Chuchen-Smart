package com.example.chuchensmart.vo;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserVo {

    private Long id;

    private String username;

    private String nickname;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
