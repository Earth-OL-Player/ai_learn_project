package com.earth.online.player.ailearn.practice.interfaces;

import java.util.List;

/**
 * 刷题聊天请求。
 *
 * @param content 用户输入内容
 * @param questionTypes 题目分类，空表示全部分类
 */
public record PracticeMessageRequest(String content, List<String> questionTypes) {
}
