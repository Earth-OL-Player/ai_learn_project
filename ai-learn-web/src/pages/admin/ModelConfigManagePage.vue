<template>
  <section class="admin-model-config-page">
    <div class="admin-page-hero model-config-hero">
      <div>
        <h2>模型配置</h2>
      </div>
    </div>

    <div class="model-config-grid">
      <el-card v-for="item in configs" :key="item.level" shadow="never" class="model-config-card">
        <div class="model-config-card-header">
          <h3>{{ item.levelText }}</h3>
          <span>{{ item.level }}</span>
        </div>

        <el-form label-position="top" class="model-config-form">
          <el-form-item label="model">
            <el-input v-model.trim="item.modelName" maxlength="128" placeholder="请输入模型名称" />
          </el-form-item>
          <el-form-item label="baseUrl">
            <el-input v-model.trim="item.baseUrl" maxlength="512" placeholder="https://模型服务地址占位符/v1" />
          </el-form-item>
          <el-form-item label="apiKey">
            <el-input v-model="item.apiKey" maxlength="512" show-password placeholder="AI_GRADING_API_KEY占位符" />
          </el-form-item>
        </el-form>

        <el-button type="primary" round :loading="savingLevel === item.level" @click="saveConfig(item)">保存配置</el-button>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ElCard } from 'element-plus/es/components/card/index.mjs';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import 'element-plus/es/components/card/style/css';
import { onMounted, ref } from 'vue';
import { fetchAdminModelConfigs, saveAdminModelConfig, type AdminModelConfig } from '../../api/adminModelConfigs';
import type { ModelLevel } from '../../api/modelEntitlements';
import { resolveErrorMessage } from '../../utils/errorMessage';

const configs = ref<AdminModelConfig[]>([]);
const savingLevel = ref<ModelLevel | null>(null);

/**
 * 加载模型配置。
 */
async function loadConfigs(): Promise<void> {
  configs.value = await fetchAdminModelConfigs();
}

/**
 * 保存单档模型配置。
 */
async function saveConfig(item: AdminModelConfig): Promise<void> {
  if (!item.modelName.trim()) {
    ElMessage.warning('model 不能为空');
    return;
  }
  savingLevel.value = item.level;
  try {
    const saved = await saveAdminModelConfig(item.level, {
      modelName: item.modelName.trim(),
      baseUrl: item.baseUrl.trim(),
      apiKey: item.apiKey.trim(),
    });
    Object.assign(item, saved);
    ElMessage.success('模型配置已保存');
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '保存失败'));
  } finally {
    savingLevel.value = null;
  }
}

onMounted(() => {
  loadConfigs().catch((error: unknown) => {
    ElMessage.error(resolveErrorMessage(error, '模型配置加载失败'));
  });
});
</script>
