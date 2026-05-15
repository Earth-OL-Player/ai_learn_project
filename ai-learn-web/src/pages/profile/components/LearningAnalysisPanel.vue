<template>
  <el-card shadow="never" class="analysis-card">
    <template #header>
      <div class="card-header">
        <span>薄弱点分析</span>
        <el-button text type="primary" :loading="loading" @click="loadAnalysis">刷新</el-button>
      </div>
    </template>

    <el-skeleton :loading="loading" animated :rows="4">
      <el-empty v-if="weakPoints.length === 0" description="完成几次刷题后，这里会展示薄弱知识点" />
      <div v-else class="weak-list">
        <article v-for="item in weakPoints" :key="item.knowledgePointId" class="weak-item">
          <div class="weak-title-row">
            <h3>{{ item.knowledgePointName }}</h3>
            <el-tag :type="item.averageScore < 60 ? 'danger' : 'warning'" effect="light">平均 {{ item.averageScore.toFixed(1) }} 分</el-tag>
          </div>
          <p>{{ item.advice }}</p>
          <span>答题 {{ item.answeredCount }} 次 · 低分 {{ item.lowScoreCount }} 次</span>
        </article>
      </div>
    </el-skeleton>
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { fetchMyLearningAnalysis } from '../../../api/learningAnalysis';
import type { LearningAnalysis } from '../../../types/learning-analysis';

const loading = ref(false);
const analysis = ref<LearningAnalysis | null>(null);
const weakPoints = computed(() => analysis.value?.weakPoints || []);

/**
 * 加载学习分析。
 */
async function loadAnalysis(): Promise<void> {
  loading.value = true;
  try {
    analysis.value = await fetchMyLearningAnalysis();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '学习分析加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(loadAnalysis);
</script>

<style scoped lang="scss">
.analysis-card { border: 1px solid #edf2f7; border-radius: 18px; }
.card-header, .weak-title-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.weak-list { display: flex; flex-direction: column; gap: 12px; }
.weak-item { padding: 16px; border-radius: 16px; background: #fbfdff; border: 1px solid #edf2f7; }
.weak-item h3, .weak-item p { margin: 0; }
.weak-item p { margin-top: 10px; color: #475467; line-height: 1.7; }
.weak-item span { display: inline-block; margin-top: 10px; color: #667085; font-size: 13px; }
</style>
