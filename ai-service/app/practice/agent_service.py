from __future__ import annotations

import re

from app.rag.index_task_service import rag_service
from app.schemas.practice import PracticeDiscussRequest, PracticeDiscussResponse, PracticeGradeRequest, PracticeGradeResponse

_SPLIT_PATTERN = re.compile(r"[\s，。、；：,.!?！？（）()\"'“”‘’]+")
_MAX_KEYWORDS = 10


class PracticeAgentService:
    """AI 智能刷题 Agent 服务。"""

    def grade_answer(self, request: PracticeGradeRequest) -> PracticeGradeResponse:
        """结合 RAG 片段和本地规则给答案评分。"""
        keywords = self._build_keywords(request)
        answer = request.userAnswer.strip()

        # 命中判断保持大小写不敏感，兼容英文技术名词。
        lowered_answer = answer.lower()
        hit_points: list[str] = []
        missing_points: list[str] = []
        for keyword in keywords:
            if keyword.lower() in lowered_answer:
                hit_points.append(f"命中了「{keyword}」相关要点")
            else:
                missing_points.append(f"缺少「{keyword}」相关说明")

        # 分数由关键词覆盖度和内容完整度共同决定。
        score = self._calculate_score(len(hit_points), len(keywords), len(answer))
        problems = self._build_problems(answer, not hit_points)
        advice = self._build_advice(score, missing_points, problems)
        return PracticeGradeResponse(
            score=score,
            correct=score >= 60,
            hitPoints=hit_points,
            missingPoints=missing_points,
            problems=problems,
            referenceAnswer=request.standardAnswer,
            improvementAdvice=advice,
            reviewKnowledgePoints=self._review_points(request.questionType, missing_points),
        )

    def discuss(self, request: PracticeDiscussRequest) -> PracticeDiscussResponse:
        """围绕当前题生成学习讨论回复。"""
        snippets = self._search_snippets(request.message)
        snippet_text = "；".join(snippets[:2]) if snippets else "暂无额外检索片段"

        # 讨论回复不持久化，只返回给后端展示。
        reply = (
            f"我们继续看这道「{request.questionType}」题。"
            f"你的问题是：{request.message.strip()}。"
            f"建议先对照参考答案确认核心点：{request.standardAnswer}。"
            f"可参考的检索片段：{snippet_text}。"
        )
        return PracticeDiscussResponse(reply=reply)

    def _build_keywords(self, request: PracticeGradeRequest) -> list[str]:
        """从题目、分类、参考答案和 RAG 片段构建关键词。"""
        keywords: list[str] = []
        self._add_keyword(keywords, request.questionType)

        # RAG 检索失败不能影响本地评分主流程。
        for snippet in self._search_snippets(request.question + " " + request.standardAnswer):
            for token in _SPLIT_PATTERN.split(snippet):
                self._add_keyword(keywords, token)

        for token in _SPLIT_PATTERN.split(request.standardAnswer):
            self._add_keyword(keywords, token)
        return keywords[:_MAX_KEYWORDS]

    def _search_snippets(self, query: str) -> list[str]:
        """从 Qdrant 检索相关片段。"""
        try:
            return [item.chunkText for item in rag_service.search(query, 3)]
        except Exception:  # noqa: BLE001 - 向量服务失败时使用本地评分兜底
            return []

    def _add_keyword(self, keywords: list[str], value: str | None) -> None:
        """追加单个关键词。"""
        if not value:
            return
        keyword = value.strip()
        if 2 <= len(keyword) <= 24 and keyword not in keywords:
            keywords.append(keyword)

    def _calculate_score(self, hit_count: int, total_count: int, answer_length: int) -> int:
        """计算百分制得分。"""
        if total_count <= 0:
            return min(100, max(0, answer_length))
        keyword_score = round(hit_count / total_count * 80)
        content_score = 20 if answer_length >= 20 else answer_length
        return min(100, max(0, keyword_score + content_score))

    def _build_problems(self, answer: str, no_keyword_hit: bool) -> list[str]:
        """生成问题点。"""
        problems: list[str] = []
        if len(answer) < 20:
            problems.append("回答较简略，建议补充关键流程和原因说明")
        if no_keyword_hit:
            problems.append("未明显覆盖参考答案中的核心关键词")
        return problems

    def _build_advice(self, score: int, missing_points: list[str], problems: list[str]) -> str:
        """生成优化建议。"""
        if score >= 90 and not missing_points:
            return "回答已经很完整，继续保持这种结构化表达。"
        if missing_points:
            return "建议优先补充：" + "；".join(missing_points) + "。"
        if problems:
            return "；".join(problems) + "。"
        return "整体回答不错，可以再补充一个工程实践案例。"

    def _review_points(self, question_type: str, missing_points: list[str]) -> list[str]:
        """提取建议复习点。"""
        points = [item.replace("缺少「", "").replace("」相关说明", "") for item in missing_points]
        return points or [question_type]


practice_agent_service = PracticeAgentService()
