<template>
  <section class="placeholder-page">
    <el-card shadow="never" class="placeholder-card">
      <div class="placeholder-icon">🚧</div>
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <el-tag type="info" effect="plain">后续迭代开放</el-tag>
    </el-card>
  </section>
</template>

<script setup lang="ts">
interface PlaceholderProps {
  title: string;
  description: string;
}

// 占位页只展示静态说明，不调用未实现接口。
defineProps<PlaceholderProps>();
</script>
