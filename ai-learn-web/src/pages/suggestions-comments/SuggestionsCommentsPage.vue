<template>
  <section ref="pageElement" class="interaction-page" @input.capture="assistant.userChangedForm()" @click.capture="handleManualControl">
    <nav class="mode-switch" aria-label="建议评论区切换">
      <button :class="['mode-button', { active: activeTab === 'suggestions' }]" type="button" @click="switchTab('suggestions')">
        建议区
      </button>
      <button :class="['mode-button', { active: activeTab === 'comments' }]" type="button" @click="switchTab('comments')">
        评论区
      </button>
    </nav>

    <section class="interaction-board">
      <header class="board-header">
        <div>
          <h2>{{ activeTitle }} <span>{{ activeTotal }}</span></h2>
        </div>
        <div class="sort-tabs" aria-label="排序方式">
          <button :class="{ active: activeSort === 'hot' }" type="button" @click="changeSort('hot')">最热</button>
          <span></span>
          <button :class="{ active: activeSort === 'latest' }" type="button" @click="changeSort('latest')">最新</button>
        </div>
      </header>

      <div class="composer-card">
        <el-avatar :size="52" :src="currentAvatarSrc">{{ currentAvatarText }}</el-avatar>
        <div class="composer-main">
          <div v-if="activeTab === 'suggestions'" class="suggestion-type-row">
            <button
              v-for="item in suggestionTypes"
              :key="item.value"
              :class="['type-chip', { active: suggestionForm.type === item.value }]"
              type="button"
              :data-assistant-type="item.value"
              @click="suggestionForm.type = item.value"
            >
              {{ item.label }}
            </button>
          </div>

          <textarea
            v-if="activeTab === 'suggestions'"
            v-model="suggestionForm.content"
            data-assistant-target="suggestion.content"
            class="composer-input"
            :maxlength="TEXT_MAX_LENGTH"
            placeholder="请输入你的建议"
            @focus="guardComposerFocus"
          ></textarea>
          <textarea
            v-else
            v-model="commentForm.content"
            data-assistant-target="comment.content"
            class="composer-input"
            :maxlength="TEXT_MAX_LENGTH"
            placeholder="请输入你的评论"
            @focus="guardComposerFocus"
          ></textarea>

          <div class="composer-footer">
            <el-button type="primary" round :loading="activeSubmitting" data-assistant-target="composer.submit" @click="submitActiveContent">
              {{ activeSubmitText }}
            </el-button>
          </div>
        </div>
      </div>

      <el-skeleton :loading="activeLoading" animated :rows="6">
        <el-empty v-if="isActiveEmpty" :description="activeEmptyText" />

        <div v-else-if="activeTab === 'suggestions'" class="feed-list">
          <article v-for="item in suggestions" :key="item.id" class="feed-item" :data-assistant-record="item.id">
            <el-avatar class="item-avatar" :size="48" :src="authorAvatarSrc(item.author)">{{ authorAvatarText(item.author) }}</el-avatar>
            <div class="item-body">
              <div class="author-line">
                <strong>{{ resolveAuthorName(item.author) }}</strong>
                <span class="rank-badge">{{ formatAuthorRank(item.author) }}</span>
                <span class="type-badge">{{ item.typeText }}</span>
              </div>
              <p class="feed-content">{{ item.content }}</p>
              <div class="feed-actions">
                <span>{{ formatTime(item.createdAt) }}</span>
                <button :class="['action-button', { active: item.liked }]" type="button" :disabled="isLikeLoading('suggestion', item.id)" @click="handleSuggestionLike(item.id)">
                  赞 <span>{{ formatLikeCount(item.likeCount) }}</span>
                </button>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="feed-list">
          <article v-for="item in comments" :key="item.id" class="feed-item" :data-assistant-record="item.id">
            <el-avatar class="item-avatar" :size="48" :src="authorAvatarSrc(item.author)">{{ authorAvatarText(item.author) }}</el-avatar>
            <div class="item-body">
              <div class="author-line">
                <strong>{{ resolveAuthorName(item.author) }}</strong>
                <span class="rank-badge">{{ formatAuthorRank(item.author) }}</span>
              </div>
              <p class="feed-content">{{ item.content }}</p>
              <div class="feed-actions">
                <span>{{ formatTime(item.createdAt) }}</span>
                <button :class="['action-button', { active: item.liked }]" type="button" :disabled="isLikeLoading('comment', item.id)" @click="handleCommentLike(item.id)">
                  赞 <span>{{ formatLikeCount(item.likeCount) }}</span>
                </button>
                <button class="action-button" data-assistant-target="reply.open" type="button" @click="startReply(item)">回复</button>
              </div>

              <div v-if="replyTarget?.id === item.id" class="reply-composer">
                <textarea
                  v-model="replyContent"
                  data-assistant-target="reply.content"
                  :maxlength="TEXT_MAX_LENGTH"
                  :placeholder="`回复 ${resolveAuthorName(item.author)}`"
                ></textarea>
                <div class="reply-footer">
                  <span>回复同样只能使用纯文字。</span>
                  <div>
                    <el-button text @click="cancelReply()">取消</el-button>
                    <el-button type="primary" round :loading="replySubmitting" data-assistant-target="reply.submit" @click="submitReply()">发布回复</el-button>
                  </div>
                </div>
              </div>

              <div v-if="item.children.length > 0" class="child-list">
                <article v-for="child in item.children" :key="child.id" class="child-item" :data-assistant-record="child.id">
                  <el-avatar :size="34" :src="authorAvatarSrc(child.author)">{{ authorAvatarText(child.author) }}</el-avatar>
                  <div class="child-body">
                    <div class="author-line child-author">
                      <strong>{{ resolveAuthorName(child.author) }}</strong>
                      <span class="rank-badge">{{ formatAuthorRank(child.author) }}</span>
                    </div>
                    <p class="feed-content child-content">{{ child.content }}</p>
                    <div class="feed-actions child-actions">
                      <span>{{ formatTime(child.createdAt) }}</span>
                      <button :class="['action-button', { active: child.liked }]" type="button" :disabled="isLikeLoading('comment', child.id)" @click="handleCommentLike(child.id)">
                        赞 <span>{{ formatLikeCount(child.likeCount) }}</span>
                      </button>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </article>
        </div>
      </el-skeleton>

      <div v-if="activeTotal > activePageSize" class="pagination-row">
        <el-pagination
          v-if="activeTab === 'suggestions'"
          v-model:current-page="suggestionPage.pageNo"
          v-model:page-size="suggestionPage.pageSize"
          layout="prev, pager, next, total"
          :total="suggestionPage.total"
          @current-change="loadSuggestions"
        />
        <el-pagination
          v-else
          v-model:current-page="commentPage.pageNo"
          v-model:page-size="commentPage.pageSize"
          layout="prev, pager, next, total"
          :total="commentPage.total"
          @current-change="loadComments"
        />
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { ElEmpty } from 'element-plus/es/components/empty/index.mjs';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs';
import { ElSkeleton } from 'element-plus/es/components/skeleton/index.mjs';
import 'element-plus/es/components/empty/style/css';
import 'element-plus/es/components/pagination/style/css';
import 'element-plus/es/components/skeleton/style/css';
import { computed, nextTick, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createComment, fetchComments, fetchCommentThread, toggleCommentLike } from '../../api/comments';
import { submitAssistantForm } from '../../api/assistant';
import { registerCommunityAdapter } from '../../assistant/actionRegistry';
import { useAssistantStore } from '../../stores/assistant';
import type { AssistantPayload, AssistantPublishResult, AssistantSubmitContext } from '../../types/assistant';
import { createSuggestion, fetchSuggestions, toggleSuggestionLike } from '../../api/suggestions';
import { useAuthStore } from '../../stores/auth';
import type { CommentItem } from '../../types/comment';
import type { AuthorSummary, SuggestionItem } from '../../types/suggestion';
import { formatRelativeDateTime as formatTime } from '../../utils/dateTimeFormat';
import { resolveErrorMessage } from '../../utils/errorMessage';
import {
  DEFAULT_VISITOR_DISPLAY_NAME,
  resolveAvatarText,
  resolveUserAvatarText,
  resolveUserDisplayName,
} from '../../utils/userDisplay';

type ActiveTab = 'suggestions' | 'comments';
type SortType = 'hot' | 'latest';
type LikeKind = 'suggestion' | 'comment';

const PAGE_SIZE = 10;
const TEXT_MIN_LENGTH = 2;
const TEXT_MAX_LENGTH = 1000;
const TEXT_LENGTH_RANGE_TEXT = `${TEXT_MIN_LENGTH}到${TEXT_MAX_LENGTH}位`;
const UNSUPPORTED_TEXT_PATTERN = /[@＠\p{Extended_Pictographic}\uFE0F\u200D]/u;

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const assistant = useAssistantStore();
const pageElement = ref<HTMLElement | null>(null);
let unregisterAssistant: (() => void) | undefined;

// 页面状态按建议区和评论区拆分，避免两个页签互相污染。
const activeTab = ref<ActiveTab>(route.query.tab === 'comments' ? 'comments' : 'suggestions');
const suggestions = ref<SuggestionItem[]>([]);
const comments = ref<CommentItem[]>([]);
const sortState = reactive<Record<ActiveTab, SortType>>({ suggestions: 'hot', comments: 'hot' });

// 加载和提交状态保持轻量，直接驱动按钮和骨架屏。
const suggestionLoading = ref(false);
const commentLoading = ref(false);
const suggestionSubmitting = ref(false);
const commentSubmitting = ref(false);
const replySubmitting = ref(false);
const likeLoadingKey = ref('');

const suggestionPage = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const commentPage = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const suggestionForm = reactive({ type: 'FEATURE', content: '' });
const commentForm = reactive({ content: '' });
const replyContent = ref('');
const replyTarget = ref<CommentItem | null>(null);

const suggestionTypes = [
  { label: '功能建议', value: 'FEATURE' },
  { label: '体验优化', value: 'EXPERIENCE' },
  { label: '问题反馈', value: 'BUG' },
  { label: '内容建议', value: 'CONTENT' },
];

const activeSort = computed(() => sortState[activeTab.value]);
const activeLoading = computed(() => (activeTab.value === 'suggestions' ? suggestionLoading.value : commentLoading.value));
const activeSubmitting = computed(() => (activeTab.value === 'suggestions' ? suggestionSubmitting.value : commentSubmitting.value));
const activeTotal = computed(() => (activeTab.value === 'suggestions' ? suggestionPage.total : commentPage.total));
const activePageSize = computed(() => (activeTab.value === 'suggestions' ? suggestionPage.pageSize : commentPage.pageSize));
const activeTitle = computed(() => (activeTab.value === 'suggestions' ? '建议' : '评论'));
const activeSubmitText = computed(() => (activeTab.value === 'suggestions' ? '发布建议' : '发表评论'));
const activeEmptyText = computed(() => (activeTab.value === 'suggestions' ? '暂无建议，期待你的第一条反馈' : '暂无评论，欢迎开始交流'));
const isActiveEmpty = computed(() => (activeTab.value === 'suggestions' ? suggestions.value.length === 0 : comments.value.length === 0));

const currentDisplayName = computed(() => resolveUserDisplayName(authStore.user, DEFAULT_VISITOR_DISPLAY_NAME));
const currentAvatarSrc = computed(() => authStore.user?.avatar || undefined);
const currentAvatarText = computed(() => resolveAvatarText(currentDisplayName.value, DEFAULT_VISITOR_DISPLAY_NAME));

/**
 * 切换建议区或评论区。
 */
async function switchTab(tab: ActiveTab): Promise<void> {
  activeTab.value = tab;
  await router.replace({ path: route.path, query: { ...route.query, tab } });
}

/**
 * 切换当前页签排序方式。
 */
async function changeSort(sort: SortType): Promise<void> {
  if (sortState[activeTab.value] === sort) {
    return;
  }

  // 切换排序后回到第一页，确保最热和最新结果直观可见。
  sortState[activeTab.value] = sort;
  if (activeTab.value === 'suggestions') {
    suggestionPage.pageNo = 1;
    await loadSuggestions();
    return;
  }
  commentPage.pageNo = 1;
  await loadComments();
}

/**
 * 加载建议分页数据。
 */
async function loadSuggestions(): Promise<void> {
  suggestionLoading.value = true;
  try {
    const result = await fetchSuggestions(suggestionPage.pageNo, suggestionPage.pageSize, sortState.suggestions);
    suggestions.value = result.records;
    suggestionPage.total = result.total;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error));
  } finally {
    suggestionLoading.value = false;
  }
}

/**
 * 加载评论分页数据。
 */
async function loadComments(): Promise<void> {
  commentLoading.value = true;
  try {
    const result = await fetchComments(commentPage.pageNo, commentPage.pageSize, sortState.comments);
    comments.value = result.records;
    commentPage.total = result.total;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error));
  } finally {
    commentLoading.value = false;
  }
}

/**
 * 聚焦输入框时拦截未登录用户。
 */
async function guardComposerFocus(): Promise<void> {
  if (!authStore.isLoggedIn) {
    await requireLogin();
  }
}

/**
 * 提交当前页签内容。
 */
async function submitActiveContent(): Promise<void> {
  if (assistant.formBusy) { ElMessage.info('助手正在操作，请先选择“我来操作”再手工提交'); return; }
  if (activeTab.value === 'suggestions') {
    await submitSuggestion();
    return;
  }
  await submitComment();
}

/**
 * 提交建议。
 */
async function submitSuggestion(context?: AssistantSubmitContext): Promise<AssistantPublishResult | void> {
  if (!authStore.isLoggedIn) {
    await requireLogin();
    return;
  }
  const content = validatePlainText(suggestionForm.content, '建议内容');
  if (!content) {
    if (context) throw new Error('建议内容未通过校验');
    return;
  }
  if (suggestionSubmitting.value) throw new Error('建议正在提交，请等待结果');

  // 建议不再需要标题和处理状态，只提交类型和正文。
  suggestionSubmitting.value = true;
  try {
    const result = context ? await submitAssistantForm(context) : undefined;
    const createdSuggestion = result ? result.record as unknown as SuggestionItem : await createSuggestion({ type: suggestionForm.type, content });
    ElMessage.success('建议发布成功');
    if (suggestionForm.content.trim() === content) suggestionForm.content = '';
    suggestionPage.pageNo = 1;
    prependCreatedSuggestion(createdSuggestion);
    return result;
  } catch (error) {
    if (context) throw error;
    ElMessage.error(resolveErrorMessage(error));
  } finally {
    suggestionSubmitting.value = false;
  }
}

/**
 * 发表父评论。
 */
async function submitComment(context?: AssistantSubmitContext): Promise<AssistantPublishResult | void> {
  if (!authStore.isLoggedIn) {
    await requireLogin();
    return;
  }
  const content = validatePlainText(commentForm.content, '评论内容');
  if (!content) {
    if (context) throw new Error('评论内容未通过校验');
    return;
  }
  if (commentSubmitting.value) throw new Error('评论正在提交，请等待结果');

  // 父评论不携带 parentId，回复入口单独处理。
  commentSubmitting.value = true;
  try {
    const result = context ? await submitAssistantForm(context) : undefined;
    const createdComment = result ? result.record as unknown as CommentItem : await createComment({ content });
    ElMessage.success('评论发布成功');
    if (commentForm.content.trim() === content) commentForm.content = '';
    commentPage.pageNo = 1;
    prependCreatedComment(createdComment);
    return result;
  } catch (error) {
    if (context) throw error;
    ElMessage.error(resolveErrorMessage(error));
  } finally {
    commentSubmitting.value = false;
  }
}

/**
 * 打开父评论回复框。
 */
async function startReply(item: CommentItem, automated = false): Promise<void> {
  if (!authStore.isLoggedIn) {
    await requireLogin();
    return;
  }

  // 本期仅支持一级回复，因此只在父评论上展示回复入口。
  if (!automated) assistant.userChangedForm();
  assistant.selectedParentId = item.id;
  if (replyTarget.value?.id === item.id) return;
  replyTarget.value = item;
  replyContent.value = '';
}

/**
 * 取消当前回复。
 */
function cancelReply(manual = true): void {
  if (manual && assistant.formBusy) assistant.userChangedForm();
  assistant.selectedParentId = '';
  replyTarget.value = null;
  replyContent.value = '';
}

/**
 * 提交一级子评论。
 */
async function submitReply(context?: AssistantSubmitContext): Promise<AssistantPublishResult | void> {
  if (!context && assistant.formBusy) { ElMessage.info('请先接管助手操作再手工提交'); return; }
  if (!replyTarget.value) {
    return;
  }
  const content = validatePlainText(replyContent.value, '回复内容');
  if (!content) {
    if (context) throw new Error('回复内容未通过校验');
    return;
  }
  if (replySubmitting.value) throw new Error('回复正在提交，请等待结果');

  // 回复统一挂在父评论下，不产生孙级评论。
  replySubmitting.value = true;
  try {
    const parentId = replyTarget.value.id;
    const result = context ? await submitAssistantForm(context) : undefined;
    const createdReply = result ? result.record as unknown as CommentItem : await createComment({ content, parentId });
    ElMessage.success('回复发布成功');
    appendCreatedReply(parentId, createdReply);
    if (replyContent.value.trim() === content && replyTarget.value?.id === parentId) cancelReply(false);
    return result;
  } catch (error) {
    if (context) throw error;
    ElMessage.error(resolveErrorMessage(error));
  } finally {
    replySubmitting.value = false;
  }
}

/**
 * 点赞或取消点赞建议。
 */
async function handleSuggestionLike(id: string): Promise<void> {
  if (!(await ensureLoggedIn())) {
    return;
  }
  await runLikeAction('suggestion', id, async () => {
    const latestSuggestion = await toggleSuggestionLike(id);
    updateSuggestionInList(latestSuggestion);
  });
}

/**
 * 点赞或取消点赞评论。
 */
async function handleCommentLike(id: string): Promise<void> {
  if (!(await ensureLoggedIn())) {
    return;
  }
  await runLikeAction('comment', id, async () => {
    const latestComment = await toggleCommentLike(id);
    updateCommentInList(latestComment);
  });
}

/**
 * 执行点赞类动作并控制重复点击。
 */
async function runLikeAction(kind: LikeKind, id: string, action: () => Promise<void>): Promise<void> {
  const loadingKey = `${kind}-${id}`;
  if (likeLoadingKey.value) {
    return;
  }

  // 点赞后只更新当前条目，避免整块列表骨架屏造成页面闪烁。
  likeLoadingKey.value = loadingKey;
  try {
    await action();
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error));
  } finally {
    likeLoadingKey.value = '';
  }
}

/**
 * 判断指定点赞按钮是否处于加载态。
 */
function isLikeLoading(kind: LikeKind, id: string): boolean {
  return likeLoadingKey.value === `${kind}-${id}`;
}

/**
 * 将新建议插入当前列表顶部。
 */
function prependCreatedSuggestion(item: SuggestionItem): void {
  // 本地即时展示发布结果，避免重新拉取列表导致视觉闪烁。
  suggestions.value = [item, ...suggestions.value.filter((suggestion) => suggestion.id !== item.id)];
  suggestionPage.total += 1;
}

/**
 * 更新当前列表中的建议点赞状态。
 */
function updateSuggestionInList(item: SuggestionItem): void {
  suggestions.value = suggestions.value.map((suggestion) => (suggestion.id === item.id ? item : suggestion));
}

/**
 * 将新父评论插入当前列表顶部。
 */
function prependCreatedComment(item: CommentItem): void {
  // 后端新建父评论通常不带子评论，这里兜底为空数组保证模板稳定。
  const createdComment = normalizeCommentChildren(item);
  comments.value = [createdComment, ...comments.value.filter((comment) => comment.id !== item.id)];
  commentPage.total += 1;
}

/**
 * 将新回复追加到对应父评论下。
 */
function appendCreatedReply(parentId: string, item: CommentItem): void {
  comments.value = comments.value.map((comment) => {
    if (comment.id !== parentId) {
      return comment;
    }

    // 仅更新命中的父评论，保持其他评论 DOM 不重建。
    const children = comment.children.filter((child) => child.id !== item.id);
    return {
      ...comment,
      children: [...children, normalizeCommentChildren(item)],
      replyCount: comment.replyCount + 1,
    };
  });
}

/**
 * 更新当前列表中的评论点赞状态。
 */
function updateCommentInList(item: CommentItem): void {
  const latestComment = normalizeCommentChildren(item);
  comments.value = comments.value.map((comment) => {
    if (comment.id === latestComment.id) {
      return { ...latestComment, children: comment.children };
    }

    // 子评论点赞时只替换命中的子评论。
    return {
      ...comment,
      children: comment.children.map((child) => (child.id === latestComment.id ? latestComment : child)),
    };
  });
}

/**
 * 规范化评论子列表为空数组。
 */
function normalizeCommentChildren(item: CommentItem): CommentItem {
  return {
    ...item,
    children: item.children || [],
  };
}

/**
 * 确保用户已经登录。
 */
async function ensureLoggedIn(): Promise<boolean> {
  if (authStore.isLoggedIn) {
    return true;
  }
  await requireLogin();
  return false;
}

/**
 * 校验纯文字内容。
 */
function validatePlainText(value: string, label: string): string | null {
  const content = value.trim();
  if (content.length < TEXT_MIN_LENGTH || content.length > TEXT_MAX_LENGTH) {
    ElMessage.warning(`${label}长度需在${TEXT_LENGTH_RANGE_TEXT}之间`);
    return null;
  }
  if (UNSUPPORTED_TEXT_PATTERN.test(content)) {
    ElMessage.warning('仅支持纯文字，不能使用表情和艾特');
    return null;
  }
  return content;
}

/**
 * 触发布局层登录引导弹窗。
 */
async function requireLogin(): Promise<void> {
  await router.replace({ path: route.path, query: { ...route.query, loginGuide: '1' } });
}

/**
 * 获取作者展示名。
 */
function resolveAuthorName(author: AuthorSummary): string {
  return resolveUserDisplayName(author);
}

/**
 * 获取作者头像地址。
 */
function authorAvatarSrc(author: AuthorSummary): string | undefined {
  return author.avatar || undefined;
}

/**
 * 获取作者默认头像文字。
 */
function authorAvatarText(author: AuthorSummary): string {
  return resolveUserAvatarText(author);
}

/**
 * 格式化作者等级段位。
 */
function formatAuthorRank(author: AuthorSummary): string {
  const level = author.level || `LV${author.levelValue || 1}`;
  const rank = author.rank || '炼气期';
  return `${level}·${rank}`;
}

/**
 * 格式化点赞数量。
 */
function formatLikeCount(value: number): string {
  if (!value) {
    return '';
  }
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}万`;
  }
  return String(value);
}

onMounted(async () => {
  registerAssistantCapabilities();
  await Promise.all([loadSuggestions(), loadComments()]);
});
onBeforeUnmount(() => { unregisterAssistant?.(); assistant.selectedParentId = ''; });

watch(() => route.query.tab, tab => { activeTab.value = tab === 'comments' ? 'comments' : 'suggestions'; });

function handleManualControl(event: MouseEvent): void {
  if (event.isTrusted && (event.target as HTMLElement).closest('.mode-button, .type-chip, .sort-tabs button, .feed-actions button')) assistant.userChangedForm();
}

/** 将真实表单的响应式字段与提交函数注册给助手，模型不能访问任意控件。 */
function registerAssistantCapabilities(): void {
  const root = pageElement.value;
  if (!root) return;
  const recordElement = (id: string) => Array.from(root.querySelectorAll<HTMLElement>('[data-assistant-record]')).find(element => element.dataset.assistantRecord === id) ?? null;
  const validate = (payload: AssistantPayload) => {
    const kind = payload.kind;
    const value = kind === 'suggestion' ? suggestionForm.content : kind === 'reply' ? replyContent.value : commentForm.content;
    if (value !== payload.content) throw new Error('内容已被编辑，请重新确认最新草稿');
    if (!validatePlainText(value, '内容')) throw new Error('内容未通过表单校验');
    if (kind === 'reply' && replyTarget.value?.id !== payload.parentId) throw new Error('回复目标已改变');
    if (kind === 'suggestion' && suggestionForm.type !== payload.type) throw new Error('建议类型已改变');
  };
  unregisterAssistant = registerCommunityAdapter({
    element: root,
    isReady: () => !activeLoading.value,
    readDraft: kind => kind === 'suggestion' ? { ...suggestionForm } : kind === 'reply' ? { content: replyContent.value, parentId: replyTarget.value?.id } : { content: commentForm.content },
    resolve: (payload, key) => {
      if (key === 'type') return Array.from(root.querySelectorAll<HTMLElement>('[data-assistant-type]')).find(element => element.dataset.assistantType === payload.type) ?? null;
      const scope = payload.kind === 'reply' ? recordElement(payload.parentId ?? '') : root;
      const targetKey = key === 'reply' ? 'reply.open' : key === 'submit' ? payload.kind === 'reply' ? 'reply.submit' : 'composer.submit' : `${payload.kind === 'suggestion' ? 'suggestion' : payload.kind === 'reply' ? 'reply' : 'comment'}.content`;
      return scope?.querySelector<HTMLElement>(`[data-assistant-target="${targetKey}"]`) ?? null;
    },
    openReply: async parentId => {
      if (!/^[0-9]+$/.test(parentId)) throw new Error('回复目标不正确');
      let item = comments.value.find(comment => comment.id === parentId);
      if (!item) { item = normalizeCommentChildren(await fetchCommentThread(parentId)); comments.value = [item, ...comments.value]; }
      if (item.parentId) throw new Error('只能回复父评论');
      await startReply(item, true);
    },
    setContent: (kind, content) => { if (kind === 'suggestion') suggestionForm.content = content; else if (kind === 'reply') replyContent.value = content; else commentForm.content = content; },
    selectType: type => {
      if (!suggestionTypes.some(item => item.value === type)) throw new Error('建议类型不正确');
      suggestionForm.type = type;
    },
    validate,
    submit: async (payload, context) => {
      validate(payload);
      const result = payload.kind === 'suggestion' ? await submitSuggestion(context) : payload.kind === 'reply' ? await submitReply(context) : await submitComment(context);
      if (!result?.success) throw new Error('表单未完成提交');
      return result;
    },
    revealResult: async (_kind, id) => { await nextTick(); return recordElement(id); },
  });
}
</script>

<style scoped lang="scss">
.interaction-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.mode-switch {
  display: inline-flex;
  align-self: flex-start;
  padding: 5px;
  border: 1px solid #eef2f7;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 10px 30px rgb(15 23 42 / 5%);
}

.mode-button {
  padding: 10px 22px;
  color: #667085;
  cursor: pointer;
  border: 0;
  border-radius: 999px;
  background: transparent;
  font-weight: 700;
}

.mode-button.active {
  color: #ffffff;
  background: #1f2937;
}

.interaction-board {
  padding: 30px 34px 24px;
  border: 1px solid #edf2f7;
  border-radius: 28px;
  background: #ffffff;
  box-shadow: 0 18px 48px rgb(15 23 42 / 5%);
}

.board-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.board-header h2 {
  margin: 0;
  color: #111827;
  font-size: 28px;
}

.board-header h2 span {
  margin-left: 6px;
  color: #98a2b3;
  font-size: 18px;
  font-weight: 500;
}

.sort-tabs {
  display: flex;
  align-items: center;
  gap: 14px;
  white-space: nowrap;
}

.sort-tabs button {
  padding: 0;
  color: #98a2b3;
  cursor: pointer;
  border: 0;
  background: transparent;
  font-size: 16px;
  font-weight: 700;
}

.sort-tabs button.active {
  color: #111827;
}

.sort-tabs span {
  width: 1px;
  height: 16px;
  background: #d0d5dd;
}

.composer-card {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 28px;
}

.composer-main {
  flex: 1;
  min-width: 0;
}

.suggestion-type-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.type-chip {
  padding: 7px 14px;
  color: #667085;
  cursor: pointer;
  border: 1px solid #e4e7ec;
  border-radius: 999px;
  background: #ffffff;
}

.type-chip.active {
  color: #0f766e;
  border-color: #99f6e4;
  background: #ecfeff;
  font-weight: 700;
}

.composer-input,
.reply-composer textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 76px;
  padding: 18px;
  color: #1f2937;
  resize: vertical;
  border: 0;
  border-radius: 12px;
  outline: none;
  background: #f1f3f5;
  font-family: inherit;
  font-size: 16px;
  line-height: 1.7;
}

.composer-input::placeholder,
.reply-composer textarea::placeholder {
  color: #9aa3af;
  font-weight: 700;
}

.composer-footer,
.reply-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  color: #98a2b3;
  font-size: 13px;
}

.composer-footer {
  justify-content: flex-end;
}

.reply-footer {
  justify-content: space-between;
}

.feed-list {
  display: flex;
  flex-direction: column;
}

.feed-item {
  display: flex;
  gap: 18px;
  padding: 22px 0;
  border-bottom: 1px solid #edf2f7;
}

.feed-item:first-child {
  padding-top: 4px;
}

.item-avatar {
  flex: 0 0 auto;
}

.item-body {
  flex: 1;
  min-width: 0;
}

.author-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-height: 24px;
}

.author-line strong {
  color: #667085;
  font-size: 15px;
}

.rank-badge,
.type-badge {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 800;
}

.rank-badge {
  color: #ff6a3d;
  border: 1px solid #ffb199;
  background: #fff7ed;
}

.type-badge {
  color: #2563eb;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
}

.feed-content {
  margin: 8px 0 0;
  color: #111827;
  font-size: 17px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.feed-actions {
  display: flex;
  align-items: center;
  gap: 22px;
  margin-top: 12px;
  color: #98a2b3;
  font-size: 14px;
}

.action-button {
  padding: 0;
  color: #98a2b3;
  cursor: pointer;
  border: 0;
  background: transparent;
  font: inherit;
}

.action-button.active,
.action-button:hover {
  color: #409eff;
}

.action-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.reply-composer {
  margin-top: 14px;
  padding: 14px;
  border-radius: 16px;
  background: #f8fafc;
}

.reply-composer textarea {
  min-height: 64px;
  background: #ffffff;
}

.child-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #fafbfc;
}

.child-item {
  display: flex;
  gap: 12px;
}

.child-body {
  flex: 1;
  min-width: 0;
}

.child-author strong {
  font-size: 14px;
}

.child-content {
  margin-top: 4px;
  font-size: 15px;
}

.child-actions {
  margin-top: 8px;
  font-size: 13px;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding-top: 20px;
}

@media (max-width: 768px) {
  .interaction-page {
    // 互动区在手机端减少分区间距，让发布框更快进入视野。
    gap: 14px;
  }

  .mode-switch {
    // 页签改为等分网格，两个入口都保留稳定触控宽度。
    align-self: stretch;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mode-button {
    min-height: 44px;
    padding: 10px 14px;
  }

  .board-header,
  .composer-footer,
  .reply-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .interaction-board {
    padding: 22px 18px;
  }

  .board-header {
    // 标题和排序纵向靠近，避免右侧排序挤压标题。
    gap: 12px;
    margin-bottom: 18px;
  }

  .board-header h2 {
    font-size: 24px;
  }

  .sort-tabs {
    align-self: stretch;
    justify-content: flex-start;
  }

  .composer-footer {
    align-items: stretch;
  }

  .composer-footer .el-button {
    width: 100%;
  }

  .composer-card,
  .feed-item {
    gap: 12px;
  }

  .suggestion-type-row {
    // 建议类型在手机端按两列排列，减少横向滚动风险。
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .type-chip {
    min-height: 44px;
  }

  .feed-actions {
    // 点赞、回复和时间允许换行，保证长时间文案不撑宽列表。
    flex-wrap: wrap;
    gap: 10px 16px;
  }

  .reply-footer > div {
    display: grid;
    width: 100%;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .child-list {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .interaction-board {
    padding: 18px 14px;
    border-radius: 22px;
  }

  .composer-card .el-avatar,
  .feed-item > .el-avatar {
    width: 40px !important;
    height: 40px !important;
    flex: 0 0 40px;
  }

  .suggestion-type-row {
    grid-template-columns: 1fr;
  }

  .feed-content {
    font-size: 16px;
  }

  .pagination-row :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
