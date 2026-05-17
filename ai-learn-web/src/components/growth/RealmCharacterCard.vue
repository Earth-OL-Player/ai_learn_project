<template>
  <section class="realm-card" :class="{ compact }">
    <div class="realm-figure-wrap">
      <img class="realm-figure" :src="figureSrc" :alt="`${rank}修仙角色全身像`" />
    </div>

    <div class="realm-info">
      <span class="realm-label">{{ rank }}</span>
      <strong>{{ nickname }}</strong>
      <small>{{ displayProgressText }}</small>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface RealmCardProps {
  nickname: string;
  rank: string;
  level: string;
  currentExperience: number;
  nextLevelExperience: number;
  levelProgressText?: string;
  compact?: boolean;
}

const REALM_IMAGE_MAP: Record<string, string> = {
  炼气期: 'qi-refining',
  筑基期: 'foundation-building',
  金丹期: 'golden-core',
  元婴期: 'nascent-soul',
  化神期: 'soul-formation',
  炼虚期: 'void-refining',
  合体期: 'body-integration',
  大乘期: 'mahayana',
  渡劫期: 'tribulation',
  真仙境: 'true-immortal',
  金仙境: 'golden-immortal',
  太乙境: 'taiyi',
  大罗境: 'great-luo',
  道祖境: 'dao-ancestor',
};
const DEFAULT_REALM_IMAGE = 'qi-refining';
const props = withDefaults(defineProps<RealmCardProps>(), {
  levelProgressText: '',
  compact: false,
});

// 使用独立透明全身像，避免精灵图裁剪造成半身展示和背景色差。
const figureSrc = computed(() => {
  const imageName = REALM_IMAGE_MAP[props.rank] || DEFAULT_REALM_IMAGE;
  return `/rank-characters/realms/${imageName}.png`;
});

// 优先使用后端返回的总经验展示文案，兼容旧接口时本地兜底拼接。
const displayProgressText = computed(() => {
  if (props.levelProgressText) {
    return props.levelProgressText;
  }
  return `${props.level} ${props.currentExperience}/${props.nextLevelExperience}`;
});
</script>

<style scoped lang="scss">
.realm-card {
  // 角色卡背景保持浅色通透，与人物透明底融合，避免明显色块割裂。
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
  overflow: hidden;
  padding: 16px;
  border: 1px solid rgba(103, 128, 186, 0.14);
  border-radius: 24px;
  background:
    radial-gradient(circle at 22% 16%, rgba(255, 232, 168, 0.26), transparent 30%),
    radial-gradient(circle at 82% 12%, rgba(197, 239, 255, 0.28), transparent 32%),
    linear-gradient(145deg, rgba(252, 254, 255, 0.96) 0%, rgba(246, 255, 250, 0.96) 100%);
  box-shadow: 0 18px 42px rgba(61, 91, 132, 0.1);
}

.realm-card.compact {
  padding: 14px;
  border-radius: 22px;
}

.realm-figure-wrap {
  // 固定展示完整人物，不再使用上下浮动动画。
  display: flex;
  align-items: flex-end;
  justify-content: center;
  min-height: 230px;
  padding: 4px 0 0;
}

.realm-card.compact .realm-figure-wrap {
  min-height: 210px;
}

.realm-figure {
  display: block;
  width: auto;
  max-width: 90%;
  height: 218px;
  object-fit: contain;
  filter: drop-shadow(0 18px 20px rgba(70, 91, 128, 0.14));
}

.realm-card.compact .realm-figure {
  height: 200px;
}

.realm-info {
  // 文案区域与角色图分离，避免昵称和等级信息压住人物。
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 5px;
  text-align: center;
}

.realm-label {
  padding: 5px 12px;
  border-radius: 999px;
  color: #1f6feb;
  font-size: 12px;
  font-weight: 800;
  background: rgba(47, 125, 246, 0.1);
}

.realm-info strong {
  max-width: 100%;
  overflow: hidden;
  color: #17233d;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.realm-info small {
  color: #667085;
  font-weight: 700;
}
</style>
