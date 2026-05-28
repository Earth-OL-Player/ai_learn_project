package com.earth.online.player.ailearn.common.util;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * CSV 文件下载响应工具。
 */
public final class CsvDownloadUtils {

    private static final MediaType UTF8_CSV_MEDIA_TYPE = new MediaType("text", "csv", StandardCharsets.UTF_8);

    /**
     * 工具类不允许实例化。
     */
    private CsvDownloadUtils() {
    }

    /**
     * 构造 UTF-8 CSV 附件响应。
     *
     * @param filename 下载文件名
     * @param content 文件内容
     * @return 文件下载响应
     */
    public static ResponseEntity<byte[]> buildUtf8CsvResponse(String filename, byte[] content) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        // 下载接口直接返回文件流，不包装统一 JSON。
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(UTF8_CSV_MEDIA_TYPE)
                .body(content);
    }
}
