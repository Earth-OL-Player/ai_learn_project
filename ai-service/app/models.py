"""刷题和站内助手共享模型构造缓存，不共享对话状态。"""

from app.practice.model_factory import PracticeModelFactory
from app.practice.provider_adapter import PracticeProviderAdapter

provider_adapter = PracticeProviderAdapter()
model_factory = PracticeModelFactory(provider_adapter)
