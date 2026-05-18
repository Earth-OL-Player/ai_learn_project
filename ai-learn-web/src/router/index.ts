import { createRouter, createWebHistory } from 'vue-router';
import AdminCenterPage from '../pages/admin/AdminCenterPage.vue';
import SystemQuestionBankManagePage from '../pages/admin/SystemQuestionBankManagePage.vue';
import UserManagePage from '../pages/admin/UserManagePage.vue';
import AppLayout from '../layout/AppLayout.vue';
import InterviewQuestionsPage from '../pages/interview-questions/InterviewQuestionsPage.vue';
import LearningRoadmapPage from '../pages/learning-roadmap/LearningRoadmapPage.vue';
import PracticeAgentPage from '../pages/practice-agent/PracticeAgentPage.vue';
import ProfilePage from '../pages/profile/ProfilePage.vue';
import SuggestionsCommentsPage from '../pages/suggestions-comments/SuggestionsCommentsPage.vue';
import { useAuthStore } from '../stores/auth';

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
          redirect: '/learning-roadmap',
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
          meta: { title: '热门面经' },
        },
        {
          path: 'practice-agent',
          name: 'practice-agent',
          component: PracticeAgentPage,
          meta: { title: 'AI智能刷题' },
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
      path: '/learning-roadmap',
      query: {
        loginGuide: '1',
        redirect: to.fullPath,
      },
    };
  }
  if (to.meta.requiresSuperAdmin && !authStore.isSuperAdmin) {
    return '/learning-roadmap';
  }
  return true;
});

export default router;

