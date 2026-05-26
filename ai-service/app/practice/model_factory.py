from __future__ import annotations

from typing import Any

from langchain.agents import create_agent
from langchain.chat_models import init_chat_model

from app.practice.prompts import DISCUSSION_SYSTEM_PROMPT, GRADE_SYSTEM_PROMPT
from app.practice.provider_adapter import PracticeProviderAdapter
from app.schemas.practice import PracticeGradeEvaluation, PracticeModelConfig


class PracticeModelFactory:
    """创建刷题场景所需的 LangChain 模型和 Agent。"""

    def __init__(self, provider_adapter: PracticeProviderAdapter) -> None:
        """初始化模型工厂依赖。"""
        self._provider_adapter = provider_adapter

    def grading_agent(self, model_config: PracticeModelConfig | None = None) -> Any:
        """创建答案评分 Agent。"""
        return create_agent(
            model=self.chat_model(model_config),
            tools=[],
            system_prompt=GRADE_SYSTEM_PROMPT,
            response_format=PracticeGradeEvaluation,
        )

    def discussion_agent(self, model_config: PracticeModelConfig | None = None) -> Any:
        """创建本题讨论 Agent。"""
        return create_agent(
            model=self.chat_model(model_config),
            tools=[],
            system_prompt=DISCUSSION_SYSTEM_PROMPT,
        )

    def chat_model(self, model_config: PracticeModelConfig | None = None) -> Any:
        """使用 LangChain 推荐入口初始化聊天模型。"""
        kwargs = self._provider_adapter.chat_model_kwargs(model_config)

        # init_chat_model 支持通过 provider/model 抽象切换底层模型供应商。
        return init_chat_model(self._provider_adapter.model_name(model_config), **kwargs)
