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
        <div class="growth-hero">
          <RealmCharacterCard
            :nickname="displayName"
            :rank="growth.rank"
            :level="growth.level"
            :current-experience="growth.currentExperience"
            :next-level-experience="growth.nextLevelExperience"
            :level-progress-text="growth.levelProgressText"
            compact
          />
          <div class="growth-grid">
            <article v-for="stat in growthStats" :key="stat.title" class="metric-card">
              <span>{{ stat.title }}</span>
              <strong>
                {{ stat.value }}
                <small v-if="stat.suffix">{{ stat.suffix }}</small>
              </strong>
            </article>

            <section class="level-summary" aria-label="当前修炼进度">
              <div class="level-copy">
                <span>修炼进度</span>
                <strong>{{ growth.levelProgressText }}</strong>
              </div>
              <el-progress :percentage="levelProgressPercent" :stroke-width="12" :show-text="false" />
              <span class="rank-pill">{{ growth.rank }}</span>
            </section>
          </div>
        </div>
        <section class="badge-wall">
          <h3>徽章墙</h3>
          <div class="badge-group-list">
            <section v-for="group in badgeGroups" :key="group.category" class="badge-group">
              <div class="badge-group-title">
                <strong>{{ group.name }}</strong>
              </div>
              <div class="badge-list">
                <div
                  v-for="badge in group.badges"
                  :key="badge.id"
                  class="badge-item"
                  :class="{ locked: !badge.acquired, acquired: badge.acquired }"
                >
                  <span>{{ badge.icon }}</span>
                  <strong>{{ badge.name }}</strong>
                  <small>{{ badge.description }}</small>
                </div>
              </div>
            </section>
            <div v-if="badgeGroups.length === 0" class="badge-empty">完成一次刷题后即可点亮第一枚勋章</div>
          </div>
        </section>      </div>
      <el-empty v-else description="暂无成长数据，完成一次刷题后即可点亮成长概览" />
    </el-skeleton>
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { computed, onMounted, ref } from 'vue';
import { fetchMyGrowth } from '../../../api/practice';
import RealmCharacterCard from '../../../components/growth/RealmCharacterCard.vue';
import { useAuthStore } from '../../../stores/auth';
import type { BadgeInfo, GrowthInfo } from '../../../types/growth';

interface BadgeGroup {
  category: string;
  name: string;
  badges: BadgeInfo[];
}

const BADGE_GROUPS = [
  { category: 'ENTRY', name: '入门类' },
  { category: 'PERSISTENCE', name: '坚持类' },
  { category: 'RARE', name: '隐藏/稀有类' },
];

const loading = ref(false);
const growth = ref<GrowthInfo | null>(null);
const authStore = useAuthStore();

// 展示角色卡昵称时优先使用用户昵称，保持与个人中心一致。
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'AI 学习者');

// 将成长指标整理为卡片数据，便于模板保持简洁。
const growthStats = computed(() => {
  if (!growth.value) {
    return [];
  }

  // 平均分保留一位小数，避免接口浮点数直接展示影响观感。
  return [
    { title: '当前总经验', value: growth.value.currentExperience },
    { title: '累计答题', value: growth.value.answeredCount },
    { title: '平均得分', value: Number(growth.value.averageScore.toFixed(1)) },
    { title: '学习天数', value: growth.value.streakDays, suffix: '天' },
  ];
});

// 徽章墙按后端分类分组，隐藏稀有勋章未获得时不展示。
const badgeGroups = computed<BadgeGroup[]>(() => {
  if (!growth.value) {
    return [];
  }

  // 前端再次过滤 hidden 未获得勋章，防止旧接口缓存导致隐藏勋章提前曝光。
  const badges = growth.value.badges;
  return BADGE_GROUPS.map((group) => ({
    ...group,
    badges: badges.filter((badge) => badge.category === group.category && (!badge.hidden || badge.acquired)),
  })).filter((group) => group.category !== 'RARE' || group.badges.length > 0);
});

// 进度条按当前等级经验计算，让右侧区域拥有更明确的成长反馈。
const levelProgressPercent = computed(() => {
  if (!growth.value || growth.value.nextLevelExperience <= 0) {
    return 0;
  }

  // 限制百分比范围，防止异常数据撑满或反向展示进度条。
  const progress = (growth.value.currentLevelExperience / growth.value.nextLevelExperience) * 100;
  return Math.min(100, Math.max(0, Number(progress.toFixed(1))));
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
.growth-card {
  border: 1px solid #edf2f7;
  border-radius: 18px;
}

.card-header,
.growth-grid {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.growth-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.growth-hero {
  // 角色卡与指标并列，提升个人中心成长体系的游戏化辨识度。
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 22px;
  align-items: stretch;
}

.growth-grid {
  // 指标区改为轻量卡片矩阵，减少大面积留白并贴合系统清爽风格。
  display: grid;
  grid-template-columns: repeat(4, minmax(116px, 1fr));
  align-content: center;
  padding: 22px;
  border: 1px solid rgba(83, 116, 170, 0.1);
  border-radius: 24px;
  background:
    radial-gradient(circle at 12% 18%, rgba(64, 158, 255, 0.08), transparent 28%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 251, 255, 0.98) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.metric-card {
  // 单个指标使用柔和描边和悬浮阴影，保持信息聚合但不过度装饰。
  min-height: 92px;
  padding: 16px;
  border: 1px solid rgba(219, 234, 254, 0.9);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 14px 32px rgba(61, 91, 132, 0.07);
}

.metric-card span {
  display: block;
  color: #667085;
  font-size: 14px;
  font-weight: 700;
}

.metric-card strong {
  // 数值字体加粗放大，形成比 Element 默认统计组件更稳定的视觉层级。
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-top: 10px;
  color: #17233d;
  font-size: 30px;
  line-height: 1;
}

.metric-card small {
  color: #667085;
  font-size: 15px;
}

.level-summary {
  // 等级进度横跨整行，让红框区域从单纯数字展示升级为成长反馈面板。
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: minmax(130px, auto) minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 16px 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 20px;
  background: rgba(245, 248, 255, 0.82);
}

.level-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.level-copy span {
  color: #667085;
  font-size: 13px;
  font-weight: 700;
}

.level-copy strong,
.rank-pill {
  color: #1f2a44;
  font-weight: 800;
}

.rank-pill {
  // 境界标签沿用浅蓝胶囊形态，与顶部用户身份胶囊保持一致。
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(47, 125, 246, 0.1);
  color: #1f6feb;
}


.badge-wall h3 {
  margin: 0 0 12px;
  color: #1f2a44;
}

.badge-group-list,
.badge-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.badge-group {
  // 勋章分类使用轻量分区，避免隐藏稀有类和普通勋章混在一起。
  padding: 14px;
  border: 1px solid rgba(219, 234, 254, 0.9);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.badge-group-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.badge-group-title strong {
  color: #17233d;
  font-size: 16px;
}

.badge-empty {
  color: #667085;
  font-size: 13px;
}

.badge-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.badge-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  background: linear-gradient(145deg, #f7fbff 0%, #ffffff 100%);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.badge-item.acquired {
  // 已获得勋章使用柔和高光点亮，保持清新但有即时反馈。
  border-color: rgba(47, 125, 246, 0.24);
  box-shadow: 0 14px 30px rgba(47, 125, 246, 0.09);
}

.badge-item span {
  font-size: 26px;
}

.badge-item strong {
  color: #1f2a44;
}

.badge-item small {
  color: #667085;
  line-height: 1.5;
}

.badge-item.locked {
  border-style: dashed;
  filter: grayscale(1);
  opacity: 0.45;
}

.badge-empty {
  // 理论上入门类和坚持类会置灰展示，此处兜底处理异常空数据。
  padding: 18px;
  border: 1px dashed #dbeafe;
  border-radius: 16px;
  text-align: center;
  background: #f8fbff;
}

@media (max-width: 900px) {
  .growth-hero {
    grid-template-columns: 1fr;
  }

  .growth-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .level-summary {
    grid-template-columns: 1fr;
  }
}
</style>

