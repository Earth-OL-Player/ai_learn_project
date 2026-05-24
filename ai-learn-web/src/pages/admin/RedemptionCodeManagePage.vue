<template>
  <section class="admin-redemption-page">
    <div class="admin-page-hero redemption-code-hero">
      <div>
        <h2>兑换码管理</h2>
      </div>
      <el-button type="primary" round :loading="exporting" @click="exportCodes">导出兑换码</el-button>
    </div>

    <el-card shadow="never" class="admin-filter-card redemption-generate-card">
      <el-form :model="generateForm" label-position="top" class="redemption-generate-form">
        <el-form-item label="兑换码类型">
          <el-select v-model="generateForm.codeType" size="large" placeholder="请选择兑换码类型">
            <el-option v-for="item in codeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生成数量">
          <el-input-number v-model="generateForm.quantity" :min="1" :max="500" :step="1" size="large" />
        </el-form-item>
        <el-form-item class="admin-filter-actions">
          <el-button type="primary" round size="large" :loading="generating" @click="generateCodes">生成兑换码</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="admin-filter-card">
      <el-form :model="filters" label-position="top" class="redemption-filter-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="搜索兑换码或使用用户" @keyup.enter="searchCodes" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.codeType" clearable placeholder="全部类型">
            <el-option v-for="item in codeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="admin-filter-actions">
          <el-button type="primary" round :loading="loading" @click="searchCodes">查询</el-button>
          <el-button round @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="admin-table-card">
      <el-table v-loading="loading" :data="codes" row-key="id" aria-label="兑换码管理列表">
        <el-table-column prop="code" label="兑换码" min-width="210" />
        <el-table-column prop="codeTypeText" label="类型" min-width="170" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'UNUSED' ? 'success' : 'info'" round>{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="使用用户" min-width="140">
          <template #default="{ row }">{{ row.usedByUsername || '-' }}</template>
        </el-table-column>
        <el-table-column label="使用时间" width="180">
          <template #default="{ row }">{{ formatTime(row.usedAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="table-action-row">
              <el-button text type="primary" :disabled="!row.editable" @click="openEditDialog(row)">编辑</el-button>
              <el-button text type="danger" :disabled="!row.deletable" @click="deleteCode(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.pageNo"
        layout="prev, pager, next, total"
        :total="page.total"
        :page-size="page.pageSize"
        aria-label="兑换码管理分页"
        @current-change="loadCodes"
      />
    </el-card>

    <el-dialog v-model="editDialogVisible" title="编辑兑换码" width="480px" class="redemption-code-dialog">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="兑换码类型">
          <el-select v-model="editForm.codeType" placeholder="请选择兑换码类型">
            <el-option v-for="item in codeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ElCard } from 'element-plus/es/components/card/index.mjs';
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs';
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs';
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs';
import { ElTag } from 'element-plus/es/components/tag/index.mjs';
import 'element-plus/es/components/card/style/css';
import 'element-plus/es/components/input-number/style/css';
import 'element-plus/es/components/pagination/style/css';
import 'element-plus/es/components/select/style/css';
import 'element-plus/es/components/table/style/css';
import 'element-plus/es/components/tag/style/css';
import { onMounted, reactive, ref } from 'vue';
import {
  deleteAdminRedemptionCode,
  exportAdminRedemptionCodes,
  fetchAdminRedemptionCodes,
  generateAdminRedemptionCodes,
  updateAdminRedemptionCode,
  type AdminRedemptionCode,
  type RedemptionCodeStatus,
  type RedemptionCodeType,
} from '../../api/adminRedemptionCodes';

const PAGE_SIZE = 10;
const loading = ref(false);
const generating = ref(false);
const saving = ref(false);
const exporting = ref(false);
const editDialogVisible = ref(false);
const editingId = ref<string | null>(null);
const codes = ref<AdminRedemptionCode[]>([]);
const page = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const filters = reactive<{ keyword: string; codeType: RedemptionCodeType | ''; status: RedemptionCodeStatus | '' }>({
  keyword: '',
  codeType: '',
  status: '',
});
const generateForm = reactive<{ codeType: RedemptionCodeType; quantity: number }>({
  codeType: 'PRO_MONTHLY',
  quantity: 10,
});
const editForm = reactive<{ codeType: RedemptionCodeType }>({ codeType: 'PRO_MONTHLY' });
const codeTypeOptions: Array<{ label: string; value: RedemptionCodeType }> = [
  { label: '高级模型一个月', value: 'PRO_MONTHLY' },
  { label: '超级模型一个月', value: 'SUPER_MONTHLY' },
  { label: '高级模型永久', value: 'PRO_PERMANENT' },
  { label: '超级模型永久', value: 'SUPER_PERMANENT' },
  { label: '高级模型永久升超', value: 'PRO_PERMANENT_TO_SUPER' },
];
const statusOptions: Array<{ label: string; value: RedemptionCodeStatus }> = [
  { label: '未使用', value: 'UNUSED' },
  { label: '已使用', value: 'USED' },
];

/**
 * 加载兑换码列表。
 */
async function loadCodes(): Promise<void> {
  loading.value = true;
  try {
    const result = await fetchAdminRedemptionCodes({ pageNo: page.pageNo, pageSize: page.pageSize, ...filters });
    codes.value = result.records;
    page.total = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '兑换码加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 查询兑换码。
 */
async function searchCodes(): Promise<void> {
  page.pageNo = 1;
  await loadCodes();
}

/**
 * 重置筛选条件。
 */
async function resetFilters(): Promise<void> {
  filters.keyword = '';
  filters.codeType = '';
  filters.status = '';
  await searchCodes();
}

/**
 * 批量生成兑换码。
 */
async function generateCodes(): Promise<void> {
  generating.value = true;
  try {
    const generatedCodes = await generateAdminRedemptionCodes(generateForm.codeType, generateForm.quantity);
    ElMessage.success(`已生成 ${generatedCodes.length} 个兑换码`);
    await searchCodes();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '生成失败');
  } finally {
    generating.value = false;
  }
}

/**
 * 打开编辑弹窗。
 */
function openEditDialog(row: AdminRedemptionCode): void {
  editingId.value = row.id;
  editForm.codeType = row.codeType;
  editDialogVisible.value = true;
}

/**
 * 保存兑换码类型。
 */
async function saveEdit(): Promise<void> {
  if (!editingId.value) {
    return;
  }
  saving.value = true;
  try {
    await updateAdminRedemptionCode(editingId.value, editForm.codeType);
    editDialogVisible.value = false;
    ElMessage.success('兑换码已更新');
    await loadCodes();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

/**
 * 删除兑换码。
 */
async function deleteCode(row: AdminRedemptionCode): Promise<void> {
  await ElMessageBox.confirm(`确认删除兑换码「${row.code}」吗？`, '删除确认', { type: 'warning' });
  await deleteAdminRedemptionCode(row.id);
  ElMessage.success('兑换码已删除');
  await loadCodes();
}

/**
 * 导出兑换码。
 */
async function exportCodes(): Promise<void> {
  exporting.value = true;
  try {
    const blob = await exportAdminRedemptionCodes({ pageNo: page.pageNo, pageSize: page.pageSize, ...filters });
    downloadBlob(blob, '模型权益兑换码.csv');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败');
  } finally {
    exporting.value = false;
  }
}

/**
 * 下载浏览器文件。
 */
function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/**
 * 格式化时间。
 */
function formatTime(value: string | null): string {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

onMounted(loadCodes);
</script>
