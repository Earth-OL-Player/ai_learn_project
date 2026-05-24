<template>
  <section :class="['practice-chat-page', { 'is-side-collapsed': isGrowthPanelCollapsed }]">
    <aside :class="['practice-side-card', { 'is-collapsed': isGrowthPanelCollapsed }]" aria-label="学习成长信息">
      <div class="practice-side-toolbar">
        <button
          type="button"
          class="practice-side-toggle"
          :aria-expanded="!isGrowthPanelCollapsed"
          aria-controls="practice-growth-panel"
          :aria-label="isGrowthPanelCollapsed ? '展开座右铭和人物段位形象' : '收起座右铭和人物段位形象'"
          @click="toggleGrowthPanel"
        >
          {{ isGrowthPanelCollapsed ? '展开' : '收起' }}
        </button>
      </div>

      <div id="practice-growth-panel" v-show="!isGrowthPanelCollapsed" class="practice-side-content">
        <div :class="['filter-card-title', { 'is-default-motto': !hasCustomMotto }]">
          <strong v-for="(line, index) in mottoLines" :key="`${line}-${index}`">{{ line }}</strong>
        </div>

        <RealmCharacterCard
          v-if="growth"
          class="practice-realm-card"
          :nickname="displayName"
          :rank="growth.rank"
          :level="growth.level"
          :current-experience="growth.currentExperience"
          :next-level-experience="growth.nextLevelExperience"
          :level-progress-text="growth.levelProgressText"
          :gender="authStore.user?.gender || null"
          compact
        />
      </div>
    </aside>

    <main class="practice-chat-card" aria-label="AI智能刷题对话区">
      <div class="practice-chat-header">
        <PracticeCategorySelector
          v-model="selectedCategories"
          :categories="categories"
          @visible-change="handleCategoryVisibleChange"
          @change="handleCategoryChange"
        />
        <ModelEntitlementSummary
          class="practice-model-summary"
          :status="modelEntitlementStatus"
          compact
          :show-remaining-days="false"
          @authorize="handleModelAuthorize"
        />
        <el-button
          class="main-action-button"
          type="primary"
          round
          :loading="loading"
          :aria-label="nextButtonText"
          @click="handleNextQuestion"
        >
          {{ nextButtonText }}
        </el-button>
        <el-button
          class="sub-action-button"
          round
          :disabled="retryButtonDisabled"
          :loading="loading"
          aria-label="重答本题"
          @click="handleRetry"
        >
          重答本题
        </el-button>
      </div>

      <PracticeMessageList
        :is-logged-in="authStore.isLoggedIn"
        :messages="messages"
        :streaming-placeholder-text="streamingPlaceholderText"
        @panel-ready="messagePanelRef = $event"
        @scroll="handleMessagePanelScroll"
      />

      <PracticeInputBar
        v-model="inputText"
        :loading="loading"
        :placeholder="inputPlaceholder"
        @focus-input="handleGuestInteraction"
        @send="sendMessage"
      />
    </main>

    <PracticeBadgeDialog
      v-model:visible="badgeDialogVisible"
      :badges="badgeDialogBadges"
      :subtitle="badgeAwardSubtitle"
      @closed="handleBadgeDialogClosed"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import RealmCharacterCard from '../../components/growth/RealmCharacterCard.vue';
import ModelEntitlementSummary from '../../components/model/ModelEntitlementSummary.vue';
import PracticeBadgeDialog from './components/PracticeBadgeDialog.vue';
import PracticeCategorySelector from './components/PracticeCategorySelector.vue';
import PracticeInputBar from './components/PracticeInputBar.vue';
import PracticeMessageList from './components/PracticeMessageList.vue';
import './styles/practice-agent.scss';
import { usePracticeChat } from './usePracticeChat';

// 左侧成长栏默认展开，保持 Web 端进入页面时的当前展示状态。
const isGrowthPanelCollapsed = ref(false);

const {
  authStore,
  badgeAwardSubtitle,
  badgeDialogBadges,
  badgeDialogVisible,
  categories,
  displayName,
  growth,
  handleBadgeDialogClosed,
  handleCategoryChange,
  handleCategoryVisibleChange,
  handleGuestInteraction,
  handleMessagePanelScroll,
  handleModelAuthorize,
  handleNextQuestion,
  handleRetry,
  inputPlaceholder,
  inputText,
  loading,
  messagePanelRef,
  modelEntitlementStatus,
  messages,
  nextButtonText,
  retryButtonDisabled,
  selectedCategories,
  sendMessage,
  streamingPlaceholderText,
} = usePracticeChat();

const DEFAULT_MOTTO_LINES = ['昨日因,今日果,前尘不咎', '今日因,明日果,当下即道'] as const;
const hasCustomMotto = computed(() => Boolean(authStore.user?.motto?.trim()));

// 有用户自定义座右铭时优先展示自定义内容，否则保持默认两行文案。
const mottoLines = computed(() => {
  if (!hasCustomMotto.value) {
    return [...DEFAULT_MOTTO_LINES];
  }

  // 支持用户在文本域中换行输入，页面按有效行展示。
  const customMotto = authStore.user?.motto?.trim() || '';
  return customMotto.split(/\r?\n/u).map((line) => line.trim()).filter(Boolean);
});

/**
 * 统一切换左侧座右铭和人物段位形象。
 */
function toggleGrowthPanel(): void {
  isGrowthPanelCollapsed.value = !isGrowthPanelCollapsed.value;
}
</script>
