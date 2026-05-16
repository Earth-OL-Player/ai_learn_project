from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


# 固定定位 ai-service 工程目录，避免从不同目录启动时读取错 .env。
BASE_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    """AI 服务配置。"""

    ai_service_token: str = "AI_SERVICE_TOKEN占位符"
    qdrant_url: str = "http://127.0.0.1:6333"
    qdrant_collection: str = "ai_learn_knowledge"
    ai_grading_base_url: str = ""
    ai_grading_api_key: str = "AI_GRADING_API_KEY占位符"
    ai_grading_model: str = "LOCAL_RULE"
    ai_grading_timeout_seconds: int = 20

    # 统一读取 ai-service/.env，保证本地启动方式稳定。
    model_config = SettingsConfigDict(env_file=BASE_DIR / ".env", env_file_encoding="utf-8", extra="ignore")


settings = Settings()
