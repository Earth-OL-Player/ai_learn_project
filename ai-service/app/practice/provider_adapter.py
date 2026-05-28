from __future__ import annotations

from typing import Any

from langchain_core.messages import AIMessage

from app.config.constants import AI_GRADING_API_KEY_PLACEHOLDER, CHAT_COMPLETIONS_PATH, LOCAL_RULE_MODEL
from app.config.settings import settings
from app.schemas.practice import PracticeModelConfig


# DeepSeek V4 默认开启思考模式，评分和讨论链路统一关闭。
DEEPSEEK_THINKING_DISABLED_BODY = {"thinking": {"type": "disabled"}}
TOKEN_USAGE_NESTED_KEYS = ("token_usage", "usage", "usage_metadata", "response_metadata")
INPUT_TOKEN_KEYS = ("input_tokens", "prompt_tokens")
OUTPUT_TOKEN_KEYS = ("output_tokens", "completion_tokens")
TOTAL_TOKEN_KEYS = ("total_tokens",)

TokenUsage = dict[str, int | None]


class PracticeProviderAdapter:
    """封装模型供应商配置、地址兼容和 LangChain 返回差异。"""

    def is_llm_enabled(self, model_config: PracticeModelConfig | None = None) -> bool:
        """判断是否具备真实大模型调用配置。"""
        model = self.model_name(model_config)
        api_key = self.api_key(model_config)

        # LOCAL_RULE 或占位符配置表示 ai-service 不发起外部模型请求。
        return (
            bool(self.base_url(model_config))
            and bool(api_key)
            and model.upper() != LOCAL_RULE_MODEL
            and api_key != AI_GRADING_API_KEY_PLACEHOLDER
        )

    def chat_model_kwargs(self, model_config: PracticeModelConfig | None = None) -> dict[str, Any]:
        """构造 LangChain 聊天模型初始化参数。"""
        kwargs: dict[str, Any] = {
            "temperature": 0.2,
            "timeout": settings.ai_grading_timeout_seconds,
            "stream_usage": True,
            "max_completion_tokens": settings.ai_grading_max_output_tokens,
        }

        # 只在配置真实值时传递供应商连接参数。
        api_key = self.api_key(model_config)
        if api_key:
            kwargs["api_key"] = api_key
        if self.base_url(model_config):
            kwargs["base_url"] = self.normalized_base_url(model_config)
        if self.model_provider().strip():
            kwargs["model_provider"] = self.model_provider().strip()
        if self.is_deepseek_provider(model_config):
            kwargs["extra_body"] = DEEPSEEK_THINKING_DISABLED_BODY
        return kwargs

    def normalized_base_url(self, model_config: PracticeModelConfig | None = None) -> str:
        """规整 OpenAI 兼容基础地址。"""
        base_url = self.base_url(model_config).rstrip("/")
        if base_url.endswith(CHAT_COMPLETIONS_PATH):
            return base_url[: -len(CHAT_COMPLETIONS_PATH)]
        return base_url

    def is_deepseek_provider(self, model_config: PracticeModelConfig | None = None) -> bool:
        """判断当前模型配置是否指向 DeepSeek 服务。"""
        provider = self.model_provider().strip().lower()
        model = self.model_name(model_config).lower()
        base_url = self.base_url(model_config).lower()

        # 按供应商、模型名或官方域名识别 DeepSeek 兼容逻辑。
        return provider == "deepseek" or model.startswith("deepseek-") or "deepseek.com" in base_url

    def model_name(self, model_config: PracticeModelConfig | None = None) -> str:
        """读取请求级或全局模型名称。"""
        if model_config and model_config.model.strip():
            return model_config.model.strip()
        return settings.ai_grading_model.strip()

    def base_url(self, model_config: PracticeModelConfig | None = None) -> str:
        """读取请求级或全局模型基础地址。"""
        if model_config and model_config.baseUrl.strip():
            return model_config.baseUrl.strip()
        return settings.ai_grading_base_url.strip()

    def api_key(self, model_config: PracticeModelConfig | None = None) -> str:
        """读取请求级或全局模型 Key。"""
        if model_config and model_config.apiKey.strip():
            return model_config.apiKey.strip()
        return settings.ai_grading_api_key.strip()

    def model_provider(self) -> str:
        """读取全局模型供应商。"""
        return settings.ai_grading_model_provider.strip()

    def agent_stream_content(self, chunk: Any) -> str:
        """读取 Agent 流式事件中的文本片段。"""
        if isinstance(chunk, dict) and chunk.get("type") == "messages":
            return self.agent_stream_content(chunk.get("data"))
        if isinstance(chunk, tuple) and chunk:
            return self.chunk_content(chunk[0])
        if isinstance(chunk, dict):
            return self.last_ai_reply(chunk)
        return self.chunk_content(chunk)

    def stream_output_tokens(self, chunk: Any) -> int | None:
        """从 LangChain 流式事件中读取供应商返回的输出 Token。"""
        if isinstance(chunk, dict) and chunk.get("type") == "messages":
            return self.stream_output_tokens(chunk.get("data"))
        if isinstance(chunk, tuple) and chunk:
            return self.message_output_tokens(chunk[0])
        if isinstance(chunk, dict):
            return self.dict_output_tokens(chunk)
        return self.message_output_tokens(chunk)

    def call_token_usage(self, result: Any) -> TokenUsage:
        """从 LangChain 调用结果中读取 Token 用量。"""
        if isinstance(result, dict):
            messages = result.get("messages")
            if isinstance(messages, list):
                for message in reversed(messages):
                    token_usage = self.message_token_usage(message)
                    if self._has_token_usage(token_usage):
                        return token_usage
            return self.dict_token_usage(result)
        return self.message_token_usage(result)

    def message_content(self, message: Any) -> str:
        """读取 LangChain 消息内容。"""
        content = getattr(message, "content", "")
        return self.normalize_content(content).strip()

    def last_ai_reply(self, result: dict[str, Any]) -> str:
        """从 Agent 执行结果中读取最后一条 AI 回复。"""
        messages = result.get("messages")
        if not isinstance(messages, list):
            return ""

        # create_agent 返回的状态中，最新模型回复通常位于 messages 最后一条。
        for message in reversed(messages):
            if isinstance(message, AIMessage) or getattr(message, "type", "") == "ai":
                return self.message_content(message)
        return ""

    def chunk_content(self, chunk: Any) -> str:
        """读取 LangChain 流式消息片段内容。"""
        content = getattr(chunk, "content", "")
        normalized_content = self.normalize_content(content)
        if normalized_content:
            return normalized_content

        # 某些模型集成会把 token 放在 text 或 content_blocks 字段中。
        text = getattr(chunk, "text", "")
        if isinstance(text, str):
            return text
        return self.normalize_content(getattr(chunk, "content_blocks", ""))

    def normalize_content(self, content: Any) -> str:
        """规整 LangChain 消息内容。"""
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            return "".join(self.normalize_content(item) for item in content)
        if isinstance(content, dict):
            return str(content.get("text") or content.get("content") or content.get("input") or "")
        return "" if content is None else str(content)

    def message_output_tokens(self, message: Any) -> int | None:
        """从消息对象中读取输出 Token。"""
        for usage_value in self._message_usage_sources(message):
            output_tokens = self.dict_output_tokens(usage_value)
            if output_tokens is not None:
                return output_tokens
        return None

    def message_token_usage(self, message: Any) -> TokenUsage:
        """从消息对象中读取输入、输出和总 Token。"""
        for usage_value in self._message_usage_sources(message):
            token_usage = self.dict_token_usage(usage_value)
            if self._has_token_usage(token_usage):
                return token_usage
        return self._empty_token_usage()

    def dict_token_usage(self, value: Any) -> TokenUsage:
        """从字典结构中读取不同供应商常见的 Token 用量字段。"""
        if not isinstance(value, dict):
            return self._empty_token_usage()

        # 兼容 OpenAI、LangChain 和 OpenAI 兼容供应商的字段命名。
        input_tokens = self._first_int(value, INPUT_TOKEN_KEYS)
        output_tokens = self._first_int(value, OUTPUT_TOKEN_KEYS)
        total_tokens = self._first_int(value, TOTAL_TOKEN_KEYS)
        if input_tokens is not None or output_tokens is not None or total_tokens is not None:
            return {"inputTokens": input_tokens, "outputTokens": output_tokens, "totalTokens": total_tokens}

        # LangChain 或供应商 SDK 可能把 usage 再嵌套一层。
        for nested_key in TOKEN_USAGE_NESTED_KEYS:
            token_usage = self.dict_token_usage(value.get(nested_key))
            if self._has_token_usage(token_usage):
                return token_usage
        return self._empty_token_usage()

    def dict_output_tokens(self, value: Any) -> int | None:
        """从字典结构中读取不同供应商常见的输出 Token 字段。"""
        if not isinstance(value, dict):
            return None

        # OpenAI 兼容供应商通常使用 output_tokens 或 completion_tokens。
        token_count = self._first_int(value, OUTPUT_TOKEN_KEYS)
        if token_count is not None:
            return token_count

        # LangChain 和供应商 SDK 可能再包一层 usage 结构。
        for nested_key in TOKEN_USAGE_NESTED_KEYS:
            token_count = self.dict_output_tokens(value.get(nested_key))
            if token_count is not None:
                return token_count
        return None

    def _message_usage_sources(self, message: Any) -> tuple[Any, Any]:
        """按优先级读取消息对象中的 Token 用量来源。"""
        return getattr(message, "usage_metadata", None), getattr(message, "response_metadata", None)

    def _empty_token_usage(self) -> TokenUsage:
        """构造空 Token 用量，避免复用可变字典。"""
        return {"inputTokens": None, "outputTokens": None, "totalTokens": None}

    def _has_token_usage(self, token_usage: TokenUsage) -> bool:
        """判断 Token 用量中是否包含供应商返回的真实值。"""
        return any(item is not None for item in token_usage.values())

    def _first_int(self, value: dict[str, Any], keys: tuple[str, ...]) -> int | None:
        """按优先级读取第一个整型字段。"""
        for key in keys:
            token_count = value.get(key)
            if isinstance(token_count, int):
                return token_count
        return None
