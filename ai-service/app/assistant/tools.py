"""工具仅定义模型可选参数，业务执行全部由 Java 和受控页面完成。"""

from typing import Literal

from langchain_core.tools import tool

from app.assistant.knowledge import search


@tool
def search_site_knowledge(query: str) -> list[dict[str, str]]:
    """查询本站公开功能说明、使用规则和学习资料。"""
    return search(query)


@tool
def navigate_page(pageKey: Literal["home", "learning-roadmap", "practice-agent", "interview-questions", "suggestions-comments", "profile"], tab: Literal["suggestions", "comments"] = "suggestions") -> str:
    """打开本站页面；评论区使用 suggestions-comments 和 comments。"""
    return "由站内执行器导航"


@tool
def list_comments(pageNo: int = 1, pageSize: int = 10, sort: Literal["hot", "latest"] = "latest") -> str:
    """查看评论列表和父评论 ID，以便选择回复对象。"""
    return "由业务后端查询"


@tool
def list_suggestions(pageNo: int = 1, pageSize: int = 10, sort: Literal["hot", "latest"] = "latest") -> str:
    """查看建议列表。"""
    return "由业务后端查询"


@tool
def prepare_community_draft(kind: Literal["comment", "reply", "suggestion"], content: str, parentId: str = "", type: Literal["FEATURE", "EXPERIENCE", "BUG", "CONTENT"] = "FEATURE") -> str:
    """在真实页面填写草稿，不发布；回复必须指定父评论 ID。"""
    return "由页面执行器填写"


@tool
def create_comment(content: str, parentId: str = "") -> str:
    """在真实表单填写并发布评论或一级回复；必须有用户发布意图，生成内容须确认。"""
    return "由页面执行器填写并提交"


@tool
def create_suggestion(content: str, type: Literal["FEATURE", "EXPERIENCE", "BUG", "CONTENT"]) -> str:
    """在真实建议表单选择类型、填写和发布；内容与类型须明确。"""
    return "由页面执行器填写并提交"


@tool
def get_my_model_entitlement() -> str:
    """查询当前登录用户的模型权益。"""
    return "由业务后端查询"


@tool
def get_my_growth() -> str:
    """查询当前登录用户的经验、等级、段位和勋章。"""
    return "由业务后端查询"


@tool
def get_my_practice_stats() -> str:
    """查询当前登录用户的智能刷题统计概览。"""
    return "由业务后端查询"


TOOLS = [search_site_knowledge, navigate_page, list_comments, list_suggestions, prepare_community_draft, create_comment, create_suggestion, get_my_model_entitlement, get_my_growth, get_my_practice_stats]
