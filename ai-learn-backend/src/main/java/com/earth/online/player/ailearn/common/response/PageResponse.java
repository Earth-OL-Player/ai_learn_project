package com.earth.online.player.ailearn.common.response;

import java.util.List;

/**
 * 统一分页响应对象。
 *
 * @param records 当前页数据
 * @param pageNo 当前页码
 * @param pageSize 每页数量
 * @param total 总记录数
 * @param <T> 数据类型
 */
public record PageResponse<T>(List<T> records, int pageNo, int pageSize, long total) {
}
