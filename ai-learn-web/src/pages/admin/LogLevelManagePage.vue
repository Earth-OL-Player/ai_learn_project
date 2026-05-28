<template>
  <section class="admin-log-level-page">
    <div class="admin-page-hero log-level-hero">
      <div>
        <h2>日志管理</h2>
      </div>
      <el-button round :loading="loading" @click="loadLogLevels">刷新</el-button>
    </div>

    <div class="log-level-grid">
      <el-card v-for="item in services" :key="item.service" shadow="never" class="log-level-card">
        <div class="log-level-card-header">
          <div>
            <h3>{{ item.serviceName }}</h3>
            <span>{{ item.service }}</span>
          </div>
          <el-tag :type="item.available ? 'success' : 'warning'" round>
            {{ item.available ? '可调整' : '不可用' }}
          </el-tag>
        </div>

        <div class="log-level-current">
          <span>当前级别</span>
          <strong>{{ item.level || '未知' }}</strong>
        </div>

        <el-form label-position="top" class="log-level-form">
          <el-form-item label="日志级别">
            <el-select
              v-model="selectedLevels[item.service]"
              :disabled="!item.available || savingService === item.service"
              placeholder="请选择日志级别"
            >
              <el-option v-for="level in logLevelOptions" :key="level" :label="level" :value="level" />
            </el-select>
          </el-form-item>
        </el-form>

        <p class="log-level-message">{{ item.message }}</p>
        <el-button
          type="primary"
          round
          :disabled="!item.available"
          :loading="savingService === item.service"
          @click="saveLogLevel(item)"
        >
          保存级别
        </el-button>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ElButton } from 'element-plus/es/components/button/index.mjs';
import { ElCard } from 'element-plus/es/components/card/index.mjs';
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs';
import { ElTag } from 'element-plus/es/components/tag/index.mjs';
import 'element-plus/es/components/button/style/css';
import 'element-plus/es/components/card/style/css';
import 'element-plus/es/components/form/style/css';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/select/style/css';
import 'element-plus/es/components/tag/style/css';
import { onMounted, reactive, ref } from 'vue';
import {
  fetchAdminLogLevels,
  updateAdminLogLevel,
  type AdminLogLevelItem,
  type AdminLogLevelValue,
} from '../../api/adminLogLevels';
import { resolveErrorMessage } from '../../utils/errorMessage';

const logLevelOptions: AdminLogLevelValue[] = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];
const services = ref<AdminLogLevelItem[]>([]);
const loading = ref(false);
const savingService = ref('');
const selectedLevels = reactive<Record<string, AdminLogLevelValue | ''>>({});

/**
 * 加载日志级别列表。
 */
async function loadLogLevels(): Promise<void> {
  loading.value = true;
  try {
    applyLogLevels(await fetchAdminLogLevels());
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '日志级别加载失败'));
  } finally {
    loading.value = false;
  }
}

/**
 * 保存单个服务日志级别。
 */
async function saveLogLevel(item: AdminLogLevelItem): Promise<void> {
  const level = selectedLevels[item.service];
  if (!isLogLevelValue(level)) {
    ElMessage.warning('请选择日志级别');
    return;
  }

  savingService.value = item.service;
  try {
    const saved = await updateAdminLogLevel(item.service, { level });
    replaceLogLevel(saved);
    ElMessage.success('日志级别已保存');
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '日志级别保存失败'));
  } finally {
    savingService.value = '';
  }
}

/**
 * 应用接口返回的日志级别列表。
 */
function applyLogLevels(items: AdminLogLevelItem[]): void {
  services.value = items;
  items.forEach((item) => {
    selectedLevels[item.service] = isLogLevelValue(item.level) ? item.level : '';
  });
}

/**
 * 替换单个服务日志级别。
 */
function replaceLogLevel(saved: AdminLogLevelItem): void {
  const index = services.value.findIndex((item) => item.service === saved.service);
  if (index >= 0) {
    services.value.splice(index, 1, saved);
  }
  selectedLevels[saved.service] = isLogLevelValue(saved.level) ? saved.level : '';
}

/**
 * 判断文本是否为支持的日志级别。
 */
function isLogLevelValue(level: string): level is AdminLogLevelValue {
  return logLevelOptions.includes(level as AdminLogLevelValue);
}

onMounted(() => {
  loadLogLevels();
});
</script>

<style scoped>
.admin-log-level-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.log-level-hero {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.log-level-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.log-level-card {
  border-radius: 8px;
}

.log-level-card-header {
  align-items: flex-start;
  display: flex;
  gap: 16px;
  justify-content: space-between;
}

.log-level-card-header h3 {
  color: #1f2937;
  font-size: 18px;
  line-height: 1.3;
  margin: 0 0 6px;
}

.log-level-card-header span {
  color: #64748b;
  font-size: 13px;
}

.log-level-current {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  margin: 18px 0;
  padding: 14px 16px;
}

.log-level-current span {
  color: #64748b;
}

.log-level-current strong {
  color: #0f766e;
  font-size: 18px;
}

.log-level-form {
  margin-top: 4px;
}

.log-level-form :deep(.el-select) {
  width: 100%;
}

.log-level-message {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
  margin: 0 0 16px;
  min-height: 42px;
}
</style>
