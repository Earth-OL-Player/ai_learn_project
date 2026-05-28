<template>
  <section class="system-question-bank-page">
    <div class="admin-page-hero system-question-hero">
      <div>
        <h2>系统题库管理</h2>
      </div>
      <div class="system-question-actions">
        <el-button round type="danger" plain :loading="clearing" aria-label="一键清除当前题库" @click="handleClearAll">一键清除当前题库</el-button>
        <el-button round @click="downloadTemplate">下载CSV模板</el-button>
        <el-upload :show-file-list="false" accept=".csv,text/csv" :before-upload="handleImportFile">
          <el-button round :loading="prechecking" aria-label="上传CSV并预检题库导入内容">上传CSV预检</el-button>
        </el-upload>
        <el-button type="primary" round @click="openCreateDialog">新增题目</el-button>
      </div>
    </div>

    <el-card shadow="never" class="admin-filter-card">
      <el-form :model="filters" label-position="top" class="admin-filter-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable aria-label="题库关键词" placeholder="搜索编码、题目或答案" @keyup.enter="searchQuestions" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.questionType" clearable filterable allow-create aria-label="题目分类筛选" placeholder="全部分类">
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
      <el-table v-loading="loading" :data="questions" row-key="id" class="system-question-table" aria-label="系统题库列表">
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
            <div class="table-action-row">
              <el-button text type="primary" :aria-label="`编辑题目 ${row.code}`" @click="openEditDialog(row)">编辑</el-button>
              <el-button text type="danger" :aria-label="`删除题目 ${row.code}`" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.pageNo"
        layout="prev, pager, next, total"
        :total="page.total"
        :page-size="page.pageSize"
        aria-label="系统题库分页"
        @current-change="loadQuestions"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑系统题目' : '新增系统题目'" width="760px" class="system-question-dialog">
      <el-form :model="form" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="题目编码（可空，空则自动生成）">
            <el-input
              v-model="form.code"
              aria-label="题目编码"
              :maxlength="QUESTION_CODE_MAX_LENGTH"
              placeholder="例如 SYSTEM-RAG-001"
            />
          </el-form-item>
          <el-form-item label="题目分类">
            <el-select v-model="form.questionType" filterable allow-create default-first-option aria-label="题目分类" placeholder="例如 RAG">
              <el-option v-for="item in questionTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="题目">
          <el-input
            v-model="form.question"
            aria-label="题目内容"
            type="textarea"
            :rows="5"
            :maxlength="QUESTION_LONG_TEXT_MAX_LENGTH"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input
            v-model="form.standardAnswer"
            aria-label="参考答案"
            type="textarea"
            :rows="5"
            :maxlength="QUESTION_LONG_TEXT_MAX_LENGTH"
            show-word-limit
          />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item label="重要性评分（0-100）">
            <el-input-number v-model="form.importanceScore" aria-label="重要性评分" :min="0" :max="100" :step="0.1" :precision="1" />
          </el-form-item>
          <el-form-item label="真实面试出现次数">
            <el-input-number v-model="form.occurrenceCount" aria-label="真实面试出现次数" :min="0" />
          </el-form-item>
        </div>
        <p v-if="formError" class="admin-form-error" role="alert">{{ formError }}</p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveQuestion">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importPreviewVisible" title="CSV导入预检" width="1080px" class="system-question-import-dialog">
      <div v-if="importPreview" class="import-preview-panel">
        <div class="import-summary-grid" aria-label="CSV导入预检汇总">
          <div class="import-summary-item">
            <span>CSV行数</span>
            <strong>{{ importPreview.totalCount }}</strong>
          </div>
          <div class="import-summary-item is-create">
            <span>新增</span>
            <strong>{{ importPreview.createdCount }}</strong>
          </div>
          <div class="import-summary-item is-update">
            <span>更新</span>
            <strong>{{ importPreview.updatedCount }}</strong>
          </div>
          <div class="import-summary-item is-warning">
            <span>冲突</span>
            <strong>{{ importPreview.conflictCount }}</strong>
          </div>
          <div class="import-summary-item is-danger">
            <span>错误</span>
            <strong>{{ importPreview.errorCount }}</strong>
          </div>
        </div>

        <el-alert
          v-if="importPreview.issues.length"
          type="warning"
          :closable="false"
          show-icon
          title="存在无法导入的行，确认导入时会跳过冲突和错误行。"
        />

        <el-table :data="importPreview.rows" max-height="460" row-key="rowIndex" class="import-preview-table" aria-label="CSV导入预检明细">
          <el-table-column prop="rowIndex" label="行号" width="76" />
          <el-table-column label="动作" width="92">
            <template #default="{ row }">
              <el-tag :type="resolveActionTagType(row.action)" effect="light">{{ row.actionText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="题目编码" min-width="170" />
          <el-table-column prop="questionType" label="分类" width="130" />
          <el-table-column label="题目" min-width="260">
            <template #default="{ row }">
              <p class="question-cell">{{ row.question }}</p>
            </template>
          </el-table-column>
          <el-table-column label="差异 / 字段问题" min-width="280">
            <template #default="{ row }">
              <div class="import-row-detail">
                <template v-if="row.issues.length">
                  <el-tag v-for="issue in row.issues" :key="`${row.rowIndex}-${issue.fieldName}-${issue.message}`" type="danger" effect="plain">
                    {{ issue.fieldLabel }}：{{ issue.message }}
                  </el-tag>
                </template>
                <template v-else-if="row.diffs.length">
                  <el-tag v-for="diff in row.diffs" :key="`${row.rowIndex}-${diff.fieldName}`" type="info" effect="plain">
                    {{ diff.fieldLabel }}
                  </el-tag>
                </template>
                <span v-else class="import-row-muted">无字段变化</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="closeImportPreview">取消</el-button>
        <el-button
          type="primary"
          :disabled="!canConfirmImport"
          :loading="importing"
          aria-label="确认导入预检通过的CSV行"
          @click="confirmImportCsv"
        >
          确认导入可导入行
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElAlert } from 'element-plus/es/components/alert/index.mjs';
import { ElCard } from 'element-plus/es/components/card/index.mjs';
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs';
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs';
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs';
import { ElTag } from 'element-plus/es/components/tag/index.mjs';
import { ElUpload } from 'element-plus/es/components/upload/index.mjs';
import 'element-plus/es/components/alert/style/css';
import 'element-plus/es/components/card/style/css';
import 'element-plus/es/components/input-number/style/css';
import 'element-plus/es/components/pagination/style/css';
import 'element-plus/es/components/select/style/css';
import 'element-plus/es/components/table/style/css';
import 'element-plus/es/components/tag/style/css';
import 'element-plus/es/components/upload/style/css';
import type { UploadRawFile } from 'element-plus/es/components/upload';
import { computed, onMounted, reactive, ref } from 'vue';
import {
  clearSystemQuestions,
  createSystemQuestion,
  deleteSystemQuestion,
  downloadSystemQuestionTemplate,
  fetchSystemQuestionTypes,
  fetchSystemQuestions,
  importSystemQuestions,
  precheckImportSystemQuestions,
  updateSystemQuestion,
} from '../../api/adminQuestions';
import { formatMediumDateTime as formatTime } from '../../utils/dateTimeFormat';
import { downloadBlobFile } from '../../utils/downloadFile';
import { resolveErrorMessage } from '../../utils/errorMessage';
import type {
  ImportSystemQuestionAction,
  ImportSystemQuestionsPrecheckResult,
  SystemQuestionItem,
  SystemQuestionPayload,
} from '../../types/system-question';

const PAGE_SIZE = 10;
const QUESTION_CODE_MAX_LENGTH = 64;
const QUESTION_LONG_TEXT_MAX_LENGTH = 10000;
const loading = ref(false);
const saving = ref(false);
const importing = ref(false);
const prechecking = ref(false);
const clearing = ref(false);
const dialogVisible = ref(false);
const importPreviewVisible = ref(false);
const editingId = ref<string | null>(null);
const importFile = ref<File | null>(null);
const importPreview = ref<ImportSystemQuestionsPrecheckResult | null>(null);
const questions = ref<SystemQuestionItem[]>([]);
const questionTypes = ref<string[]>([]);
const page = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const filters = reactive({ keyword: '', questionType: '' });
const form = reactive<SystemQuestionPayload>(buildEmptyForm());
const formError = ref('');
const canConfirmImport = computed(() => Boolean(importFile.value && importPreview.value?.importableCount));

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
  formError.value = '';
  Object.assign(form, buildEmptyForm());
  dialogVisible.value = true;
}

/**
 * 打开编辑弹窗。
 */
function openEditDialog(row: SystemQuestionItem): void {
  editingId.value = row.id;
  formError.value = '';
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
    formError.value = '请填写题目、分类和参考答案';
    ElMessage.warning(formError.value);
    return;
  }
  saving.value = true;
  formError.value = '';
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
    formError.value = resolveErrorMessage(error, '保存失败');
    ElMessage.error(formError.value);
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
 * 一键清空系统题库。
 */
async function handleClearAll(): Promise<void> {
  await ElMessageBox.confirm('该操作会真实清空当前题库数据并重置自增ID，确认继续吗？', '清空题库确认', {
    type: 'warning',
    confirmButtonText: '确认清空',
    cancelButtonText: '取消',
  });
  clearing.value = true;
  try {
    await clearSystemQuestions();
    ElMessage.success('当前题库已清空');
    await Promise.all([loadQuestionTypes(), searchQuestions()]);
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '清空题库失败'));
  } finally {
    clearing.value = false;
  }
}

/**
 * 下载 CSV 模板。
 */
async function downloadTemplate(): Promise<void> {
  const blob = await downloadSystemQuestionTemplate();
  downloadBlobFile(blob, '系统题库导入模板.csv');
}

/**
 * 预检 CSV 文件。
 */
async function handleImportFile(file: UploadRawFile): Promise<boolean> {
  prechecking.value = true;
  try {
    importFile.value = file;
    importPreview.value = await precheckImportSystemQuestions(file);
    importPreviewVisible.value = true;
    ElMessage.success('CSV预检完成，请确认导入内容');
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '预检失败'));
  } finally {
    prechecking.value = false;
  }
  return false;
}

/**
 * 关闭 CSV 导入预检弹窗。
 */
function closeImportPreview(): void {
  importPreviewVisible.value = false;
  importPreview.value = null;
  importFile.value = null;
}

/**
 * 确认导入预检通过的 CSV 文件。
 */
async function confirmImportCsv(): Promise<void> {
  if (!importFile.value) {
    ElMessage.warning('请先上传CSV文件');
    return;
  }
  importing.value = true;
  try {
    const result = await importSystemQuestions(importFile.value);
    if (result.skippedCount > 0) {
      ElMessage.warning(`导入完成：新增 ${result.createdCount} 道，更新 ${result.updatedCount} 道，跳过 ${result.skippedCount} 行`);
      if (importPreview.value) {
        importPreview.value = { ...importPreview.value, importableCount: 0 };
      }
      importFile.value = null;
      await Promise.all([loadQuestionTypes(), searchQuestions()]);
      return;
    }
    ElMessage.success(`导入成功：新增 ${result.createdCount} 道，更新 ${result.updatedCount} 道`);
    closeImportPreview();
    await Promise.all([loadQuestionTypes(), searchQuestions()]);
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '导入失败'));
  } finally {
    importing.value = false;
  }
}

/**
 * 解析预检动作标签类型。
 */
function resolveActionTagType(action: ImportSystemQuestionAction): 'success' | 'primary' | 'warning' | 'danger' {
  if (action === 'CREATE') {
    return 'success';
  }
  if (action === 'UPDATE') {
    return 'primary';
  }
  if (action === 'CONFLICT') {
    return 'warning';
  }
  return 'danger';
}

onMounted(async () => {
  await Promise.all([loadQuestionTypes(), loadQuestions()]);
});
</script>

