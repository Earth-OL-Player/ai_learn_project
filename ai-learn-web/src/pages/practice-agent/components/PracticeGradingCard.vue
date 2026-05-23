<template>
  <section class="grading-bubble-card" :aria-label="`本次评分 ${grading.score} 分`">
    <div class="grading-score-row">
      <strong>{{ grading.score }} 分</strong>
      <el-tag :type="scoreTagType(grading.score)" effect="light">
        {{ scoreLevelText(grading.score) }}
      </el-tag>
      <el-tag v-if="grading.fallbackUsed" type="info" effect="plain">本地兜底评分</el-tag>
      <el-tooltip :content="experienceTooltip(grading)" placement="top" effect="light">
        <div
          class="experience-float"
          :class="grading.earnedExperience > 0 ? 'gain' : 'same'"
          tabindex="0"
          :aria-label="experienceTooltip(grading)"
        >
          <span v-if="grading.earnedExperience > 0">↗ +{{ grading.earnedExperience }} 经验</span>
          <span v-else>经验不变</span>
        </div>
      </el-tooltip>
    </div>
    <p class="grading-advice">
      <span v-if="gradingProblemText(grading)" class="grading-advice-line">
        <strong>当前问题：</strong>{{ gradingProblemText(grading) }}
      </span>
      <span class="grading-advice-line">
        <strong>优化建议：</strong>{{ gradingAdviceText(grading) }}
      </span>
    </p>
    <el-collapse>
      <el-collapse-item title="查看评分详情" name="detail">
        <section class="grading-detail-grid">
          <div>
            <h4>命中点</h4>
            <ul class="grading-point-list">
              <li v-for="(point, index) in normalizeList(grading.hitPoints)" :key="`hit-${index}-${point}`">{{ point }}</li>
            </ul>
          </div>
          <div>
            <h4>缺失点</h4>
            <ul class="grading-point-list">
              <li v-for="(point, index) in normalizeList(grading.missingPoints)" :key="`missing-${index}-${point}`">{{ point }}</li>
            </ul>
          </div>
          <div>
            <h4>参考答案</h4>
            <p>{{ grading.referenceAnswer }}</p>
          </div>
        </section>
      </el-collapse-item>
    </el-collapse>
  </section>
</template>

<script setup lang="ts">
import { ElCollapse, ElCollapseItem } from 'element-plus/es/components/collapse/index.mjs';
import { ElTag } from 'element-plus/es/components/tag/index.mjs';
import { ElTooltip } from 'element-plus/es/components/tooltip/index.mjs';
import 'element-plus/es/components/collapse/style/css';
import 'element-plus/es/components/tag/style/css';
import 'element-plus/es/components/tooltip/style/css';
import type { PracticeGrading } from '../../../api/practice';
import {
  experienceTooltip,
  gradingAdviceText,
  gradingProblemText,
  normalizeList,
  scoreLevelText,
  scoreTagType,
} from '../practiceMessageFormat';

interface PracticeGradingCardProps {
  grading: PracticeGrading;
}

defineProps<PracticeGradingCardProps>();
</script>
