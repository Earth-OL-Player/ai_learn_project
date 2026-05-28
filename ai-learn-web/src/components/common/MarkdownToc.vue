<template>
  <nav
    v-if="items.length"
    :class="['markdown-toc-card', { 'is-collapsed': collapsed }]"
    aria-label="目录"
  >
    <div class="markdown-toc-header">
      <h3 v-if="!collapsed">目录</h3>
      <button
        type="button"
        class="markdown-toc-toggle"
        :aria-expanded="!collapsed"
        :aria-controls="listId"
        :aria-label="collapsed ? '展开目录' : '收起目录'"
        @click="emit('toggle')"
      >
        {{ collapsed ? '展开' : '收起' }}
      </button>
    </div>

    <div :id="listId" v-show="!collapsed" class="markdown-toc-list">
      <a
        v-for="item in items"
        :key="item.id"
        :href="`#${item.id}`"
        :class="[
          'markdown-toc-link',
          `markdown-toc-level-${item.level}`,
          { 'is-active': activeId === item.id },
        ]"
        :aria-current="activeId === item.id ? 'location' : undefined"
        @click="emit('select', item.id)"
      >
        {{ item.title }}
      </a>
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { MarkdownTocItem } from '../../utils/markdownToc';

defineProps<{
  items: MarkdownTocItem[];
  activeId: string;
  collapsed: boolean;
  listId: string;
}>();

const emit = defineEmits<{
  select: [tocId: string];
  toggle: [];
}>();
</script>
