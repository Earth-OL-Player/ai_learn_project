<template>
  <div class="question-bubble-card">
    <div class="question-meta-row">
      <el-tag effect="plain">{{ question.questionType }}</el-tag>
      <el-tag type="warning" effect="light">重要性 {{ question.importanceScore }}</el-tag>
      <el-tag type="success" effect="light">真实面试 {{ question.occurrenceCount }} 次</el-tag>
      <el-tag type="info" effect="plain">您的历史最高分 {{ question.bestScore }}</el-tag>
    </div>
    <h3>{{ question.question }}</h3>
  </div>
</template>

<script setup lang="ts">
import type { PracticeQuestion } from '../../../api/practice';

interface PracticeQuestionCardProps {
  question: PracticeQuestion;
}

defineProps<PracticeQuestionCardProps>();
</script>
