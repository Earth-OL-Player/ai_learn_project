<template>
  <section class="roadmap-page">
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="page-alert"
    />

    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else-if="roadmap">
      <section class="hero-card">
        <div class="hero-content">
          <el-tag type="success" effect="dark">公开首页</el-tag>
          <h1>{{ roadmap.title }}</h1>
          <p class="hero-description">{{ roadmap.description }}</p>
          <p class="hero-intro">{{ roadmap.platformIntro }}</p>
        </div>
        <div class="hero-panel">
          <span>学习路线</span>
          <strong>4 阶段</strong>
          <small>基础 → 进阶 → 工程 → 实战</small>
        </div>
      </section>

      <el-card shadow="never" class="overview-card">
        <template #header>
          <div class="card-header">路线总览</div>
        </template>
        <p>{{ roadmap.overview }}</p>
      </el-card>

      <section class="section-grid">
        <el-card
          v-for="section in roadmap.sections"
          :key="section.title"
          shadow="hover"
          class="stage-card"
        >
          <div class="stage-header">
            <h3>{{ section.title }}</h3>
            <span>{{ section.items.length }} 项</span>
          </div>
          <p>{{ section.summary }}</p>
          <div class="stage-tags">
            <el-tag
              v-for="item in section.items"
              :key="item"
              effect="plain"
              type="primary"
            >
              {{ item }}
            </el-tag>
          </div>
        </el-card>
      </section>

      <section class="resource-layout">
        <el-card shadow="never" class="resource-card">
          <template #header>
            <div class="card-header">资料区</div>
          </template>
          <div class="resource-list">
            <a
              v-for="resource in roadmap.resources"
              :key="resource.title"
              :href="resource.url"
              target="_blank"
              rel="noreferrer"
              class="resource-item"
            >
              <strong>{{ resource.title }}</strong>
              <span>{{ resource.description }}</span>
            </a>
          </div>
        </el-card>

        <el-card shadow="never" class="suggestion-card">
          <template #header>
            <div class="card-header">学习建议</div>
          </template>
          <ol>
            <li v-for="suggestion in roadmap.suggestions" :key="suggestion">
              {{ suggestion }}
            </li>
          </ol>
        </el-card>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getLearningRoadmap } from '../../api/learning';
import type { LearningRoadmap } from '../../types/learning';

const loading = ref(true);
const errorMessage = ref('');
const roadmap = ref<LearningRoadmap | null>(null);

/**
 * 加载学习路线数据。
 */
async function loadRoadmap(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';

  try {
    roadmap.value = await getLearningRoadmap();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '网络异常，请稍后重试';
  } finally {
    loading.value = false;
  }
}

// 页面挂载后加载公开学习路线。
onMounted(() => {
  void loadRoadmap();
});
</script>
