<template>
  <div class="practice-category-picker">
    <label id="practice-category-label" for="practice-category-select">请选择题目分类</label>
    <el-select
      id="practice-category-select"
      v-model="selectedValues"
      multiple
      collapse-tags
      collapse-tags-tooltip
      filterable
      popper-class="practice-category-select-popper"
      placeholder="全部分类"
      aria-labelledby="practice-category-label"
      @visible-change="emit('visibleChange', $event)"
      @change="emit('change')"
    >
      <el-option v-for="item in categories" :key="item" :label="item" :value="item">
        <div class="practice-category-option-content" :class="{ 'is-checked': selectedValues.includes(item) }">
          <span class="practice-category-checkbox" aria-hidden="true"></span>
          <span class="practice-category-option-label">{{ item }}</span>
        </div>
      </el-option>
    </el-select>
  </div>
</template>

<script setup lang="ts">
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs';
import 'element-plus/es/components/select/style/css';
import { computed } from 'vue';

interface PracticeCategorySelectorProps {
  categories: string[];
  modelValue: string[];
}

const props = defineProps<PracticeCategorySelectorProps>();

const emit = defineEmits<{
  change: [];
  'update:modelValue': [value: string[]];
  visibleChange: [visible: boolean];
}>();

// 用计算属性桥接 v-model，避免组件内部直接修改父级数组引用。
const selectedValues = computed({
  get: () => props.modelValue,
  set: (value: string[]) => emit('update:modelValue', value),
});
</script>
