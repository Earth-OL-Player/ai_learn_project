package com.earth.online.player.ailearn.question.infrastructure;

/**
 * 知识点查询投影。
 */
public class KnowledgePointRecord {

    private Long id;
    private String name;
    private String description;

    /** 获取知识点ID。 */
    public Long getId() { return id; }

    /** 设置知识点ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取知识点名称。 */
    public String getName() { return name; }

    /** 设置知识点名称。 */
    public void setName(String name) { this.name = name; }

    /** 获取知识点说明。 */
    public String getDescription() { return description; }

    /** 设置知识点说明。 */
    public void setDescription(String description) { this.description = description; }
}
