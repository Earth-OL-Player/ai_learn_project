import time


def elapsed_milliseconds(start_time: float) -> int:
    """根据 perf_counter 起点计算已耗时毫秒。"""
    # 保持原有 round 语义，避免耗时日志和观测指标口径变化。
    return round((time.perf_counter() - start_time) * 1000)
