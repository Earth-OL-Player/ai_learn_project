/// <reference types="vite/client" />

import 'vue-router';

declare module '*.md?raw' {
  const content: string;
  export default content;
}

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    description?: string;
    keywords?: string;
    canonicalPath?: string;
    structuredDataType?: 'WebPage' | 'Article' | 'Course' | 'LearningResource' | 'DiscussionForumPosting';
    keepAlive?: boolean;
    requiresAuth?: boolean;
    requiresSuperAdmin?: boolean;
    noIndex?: boolean;
  }
}

export {};
