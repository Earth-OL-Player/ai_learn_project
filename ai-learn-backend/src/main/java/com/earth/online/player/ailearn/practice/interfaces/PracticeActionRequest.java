package com.earth.online.player.ailearn.practice.interfaces;

import java.util.List;

/**
 * 刷题动作请求。
 *
 * @param questionTypes 题目分类，空表示全部分类
 */
public record PracticeActionRequest(List<String> questionTypes) {
}
