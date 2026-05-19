"""AI 服务通用常量。"""

# 内部接口鉴权 Token 示例占位符，真实值由本地或服务器私有配置注入。
AI_SERVICE_TOKEN_PLACEHOLDER = "AI_SERVICE_TOKEN占位符"

# 大模型 Key 示例占位符，真实 Key 禁止提交仓库。
AI_GRADING_API_KEY_PLACEHOLDER = "AI_GRADING_API_KEY占位符"

# 本地规则模型名，配置该值时 ai-service 不发起外部模型请求。
LOCAL_RULE_MODEL = "LOCAL_RULE"

# OpenAI 兼容 Chat Completions 路径，用于兼容用户误填完整接口地址。
CHAT_COMPLETIONS_PATH = "/chat/completions"
