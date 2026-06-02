import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '../layout/AppLayout.vue';
import { useAuthStore } from '../stores/auth';
import { updatePageSeo } from '../utils/seo';

// 页面组件按路由懒加载，避免所有业务页面打进首屏入口包。
const HomePage = () => import('../pages/home/HomePage.vue');
const LearningRoadmapPage = () => import('../pages/learning-roadmap/LearningRoadmapPage.vue');
const SuggestionsCommentsPage = () => import('../pages/suggestions-comments/SuggestionsCommentsPage.vue');
const InterviewQuestionsPage = () => import('../pages/interview-questions/InterviewQuestionsPage.vue');
const PracticeAgentPage = () => import('../pages/practice-agent/PracticeAgentPage.vue');
const ProfilePage = () => import('../pages/profile/ProfilePage.vue');

// 管理后台访问频率较低，单独懒加载可减少普通用户首屏体积。
const AdminCenterPage = () => import('../pages/admin/AdminCenterPage.vue');
const UserManagePage = () => import('../pages/admin/UserManagePage.vue');
const SystemQuestionBankManagePage = () => import('../pages/admin/SystemQuestionBankManagePage.vue');
const RedemptionCodeManagePage = () => import('../pages/admin/RedemptionCodeManagePage.vue');
const ModelConfigManagePage = () => import('../pages/admin/ModelConfigManagePage.vue');
const LogLevelManagePage = () => import('../pages/admin/LogLevelManagePage.vue');

/**
 * 创建前端路由实例。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          redirect: '/home',
        },
        {
          path: 'home',
          name: 'home',
          component: HomePage,
          meta: {
            title: 'AI StudyHub - AI Agent 应用开发学习平台',
            description: 'AI StudyHub 是面向普通开发者的 AI Agent 与 AI 应用开发学习平台，持续整理 Agent学习资料、学习路线、热门面试题、AI 智能刷题和建议社区。',
            keywords: 'Agent学习资料,AI Agent学习资料,AI学习平台,AI Agent,AI应用开发,大模型应用开发,RAG,智能刷题,AI面试题,学习路线',
            canonicalPath: '/home',
            structuredDataType: 'WebPage',
          },
        },
        {
          path: 'learning-roadmap',
          name: 'learning-roadmap',
          component: LearningRoadmapPage,
          meta: {
            title: 'Agent学习资料与 AI 应用开发学习路线',
            description: '系统整理 Agent学习资料、AI 应用开发、AI Agent、RAG、LangChain、LangGraph、向量数据库和大模型工程化学习路线，帮助开发者少走弯路。',
            keywords: 'Agent学习资料,AI Agent学习资料,AI应用开发学习路线,AI Agent学习路线,RAG学习资料,LangChain教程,LangGraph教程,向量数据库',
            canonicalPath: '/learning-roadmap',
            structuredDataType: 'LearningResource',
          },
        },
        {
          path: 'suggestions-comments',
          name: 'suggestions-comments',
          component: SuggestionsCommentsPage,
          meta: {
            title: '建议评论区',
            description: '在 AI StudyHub 建议评论区提交学习资料建议、题库反馈、功能建议和使用体验，共建 AI 应用开发学习社区。',
            keywords: 'AI学习社区,AI学习建议,AI学习评论区,AI StudyHub反馈,AI应用开发社区',
            canonicalPath: '/suggestions-comments',
            structuredDataType: 'DiscussionForumPosting',
          },
        },
        {
          path: 'interview-questions',
          name: 'interview-questions',
          component: InterviewQuestionsPage,
          meta: {
            title: 'AI Agent 热门面试题',
            description: '按知识点分类查看 AI Agent、RAG、大模型应用开发、向量检索和工程化方向热门面试题，配套参考答案和重要性信息。',
            keywords: 'AI面试题,AI Agent面试题,RAG面试题,大模型面试题,AI应用开发面试题,向量检索面试题',
            canonicalPath: '/interview-questions',
            structuredDataType: 'LearningResource',
          },
        },
        {
          path: 'practice-agent',
          name: 'practice-agent',
          component: PracticeAgentPage,
          meta: {
            title: 'AI 智能刷题',
            description: '使用 AI 智能刷题练习 AI Agent 和大模型应用开发面试题，支持智能出题、AI 评分、追问讨论、成长经验和刷题记录。',
            keywords: 'AI智能刷题,AI刷题网站,AI面试刷题,大模型刷题,AI Agent刷题,AI评分',
            canonicalPath: '/practice-agent',
            structuredDataType: 'LearningResource',
            keepAlive: true,
          },
        },
        {
          path: 'profile',
          name: 'profile',
          component: ProfilePage,
          meta: { title: '个人中心', requiresAuth: true, noIndex: true },
        },
        {
          path: 'admin',
          component: AdminCenterPage,
          meta: { title: '管理者中心', requiresAuth: true, requiresSuperAdmin: true, noIndex: true },
          children: [
            {
              path: '',
              redirect: '/admin/users',
            },
            {
              path: 'users',
              name: 'admin-users',
              component: UserManagePage,
              meta: { title: '用户管理', requiresAuth: true, requiresSuperAdmin: true, noIndex: true },
            },
            {
              path: 'system-question-bank',
              name: 'admin-system-question-bank',
              component: SystemQuestionBankManagePage,
              meta: { title: '系统题库管理', requiresAuth: true, requiresSuperAdmin: true, noIndex: true },
            },
            {
              path: 'redemption-codes',
              name: 'admin-redemption-codes',
              component: RedemptionCodeManagePage,
              meta: { title: '兑换码管理', requiresAuth: true, requiresSuperAdmin: true, noIndex: true },
            },
            {
              path: 'model-configs',
              name: 'admin-model-configs',
              component: ModelConfigManagePage,
              meta: { title: '模型配置', requiresAuth: true, requiresSuperAdmin: true, noIndex: true },
            },
            {
              path: 'log-levels',
              name: 'admin-log-levels',
              component: LogLevelManagePage,
              meta: { title: '日志管理', requiresAuth: true, requiresSuperAdmin: true, noIndex: true },
            },
          ],
        },
      ],
    },
  ],
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  await authStore.initialize();

  // 受保护页面统一阻止游客进入，并通过查询参数触发布局层登录引导。
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return {
      path: '/home',
      query: {
        loginGuide: '1',
        redirect: to.fullPath,
      },
    };
  }
  if (to.meta.requiresSuperAdmin && !authStore.isSuperAdmin) {
    return '/home';
  }
  return true;
});

router.afterEach((to) => {
  // 路由确认后再刷新 SEO，避免守卫重定向期间写入错误页面信息。
  updatePageSeo(to);
});

export default router;

