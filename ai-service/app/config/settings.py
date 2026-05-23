from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

from app.config.constants import AI_GRADING_API_KEY_PLACEHOLDER, AI_SERVICE_TOKEN_PLACEHOLDER, LOCAL_RULE_MODEL


# 固定定位 ai-service 工程目录，避免从不同目录启动时读取错 .env。
BASE_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    """AI 服务配置。"""

    ai_service_token: str = AI_SERVICE_TOKEN_PLACEHOLDER
    ai_service_log_level: str = "INFO"
    ai_grading_base_url: str = ""
    ai_grading_api_key: str = AI_GRADING_API_KEY_PLACEHOLDER
    ai_grading_model: str = LOCAL_RULE_MODEL
    ai_grading_model_provider: str = ""
    ai_grading_timeout_seconds: int = 20
    ai_grading_max_output_tokens: int = 800

    # 统一读取 ai-service/.env，保证本地启动方式稳定。
    model_config = SettingsConfigDict(env_file=BASE_DIR / ".env", env_file_encoding="utf-8", extra="ignore")


settings = Settings()

