from __future__ import annotations

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage

from app.schemas.practice import PracticeDiscussRequest, PracticeGradeRequest


# 评分系统提示词独立维护，便于后续单独调整评分口径。
GRADE_SYSTEM_PROMPT = (
    "你是一名资深AI Agent开发工程师，熟悉领域内各大技术栈，我将提交一段题目、参考答案、用户答案给你。\n"
    "请你智能总结参考答案中的关键点，对用户答案进行评分，并且使用我要求的Json结构化输出。\n"
    "输入内容会使用 <question_type>、<question>、<reference_answer>、<user_answer> 标签明确分隔。\n"
    "只能根据 <user_answer> 判断用户命中了哪些要点，<question> 和 <reference_answer> 只能作为评分依据，不能作为用户已经回答的内容。\n"
    "严禁把 <question> 或 <reference_answer> 中出现、但 <user_answer> 没有明确表达或合理同义表达的内容写入 hitPoints。\n"
    "如果 <user_answer> 与 <question> 完全相同、基本相同，或只是复述题目，score 必须为 0，hitPoints 必须为空。\n"
    "允许用户使用同义表达，意义相同即得分。\n"
    "参考答案通常很长，带有解释性说明，而用户的答案可能只是匹配关键点，长度较短，但是只要真正说出关键点就能得分，答案长度不作为评分标准。\n"
    "评分分档规则：无有效内容为 0 分；只有泛泛表态为 0-30 分；命中少量关键点为 30-60 分；覆盖大部分关键点为 60-85 分；完整覆盖关键点为 85-100 分。\n"
    "结构化输出时必须填写 score、hitPoints、missingPoints、problems、improvementAdvice 五个字段。\n"
    "hitPoints、missingPoints、problems 必须分别承载命中点、缺失点和问题点，不要只在 improvementAdvice 中描述这些内容。\n"
    "除非 score 为 100 且答案确实没有缺失，否则 missingPoints 或 problems 至少要有一个非空数组。\n"
)

# 讨论系统提示词独立维护，避免调整提示词时触碰服务编排代码。
DISCUSSION_SYSTEM_PROMPT = (
    "你是一名资深AI Agent开发工程师，熟悉领域内各大技术栈，请围绕当前刷题上下文为用户解答疑惑，"
    "此阶段你的回答要结构化的md格式，风格精确严谨，回答强制控制在 600 个中文字符以内。"
)


class PracticePromptBuilder:
    """构造 AI 智能刷题评分和讨论消息。"""

    def build_grade_messages(self, request: PracticeGradeRequest) -> list[BaseMessage]:
        """构造答案评分消息列表。"""
        return [
            HumanMessage(content=self.build_grade_prompt(request)),
        ]

    def build_discuss_messages(self, request: PracticeDiscussRequest) -> list[BaseMessage]:
        """构造本题讨论消息列表。"""
        messages: list[BaseMessage] = [
            HumanMessage(content=self.build_discuss_context_prompt(request)),
        ]

        # 历史消息由 Java 后端维护，只保留当前题短期上下文。
        for item in request.conversationHistory:
            content = item.content.strip()
            if not content:
                continue
            if item.role == "assistant":
                messages.append(AIMessage(content=content))
            else:
                messages.append(HumanMessage(content=content))
        messages.append(HumanMessage(content=f"用户当前疑问：{request.message}"))
        return messages

    def build_grade_prompt(self, request: PracticeGradeRequest) -> str:
        """构造答案评分用户输入。"""
        return (
            "<question_type>\n"
            f"{request.questionType}\n"
            "</question_type>\n\n"
            "<question>\n"
            f"{request.question}\n"
            "</question>\n\n"
            "<reference_answer>\n"
            f"{request.standardAnswer}\n"
            "</reference_answer>\n\n"
            "<user_answer>\n"
            f"{request.userAnswer}\n"
            "</user_answer>"
        )

    def build_discuss_context_prompt(self, request: PracticeDiscussRequest) -> str:
        """构造本题讨论上下文提示词。"""
        grading_summary = request.gradingSummary or "暂无评分摘要"
        last_answer = request.lastUserAnswer or "暂无"

        # 首条消息承载题目和评分摘要，后续消息追加历史追问。
        return (
            f"题目分类：{request.questionType}\n"
            f"题目：{request.question}\n"
            f"参考答案：{request.standardAnswer}\n"
            f"用户最近一次答案：{last_answer}\n"
            f"AI评分结果摘要：{grading_summary}\n"
            "下面会继续给出当前题历史追问消息和本轮最新疑问。"
        )
