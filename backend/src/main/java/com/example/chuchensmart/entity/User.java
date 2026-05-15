/**
 * @author 小李
 * @Description
 * @create 2026-05-11 14:49
 */

package com.example.chuchensmart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
