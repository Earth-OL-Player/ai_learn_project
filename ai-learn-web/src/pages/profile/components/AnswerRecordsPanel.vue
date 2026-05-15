<template>
  <el-card shadow="never" class="record-card">
    <template #header>
      <div class="card-header">
        <span>我的答题记录</span>
        <el-button text type="primary" :loading="loading" @click="loadRecords">刷新</el-button>
      </div>
    </template>

    <el-skeleton :loading="loading" animated :rows="5">
      <el-empty v-if="records.length === 0" description="暂无答题记录，去 AI智能刷题 完成第一题吧" />
      <div v-else class="record-list">
        <article v-for="item in records" :key="item.id" class="record-item">
          <div class="record-title-row">
            <h3>{{ item.questionTitle }}</h3>
            <el-tag :type="item.isCorrect ? 'success' : 'danger'" effect="light">{{ item.score }} 分</el-tag>
          </div>
          <div class="tag-row">
            <el-tag effect="plain">{{ item.questionTypeText }}</el-tag>
            <el-tag effect="light">{{ item.difficultyText }}</el-tag>
            <el-tag v-if="item.firstAttempt" type="success" effect="plain">首次作答</el-tag>
          </div>
          <p>{{ item.improvementAdvice || '暂无改进建议' }}</p>
          <span class="time-line">{{ formatTime(item.createdAt) }} · 用时 {{ item.durationSeconds ?? 0 }} 秒</span>
        </article>
      </div>
    </el-skeleton>

    <el-pagination
      v-model:current-page="page.pageNo"
      layout="prev, pager, next, total"
      :total="page.total"
      :page-size="page.pageSize"
      @current-change="loadRecords"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { fetchMyAnswerRecords } from '../../../api/practice';
import type { AnswerRecordItem } from '../../../types/answer-record';

const PAGE_SIZE = 5;
const loading = ref(false);
const records = ref<AnswerRecordItem[]>([]);
const page = reactive({ pageNo: 1, pageSize: PAGE_SIZE, total: 0 });

/**
 * 加载答题记录。
 */
async function loadRecords(): Promise<void> {
  loading.value = true;
  try {
    const result = await fetchMyAnswerRecords(page.pageNo, page.pageSize);
    records.value = result.records;
    page.total = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '答题记录加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 格式化本地展示时间。
 */
function formatTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

onMounted(loadRecords);
</script>

<style scoped lang="scss">
.record-card {
  border: 1px solid #edf2f7;
  border-radius: 18px;
}

.card-header,
.record-title-row,
.tag-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.record-item {
  padding: 16px;
  border: 1px solid #edf2f7;
  border-radius: 16px;
  background: #fbfdff;
}

.record-item h3,
.record-item p {
  margin: 0;
}

.record-item p {
  margin-top: 10px;
  color: #475467;
  line-height: 1.7;
}

.tag-row {
  justify-content: flex-start;
  flex-wrap: wrap;
  margin-top: 10px;
}

.time-line {
  display: inline-block;
  margin-top: 10px;
  color: #667085;
  font-size: 13px;
}
</style>
