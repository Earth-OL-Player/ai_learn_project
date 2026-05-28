import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '../layout/AppLayout.vue';
import { useAuthStore } from '../stores/auth';

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
          meta: { title: '首页' },
        },
        {
          path: 'learning-roadmap',
          name: 'learning-roadmap',
          component: LearningRoadmapPage,
          meta: { title: '路线和资料' },
        },
        {
          path: 'suggestions-comments',
          name: 'suggestions-comments',
          component: SuggestionsCommentsPage,
          meta: { title: '建议评论区' },
        },
        {
          path: 'interview-questions',
          name: 'interview-questions',
          component: InterviewQuestionsPage,
          meta: { title: '热门面试题' },
        },
        {
          path: 'practice-agent',
          name: 'practice-agent',
          component: PracticeAgentPage,
          meta: { title: 'AI智能刷题', keepAlive: true },
        },
        {
          path: 'profile',
          name: 'profile',
          component: ProfilePage,
          meta: { title: '个人中心', requiresAuth: true },
        },
        {
          path: 'admin',
          component: AdminCenterPage,
          meta: { title: '管理者中心', requiresAuth: true, requiresSuperAdmin: true },
          children: [
            {
              path: '',
              redirect: '/admin/users',
            },
            {
              path: 'users',
              name: 'admin-users',
              component: UserManagePage,
              meta: { title: '用户管理', requiresAuth: true, requiresSuperAdmin: true },
            },
            {
              path: 'system-question-bank',
              name: 'admin-system-question-bank',
              component: SystemQuestionBankManagePage,
              meta: { title: '系统题库管理', requiresAuth: true, requiresSuperAdmin: true },
            },
            {
              path: 'redemption-codes',
              name: 'admin-redemption-codes',
              component: RedemptionCodeManagePage,
              meta: { title: '兑换码管理', requiresAuth: true, requiresSuperAdmin: true },
            },
            {
              path: 'model-configs',
              name: 'admin-model-configs',
              component: ModelConfigManagePage,
              meta: { title: '模型配置', requiresAuth: true, requiresSuperAdmin: true },
            },
            {
              path: 'log-levels',
              name: 'admin-log-levels',
              component: LogLevelManagePage,
              meta: { title: '日志管理', requiresAuth: true, requiresSuperAdmin: true },
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

export default router;

