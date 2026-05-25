<template>
  <section class="profile-center-page">
    <aside class="profile-sidebar" aria-label="个人中心导航">
      <div class="profile-sidebar-header">
        <span class="profile-sidebar-icon">我</span>
        <div>
          <h2>个人中心</h2>
          <p>@{{ authStore.user?.username }}</p>
        </div>
      </div>

      <!-- 左侧导航保持两层以内，默认进入个人资料页。 -->
      <el-menu class="profile-menu" :default-active="activeSection" aria-label="个人功能导航" @select="handleNavigationSelect">
        <el-menu-item index="profile">
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item index="question-stats">
          <span>智能刷题记录</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="profile-content" aria-live="polite">
      <section v-show="activeSection === 'profile'" class="profile-section">
        <el-card shadow="never" class="profile-card">
          <div class="profile-header">
            <el-avatar :size="88" :src="authStore.user?.avatar || undefined" class="profile-avatar">
              {{ avatarText }}
            </el-avatar>
            <div>
              <h2>{{ displayName }}</h2>
              <p>@{{ authStore.user?.username }}</p>
            </div>
            <el-button class="profile-edit-button" type="primary" round @click="openProfileDialog">编辑资料</el-button>
          </div>

          <el-descriptions :column="profileDescriptionColumn" border class="profile-descriptions">
            <el-descriptions-item label="用户名">{{ authStore.user?.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ authStore.user?.nickname || '暂未设置' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ genderText }}</el-descriptions-item>
            <el-descriptions-item label="座右铭" :span="profileDescriptionColumn">
              <span class="profile-motto-text">{{ authStore.user?.motto || '暂未设置' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ authStore.user?.email || '暂未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formattedCreatedAt }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="profile-model-card">
          <div class="profile-model-header">
            <div>
              <h3>AI刷题模型</h3>
            </div>
          </div>

          <ModelEntitlementSummary :status="modelEntitlementStatus" @authorize="handleModelAuthorize" />

          <div class="profile-redeem-panel">
            <el-input
              v-model.trim="redeemForm.code"
              maxlength="32"
              clearable
              size="large"
              placeholder="请输入授权码"
              aria-label="授权码"
              @keyup.enter="redeemCode"
            />
            <el-button type="primary" round size="large" :loading="redeeming" @click="redeemCode">授权</el-button>
          </div>
        </el-card>

        <GrowthOverviewPanel />
      </section>

      <section v-show="activeSection === 'question-stats'" class="question-stats-section">
        <el-card shadow="never" class="question-stats-hero">
          <div>
            <h2>智能刷题记录</h2>
          </div>
          <el-button type="primary" round :loading="questionStatsLoading" @click="refreshQuestionStats">刷新</el-button>
        </el-card>

        <div class="stats-summary-grid" aria-label="刷题记录概览">
          <div class="stats-summary-item">
            <span>已练题目</span>
            <strong>{{ overviewMetric.practicedQuestionCount }}</strong>
          </div>
          <div class="stats-summary-item">
            <span>累计答题</span>
            <strong>{{ overviewMetric.totalAnswerCount }}</strong>
          </div>
          <div class="stats-summary-item">
            <span>平均最高分</span>
            <strong>{{ formatScore(overviewMetric.averageBestScore) }}</strong>
          </div>
          <div class="stats-summary-item is-warning">
            <span>薄弱题目</span>
            <strong>{{ overviewMetric.weakQuestionCount }}</strong>
          </div>
        </div>

        <el-card shadow="never" class="question-stats-panel">
          <el-tabs v-model="activeStatsTab" class="question-stats-tabs">
            <el-tab-pane label="记录列表" name="records">
              <form class="stats-filter-form" @submit.prevent="searchQuestionStats">
                <el-input
                  v-model.trim="questionStatsQuery.keyword"
                  clearable
                  size="large"
                  placeholder="搜索题干关键词"
                  aria-label="搜索题干关键词"
                />
                <el-select
                  v-model="questionStatsQuery.questionType"
                  clearable
                  filterable
                  size="large"
                  placeholder="选择题型"
                  aria-label="选择题型"
                >
                  <el-option
                    v-for="questionType in questionTypeOptions"
                    :key="questionType"
                    :label="questionType"
                    :value="questionType"
                  />
                </el-select>
                <el-button type="primary" round native-type="submit" :loading="questionStatsLoading">查询</el-button>
              </form>

              <el-skeleton :loading="questionStatsLoading" animated :rows="8">
                <el-empty v-if="questionStatsPage.records.length === 0" description="暂无智能刷题记录" />
                <div v-else class="stats-table-wrap">
                  <el-table
                    :data="questionStatsPage.records"
                    row-key="questionCode"
                    class="question-stats-table"
                    header-cell-class-name="question-stats-table-header"
                  >
                    <el-table-column prop="question" label="题目" min-width="320" show-overflow-tooltip>
                      <template #default="{ row }">
                        <p class="question-title-cell">{{ row.question }}</p>
                      </template>
                    </el-table-column>
                    <el-table-column prop="questionType" label="题型" min-width="150">
                      <template #default="{ row }">
                        <el-tag effect="plain" class="question-type-tag">{{ row.questionType }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="answerCount" label="次数" width="92" align="center" />
                    <el-table-column prop="bestScore" label="最高分" width="104" align="center">
                      <template #default="{ row }">
                        <span :class="['score-text', resolveScoreClass(row.bestScore)]">{{ row.bestScore }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column prop="lastScore" label="最近分" width="104" align="center">
                      <template #default="{ row }">
                        <span :class="['score-text', resolveScoreClass(row.lastScore)]">{{ row.lastScore }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column prop="lastAnsweredAt" label="最近练习" min-width="168">
                      <template #default="{ row }">
                        {{ formatDateTime(row.lastAnsweredAt) }}
                      </template>
                    </el-table-column>
                  </el-table>

                  <el-pagination
                    v-model:current-page="questionStatsQuery.pageNo"
                    v-model:page-size="questionStatsQuery.pageSize"
                    class="stats-pagination"
                    layout="total, sizes, prev, pager, next"
                    :page-sizes="[10, 20, 50]"
                    :total="questionStatsPage.total"
                    @size-change="handleQuestionStatsSizeChange"
                    @current-change="loadQuestionStatsPage"
                  />
                </div>
              </el-skeleton>
            </el-tab-pane>

            <el-tab-pane label="薄弱点分析" name="weakness">
              <el-empty v-if="weakTypeStats.length === 0" description="暂无薄弱题型" />
              <div v-else class="weakness-grid">
                <article v-for="item in weakTypeStats" :key="item.questionType" class="weakness-card">
                  <div class="weakness-card-head">
                    <strong>{{ item.questionType }}</strong>
                    <span>{{ item.weakCount }} 个薄弱点</span>
                  </div>
                  <el-progress
                    :percentage="resolveWeakPercent(item)"
                    :stroke-width="10"
                    :show-text="false"
                    class="weakness-progress"
                  />
                  <div class="weakness-card-meta">
                    <span>平均最高分 {{ formatScore(item.averageBestScore) }}</span>
                    <span>已练 {{ item.questionCount }} 题</span>
                  </div>
                </article>
              </div>
            </el-tab-pane>

            <el-tab-pane label="多维视图" name="overview">
              <el-empty v-if="typeStatsForChart.length === 0" description="暂无题型维度数据" />
              <div v-else class="dimension-panel">
                <article v-for="item in typeStatsForChart" :key="item.questionType" class="dimension-row">
                  <div class="dimension-row-title">
                    <strong>{{ item.questionType }}</strong>
                    <span>{{ item.questionCount }} 题 / {{ item.answerCount }} 次</span>
                  </div>
                  <div class="dimension-bars" aria-label="题型得分和练习分布">
                    <div class="dimension-bar-line">
                      <span>最高分</span>
                      <i :style="{ width: `${clampPercent(item.averageBestScore)}%` }"></i>
                      <b>{{ formatScore(item.averageBestScore) }}</b>
                    </div>
                    <div class="dimension-bar-line is-last">
                      <span>最近分</span>
                      <i :style="{ width: `${clampPercent(item.averageLastScore)}%` }"></i>
                      <b>{{ formatScore(item.averageLastScore) }}</b>
                    </div>
                  </div>
                </article>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </section>

      <el-dialog v-model="profileDialogVisible" title="编辑资料" width="520px" class="profile-edit-dialog" destroy-on-close align-center>
        <el-form :model="profileForm" label-position="top" class="profile-edit-form" @submit.prevent>
          <el-form-item label="昵称">
            <el-input v-model.trim="profileForm.nickname" maxlength="64" show-word-limit clearable placeholder="请输入昵称" size="large" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="profileForm.gender" clearable placeholder="-" size="large">
              <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="座右铭">
            <el-input
              v-model.trim="profileForm.motto"
              maxlength="60"
              show-word-limit
              clearable
              placeholder="请输入座右铭"
              size="large"
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <div class="profile-edit-actions">
            <el-button round size="large" @click="profileDialogVisible = false">取消</el-button>
            <el-button type="primary" round size="large" :loading="savingProfile" @click="saveProfile">保存</el-button>
          </div>
        </template>
      </el-dialog>
    </main>
  </section>
</template>

<script setup lang="ts">
import { ElCard } from 'element-plus/es/components/card/index.mjs';
import { ElDescriptions, ElDescriptionsItem } from 'element-plus/es/components/descriptions/index.mjs';
import { ElEmpty } from 'element-plus/es/components/empty/index.mjs';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs';
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs';
import { ElProgress } from 'element-plus/es/components/progress/index.mjs';
import { ElSkeleton } from 'element-plus/es/components/skeleton/index.mjs';
import { ElTabPane, ElTabs } from 'element-plus/es/components/tabs/index.mjs';
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs';
import { ElTag } from 'element-plus/es/components/tag/index.mjs';
import 'element-plus/es/components/card/style/css';
import 'element-plus/es/components/descriptions/style/css';
import 'element-plus/es/components/empty/style/css';
import 'element-plus/es/components/pagination/style/css';
import 'element-plus/es/components/progress/style/css';
import 'element-plus/es/components/select/style/css';
import 'element-plus/es/components/skeleton/style/css';
import 'element-plus/es/components/tabs/style/css';
import 'element-plus/es/components/table/style/css';
import 'element-plus/es/components/tag/style/css';
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { fetchModelEntitlementStatus, redeemModelCode, type ModelEntitlementStatus } from '../../api/modelEntitlements';
import {
  fetchCurrentUserQuestionStats,
  fetchCurrentUserQuestionStatsOverview,
  type GenderCode,
  type UserQuestionStatsItem,
  type UserQuestionStatsOverview,
  type UserQuestionTypeStats,
} from '../../api/user';
import { useAuthStore } from '../../stores/auth';
import type { PageResponse } from '../../types/page';
import ModelEntitlementSummary from '../../components/model/ModelEntitlementSummary.vue';
import GrowthOverviewPanel from './components/GrowthOverviewPanel.vue';
import { openModelAuthorization } from '../../utils/modelAuthorization';

const PROFILE_MOBILE_QUERY = '(max-width: 720px)';
const DEFAULT_QUESTION_STATS_PAGE_SIZE = 10;
const WEAK_SCORE_LINE = 60;
const CHART_TOP_LIMIT = 8;
const authStore = useAuthStore();
const savingProfile = ref(false);
const redeeming = ref(false);
const profileDialogVisible = ref(false);
const isProfileMobile = ref(false);
const activeSection = ref('profile');
const activeStatsTab = ref('records');
const questionStatsInitialized = ref(false);
const questionStatsLoading = ref(false);
const modelEntitlementStatus = ref<ModelEntitlementStatus | null>(null);
let profileMediaQuery: MediaQueryList | null = null;

const genderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
] as const;

// 资料表单只维护用户允许本人修改的字段。
const profileForm = reactive<{ nickname: string; gender: GenderCode | ''; motto: string }>({
  nickname: '',
  gender: '',
  motto: '',
});
const redeemForm = reactive({ code: '' });

// 刷题记录查询条件只影响列表，不改变任何刷题业务状态。
const questionStatsQuery = reactive({
  pageNo: 1,
  pageSize: DEFAULT_QUESTION_STATS_PAGE_SIZE,
  keyword: '',
  questionType: '',
});

// 概览与分页数据分开存放，避免翻页影响分析视图。
const questionStatsOverview = ref<UserQuestionStatsOverview | null>(null);
const questionStatsPage = ref<PageResponse<UserQuestionStatsItem>>({
  records: [],
  pageNo: 1,
  pageSize: DEFAULT_QUESTION_STATS_PAGE_SIZE,
  total: 0,
});

// 展示名优先使用昵称，未设置时回退用户名。
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'AI 学习者');
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());
const genderText = computed(() => resolveGenderText(authStore.user?.gender || null));
const profileDescriptionColumn = computed(() => (isProfileMobile.value ? 1 : 2));

// 概览数据统一做空值兜底，模板无需重复判空。
const overviewMetric = computed(() => ({
  practicedQuestionCount: questionStatsOverview.value?.practicedQuestionCount ?? 0,
  totalAnswerCount: questionStatsOverview.value?.totalAnswerCount ?? 0,
  averageBestScore: Number(questionStatsOverview.value?.averageBestScore ?? 0),
  averageLastScore: Number(questionStatsOverview.value?.averageLastScore ?? 0),
  weakQuestionCount: questionStatsOverview.value?.weakQuestionCount ?? 0,
}));

// 题型选项来自当前用户实际已练数据。
const questionTypeOptions = computed(() => questionStatsOverview.value?.questionTypes ?? []);
const weakTypeStats = computed(() => {
  const typeStats = questionStatsOverview.value?.typeStats ?? [];
  return typeStats
    .filter((item) => item.weakCount > 0 || Number(item.averageBestScore) < WEAK_SCORE_LINE)
    .slice(0, CHART_TOP_LIMIT);
});
const typeStatsForChart = computed(() => (questionStatsOverview.value?.typeStats ?? []).slice(0, CHART_TOP_LIMIT));

// 注册时间统一展示为本地可读格式。
const formattedCreatedAt = computed(() => {
  if (!authStore.user?.createdAt) {
    return '暂未获取';
  }
  return new Date(authStore.user.createdAt).toLocaleString('zh-CN', { hour12: false });
});

/**
 * 切换个人中心左侧导航。
 *
 * @param section 目标导航项
 */
function handleNavigationSelect(section: string): void {
  activeSection.value = section;
  if (section === 'question-stats') {
    void ensureQuestionStatsLoaded();
  }
}

/**
 * 打开资料编辑弹窗。
 */
function openProfileDialog(): void {
  resetProfileForm();
  profileDialogVisible.value = true;
}

/**
 * 重置资料表单。
 */
function resetProfileForm(): void {
  profileForm.nickname = authStore.user?.nickname || '';
  profileForm.gender = authStore.user?.gender || '';
  profileForm.motto = authStore.user?.motto || '';
}

/**
 * 保存用户资料。
 */
async function saveProfile(): Promise<void> {
  const nickname = profileForm.nickname.trim();
  if (!nickname || nickname.length > 64) {
    ElMessage.warning('昵称不能为空，且不能超过64位');
    return;
  }
  if (profileForm.motto.trim().length > 60) {
    ElMessage.warning('座右铭不能超过60位');
    return;
  }

  savingProfile.value = true;
  try {
    // 空座右铭按未设置处理，由 AI 刷题页展示默认文案。
    const motto = profileForm.motto.trim();
    await authStore.updateProfile({
      nickname,
      gender: profileForm.gender || null,
      motto: motto || null,
    });
    profileDialogVisible.value = false;
    resetProfileForm();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资料保存失败');
  } finally {
    savingProfile.value = false;
  }
}

/**
 * 加载模型权益信息。
 */
async function loadModelEntitlementStatus(): Promise<void> {
  modelEntitlementStatus.value = await fetchModelEntitlementStatus();
}

/**
 * 打开模型授权入口。
 */
async function handleModelAuthorize(): Promise<void> {
  await openModelAuthorization(modelEntitlementStatus.value);
}

/**
 * 授权模型权益授权码。
 */
async function redeemCode(): Promise<void> {
  const code = redeemForm.code.trim();
  if (!code) {
    ElMessage.warning('请输入授权码');
    return;
  }
  redeeming.value = true;
  try {
    const result = await redeemModelCode(code);
    modelEntitlementStatus.value = result.entitlement;
    redeemForm.code = '';
    ElMessage.success(result.message);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '授权失败');
  } finally {
    redeeming.value = false;
  }
}

/**
 * 确保刷题记录只在首次进入时加载。
 */
async function ensureQuestionStatsLoaded(): Promise<void> {
  if (questionStatsInitialized.value) {
    return;
  }
  await refreshQuestionStats();
  questionStatsInitialized.value = true;
}

/**
 * 刷新智能刷题记录概览和列表。
 */
async function refreshQuestionStats(): Promise<void> {
  questionStatsLoading.value = true;
  try {
    // 两个只读接口均不读取答案和答题明细。
    const [overview, page] = await Promise.all([
      fetchCurrentUserQuestionStatsOverview(),
      fetchCurrentUserQuestionStats({
        pageNo: questionStatsQuery.pageNo,
        pageSize: questionStatsQuery.pageSize,
        keyword: questionStatsQuery.keyword,
        questionType: questionStatsQuery.questionType,
      }),
    ]);
    questionStatsOverview.value = overview;
    questionStatsPage.value = page;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '智能刷题记录加载失败');
  } finally {
    questionStatsLoading.value = false;
  }
}

/**
 * 查询智能刷题记录列表。
 */
async function searchQuestionStats(): Promise<void> {
  questionStatsQuery.pageNo = 1;
  await loadQuestionStatsPage();
}

/**
 * 加载当前页智能刷题记录。
 */
async function loadQuestionStatsPage(): Promise<void> {
  questionStatsLoading.value = true;
  try {
    // 分页查询只刷新表格，概览保持当前用户全量汇总。
    questionStatsPage.value = await fetchCurrentUserQuestionStats({
      pageNo: questionStatsQuery.pageNo,
      pageSize: questionStatsQuery.pageSize,
      keyword: questionStatsQuery.keyword,
      questionType: questionStatsQuery.questionType,
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '智能刷题记录查询失败');
  } finally {
    questionStatsLoading.value = false;
  }
}

/**
 * 处理刷题记录每页数量变化。
 */
function handleQuestionStatsSizeChange(): void {
  questionStatsQuery.pageNo = 1;
  void loadQuestionStatsPage();
}

/**
 * 解析性别展示文案。
 */
function resolveGenderText(gender: GenderCode | null): string {
  if (gender === 'MALE') {
    return '男';
  }
  if (gender === 'FEMALE') {
    return '女';
  }
  return '-';
}

/**
 * 格式化得分。
 *
 * @param value 原始得分
 * @return 展示得分
 */
function formatScore(value: number | string | null | undefined): string {
  const score = Number(value ?? 0);
  return Number.isInteger(score) ? String(score) : score.toFixed(1).replace(/\.0$/, '');
}

/**
 * 格式化日期时间。
 *
 * @param value 原始时间
 * @return 展示时间
 */
function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

/**
 * 解析得分颜色等级。
 *
 * @param score 原始得分
 * @return 样式类名
 */
function resolveScoreClass(score: number): string {
  if (score < WEAK_SCORE_LINE) {
    return 'is-weak';
  }
  if (score >= 85) {
    return 'is-strong';
  }
  return 'is-normal';
}

/**
 * 计算薄弱点占比。
 *
 * @param item 题型汇总
 * @return 百分比
 */
function resolveWeakPercent(item: UserQuestionTypeStats): number {
  if (item.questionCount <= 0) {
    return 0;
  }
  return Math.round((item.weakCount / item.questionCount) * 100);
}

/**
 * 限制图表百分比范围。
 *
 * @param value 原始百分值
 * @return 安全百分值
 */
function clampPercent(value: number | string | null | undefined): number {
  const percent = Number(value ?? 0);
  return Math.max(0, Math.min(100, Math.round(percent)));
}

/**
 * 同步个人资料表格列数断点。
 *
 * @param event 媒体查询变化事件
 */
function syncProfileViewport(event?: MediaQueryListEvent): void {
  isProfileMobile.value = event ? event.matches : Boolean(profileMediaQuery?.matches);
}

watch(
  () => [authStore.user?.nickname, authStore.user?.gender, authStore.user?.motto] as const,
  () => resetProfileForm(),
  { immediate: true },
);

onMounted(() => {
  // 资料描述组件需要真实列数，避免手机端两列表格撑破屏幕。
  profileMediaQuery = window.matchMedia(PROFILE_MOBILE_QUERY);
  syncProfileViewport();
  profileMediaQuery.addEventListener('change', syncProfileViewport);
  loadModelEntitlementStatus().catch((error: unknown) => {
    ElMessage.error(error instanceof Error ? error.message : '模型权益加载失败');
  });
});

onBeforeUnmount(() => {
  profileMediaQuery?.removeEventListener('change', syncProfileViewport);
  profileMediaQuery = null;
});
</script>

<style scoped lang="scss">
.profile-center-page {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 24px;
  max-width: 1440px;
  padding: 0 8px;
  margin: 0 auto;
}

.profile-sidebar {
  min-height: 560px;
  padding: 18px;
  background: var(--color-glass-surface-strong);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-card);
}

.profile-sidebar-header {
  display: flex;
  gap: 14px;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border-soft);
}

.profile-sidebar-icon {
  display: grid;
  width: 46px;
  height: 46px;
  color: var(--color-surface);
  font-weight: 700;
  background: var(--color-brand-gradient);
  border-radius: var(--radius-lg);
  place-items: center;
}

.profile-sidebar-header h2 {
  margin: 0 0 6px;
  color: var(--color-heading);
  font-size: 20px;
}

.profile-sidebar-header p {
  margin: 0;
  color: var(--color-subtle);
  font-size: 13px;
}

.profile-menu {
  margin-top: 18px;
  border-right: 0;
  background: transparent;
}

.profile-menu .el-menu-item {
  height: 46px;
  margin-bottom: 8px;
  color: var(--color-muted);
  font-weight: 700;
  border-radius: var(--radius-md);
}

.profile-menu .el-menu-item.is-active {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.profile-content,
.profile-section,
.question-stats-section {
  min-width: 0;
}

.profile-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-card,
.profile-model-card,
.question-stats-panel,
.question-stats-hero {
  border: 1px solid var(--color-border);
  border-radius: 18px;
}

.profile-model-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.profile-model-header h3 {
  margin: 0;
  color: var(--color-heading);
  font-size: 20px;
}

.profile-redeem-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  margin-top: 18px;
}

.profile-redeem-panel .el-button {
  min-width: 112px;
  font-weight: 800;
}

.profile-header {
  // 基础资料卡只保留用户身份信息，成长信息统一放入下方成长概览。
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 22px;
  padding: 18px 10px 6px;
}

.profile-header h2 {
  margin: 0;
  color: var(--color-heading);
}

.profile-header p {
  margin: 6px 0 0;
  color: var(--color-muted);
}

.profile-edit-button {
  margin-left: auto;
  font-weight: 700;
}

.profile-descriptions {
  // 个人资料表格使用固定布局，右侧内容列预留更多空间承载座右铭。
  :deep(.el-descriptions__table) {
    table-layout: fixed;
    width: 100%;
  }

  :deep(col:nth-child(1)),
  :deep(col:nth-child(3)) {
    width: 8%;
  }

  :deep(col:nth-child(2)) {
    width: 32%;
  }

  :deep(col:nth-child(4)) {
    width: 52%;
  }

  :deep(.el-descriptions__cell) {
    overflow-wrap: break-word;
    word-break: break-word;
  }

  :deep(.el-descriptions__label) {
    white-space: nowrap;
  }

  :deep(.el-descriptions__cell:nth-child(2)) {
    width: 32%;
  }

  :deep(.el-descriptions__cell:nth-child(4)) {
    width: 52%;
  }
}

.profile-edit-form {
  // 弹窗内只保留用户可编辑资料，避免混入系统内部形象逻辑。
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-edit-form :deep(.el-select) {
  width: 100%;
}

.profile-motto-text {
  // 座右铭展示占满资料单元格，避免固定窄宽导致右侧留白过大。
  display: block;
  width: 100%;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  word-break: break-word;
}

.profile-edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.question-stats-section {
  display: grid;
  gap: 18px;
}

.question-stats-hero :deep(.el-card__body) {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

.question-stats-hero h2 {
  margin: 0 0 8px;
  color: var(--color-heading);
  font-size: 28px;
}

.question-stats-hero p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.7;
}

.stats-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stats-summary-item {
  display: grid;
  gap: 8px;
  padding: 18px;
  background: var(--color-glass-surface-strong);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-subtle);
}

.stats-summary-item span {
  color: var(--color-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.stats-summary-item strong {
  color: var(--color-heading);
  font-size: 28px;
  line-height: 1;
}

.stats-summary-item.is-warning strong {
  color: #d97706;
}

.question-stats-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.stats-filter-form {
  display: grid;
  grid-template-columns: minmax(240px, 1.4fr) minmax(180px, 0.7fr) auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
}

.stats-filter-form .el-select {
  width: 100%;
}

.stats-table-wrap {
  min-width: 0;
}

.question-title-cell {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--color-text);
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.question-type-tag {
  max-width: 100%;
  white-space: normal;
}

.score-text {
  font-weight: 800;
}

.score-text.is-weak {
  color: #dc2626;
}

.score-text.is-normal {
  color: #d97706;
}

.score-text.is-strong {
  color: #16a34a;
}

.stats-pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

.weakness-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.weakness-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.weakness-card-head,
.weakness-card-meta,
.dimension-row-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.weakness-card-head strong,
.dimension-row-title strong {
  color: var(--color-heading);
}

.weakness-card-head span,
.weakness-card-meta,
.dimension-row-title span {
  color: var(--color-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.weakness-progress :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #f59e0b, #ef4444);
}

.dimension-panel {
  display: grid;
  gap: 14px;
}

.dimension-row {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.dimension-bars {
  display: grid;
  gap: 10px;
}

.dimension-bar-line {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) 52px;
  gap: 10px;
  align-items: center;
  color: var(--color-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.dimension-bar-line i {
  display: block;
  height: 10px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
  border-radius: 999px;
}

.dimension-bar-line.is-last i {
  background: linear-gradient(90deg, #f59e0b, #22c55e);
}

.dimension-bar-line b {
  color: var(--color-heading);
  text-align: right;
}

@media (max-width: 980px) {
  .stats-summary-grid,
  .weakness-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .profile-center-page {
    // 手机端取消双列，把导航压缩为横向滚动入口。
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 0;
  }

  .profile-sidebar {
    min-height: auto;
    padding: 14px;
    border-radius: 20px;
  }

  .profile-sidebar-header {
    padding-bottom: 12px;
  }

  .profile-menu {
    display: flex;
    gap: 8px;
    max-width: 100%;
    margin-top: 12px;
    overflow-x: auto;
  }

  .profile-menu .el-menu-item {
    flex: 0 0 auto;
    height: 44px;
    min-height: 44px;
    margin-bottom: 0;
    padding: 0 16px;
  }

  .question-stats-hero :deep(.el-card__body),
  .weakness-card-head,
  .weakness-card-meta,
  .dimension-row-title {
    align-items: stretch;
    flex-direction: column;
  }

  .stats-filter-form {
    grid-template-columns: 1fr;
  }

  .stats-filter-form .el-button {
    width: 100%;
    min-height: 44px;
  }

  .question-stats-panel :deep(.el-card__body) {
    overflow-x: auto;
  }

  .question-stats-table {
    min-width: 820px;
  }
}

@media (max-width: 720px) {
  .profile-section {
    gap: 14px;
  }

  .profile-card {
    border-radius: 18px;
  }

  .profile-header {
    align-items: flex-start;
    flex-wrap: wrap;
    padding: 12px 0 4px;
  }

  .profile-header :deep(.el-avatar) {
    width: 64px !important;
    height: 64px !important;
    font-size: 22px;
  }

  .profile-header h2 {
    font-size: 23px;
  }

  .profile-edit-button {
    width: 100%;
    margin-left: 0;
  }

  .profile-descriptions {
    :deep(.el-descriptions__table) {
      table-layout: auto;
    }

    :deep(.el-descriptions__label) {
      width: 88px;
    }

    :deep(col:nth-child(1)),
    :deep(col:nth-child(2)),
    :deep(col:nth-child(3)),
    :deep(col:nth-child(4)),
    :deep(.el-descriptions__cell:nth-child(2)),
    :deep(.el-descriptions__cell:nth-child(4)) {
      width: auto;
    }
  }

  .profile-edit-actions {
    justify-content: stretch;
  }

  .profile-edit-actions .el-button {
    flex: 1;
    min-height: 44px;
  }

  .profile-redeem-panel,
  .stats-summary-grid,
  .weakness-grid {
    grid-template-columns: 1fr;
  }

  .profile-redeem-panel .el-button {
    width: 100%;
    min-height: 44px;
  }
}

@media (max-width: 480px) {
  .profile-sidebar,
  .question-stats-panel :deep(.el-card__body),
  .question-stats-hero :deep(.el-card__body) {
    padding: 14px;
  }

  .stats-summary-item,
  .weakness-card,
  .dimension-row {
    padding: 14px;
  }
}
</style>
