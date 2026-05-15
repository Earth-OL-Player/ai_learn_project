<template>
  <section class="my-question-page">
    <div class="page-hero">
      <div>
        <p class="eyebrow">个人题库</p>
        <h2>我的题库</h2>
        <p>维护自己的面试题和复习题，AI智能刷题可选择从个人题库中推荐。</p>
      </div>
      <div class="hero-actions">
        <el-button round @click="showImportDialog = true">JSON 导入</el-button>
        <el-button type="primary" round @click="openCreateDialog">新增题目</el-button>
      </div>
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
        <el-form-item class="filter-actions">
          <el-button type="primary" round :loading="loading" @click="searchQuestions">查询</el-button>
          <el-button round @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="list-card">
      <el-skeleton :loading="loading" animated :rows="5">
        <el-empty v-if="questions.length === 0" description="暂无个人题目，可以新增或导入一批题目" />
        <div v-else class="question-list">
          <article v-for="item in questions" :key="item.id" class="question-card">
            <div class="question-title-row">
              <h3>{{ item.title }}</h3>
              <div class="tag-row">
                <el-tag type="warning" effect="light">{{ item.difficultyText }}</el-tag>
                <el-tag effect="plain">{{ item.questionTypeText }}</el-tag>
              </div>
            </div>
            <p>{{ item.content }}</p>
            <div class="tag-row">
              <el-tag v-for="tag in item.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
            </div>
            <p class="knowledge-line">知识点：{{ item.knowledgePoints.join('、') || '暂未关联' }}</p>
            <div class="card-actions">
              <el-button text type="primary" @click="openDetail(item)">查看答案</el-button>
              <el-button text type="danger" @click="handleDelete(item.id)">删除</el-button>
            </div>
          </article>
        </div>
      </el-skeleton>
      <el-pagination
        v-model:current-page="page.pageNo"
        layout="prev, pager, next, total"
        :total="page.total"
        :page-size="page.pageSize"
        @current-change="loadQuestions"
      />
    </el-card>

    <el-dialog v-model="showCreateDialog" title="新增个人题目" width="760px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" maxlength="120" /></el-form-item>
        <el-form-item label="题目内容"><el-input v-model="form.content" type="textarea" :rows="4" maxlength="5000" /></el-form-item>
        <div class="dialog-grid">
          <el-form-item label="题型">
            <el-select v-model="form.questionType"><el-option label="简答题" value="SHORT_ANSWER" /><el-option label="场景题" value="SCENARIO" /><el-option label="选择题" value="CHOICE" /><el-option label="编程题" value="CODE" /></el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty"><el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" /></el-select>
          </el-form-item>
        </div>
        <el-form-item label="标签，逗号分隔"><el-input v-model="tagText" placeholder="RAG,Agent" /></el-form-item>
        <el-form-item label="知识点，逗号分隔"><el-input v-model="knowledgeText" placeholder="RAG,向量数据库" /></el-form-item>
        <el-form-item label="参考答案"><el-input v-model="form.standardAnswer" type="textarea" :rows="4" maxlength="5000" /></el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="3" maxlength="5000" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImportDialog" title="JSON 批量导入" width="760px">
      <el-alert type="info" :closable="false" title="导入内容必须是题目数组，真实数据请勿包含敏感信息。" />
      <el-radio-group v-model="importMode" class="import-mode">
        <el-radio-button label="APPEND">追加</el-radio-button>
        <el-radio-button label="REPLACE">全量替换</el-radio-button>
      </el-radio-group>
      <el-input v-model="importJson" type="textarea" :rows="12" placeholder='[{"title":"题目","content":"内容","questionType":"SHORT_ANSWER","difficulty":"EASY","tags":["RAG"],"knowledgePoints":["RAG"],"standardAnswer":"参考答案"}]' />
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="参考答案" width="680px">
      <section v-if="detail" class="answer-detail">
        <h3>{{ detail.title }}</h3>
        <h4>参考答案</h4>
        <p>{{ detail.standardAnswer }}</p>
        <h4>解析</h4>
        <p>{{ detail.analysis || '暂无解析' }}</p>
      </section>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { createMyQuestion, deleteMyQuestion, fetchMyQuestions, importMyQuestions } from '../../api/myQuestions';
import type { MyQuestionItem, MyQuestionPayload } from '../../types/my-question';

const PAGE_SIZE = 10;
const loading = ref(false);
const saving = ref(false);
const importing = ref(false);
const showCreateDialog = ref(false);
const showImportDialog = ref(false);
const showDetailDialog = ref(false);
const questions = ref<MyQuestionItem[]>([]);
const detail = ref<MyQuestionItem | null>(null);
const tagText = ref('');
const knowledgeText = ref('');
const importMode = ref<'APPEND' | 'REPLACE'>('APPEND');
const importJson = ref('');
const page = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const filters = reactive({ keyword: '', difficulty: '', questionType: '' });
const form = reactive<MyQuestionPayload>(buildEmptyForm());

/**
 * 构建空表单。
 */
function buildEmptyForm(): MyQuestionPayload {
  return { title: '', content: '', questionType: 'SHORT_ANSWER', difficulty: 'EASY', tags: [], knowledgePoints: [], standardAnswer: '', analysis: '' };
}

/**
 * 加载个人题目。
 */
async function loadQuestions(): Promise<void> {
  loading.value = true;
  try {
    const result = await fetchMyQuestions({ pageNo: page.pageNo, pageSize: page.pageSize, ...filters });
    questions.value = result.records;
    page.total = result.total;
  } finally {
    loading.value = false;
  }
}

/**
 * 查询个人题目。
 */
async function searchQuestions(): Promise<void> {
  page.pageNo = 1;
  await loadQuestions();
}

/**
 * 重置筛选。
 */
async function resetFilters(): Promise<void> {
  filters.keyword = '';
  filters.difficulty = '';
  filters.questionType = '';
  await searchQuestions();
}

/**
 * 打开新增弹窗。
 */
function openCreateDialog(): void {
  Object.assign(form, buildEmptyForm());
  tagText.value = '';
  knowledgeText.value = '';
  showCreateDialog.value = true;
}

/**
 * 保存个人题目。
 */
async function handleCreate(): Promise<void> {
  if (!form.title.trim() || !form.content.trim() || !form.standardAnswer.trim()) {
    ElMessage.warning('请填写标题、内容和参考答案');
    return;
  }
  saving.value = true;
  try {
    await createMyQuestion({ ...form, tags: splitText(tagText.value), knowledgePoints: splitText(knowledgeText.value) });
    ElMessage.success('个人题目已新增');
    showCreateDialog.value = false;
    await searchQuestions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

/**
 * 导入个人题库。
 */
async function handleImport(): Promise<void> {
  try {
    const questionsPayload = JSON.parse(importJson.value) as MyQuestionPayload[];
    if (!Array.isArray(questionsPayload)) {
      ElMessage.warning('JSON 必须是题目数组');
      return;
    }
    importing.value = true;
    const result = await importMyQuestions({ mode: importMode.value, questions: questionsPayload });
    ElMessage.success(`已导入 ${result.importedCount} 道题`);
    showImportDialog.value = false;
    await searchQuestions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'JSON 格式不合法');
  } finally {
    importing.value = false;
  }
}

/**
 * 删除个人题目。
 */
async function handleDelete(id: string): Promise<void> {
  await ElMessageBox.confirm('确认删除这道个人题目吗？', '删除确认', { type: 'warning' });
  await deleteMyQuestion(id);
  ElMessage.success('已删除');
  await loadQuestions();
}

/**
 * 打开答案详情。
 */
function openDetail(item: MyQuestionItem): void {
  detail.value = item;
  showDetailDialog.value = true;
}

/**
 * 拆分逗号文本。
 */
function splitText(value: string): string[] {
  return value.split(/[,，]/).map((item) => item.trim()).filter(Boolean);
}

onMounted(loadQuestions);
</script>

<style scoped lang="scss">
.my-question-page { display: flex; flex-direction: column; gap: 18px; }
.page-hero { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 30px; border-radius: 24px; background: linear-gradient(135deg, #f7fbff 0%, #f7fff8 100%); }
.eyebrow { margin: 0 0 8px; color: #3b82f6; font-size: 13px; font-weight: 700; }
.page-hero h2 { margin: 0; color: #1f2a44; font-size: 30px; }
.page-hero p, .question-card p, .knowledge-line, .answer-detail p { color: #475467; line-height: 1.8; white-space: pre-wrap; }
.hero-actions, .question-title-row, .tag-row, .card-actions { display: flex; align-items: center; gap: 12px; }
.filter-card, .list-card { border: 1px solid #edf2f7; border-radius: 18px; }
.filter-form { display: grid; grid-template-columns: 1.5fr 1fr 1fr auto; gap: 14px; align-items: end; }
.question-list { display: flex; flex-direction: column; gap: 14px; margin-bottom: 18px; }
.question-card { padding: 18px; border: 1px solid #edf2f7; border-radius: 16px; background: #fbfdff; }
.question-title-row { justify-content: space-between; }
.question-title-row h3 { margin: 0; color: #1f2a44; }
.tag-row { flex-wrap: wrap; }
.card-actions { justify-content: flex-end; }
.dialog-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.import-mode { margin: 16px 0; }
.answer-detail h3, .answer-detail h4 { color: #1f2a44; }
@media (max-width: 900px) { .filter-form, .dialog-grid { grid-template-columns: 1fr; } .page-hero, .question-title-row { align-items: flex-start; flex-direction: column; } }
</style>
