from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """AI 服务配置。"""

    ai_service_token: str = "AI_SERVICE_TOKEN占位符"
    model_provider: str = "LOCAL_RULE"
    openai_api_key: str = "OPENAI_API_KEY占位符"
    qdrant_url: str = "http://127.0.0.1:6333"
    qdrant_collection: str = "ai_learn_knowledge"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")


settings = Settings()
