<template>
  <el-card shadow="never" class="growth-card">
    <template #header>
      <div class="card-header">
        <span>成长概览</span>
        <el-button text type="primary" :loading="loading" @click="loadGrowth">刷新</el-button>
      </div>
    </template>

    <el-skeleton :loading="loading" animated :rows="5">
      <div v-if="growth" class="growth-content">
        <div class="growth-grid">
          <el-statistic title="当前经验" :value="growth.currentExperience" />
          <el-statistic title="累计答题" :value="growth.answeredCount" />
          <el-statistic title="平均得分" :value="Number(growth.averageScore.toFixed(1))" />
          <el-statistic title="连续学习" :value="growth.streakDays" suffix="天" />
          <div class="level-summary">{{ growth.level }} · {{ growth.levelName }} · {{ growth.rank }}</div>
        </div>
        <div class="progress-block">
          <span>距离下一级还需 {{ growth.experienceToNextLevel }} 经验</span>
          <el-progress :percentage="progressPercent" :stroke-width="12" />
        </div>
        <section class="badge-wall">
          <h3>徽章墙</h3>
          <div class="badge-list">
            <div v-for="badge in growth.badges" :key="badge.id" class="badge-item" :class="{ locked: !badge.acquired }">
              <span>{{ badge.icon }}</span>
              <strong>{{ badge.name }}</strong>
              <small>{{ badge.description }}</small>
            </div>
          </div>
        </section>
        <section v-if="growth.recentEvents.length > 0" class="event-list">
          <h3>成长明细</h3>
          <p v-for="event in growth.recentEvents" :key="event.id">{{ event.title }} · {{ event.experienceDelta }} 经验</p>
        </section>
      </div>
      <el-empty v-else description="暂无成长数据，完成一次刷题后即可点亮成长概览" />
    </el-skeleton>
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { fetchMyGrowth } from '../../../api/practice';
import type { GrowthInfo } from '../../../types/growth';

const loading = ref(false);
const growth = ref<GrowthInfo | null>(null);

// 经验进度只做展示，满级时固定显示100%。
const progressPercent = computed(() => {
  if (!growth.value) {
    return 0;
  }
  if (growth.value.experienceToNextLevel === 0 || growth.value.nextLevelExperience <= growth.value.currentExperience) {
    return 100;
  }

  // 当前接口不额外返回上一等级阈值，这里按总进度做轻量展示。
  return Math.max(0, Math.min(100, Math.round((growth.value.currentExperience / growth.value.nextLevelExperience) * 100)));
});

/**
 * 加载成长概览。
 */
async function loadGrowth(): Promise<void> {
  loading.value = true;
  try {
    growth.value = await fetchMyGrowth();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '成长信息加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(loadGrowth);
</script>

<style scoped lang="scss">
.growth-card { border: 1px solid #edf2f7; border-radius: 18px; }
.card-header, .growth-grid { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.growth-content { display: flex; flex-direction: column; gap: 18px; }
.growth-grid { flex-wrap: wrap; }
.level-summary { padding: 12px 16px; color: #1f2a44; font-weight: 700; border-radius: 14px; background: #f5f8ff; }
.progress-block span { display: inline-block; margin-bottom: 8px; color: #667085; }
.badge-wall h3, .event-list h3 { margin: 0 0 12px; color: #1f2a44; }
.badge-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
.badge-item { display: flex; flex-direction: column; gap: 6px; padding: 14px; border-radius: 16px; background: #f7fbff; border: 1px solid #dbeafe; }
.badge-item span { font-size: 26px; }
.badge-item strong { color: #1f2a44; }
.badge-item small { color: #667085; line-height: 1.5; }
.badge-item.locked { filter: grayscale(1); opacity: 0.45; }
.event-list p { margin: 6px 0; color: #475467; }
</style>
