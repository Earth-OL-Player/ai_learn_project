<template>
  <section class="system-question-bank-page">
    <div class="admin-page-hero system-question-hero">
      <div>
        <p class="eyebrow">系统题库</p>
        <h2>系统题库管理</h2>
        <p>统一维护 AI 智能刷题使用的系统题库，题目编码会作为用户刷题汇总的稳定关联字段。</p>
      </div>
      <div class="system-question-actions">
        <el-button round @click="downloadTemplate">下载CSV模板</el-button>
        <el-upload :show-file-list="false" accept=".csv,text/csv" :before-upload="handleImportFile">
          <el-button round :loading="importing">上传CSV</el-button>
        </el-upload>
        <el-button type="primary" round @click="openCreateDialog">新增题目</el-button>
      </div>
    </div>

    <el-card shadow="never" class="admin-filter-card">
      <el-form :model="filters" label-position="top" class="admin-filter-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="搜索编码、题目或答案" @keyup.enter="searchQuestions" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.questionType" clearable filterable allow-create placeholder="全部分类">
            <el-option v-for="item in questionTypes" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item class="admin-filter-actions">
          <el-button type="primary" round :loading="loading" @click="searchQuestions">查询</el-button>
          <el-button round @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="admin-table-card">
      <el-table v-loading="loading" :data="questions" row-key="id" class="system-question-table">
        <el-table-column prop="code" label="题目编码" width="180" />
        <el-table-column prop="questionType" label="分类" width="130" />
        <el-table-column label="题目" min-width="320">
          <template #default="{ row }">
            <p class="question-cell">{{ row.question }}</p>
          </template>
        </el-table-column>
        <el-table-column prop="importanceScore" label="重要性" width="100" />
        <el-table-column prop="occurrenceCount" label="真实面试出现次数" width="150" />
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.pageNo"
        layout="prev, pager, next, total"
        :total="page.total"
        :page-size="page.pageSize"
        @current-change="loadQuestions"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑系统题目' : '新增系统题目'" width="760px" class="system-question-dialog">
      <el-form :model="form" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="题目编码（可空，空则自动生成）">
            <el-input v-model="form.code" maxlength="64" placeholder="例如 SYSTEM-RAG-001" />
          </el-form-item>
          <el-form-item label="题目分类">
            <el-select v-model="form.questionType" filterable allow-create default-first-option placeholder="例如 RAG">
              <el-option v-for="item in questionTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="题目">
          <el-input v-model="form.question" type="textarea" :rows="5" maxlength="10000" show-word-limit />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="form.standardAnswer" type="textarea" :rows="5" maxlength="10000" show-word-limit />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item label="重要性评分（0-100）">
            <el-input-number v-model="form.importanceScore" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="真实面试出现次数">
            <el-input-number v-model="form.occurrenceCount" :min="0" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveQuestion">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox, type UploadRawFile } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import {
  createSystemQuestion,
  deleteSystemQuestion,
  downloadSystemQuestionTemplate,
  fetchSystemQuestionTypes,
  fetchSystemQuestions,
  importSystemQuestions,
  updateSystemQuestion,
} from '../../api/adminQuestions';
import type { SystemQuestionItem, SystemQuestionPayload } from '../../types/system-question';

const PAGE_SIZE = 10;
const loading = ref(false);
const saving = ref(false);
const importing = ref(false);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const questions = ref<SystemQuestionItem[]>([]);
const questionTypes = ref<string[]>([]);
const page = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const filters = reactive({ keyword: '', questionType: '' });
const form = reactive<SystemQuestionPayload>(buildEmptyForm());

/**
 * 构建空表单。
 */
function buildEmptyForm(): SystemQuestionPayload {
  return { code: '', question: '', questionType: '', standardAnswer: '', importanceScore: 60, occurrenceCount: 0 };
}

/**
 * 加载题目分类。
 */
async function loadQuestionTypes(): Promise<void> {
  questionTypes.value = await fetchSystemQuestionTypes();
}

/**
 * 加载系统题库。
 */
async function loadQuestions(): Promise<void> {
  loading.value = true;
  try {
    const result = await fetchSystemQuestions({ pageNo: page.pageNo, pageSize: page.pageSize, ...filters });
    questions.value = result.records;
    page.total = result.total;
  } finally {
    loading.value = false;
  }
}

/**
 * 按条件查询题库。
 */
async function searchQuestions(): Promise<void> {
  page.pageNo = 1;
  await loadQuestions();
}

/**
 * 重置筛选条件。
 */
async function resetFilters(): Promise<void> {
  filters.keyword = '';
  filters.questionType = '';
  await searchQuestions();
}

/**
 * 打开新增弹窗。
 */
function openCreateDialog(): void {
  editingId.value = null;
  Object.assign(form, buildEmptyForm());
  dialogVisible.value = true;
}

/**
 * 打开编辑弹窗。
 */
function openEditDialog(row: SystemQuestionItem): void {
  editingId.value = row.id;
  Object.assign(form, {
    code: row.code,
    question: row.question,
    questionType: row.questionType,
    standardAnswer: row.standardAnswer,
    importanceScore: row.importanceScore,
    occurrenceCount: row.occurrenceCount,
  });
  dialogVisible.value = true;
}

/**
 * 保存题目。
 */
async function saveQuestion(): Promise<void> {
  if (!form.question.trim() || !form.questionType.trim() || !form.standardAnswer.trim()) {
    ElMessage.warning('请填写题目、分类和参考答案');
    return;
  }
  saving.value = true;
  try {
    const payload = { ...form, code: form.code?.trim() || undefined, questionType: form.questionType.trim() };
    if (editingId.value) {
      await updateSystemQuestion(editingId.value, payload);
      ElMessage.success('题目已更新');
    } else {
      await createSystemQuestion(payload);
      ElMessage.success('题目已新增');
    }
    dialogVisible.value = false;
    await Promise.all([loadQuestionTypes(), loadQuestions()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

/**
 * 删除题目。
 */
async function handleDelete(row: SystemQuestionItem): Promise<void> {
  await ElMessageBox.confirm(`确认删除题目「${row.code}」吗？`, '删除确认', { type: 'warning' });
  await deleteSystemQuestion(row.id);
  ElMessage.success('题目已删除');
  await loadQuestions();
}

/**
 * 下载 CSV 模板。
 */
async function downloadTemplate(): Promise<void> {
  const blob = await downloadSystemQuestionTemplate();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = '系统题库导入模板.csv';
  anchor.click();
  URL.revokeObjectURL(url);
}

/**
 * 导入 CSV 文件。
 */
async function handleImportFile(file: UploadRawFile): Promise<boolean> {
  importing.value = true;
  try {
    const result = await importSystemQuestions(file);
    ElMessage.success(`导入成功：新增 ${result.createdCount} 道，更新 ${result.updatedCount} 道`);
    await Promise.all([loadQuestionTypes(), searchQuestions()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败');
  } finally {
    importing.value = false;
  }
  return false;
}

/**
 * 格式化时间。
 */
function formatTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

onMounted(async () => {
  await Promise.all([loadQuestionTypes(), loadQuestions()]);
});
</script>
