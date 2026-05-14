<template>
  <section class="interaction-page">
    <div class="page-hero">
      <div>
        <p class="eyebrow">AI 学习共建社区</p>
        <h2>建议评论区</h2>
        <p class="hero-desc">欢迎提出学习平台建议，也可以在评论区交流 AI 应用开发学习心得。</p>
      </div>
      <el-tag effect="light" type="success">真实数据闭环</el-tag>
    </div>

    <el-tabs v-model="activeTab" class="interaction-tabs">
      <el-tab-pane label="建议区" name="suggestions">
        <div class="tab-grid">
          <el-card shadow="never" class="form-card">
            <template #header>
              <div class="card-header">
                <span>提交建议</span>
                <el-tag type="info" effect="plain">默认待处理</el-tag>
              </div>
            </template>

            <template v-if="authStore.isLoggedIn">
              <el-form ref="suggestionFormRef" :model="suggestionForm" :rules="suggestionRules" label-position="top">
                <el-form-item label="建议标题" prop="title">
                  <el-input v-model="suggestionForm.title" maxlength="80" show-word-limit placeholder="请输入 2-80 位标题" />
                </el-form-item>
                <el-form-item label="建议类型" prop="type">
                  <el-select v-model="suggestionForm.type" class="full-width" placeholder="请选择建议类型">
                    <el-option v-for="item in suggestionTypes" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="建议内容" prop="content">
                  <el-input v-model="suggestionForm.content" type="textarea" :rows="6" maxlength="2000" show-word-limit placeholder="请描述你的建议或问题" />
                </el-form-item>
                <el-button type="primary" round :loading="suggestionSubmitting" @click="submitSuggestion">提交建议</el-button>
              </el-form>
            </template>
            <login-panel v-else @require-login="requireLogin" />
          </el-card>

          <el-card shadow="never" class="list-card">
            <template #header>
              <div class="card-header">
                <span>最新建议</span>
                <el-button text type="primary" :loading="suggestionLoading" @click="loadSuggestions">刷新</el-button>
              </div>
            </template>

            <el-skeleton :loading="suggestionLoading" animated :rows="4">
              <el-empty v-if="suggestions.length === 0" description="暂无建议，期待你的第一条反馈" />
              <div v-else class="suggestion-list">
                <article v-for="item in suggestions" :key="item.id" class="suggestion-card">
                  <div class="item-title-row">
                    <h3>{{ item.title }}</h3>
                    <el-tag type="warning" effect="plain">{{ item.statusText }}</el-tag>
                  </div>
                  <p>{{ item.content }}</p>
                  <div class="item-meta">
                    <el-tag effect="plain">{{ item.typeText }}</el-tag>
                    <span>{{ resolveAuthorName(item.author) }}</span>
                    <span>{{ formatTime(item.createdAt) }}</span>
                  </div>
                </article>
              </div>
            </el-skeleton>

            <el-pagination
              v-model:current-page="suggestionPage.pageNo"
              v-model:page-size="suggestionPage.pageSize"
              layout="prev, pager, next, total"
              :total="suggestionPage.total"
              @current-change="loadSuggestions"
            />
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="评论区" name="comments">
        <div class="tab-grid">
          <el-card shadow="never" class="form-card">
            <template #header>发表评论</template>
            <template v-if="authStore.isLoggedIn">
              <el-form ref="commentFormRef" :model="commentForm" :rules="commentRules" label-position="top">
                <el-form-item label="评论内容" prop="content">
                  <el-input v-model="commentForm.content" type="textarea" :rows="7" maxlength="1000" show-word-limit placeholder="分享你的想法，2-1000 位" />
                </el-form-item>
                <el-button type="primary" round :loading="commentSubmitting" @click="submitComment">发表评论</el-button>
              </el-form>
            </template>
            <login-panel v-else @require-login="requireLogin" />
          </el-card>

          <el-card shadow="never" class="list-card">
            <template #header>
              <div class="card-header">
                <span>最新评论</span>
                <el-button text type="primary" :loading="commentLoading" @click="loadComments">刷新</el-button>
              </div>
            </template>

            <el-skeleton :loading="commentLoading" animated :rows="4">
              <el-empty v-if="comments.length === 0" description="暂无评论，欢迎开始交流" />
              <div v-else class="comment-list">
                <article v-for="item in comments" :key="item.id" class="comment-card">
                  <el-avatar :src="item.author.avatar || undefined">{{ resolveAuthorName(item.author).slice(0, 1) }}</el-avatar>
                  <div class="comment-content">
                    <div class="comment-meta">
                      <strong>{{ resolveAuthorName(item.author) }}</strong>
                      <span>{{ formatTime(item.createdAt) }}</span>
                    </div>
                    <p>{{ item.content }}</p>
                    <span class="like-count">点赞 {{ item.likeCount || 0 }}</span>
                  </div>
                </article>
              </div>
            </el-skeleton>

            <el-pagination
              v-model:current-page="commentPage.pageNo"
              v-model:page-size="commentPage.pageSize"
              layout="prev, pager, next, total"
              :total="commentPage.total"
              @current-change="loadComments"
            />
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { defineComponent, h, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createComment, fetchComments } from '../../api/comments';
import { createSuggestion, fetchSuggestions } from '../../api/suggestions';
import { useAuthStore } from '../../stores/auth';
import type { AuthorSummary, SuggestionItem } from '../../types/suggestion';
import type { CommentItem } from '../../types/comment';

const PAGE_SIZE = 10;
const activeTab = ref('suggestions');
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

// 轻量登录引导卡片，复用布局层弹窗能力。
const LoginPanel = defineComponent({
  emits: ['require-login'],
  setup(_, { emit }) {
    return () => h('div', { class: 'login-panel' }, [
      h('p', '登录后即可使用该功能。'),
      h('button', { class: 'login-panel-button', type: 'button', onClick: () => emit('require-login') }, '去登录'),
    ]);
  },
});

const suggestionTypes = [
  { label: '功能建议', value: 'FEATURE' },
  { label: '体验优化', value: 'EXPERIENCE' },
  { label: '内容建议', value: 'CONTENT' },
  { label: '问题反馈', value: 'BUG' },
  { label: '其他', value: 'OTHER' },
];

const suggestions = ref<SuggestionItem[]>([]);
const comments = ref<CommentItem[]>([]);
const suggestionLoading = ref(false);
const commentLoading = ref(false);
const suggestionSubmitting = ref(false);
const commentSubmitting = ref(false);
const suggestionFormRef = ref<FormInstance>();
const commentFormRef = ref<FormInstance>();

const suggestionPage = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const commentPage = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });
const suggestionForm = reactive({ title: '', type: 'FEATURE', content: '' });
const commentForm = reactive({ content: '' });

const suggestionRules: FormRules = {
  title: [{ required: true, min: 2, max: 80, message: '建议标题长度需在2到80位之间', trigger: 'blur' }],
  type: [{ required: true, message: '请选择建议类型', trigger: 'change' }],
  content: [{ required: true, min: 5, max: 2000, message: '建议内容长度需在5到2000位之间', trigger: 'blur' }],
};
const commentRules: FormRules = {
  content: [{ required: true, min: 2, max: 1000, message: '评论内容长度需在2到1000位之间', trigger: 'blur' }],
};

/**
 * 加载建议分页数据。
 */
async function loadSuggestions(): Promise<void> {
  suggestionLoading.value = true;
  try {
    const result = await fetchSuggestions(suggestionPage.pageNo, suggestionPage.pageSize);
    suggestions.value = result.records;
    suggestionPage.total = result.total;
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
    const result = await fetchComments(commentPage.pageNo, commentPage.pageSize);
    comments.value = result.records;
    commentPage.total = result.total;
  } finally {
    commentLoading.value = false;
  }
}

/**
 * 提交建议并刷新第一页。
 */
async function submitSuggestion(): Promise<void> {
  await suggestionFormRef.value?.validate();
  suggestionSubmitting.value = true;
  try {
    await createSuggestion({ ...suggestionForm });
    ElMessage.success('建议提交成功');
    suggestionForm.title = '';
    suggestionForm.content = '';
    suggestionPage.pageNo = 1;
    await loadSuggestions();
  } finally {
    suggestionSubmitting.value = false;
  }
}

/**
 * 发表评论并刷新第一页。
 */
async function submitComment(): Promise<void> {
  await commentFormRef.value?.validate();
  commentSubmitting.value = true;
  try {
    await createComment({ content: commentForm.content });
    ElMessage.success('评论发布成功');
    commentForm.content = '';
    commentPage.pageNo = 1;
    await loadComments();
  } finally {
    commentSubmitting.value = false;
  }
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
 * 格式化本地展示时间。
 */
function formatTime(value: string | null): string {
  if (!value) {
    return '刚刚';
  }
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
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

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px;
  border-radius: 24px;
  background: linear-gradient(135deg, #f4fbff 0%, #f7fff8 100%);
}

.eyebrow {
  margin: 0 0 8px;
  color: #22a06b;
  font-size: 13px;
  font-weight: 700;
}

.page-hero h2 {
  margin: 0;
  color: #1f2a44;
  font-size: 30px;
}

.hero-desc {
  margin: 10px 0 0;
  color: #667085;
}

.interaction-tabs {
  padding: 20px;
  border-radius: 24px;
  background: #ffffff;
}

.tab-grid {
  display: grid;
  grid-template-columns: minmax(280px, 360px) 1fr;
  gap: 18px;
}

.form-card,
.list-card {
  border: 1px solid #edf2f7;
  border-radius: 18px;
}

.card-header,
.item-title-row,
.comment-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.full-width {
  width: 100%;
}

.suggestion-list,
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 18px;
}

.suggestion-card,
.comment-card {
  padding: 16px;
  border: 1px solid #edf2f7;
  border-radius: 16px;
  background: #fbfdff;
}

.suggestion-card h3,
.suggestion-card p,
.comment-card p {
  margin: 0;
}

.suggestion-card p,
.comment-card p {
  margin-top: 10px;
  color: #475467;
  line-height: 1.7;
  white-space: pre-wrap;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
  color: #98a2b3;
  font-size: 13px;
}

.comment-card {
  display: flex;
  gap: 12px;
}

.comment-content {
  flex: 1;
}

.comment-meta span,
.like-count {
  color: #98a2b3;
  font-size: 13px;
}

.login-panel {
  display: flex;
  min-height: 180px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: #667085;
  border-radius: 16px;
  background: #f8fafc;
}

.login-panel-button {
  padding: 9px 22px;
  color: #ffffff;
  cursor: pointer;
  border: 0;
  border-radius: 999px;
  background: #409eff;
}

@media (max-width: 960px) {
  .tab-grid {
    grid-template-columns: 1fr;
  }
}
</style>
