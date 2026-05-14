<template>
  <section class="question-page">
    <div class="question-hero">
      <div>
        <p class="eyebrow">默认题库基础</p>
        <h2>热门面经</h2>
        <p class="hero-desc">浏览 AI、RAG、Agent 等方向的精选题目，为后续智能刷题打好基础。</p>
      </div>
      <el-tag type="success" effect="light">登录后可浏览</el-tag>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="filters" label-position="top" class="filter-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="搜索标题或内容" @keyup.enter="searchQuestions" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="filters.difficulty" clearable placeholder="全部难度">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="filters.questionType" clearable placeholder="全部题型">
            <el-option label="简答题" value="SHORT_ANSWER" />
            <el-option label="选择题" value="CHOICE" />
            <el-option label="编程题" value="CODE" />
            <el-option label="场景题" value="SCENARIO" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="filters.knowledgePointId" clearable filterable placeholder="全部知识点">
            <el-option v-for="item in knowledgePoints" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" round :loading="loading" @click="searchQuestions">查询</el-button>
          <el-button round @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="card-header">
          <span>题目列表</span>
          <el-button text type="primary" :loading="loading" @click="loadQuestions">刷新</el-button>
        </div>
      </template>

      <el-skeleton :loading="loading" animated :rows="5">
        <el-empty v-if="questions.length === 0" description="暂无匹配题目，换个筛选条件试试" />
        <div v-else class="question-list">
          <article v-for="item in questions" :key="item.id" class="question-card" @click="openDetail(item.id)">
            <div class="question-title-row">
              <h3>{{ item.title }}</h3>
              <div class="tag-row">
                <el-tag :type="resolveDifficultyType(item.difficulty)" effect="light">{{ item.difficultyText }}</el-tag>
                <el-tag effect="plain">{{ item.questionTypeText }}</el-tag>
              </div>
            </div>
            <div class="tag-row muted-tags">
              <el-tag v-for="tag in item.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
            </div>
            <p class="knowledge-line">知识点：{{ item.knowledgePoints.join('、') || '暂未关联' }}</p>
            <p class="time-line">创建时间：{{ formatTime(item.createdAt) }}</p>
          </article>
        </div>
      </el-skeleton>

      <el-pagination
        v-model:current-page="page.pageNo"
        v-model:page-size="page.pageSize"
        layout="prev, pager, next, total"
        :total="page.total"
        @current-change="loadQuestions"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="题目详情" width="760px" class="question-detail-dialog">
      <el-skeleton :loading="detailLoading" animated :rows="8">
        <div v-if="detail" class="detail-content">
          <h3>{{ detail.title }}</h3>
          <div class="tag-row">
            <el-tag :type="resolveDifficultyType(detail.difficulty)" effect="light">{{ detail.difficultyText }}</el-tag>
            <el-tag effect="plain">{{ detail.questionTypeText }}</el-tag>
            <el-tag v-for="tag in detail.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
          </div>
          <section>
            <h4>题目内容</h4>
            <p>{{ detail.content }}</p>
          </section>
          <section>
            <h4>参考答案</h4>
            <p>{{ detail.standardAnswer }}</p>
          </section>
          <section>
            <h4>解析</h4>
            <p>{{ detail.analysis || '暂无解析' }}</p>
          </section>
          <section>
            <h4>知识点</h4>
            <p>{{ detail.knowledgePoints.join('、') || '暂未关联' }}</p>
          </section>
        </div>
      </el-skeleton>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref, watch } from 'vue';
import { fetchKnowledgePoints, fetchQuestionDetail, fetchQuestions } from '../../api/questions';
import type { KnowledgePointItem, QuestionDetail, QuestionListItem } from '../../types/question';

const PAGE_SIZE = 10;
const loading = ref(false);
const detailLoading = ref(false);
const detailVisible = ref(false);
const questions = ref<QuestionListItem[]>([]);
const knowledgePoints = ref<KnowledgePointItem[]>([]);
const detail = ref<QuestionDetail | null>(null);

const page = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const filters = reactive({ keyword: '', difficulty: '', questionType: '', knowledgePointId: '' });

watch(
  () => [filters.difficulty, filters.questionType, filters.knowledgePointId],
  () => searchQuestions(),
);

/**
 * 加载知识点筛选数据。
 */
async function loadKnowledgePoints(): Promise<void> {
  knowledgePoints.value = await fetchKnowledgePoints();
}

/**
 * 按当前条件加载题目列表。
 */
async function loadQuestions(): Promise<void> {
  loading.value = true;
  try {
    const result = await fetchQuestions({
      pageNo: page.pageNo,
      pageSize: page.pageSize,
      keyword: filters.keyword.trim(),
      difficulty: filters.difficulty,
      questionType: filters.questionType,
      knowledgePointId: filters.knowledgePointId,
    });
    questions.value = result.records;
    page.total = result.total;
  } finally {
    loading.value = false;
  }
}

/**
 * 重置到第一页并查询。
 */
async function searchQuestions(): Promise<void> {
  page.pageNo = 1;
  await loadQuestions();
}

/**
 * 清空筛选条件。
 */
async function resetFilters(): Promise<void> {
  filters.keyword = '';
  filters.difficulty = '';
  filters.questionType = '';
  filters.knowledgePointId = '';
  await searchQuestions();
}

/**
 * 打开题目详情弹窗。
 */
async function openDetail(id: string): Promise<void> {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = null;
  try {
    detail.value = await fetchQuestionDetail(id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '题目详情加载失败');
    detailVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
}

/**
 * 解析难度标签样式。
 */
function resolveDifficultyType(difficulty: string): 'success' | 'warning' | 'danger' | 'info' {
  if (difficulty === 'EASY') {
    return 'success';
  }
  if (difficulty === 'MEDIUM') {
    return 'warning';
  }
  if (difficulty === 'HARD') {
    return 'danger';
  }
  return 'info';
}

/**
 * 格式化本地展示时间。
 */
function formatTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

onMounted(async () => {
  await Promise.all([loadKnowledgePoints(), loadQuestions()]);
});
</script>

<style scoped lang="scss">
.question-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.question-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px;
  border-radius: 24px;
  background: linear-gradient(135deg, #f5f8ff 0%, #f8fffb 100%);
}

.eyebrow {
  margin: 0 0 8px;
  color: #3b82f6;
  font-size: 13px;
  font-weight: 700;
}

.question-hero h2 {
  margin: 0;
  color: #1f2a44;
  font-size: 30px;
}

.hero-desc {
  margin: 10px 0 0;
  color: #667085;
}

.filter-card,
.list-card {
  border: 1px solid #edf2f7;
  border-radius: 18px;
}

.filter-form {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1fr auto;
  gap: 14px;
  align-items: end;
}

.filter-actions {
  margin-bottom: 18px;
}

.card-header,
.question-title-row,
.tag-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 18px;
}

.question-card {
  padding: 18px;
  cursor: pointer;
  border: 1px solid #edf2f7;
  border-radius: 16px;
  background: #fbfdff;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.question-card:hover {
  border-color: #9cc8ff;
  transform: translateY(-1px);
}

.question-card h3 {
  margin: 0;
  color: #1f2a44;
}

.tag-row {
  flex-wrap: wrap;
  justify-content: flex-start;
}

.muted-tags {
  margin-top: 12px;
}

.knowledge-line,
.time-line {
  margin: 10px 0 0;
  color: #667085;
  font-size: 14px;
}

.detail-content h3 {
  margin: 0 0 14px;
  color: #1f2a44;
  font-size: 22px;
}

.detail-content section {
  margin-top: 20px;
  padding: 16px;
  border-radius: 14px;
  background: #f8fafc;
}

.detail-content h4 {
  margin: 0 0 8px;
  color: #344054;
}

.detail-content p {
  margin: 0;
  color: #475467;
  line-height: 1.8;
  white-space: pre-wrap;
}

@media (max-width: 1080px) {
  .filter-form {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .filter-form {
    grid-template-columns: 1fr;
  }

  .question-title-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
