import re

from app.schemas.grading import GradeAnswerRequest, GradeAnswerResponse

_SPLIT_REGEX = re.compile(r"[\s，。、；：,.!?！？（）()\"'“”‘’]+")


def grade_answer(request: GradeAnswerRequest) -> GradeAnswerResponse:
    """使用本地可解释规则生成评分结果。"""
    keywords = _build_keywords(request.standardAnswer, request.knowledgePoints)
    answer = request.userAnswer.strip()
    lower_answer = answer.lower()
    hit_points: list[str] = []
    missing_points: list[str] = []

    for keyword in keywords:
        if keyword.lower() in lower_answer:
            hit_points.append(f"命中了「{keyword}」相关要点")
        else:
            missing_points.append(f"缺少「{keyword}」相关说明")

    score = _calculate_score(len(hit_points), len(keywords), len(answer))
    problems = []
    if len(answer) < 20:
        problems.append("回答较简略，建议补充关键流程和原因说明")
    if not hit_points:
        problems.append("未明显覆盖标准答案中的核心关键词")

    advice = "整体回答较完整，建议继续补充工程化细节和实际案例。"
    if missing_points:
        advice = "建议优先补充：" + "；".join(missing_points) + "。"
    elif problems:
        advice = "；".join(problems) + "。"

    return GradeAnswerResponse(
        score=score,
        isCorrect=score >= 60,
        hitPoints=hit_points,
        missingPoints=missing_points,
        problems=problems,
        referenceAnswer=request.standardAnswer,
        improvementAdvice=advice,
        reviewKnowledgePoints=request.knowledgePoints or [item.replace("缺少「", "").replace("」相关说明", "") for item in missing_points],
    )


def _build_keywords(standard_answer: str, knowledge_points: list[str]) -> list[str]:
    """从知识点和标准答案提取关键词。"""
    keywords: list[str] = []
    for value in [*knowledge_points, *_SPLIT_REGEX.split(standard_answer)]:
        item = value.strip()
        if 2 <= len(item) <= 20 and item not in keywords:
            keywords.append(item)
        if len(keywords) >= 8:
            break
    return keywords


def _calculate_score(hit_count: int, total_count: int, answer_length: int) -> int:
    """计算 0-100 分。"""
    if total_count == 0:
        return min(100, 20 + min(answer_length, 80))
    keyword_score = round(hit_count * 80 / total_count)
    content_score = 20 if answer_length >= 20 else answer_length
    return max(0, min(100, keyword_score + content_score))
