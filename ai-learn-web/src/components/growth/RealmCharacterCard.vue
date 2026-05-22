<template>
  <section class="realm-card" :class="{ compact }">
    <div class="realm-figure-wrap">
      <svg
        v-if="isMaleFigure"
        class="realm-figure male-realm-figure"
        viewBox="0 0 220 360"
        role="img"
        :aria-label="figureAlt"
      >
        <defs>
          <linearGradient :id="`${figureIdPrefix}-robe`" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" :stop-color="maleRealmStyle.robeStart" />
            <stop offset="100%" :stop-color="maleRealmStyle.robeEnd" />
          </linearGradient>
          <linearGradient :id="`${figureIdPrefix}-accent`" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" :stop-color="maleRealmStyle.accent" />
            <stop offset="100%" :stop-color="maleRealmStyle.ornament" />
          </linearGradient>
        </defs>

        <ellipse cx="110" cy="334" rx="55" ry="10" fill="rgba(70,91,128,0.14)" />
        <path class="male-aura" :d="maleAuraPath" fill="none" :stroke="maleRealmStyle.aura" stroke-width="4" stroke-linecap="round" />
        <circle cx="162" cy="74" r="4" :fill="maleRealmStyle.ornament" opacity="0.72" />
        <circle cx="52" cy="126" r="3.2" :fill="maleRealmStyle.aura" opacity="0.58" />

        <g class="male-character-body">
          <path d="M72 150 C58 178 48 224 44 302 L176 302 C172 225 162 178 148 150 Z" :fill="`url(#${figureIdPrefix}-robe)`" />
          <path d="M96 154 L110 300 L124 154 C116 161 104 161 96 154 Z" fill="#fffaf2" opacity="0.94" />
          <path d="M73 160 C54 188 42 229 28 280 C38 287 49 286 58 276 C63 232 75 194 91 166 Z" :fill="maleRealmStyle.robeEnd" opacity="0.9" />
          <path d="M147 160 C166 188 178 229 192 280 C182 287 171 286 162 276 C157 232 145 194 129 166 Z" :fill="maleRealmStyle.robeStart" opacity="0.9" />
          <path d="M74 218 C96 229 124 229 146 218 L149 236 C124 248 96 248 71 236 Z" :fill="`url(#${figureIdPrefix}-accent)`" />
          <path d="M98 219 L110 239 L122 219" fill="none" stroke="#ffffff" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" opacity="0.82" />
          <path d="M84 302 L76 330 L98 330 L106 302 Z" fill="#313747" />
          <path d="M136 302 L144 330 L122 330 L114 302 Z" fill="#313747" />
        </g>

        <g class="male-character-head">
          <path d="M70 92 C69 58 88 36 112 36 C141 36 158 60 153 94 C146 77 127 68 105 70 C89 71 77 79 70 92 Z" :fill="maleRealmStyle.hair" />
          <path d="M74 86 C72 61 88 44 111 44 C136 44 151 62 148 89 C134 77 92 75 74 86 Z" fill="#111827" opacity="0.24" />
          <circle cx="110" cy="108" r="49" fill="#ffe4cf" />
          <path d="M64 112 C63 99 68 90 77 85 C78 103 83 113 92 119 C82 121 72 118 64 112 Z" :fill="maleRealmStyle.hair" />
          <path d="M156 112 C157 99 152 90 143 85 C142 103 137 113 128 119 C138 121 148 118 156 112 Z" :fill="maleRealmStyle.hair" />
          <path d="M86 105 C93 100 100 100 106 105" fill="none" stroke="#2f3340" stroke-width="4" stroke-linecap="round" />
          <path d="M114 105 C121 100 128 100 134 105" fill="none" stroke="#2f3340" stroke-width="4" stroke-linecap="round" />
          <circle cx="96" cy="120" r="6" fill="#1f2937" />
          <circle cx="124" cy="120" r="6" fill="#1f2937" />
          <path d="M101 137 C107 142 114 142 120 137" fill="none" stroke="#b97966" stroke-width="3" stroke-linecap="round" />
          <path d="M100 42 L110 20 L120 42 Z" :fill="`url(#${figureIdPrefix}-accent)`" />
          <circle cx="110" cy="42" r="8" :fill="maleRealmStyle.ornament" />
        </g>
      </svg>
      <img v-else class="realm-figure" :src="figureSrc" :alt="figureAlt" />
    </div>

    <div class="realm-info">
      <span class="realm-label">{{ rank }}</span>
      <strong>{{ nickname }}</strong>
      <small>{{ displayProgressText }}</small>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, useId } from 'vue';

interface RealmCardProps {
  nickname: string;
  rank: string;
  level: string;
  currentExperience: number;
  nextLevelExperience: number;
  levelProgressText?: string;
  gender?: 'MALE' | 'FEMALE' | null;
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
const DEFAULT_MALE_REALM_STYLE = {
  robeStart: '#f7fafc',
  robeEnd: '#d8e3ef',
  accent: '#6b7280',
  ornament: '#9ca3af',
  aura: '#9bd8b8',
  hair: '#252a36',
};
const MALE_REALM_STYLE_MAP: Record<string, typeof DEFAULT_MALE_REALM_STYLE> = {
  炼气期: { robeStart: '#f8fafc', robeEnd: '#e5edf6', accent: '#8a6a45', ornament: '#d6b36a', aura: '#9bd8b8', hair: '#252a36' },
  筑基期: { robeStart: '#d8f1df', robeEnd: '#8fc7a2', accent: '#326f58', ornament: '#d7c27b', aura: '#9bd8b8', hair: '#252a36' },
  金丹期: { robeStart: '#f2e6ca', robeEnd: '#caa45c', accent: '#7a5424', ornament: '#f0c15e', aura: '#e9c46a', hair: '#252a36' },
  元婴期: { robeStart: '#e7efff', robeEnd: '#9fb8ee', accent: '#4f6fb7', ornament: '#c5d5ff', aura: '#88a9ff', hair: '#202737' },
  化神期: { robeStart: '#e8f4ff', robeEnd: '#80c7dd', accent: '#257e98', ornament: '#b9edf4', aura: '#66c7d6', hair: '#1f2937' },
  炼虚期: { robeStart: '#eef2ff', robeEnd: '#a5b4fc', accent: '#4f46e5', ornament: '#c4b5fd', aura: '#a78bfa', hair: '#202033' },
  合体期: { robeStart: '#ecfdf5', robeEnd: '#74c69d', accent: '#2d6a4f', ornament: '#b7e4c7', aura: '#52b788', hair: '#1f2937' },
  大乘期: { robeStart: '#fff7ed', robeEnd: '#f3a35c', accent: '#9a5a24', ornament: '#fed7aa', aura: '#fb923c', hair: '#251c1a' },
  渡劫期: { robeStart: '#f4f4f5', robeEnd: '#a1a1aa', accent: '#3f3f46', ornament: '#e4e4e7', aura: '#71717a', hair: '#18181b' },
  真仙境: { robeStart: '#f0f9ff', robeEnd: '#7dd3fc', accent: '#0369a1', ornament: '#bae6fd', aura: '#38bdf8', hair: '#172554' },
  金仙境: { robeStart: '#fffbeb', robeEnd: '#f6c453', accent: '#92400e', ornament: '#fde68a', aura: '#fbbf24', hair: '#292524' },
  太乙境: { robeStart: '#f5f3ff', robeEnd: '#a78bfa', accent: '#6d28d9', ornament: '#ddd6fe', aura: '#8b5cf6', hair: '#1e1b4b' },
  大罗境: { robeStart: '#fdf2f8', robeEnd: '#f0abfc', accent: '#a21caf', ornament: '#f5d0fe', aura: '#d946ef', hair: '#2e1065' },
  道祖境: { robeStart: '#fff7ed', robeEnd: '#f5d279', accent: '#7c2d12', ornament: '#facc15', aura: '#f59e0b', hair: '#1c1917' },
};
const props = withDefaults(defineProps<RealmCardProps>(), {
  levelProgressText: '',
  gender: null,
  compact: false,
});
const figureInstanceId = useId();

// 使用独立透明全身像，避免精灵图裁剪造成半身展示和背景色差。
const figureSrc = computed(() => {
  const imageName = REALM_IMAGE_MAP[props.rank] || DEFAULT_REALM_IMAGE;
  return `/rank-characters/realms/${imageName}.png`;
});
const isMaleFigure = computed(() => props.gender === 'MALE');
const figureAlt = computed(() => `${props.rank}${isMaleFigure.value ? '男性' : '女性'}修仙角色全身像`);
const maleRealmStyle = computed(() => MALE_REALM_STYLE_MAP[props.rank] || DEFAULT_MALE_REALM_STYLE);
const figureIdPrefix = computed(() => {
  const imageName = REALM_IMAGE_MAP[props.rank] || DEFAULT_REALM_IMAGE;
  return `male-realm-${figureInstanceId}-${imageName}`;
});

// 高阶境界使用更完整的环形灵气，低阶境界保持轻量，避免画面过度拥挤。
const maleAuraPath = computed(() => {
  const highRankRealms = ['真仙境', '金仙境', '太乙境', '大罗境', '道祖境'];
  return highRankRealms.includes(props.rank)
    ? 'M42 118 C36 58 78 18 126 30 C178 43 190 112 158 156'
    : 'M45 178 C32 128 58 82 98 66 C138 50 174 76 178 120';
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

.male-realm-figure {
  // 男性套图使用同一视窗和高度，保证与当前女性 PNG 形象切换时不跳动。
  width: min(90%, 170px);
}

.realm-card.compact .realm-figure {
  height: 200px;
}

.male-aura {
  opacity: 0.68;
}

.male-character-body,
.male-character-head {
  transform-origin: center;
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
