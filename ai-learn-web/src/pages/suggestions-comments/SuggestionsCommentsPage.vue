<template>
  <section class="interaction-page">
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
              @click="suggestionForm.type = item.value"
            >
              {{ item.label }}
            </button>
          </div>

          <textarea
            v-if="activeTab === 'suggestions'"
            v-model="suggestionForm.content"
            class="composer-input"
            maxlength="1000"
            placeholder="请输入你的建议"
            @focus="guardComposerFocus"
          ></textarea>
          <textarea
            v-else
            v-model="commentForm.content"
            class="composer-input"
            maxlength="1000"
            placeholder="请输入你的评论"
            @focus="guardComposerFocus"
          ></textarea>

          <div class="composer-footer">
            <el-button type="primary" round :loading="activeSubmitting" @click="submitActiveContent">
              {{ activeSubmitText }}
            </el-button>
          </div>
        </div>
      </div>

      <el-skeleton :loading="activeLoading" animated :rows="6">
        <el-empty v-if="isActiveEmpty" :description="activeEmptyText" />

        <div v-else-if="activeTab === 'suggestions'" class="feed-list">
          <article v-for="item in suggestions" :key="item.id" class="feed-item">
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
          <article v-for="item in comments" :key="item.id" class="feed-item">
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
                <button class="action-button" type="button" @click="startReply(item)">回复</button>
              </div>

              <div v-if="replyTarget?.id === item.id" class="reply-composer">
                <textarea v-model="replyContent" maxlength="1000" :placeholder="`回复 ${resolveAuthorName(item.author)}`"></textarea>
                <div class="reply-footer">
                  <span>回复同样只能使用纯文字。</span>
                  <div>
                    <el-button text @click="cancelReply">取消</el-button>
                    <el-button type="primary" round :loading="replySubmitting" @click="submitReply">发布回复</el-button>
                  </div>
                </div>
              </div>

              <div v-if="item.children.length > 0" class="child-list">
                <article v-for="child in item.children" :key="child.id" class="child-item">
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
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createComment, fetchComments, toggleCommentLike } from '../../api/comments';
import { createSuggestion, fetchSuggestions, toggleSuggestionLike } from '../../api/suggestions';
import { useAuthStore } from '../../stores/auth';
import type { CommentItem } from '../../types/comment';
import type { AuthorSummary, SuggestionItem } from '../../types/suggestion';

type ActiveTab = 'suggestions' | 'comments';
type SortType = 'hot' | 'latest';
type LikeKind = 'suggestion' | 'comment';

const PAGE_SIZE = 10;
const TEXT_MIN_LENGTH = 2;
const TEXT_MAX_LENGTH = 1000;
const UNSUPPORTED_TEXT_PATTERN = /[@＠\p{Extended_Pictographic}\uFE0F\u200D]/u;

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// 页面状态按建议区和评论区拆分，避免两个页签互相污染。
const activeTab = ref<ActiveTab>('suggestions');
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

const currentDisplayName = computed(() => authStore.user?.nickname || authStore.user?.username || '访客');
const currentAvatarSrc = computed(() => authStore.user?.avatar || undefined);
const currentAvatarText = computed(() => currentDisplayName.value.slice(0, 1).toUpperCase());

/**
 * 切换建议区或评论区。
 */
async function switchTab(tab: ActiveTab): Promise<void> {
  activeTab.value = tab;
  cancelReply();
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
  if (activeTab.value === 'suggestions') {
    await submitSuggestion();
    return;
  }
  await submitComment();
}

/**
 * 提交建议。
 */
async function submitSuggestion(): Promise<void> {
  if (!authStore.isLoggedIn) {
    await requireLogin();
    return;
  }
  const content = validatePlainText(suggestionForm.content, '建议内容');
  if (!content) {
    return;
  }

  // 建议不再需要标题和处理状态，只提交类型和正文。
  suggestionSubmitting.value = true;
  try {
    const createdSuggestion = await createSuggestion({ type: suggestionForm.type, content });
    ElMessage.success('建议发布成功');
    suggestionForm.content = '';
    suggestionPage.pageNo = 1;
    prependCreatedSuggestion(createdSuggestion);
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error));
  } finally {
    suggestionSubmitting.value = false;
  }
}

/**
 * 发表父评论。
 */
async function submitComment(): Promise<void> {
  if (!authStore.isLoggedIn) {
    await requireLogin();
    return;
  }
  const content = validatePlainText(commentForm.content, '评论内容');
  if (!content) {
    return;
  }

  // 父评论不携带 parentId，回复入口单独处理。
  commentSubmitting.value = true;
  try {
    const createdComment = await createComment({ content });
    ElMessage.success('评论发布成功');
    commentForm.content = '';
    commentPage.pageNo = 1;
    prependCreatedComment(createdComment);
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error));
  } finally {
    commentSubmitting.value = false;
  }
}

/**
 * 打开父评论回复框。
 */
async function startReply(item: CommentItem): Promise<void> {
  if (!authStore.isLoggedIn) {
    await requireLogin();
    return;
  }

  // 本期仅支持一级回复，因此只在父评论上展示回复入口。
  replyTarget.value = item;
  replyContent.value = '';
}

/**
 * 取消当前回复。
 */
function cancelReply(): void {
  replyTarget.value = null;
  replyContent.value = '';
}

/**
 * 提交一级子评论。
 */
async function submitReply(): Promise<void> {
  if (!replyTarget.value) {
    return;
  }
  const content = validatePlainText(replyContent.value, '回复内容');
  if (!content) {
    return;
  }

  // 回复统一挂在父评论下，不产生孙级评论。
  replySubmitting.value = true;
  try {
    const createdReply = await createComment({ content, parentId: replyTarget.value.id });
    ElMessage.success('回复发布成功');
    appendCreatedReply(replyTarget.value.id, createdReply);
    cancelReply();
  } catch (error) {
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
    ElMessage.warning(`${label}长度需在2到1000位之间`);
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
  return author.nickname || author.username || 'AI 学习者';
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
  return resolveAuthorName(author).slice(0, 1).toUpperCase();
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

/**
 * 格式化本地展示时间。
 */
function formatTime(value: string | null): string {
  if (!value) {
    return '刚刚';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '刚刚';
  }

  // 一天内显示相对时间，历史内容显示完整日期和分钟。
  const diffMs = Date.now() - date.getTime();
  const minuteMs = 60 * 1000;
  const hourMs = 60 * minuteMs;
  if (diffMs >= 0 && diffMs < minuteMs) {
    return '刚刚';
  }
  if (diffMs >= minuteMs && diffMs < hourMs) {
    return `${Math.floor(diffMs / minuteMs)}分钟前`;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

/**
 * 解析接口错误文案。
 */
function resolveErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return '操作失败，请稍后重试';
}

onMounted(async () => {
  await Promise.all([loadSuggestions(), loadComments()]);
});
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
  .board-header,
  .composer-footer,
  .reply-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .interaction-board {
    padding: 22px 18px;
  }

  .composer-footer {
    align-items: flex-end;
  }

  .composer-card,
  .feed-item {
    gap: 12px;
  }
}
</style>
