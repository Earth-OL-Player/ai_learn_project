from __future__ import annotations

from collections import OrderedDict
from collections.abc import Callable
from dataclasses import dataclass
from hashlib import sha256
from threading import RLock
from typing import Any

from langchain.agents import create_agent
from langchain.chat_models import init_chat_model

from app.practice.prompts import DISCUSSION_SYSTEM_PROMPT
from app.practice.provider_adapter import PracticeProviderAdapter
from app.schemas.practice import PracticeGradeEvaluation, PracticeModelConfig


CACHE_LIMIT = 16


@dataclass(frozen=True)
class ModelCacheKey:
    """模型和 Agent 缓存键。"""

    provider: str
    model: str
    base_url: str
    api_key_hash: str
    temperature: float
    timeout_seconds: int
    max_output_tokens: int
    config_fingerprint: str


class PracticeModelFactory:
    """创建刷题场景所需的 LangChain 模型和 Agent。"""

    def __init__(self, provider_adapter: PracticeProviderAdapter) -> None:
        """初始化模型工厂依赖和构造结果缓存。"""
        self._provider_adapter = provider_adapter
        self._cache_lock = RLock()
        self._chat_models: OrderedDict[ModelCacheKey, Any] = OrderedDict()
        self._grading_models: OrderedDict[ModelCacheKey, Any] = OrderedDict()
        self._discussion_agents: OrderedDict[ModelCacheKey, Any] = OrderedDict()

    def discussion_agent(self, model_config: PracticeModelConfig | None = None) -> Any:
        """获取本题讨论 Agent。"""
        key = self._cache_key(model_config)

        # 讨论 Agent 需要支持自由文本和流式输出，单独维护缓存实例。
        return self._get_or_create(
            self._discussion_agents,
            key,
            lambda: self._create_agent(model_config, DISCUSSION_SYSTEM_PROMPT),
        )

    def chat_model(self, model_config: PracticeModelConfig | None = None) -> Any:
        """获取聊天模型。"""
        key = self._cache_key(model_config)

        # init_chat_model 支持通过 provider/model 抽象切换底层模型供应商。
        return self._get_or_create(
            self._chat_models,
            key,
            lambda: init_chat_model(
                self._provider_adapter.model_name(model_config),
                **self._provider_adapter.chat_model_kwargs(model_config),
            ),
        )

    def grading_model(self, model_config: PracticeModelConfig | None = None) -> Any:
        """获取答案评分结构化模型。"""
        key = self._cache_key(model_config)

        # 评分链路没有真实工具，直接结构化模型可避免 Agent 工具调用协议兼容问题。
        return self._get_or_create(
            self._grading_models,
            key,
            lambda: self._create_grading_model(model_config),
        )

    def clear_cached_objects(self) -> int:
        """清理模型和 Agent 构造缓存。"""
        with self._cache_lock:
            cleared_count = (
                len(self._chat_models)
                + len(self._grading_models)
                + len(self._discussion_agents)
            )
            self._chat_models.clear()
            self._grading_models.clear()
            self._discussion_agents.clear()
            return cleared_count

    def _cache_key(self, model_config: PracticeModelConfig | None) -> ModelCacheKey:
        """生成不包含明文密钥的模型缓存键。"""
        kwargs = self._provider_adapter.chat_model_kwargs(model_config)
        api_key = self._provider_adapter.api_key(model_config)

        # 后台模型配置变化后，Java 传入的新指纹会让后续请求自然避开旧缓存。
        return ModelCacheKey(
            provider=self._provider_adapter.model_provider(),
            model=self._provider_adapter.model_name(model_config),
            base_url=self._provider_adapter.normalized_base_url(model_config),
            api_key_hash=sha256(api_key.encode("utf-8")).hexdigest(),
            temperature=float(kwargs.get("temperature") or 0.0),
            timeout_seconds=int(kwargs.get("timeout") or 0),
            max_output_tokens=int(kwargs.get("max_completion_tokens") or kwargs.get("max_tokens") or 0),
            config_fingerprint=(model_config.configFingerprint if model_config else "").strip(),
        )

    def _create_agent(
        self,
        model_config: PracticeModelConfig | None,
        system_prompt: str,
        response_format: Any | None = None,
    ) -> Any:
        """按刷题场景统一创建 LangChain Agent。"""
        agent_kwargs: dict[str, Any] = {
            "model": self.chat_model(model_config),
            "tools": [],
            "system_prompt": system_prompt,
        }
        if response_format is not None:
            agent_kwargs["response_format"] = response_format
        return create_agent(**agent_kwargs)

    def _create_grading_model(self, model_config: PracticeModelConfig | None) -> Any:
        """创建答案评分结构化模型。"""
        method = "json_mode" if self._provider_adapter.is_deepseek_provider(model_config) else "json_schema"

        # DeepSeek 的 json_mode 不走工具调用，更适合当前纯评分结构化输出场景。
        return self.chat_model(model_config).with_structured_output(
            PracticeGradeEvaluation,
            method=method,
        )

    def _get_or_create(
        self,
        cache: OrderedDict[ModelCacheKey, Any],
        key: ModelCacheKey,
        builder: Callable[[], Any],
    ) -> Any:
        """从小容量 LRU 缓存读取对象。"""
        with self._cache_lock:
            cached = cache.get(key)
            if cached is not None:
                cache.move_to_end(key)
                return cached

            # 缓存只保留少量模型变体，避免旧供应商客户端长期占用内存。
            created = builder()
            cache[key] = created
            if len(cache) > CACHE_LIMIT:
                cache.popitem(last=False)
            return created
