<template>
  <section class="system-question-bank-page">
    <div class="admin-page-hero">
      <div>
        <p class="eyebrow">系统题库</p>
        <h2>系统题库管理</h2>
        <p>当前迭代先开放管理入口和空白承载页，具体题目维护能力将在后续迭代补齐。</p>
      </div>
      <el-tag type="info" effect="plain">空白页</el-tag>
    </div>

    <el-card shadow="never" class="admin-empty-card">
      <!-- 空白页不提供未实现操作按钮，避免误导管理员。 -->
      <el-empty description="系统题库管理功能待后续迭代开放" />
    </el-card>
  </section>
</template>
