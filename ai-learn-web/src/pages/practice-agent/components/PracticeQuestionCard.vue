<template>
  <section class="question-bubble-card" :aria-label="`当前题目：${question.question}`">
    <div class="question-meta-row">
      <el-tag effect="plain">{{ question.questionType }}</el-tag>
      <el-tag type="warning" effect="light">重要性 {{ question.importanceScore }}</el-tag>
      <el-tag type="success" effect="light">真实面试 {{ question.occurrenceCount }} 次</el-tag>
      <el-tag type="info" effect="plain">您的历史最高分 {{ question.bestScore }}</el-tag>
    </div>
    <h3>{{ question.question }}</h3>
  </section>
</template>

<script setup lang="ts">
import { ElTag } from 'element-plus/es/components/tag/index.mjs';
import 'element-plus/es/components/tag/style/css';
import type { PracticeQuestion } from '../../../api/practice';

interface PracticeQuestionCardProps {
  question: PracticeQuestion;
}

defineProps<PracticeQuestionCardProps>();
</script>
