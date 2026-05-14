import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '../layout/AppLayout.vue';
import LearningRoadmapPage from '../pages/learning-roadmap/LearningRoadmapPage.vue';
import PlaceholderPage from '../pages/placeholder/PlaceholderPage.vue';
import ProfilePage from '../pages/profile/ProfilePage.vue';
import { useAuthStore } from '../stores/auth';

// 占位页面说明集中管理，避免页面内重复硬编码。
const placeholderDescriptions = {
  suggestions: '建议评论区能力将在 sprint202603 开放，后续将支持用户反馈、建议收集和评论互动。',
  questions: '热门面经能力将在 sprint202604 开放，后续将提供 AI 方向面试题分类浏览。',
  agent: 'AI智能刷题能力将在 sprint202605 开放，后续将支持智能出题、答题和讲解。',
};

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
          component: PlaceholderPage,
          props: {
            title: '建议评论区',
            description: placeholderDescriptions.suggestions,
          },
          meta: { title: '建议评论区' },
        },
        {
          path: 'interview-questions',
          name: 'interview-questions',
          component: PlaceholderPage,
          props: {
            title: '热门面经',
            description: placeholderDescriptions.questions,
          },
          meta: { title: '热门面经', requiresAuth: true },
        },
        {
          path: 'practice-agent',
          name: 'practice-agent',
          component: PlaceholderPage,
          props: {
            title: 'AI智能刷题',
            description: placeholderDescriptions.agent,
          },
          meta: { title: 'AI智能刷题', requiresAuth: true },
        },
        {
          path: 'profile',
          name: 'profile',
          component: ProfilePage,
          meta: { title: '个人中心', requiresAuth: true },
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
  return true;
});

export default router;
