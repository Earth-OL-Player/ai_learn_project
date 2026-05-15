<template>
  <section class="practice-page">
    <div class="practice-hero">
      <div>
        <p class="eyebrow">AI智能刷题基础闭环</p>
        <h2>开始一次可解释的刷题练习</h2>
        <p>本期使用默认题库和本地规则评分，先完成出题、答题、评分、记录和成长反馈闭环。</p>
      </div>
      <el-button type="primary" round :loading="starting" @click="handleStart">{{ question ? '再来一题' : '开始刷题' }}</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="filters" label-position="top" class="filter-form">
        <el-form-item label="难度">
          <el-select v-model="filters.difficulty" clearable placeholder="不限难度">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="filters.questionType" clearable placeholder="不限题型">
            <el-option label="简答题" value="SHORT_ANSWER" />
            <el-option label="选择题" value="CHOICE" />
            <el-option label="编程题" value="CODE" />
            <el-option label="场景题" value="SCENARIO" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="filters.knowledgePointIds" multiple clearable filterable placeholder="不限知识点">
            <el-option v-for="item in knowledgePoints" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题库范围">
          <el-select v-model="filters.sourceScope" placeholder="默认题库">
            <el-option label="默认题库" value="DEFAULT" />
            <el-option label="我的题库" value="MINE" />
            <el-option label="混合题库" value="MIXED" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty v-if="!question" class="idle-empty" description="点击开始刷题，系统会优先推荐未刷过的默认题。" />

    <el-card v-else shadow="never" class="question-card">
      <template #header>
        <div class="card-header">
          <span>{{ question.title }}</span>
          <div class="tag-row">
            <el-tag type="warning" effect="light">{{ question.difficultyText }}</el-tag>
            <el-tag effect="plain">{{ question.questionTypeText }}</el-tag>
          </div>
        </div>
      </template>

      <p class="question-content">{{ question.content }}</p>
      <p class="knowledge-line">知识点：{{ question.knowledgePoints.join('、') || '暂未关联' }}</p>
      <el-alert class="recommend-alert" type="success" show-icon :closable="false" :title="question.recommendReason" />

      <el-input
        v-model="answer"
        type="textarea"
        :autosize="{ minRows: 6, maxRows: 10 }"
        maxlength="5000"
        show-word-limit
        placeholder="请输入你的答案，尽量说明关键概念、流程和原因。"
      />

      <div class="answer-actions">
        <span class="time-tip">已用时：{{ elapsedSeconds }} 秒</span>
        <el-button type="primary" round :loading="submitting" @click="handleSubmit">提交答案</el-button>
      </div>
    </el-card>

    <el-card v-if="result" shadow="never" class="result-card">
      <template #header>
        <div class="card-header">
          <span>评分结果</span>
          <el-tag :type="result.isCorrect ? 'success' : 'danger'" effect="light">{{ result.score }} 分</el-tag>
        </div>
      </template>

      <el-alert type="info" show-icon :closable="false" title="评分结果仅供学习参考。" />
      <p class="grading-source">评分来源：{{ result.gradingSource === 'AI_SERVICE' ? 'AI 服务评分' : '本地规则评分' }}</p>
      <el-alert
        v-if="result.growth.newBadges.length > 0"
        type="success"
        show-icon
        :closable="false"
        :title="`本次获得新徽章：${result.growth.newBadges.map((badge) => `${badge.icon} ${badge.name}`).join('、')}`"
      />
      <div class="result-grid">
        <section>
          <h3>命中点</h3>
          <el-empty v-if="result.hitPoints.length === 0" description="暂无明显命中点" :image-size="80" />
          <ul v-else><li v-for="item in result.hitPoints" :key="item">{{ item }}</li></ul>
        </section>
        <section>
          <h3>缺失点</h3>
          <el-empty v-if="result.missingPoints.length === 0" description="暂无明显缺失点" :image-size="80" />
          <ul v-else><li v-for="item in result.missingPoints" :key="item">{{ item }}</li></ul>
        </section>
      </div>

      <section class="result-section">
        <h3>问题提醒</h3>
        <p>{{ result.problems.join('；') || '暂无明显问题。' }}</p>
      </section>
      <section class="result-section">
        <h3>参考答案</h3>
        <p>{{ result.referenceAnswer }}</p>
      </section>
      <section class="result-section">
        <h3>改进建议</h3>
        <p>{{ result.improvementAdvice }}</p>
      </section>
      <section class="growth-line">
        <el-statistic title="本次经验" :value="result.growth.earnedExperience" />
        <el-statistic title="当前经验" :value="result.growth.currentExperience" />
        <el-statistic title="累计答题" :value="result.growth.answeredCount" />
        <el-statistic title="连续学习" :value="result.growth.streakDays" suffix="天" />
        <div class="level-box">{{ result.growth.level }} · {{ result.growth.levelName }} · {{ result.growth.rank }}</div>
      </section>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { fetchKnowledgePoints } from '../../api/questions';
import { startPractice, submitPractice } from '../../api/practice';
import type { KnowledgePointItem } from '../../types/question';
import type { PracticeQuestion, PracticeSubmitResult } from '../../types/practice';

const starting = ref(false);
const submitting = ref(false);
const answer = ref('');
const question = ref<PracticeQuestion | null>(null);
const result = ref<PracticeSubmitResult | null>(null);
const knowledgePoints = ref<KnowledgePointItem[]>([]);
const startedAt = ref<number | null>(null);
const now = ref(Date.now());
let timer: number | undefined;

const filters = reactive({ difficulty: '', questionType: '', knowledgePointIds: [] as string[], sourceScope: 'DEFAULT' });

// 答题计时只在当前题目存在时展示，避免后台空转。
const elapsedSeconds = computed(() => {
  if (!startedAt.value) {
    return 0;
  }
  return Math.max(0, Math.floor((now.value - startedAt.value) / 1000));
});

/**
 * 开始刷题。
 */
async function handleStart(): Promise<void> {
  starting.value = true;
  try {
    question.value = await startPractice({
      difficulty: filters.difficulty || undefined,
      questionType: filters.questionType || undefined,
      knowledgePointIds: filters.knowledgePointIds,
      sourceScope: filters.sourceScope,
    });
    answer.value = '';
    result.value = null;
    startedAt.value = Date.now();
    now.value = Date.now();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '开始刷题失败');
  } finally {
    starting.value = false;
  }
}

/**
 * 提交答案。
 */
async function handleSubmit(): Promise<void> {
  if (!question.value) {
    return;
  }
  if (!answer.value.trim()) {
    ElMessage.warning('请先输入答案');
    return;
  }
  submitting.value = true;
  try {
    result.value = await submitPractice({
      sessionId: question.value.sessionId,
      questionId: question.value.questionId,
      userAnswer: answer.value.trim(),
      durationSeconds: elapsedSeconds.value,
    });
    ElMessage.success('评分完成，答题记录已保存');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交答案失败');
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  knowledgePoints.value = await fetchKnowledgePoints();
  timer = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

onBeforeUnmount(() => {
  if (timer !== undefined) {
    window.clearInterval(timer);
  }
});
</script>

<style scoped lang="scss">
.practice-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.practice-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 30px;
  border-radius: 24px;
  background: linear-gradient(135deg, #f7fbff 0%, #f7fff8 100%);
}

.eyebrow {
  margin: 0 0 8px;
  color: #3b82f6;
  font-size: 13px;
  font-weight: 700;
}

.practice-hero h2 {
  margin: 0;
  color: #1f2a44;
  font-size: 30px;
}

.practice-hero p {
  margin: 10px 0 0;
  color: #667085;
}

.filter-card,
.question-card,
.result-card {
  border: 1px solid #edf2f7;
  border-radius: 18px;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.idle-empty {
  padding: 42px 0;
  border: 1px dashed #d9e5f5;
  border-radius: 20px;
  background: #fbfdff;
}

.card-header,
.tag-row,
.answer-actions,
.growth-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.question-content,
.knowledge-line,
.result-section p {
  color: #475467;
  line-height: 1.8;
  white-space: pre-wrap;
}

.knowledge-line {
  margin-bottom: 18px;
}

.recommend-alert {
  margin-bottom: 18px;
}

.grading-source {
  color: #667085;
  font-size: 14px;
}

.answer-actions {
  margin-top: 16px;
}

.time-tip {
  color: #667085;
  font-size: 14px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.result-grid section,
.result-section {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.result-grid h3,
.result-section h3 {
  margin: 0 0 10px;
  color: #1f2a44;
}

.result-grid ul {
  margin: 0;
  padding-left: 18px;
  color: #475467;
  line-height: 1.9;
}

.growth-line {
  flex-wrap: wrap;
  margin-top: 18px;
  padding: 18px;
  border-radius: 16px;
  background: #f5f8ff;
}

.level-box {
  color: #1f2a44;
  font-weight: 700;
}

@media (max-width: 900px) {
  .filter-form,
  .result-grid {
    grid-template-columns: 1fr;
  }

  .practice-hero,
  .card-header,
  .answer-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
