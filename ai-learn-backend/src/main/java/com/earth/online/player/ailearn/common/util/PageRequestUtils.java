package com.earth.online.player.ailearn.common.util;

import com.earth.online.player.ailearn.common.constant.PageConstants;

/**
 * 分页参数工具。
 */
public final class PageRequestUtils {

    private PageRequestUtils() {
    }

    /**
     * 规整页码。
     *
     * @param pageNo 原始页码
     * @return 安全页码
     */
    public static int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < PageConstants.DEFAULT_PAGE_NO) {
            return PageConstants.DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    /**
     * 规整每页数量。
     *
     * @param pageSize 原始每页数量
     * @return 安全每页数量
     */
    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, PageConstants.MAX_PAGE_SIZE);
    }

    /**
     * 计算分页偏移量，避免极端页码导致整数溢出。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 数据库偏移量
     */
    public static int calculateOffset(int pageNo, int pageSize) {
        long offset = (long) (pageNo - 1) * pageSize;
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
    }
}
