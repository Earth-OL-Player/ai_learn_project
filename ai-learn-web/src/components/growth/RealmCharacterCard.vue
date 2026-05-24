<template>
  <section class="realm-card" :class="{ compact }">
    <div class="realm-figure-wrap">
      <span class="realm-figure-backdrop" aria-hidden="true"></span>
      <img class="realm-figure" :src="figureSrc" :alt="figureAlt" loading="lazy" decoding="async" />
      <span class="realm-figure-vignette" aria-hidden="true"></span>
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
  gender?: 'MALE' | 'FEMALE' | null;
  compact?: boolean;
}

interface RankCharacterImage {
  levelRange: string;
  pinyin: string;
}

type CharacterAssetGender = 'male' | 'female';

const DEFAULT_RANK_CHARACTER_IMAGE: RankCharacterImage = {
  levelRange: '001-010',
  pinyin: 'lianqi',
};

const CHARACTER_ASSET_CONFIG: Record<CharacterAssetGender, { directory: string; prefix: string; altName: string }> = {
  male: { directory: 'realman', prefix: 'male', altName: '男性' },
  female: { directory: 'realwoman', prefix: 'female', altName: '女性' },
};

const RANK_CHARACTER_IMAGE_MAP: Record<string, RankCharacterImage> = {
  炼气期: { levelRange: '001-010', pinyin: 'lianqi' },
  筑基期: { levelRange: '011-020', pinyin: 'zhuji' },
  金丹期: { levelRange: '021-030', pinyin: 'jindan' },
  元婴期: { levelRange: '031-040', pinyin: 'yuanying' },
  化神期: { levelRange: '041-050', pinyin: 'huashen' },
  炼虚期: { levelRange: '051-060', pinyin: 'lianxu' },
  合体期: { levelRange: '061-070', pinyin: 'heti' },
  大乘期: { levelRange: '071-080', pinyin: 'dacheng' },
  渡劫期: { levelRange: '081-090', pinyin: 'dujie' },
  真仙境: { levelRange: '091-100', pinyin: 'zhenxian' },
  金仙境: { levelRange: '101-110', pinyin: 'jinxian' },
  太乙境: { levelRange: '111-120', pinyin: 'taiyi' },
  大罗境: { levelRange: '121-130', pinyin: 'daluo' },
  道祖境: { levelRange: '131-140', pinyin: 'daozu' },
};

const props = withDefaults(defineProps<RealmCardProps>(), {
  levelProgressText: '',
  gender: null,
  compact: false,
});

// 男性用户使用 realman 套图，女性和未设置性别的新用户统一使用 realwoman 套图。
const characterAssetGender = computed<CharacterAssetGender>(() => (props.gender === 'MALE' ? 'male' : 'female'));

// 根据段位中文名和性别配置组装静态资源路径。
const figureSrc = computed(() => {
  return buildCharacterImagePath(props.rank, characterAssetGender.value);
});

// 图片描述随当前选择的角色套图同步变化，便于无障碍识别。
const figureAlt = computed(() => `${props.rank}${CHARACTER_ASSET_CONFIG[characterAssetGender.value].altName}修仙角色全身像`);

// 优先使用后端返回的总经验展示文案，兼容旧接口时本地兜底拼接。
const displayProgressText = computed(() => {
  if (props.levelProgressText) {
    return props.levelProgressText;
  }
  return `${props.level} ${props.currentExperience}/${props.nextLevelExperience}`;
});

/**
 * 组装段位角色图片路径。
 */
function buildCharacterImagePath(rank: string, gender: CharacterAssetGender): string {
  const assetConfig = CHARACTER_ASSET_CONFIG[gender];
  const imageConfig = RANK_CHARACTER_IMAGE_MAP[rank] || DEFAULT_RANK_CHARACTER_IMAGE;

  // 文件名遵循 male/female_lv等级范围_段位拼音.png 的资源命名规则。
  return `/rank-characters/${assetConfig.directory}/${assetConfig.prefix}_lv${imageConfig.levelRange}_${imageConfig.pinyin}.png`;
}
</script>

<style scoped lang="scss">
.realm-card {
  // 角色卡使用轻量场景底色承接原图暖白背景，降低贴图边界感。
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
  overflow: hidden;
  padding: 16px;
  border: 1px solid rgba(103, 128, 186, 0.14);
  border-radius: 24px;
  background:
    radial-gradient(circle at 18% 12%, rgba(255, 239, 196, 0.38), transparent 32%),
    radial-gradient(circle at 86% 16%, rgba(198, 241, 255, 0.36), transparent 34%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 252, 255, 0.94) 100%);
  box-shadow: 0 18px 42px rgba(61, 91, 132, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.realm-card::before {
  // 细微环境光从卡片内侧铺开，让角色和信息区处在同一张画布上。
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.48) 0%, transparent 38%),
    radial-gradient(ellipse at 50% 40%, rgba(91, 216, 166, 0.12), transparent 58%);
  content: '';
  pointer-events: none;
}

.realm-card.compact {
  padding: 14px;
  border-radius: 22px;
}

.realm-figure-wrap {
  // 场景层负责柔化原图边缘，并为角色提供稳定站位。
  position: relative;
  z-index: 1;
  isolation: isolate;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  min-height: 292px;
  overflow: hidden;
  padding: 10px 0 30px;
  border-radius: 22px;
  background:
    radial-gradient(ellipse at 50% 18%, rgba(255, 255, 255, 0.86), transparent 52%),
    linear-gradient(180deg, rgba(241, 249, 255, 0.72) 0%, rgba(248, 253, 255, 0.34) 100%);
}

.realm-card.compact .realm-figure-wrap {
  min-height: 264px;
  padding-bottom: 28px;
}

.realm-figure-backdrop,
.realm-figure-vignette {
  position: absolute;
  pointer-events: none;
}

.realm-figure-backdrop {
  // 背后光晕贴合人物原画色调，弱化独立图片层的生硬边缘。
  inset: 18px 12px 32px;
  z-index: 0;
  border-radius: 999px;
  background:
    radial-gradient(ellipse at 50% 45%, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0.62) 34%, transparent 70%),
    radial-gradient(ellipse at 50% 82%, rgba(91, 216, 166, 0.2) 0%, rgba(47, 125, 246, 0.1) 38%, transparent 66%);
  filter: blur(2px);
}

.realm-figure-vignette {
  // 前景薄雾覆盖图片四周，让原图暖白背景自然过渡到页面底色。
  inset: 0;
  z-index: 3;
  background:
    linear-gradient(90deg, rgba(249, 252, 255, 0.84) 0%, transparent 18%, transparent 82%, rgba(249, 252, 255, 0.84) 100%),
    linear-gradient(180deg, rgba(249, 252, 255, 0.72) 0%, transparent 18%, transparent 74%, rgba(248, 252, 255, 0.42) 100%),
    radial-gradient(ellipse at 50% 94%, rgba(255, 255, 255, 0.34) 0%, rgba(91, 216, 166, 0.08) 28%, transparent 56%);
  mix-blend-mode: screen;
}

.realm-figure {
  // 图片本体使用绝对定位，避免大尺寸原图反向撑高侧栏布局。
  position: absolute;
  bottom: -30px;
  left: 50%;
  z-index: 2;
  display: block;
  width: auto;
  max-width: 118%;
  height: 352px;
  object-fit: contain;
  filter: saturate(1.02) contrast(1.04) brightness(1.01) drop-shadow(0 18px 20px rgba(70, 91, 128, 0.14));
  opacity: 0.98;
  transform: translateX(-50%);
  transform-origin: center bottom;
  -webkit-mask-image: radial-gradient(ellipse at 50% 54%, #000 0%, #000 76%, rgba(0, 0, 0, 0.9) 88%, rgba(0, 0, 0, 0.52) 97%, transparent 100%);
  mask-image: radial-gradient(ellipse at 50% 54%, #000 0%, #000 76%, rgba(0, 0, 0, 0.9) 88%, rgba(0, 0, 0, 0.52) 97%, transparent 100%);
}

.realm-card.compact .realm-figure {
  bottom: -30px;
  height: 330px;
}

.realm-info {
  // 文案区域与角色图分离，避免昵称和等级信息压住人物。
  position: relative;
  z-index: 1;
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

@media (max-width: 768px) {
  .realm-card,
  .realm-card.compact {
    padding: 12px;
    border-radius: 20px;
  }

  .realm-figure-wrap,
  .realm-card.compact .realm-figure-wrap {
    // 手机端保留足够人物站位，避免全身图被压缩后只露出下半身。
    min-height: clamp(310px, 82vw, 390px);
    padding: 12px 0 18px;
    border-radius: 20px;
  }

  .realm-figure,
  .realm-card.compact .realm-figure {
    // 原图四周留白较多，手机端温和放大并裁掉外圈留白，让人物占比更饱满。
    bottom: -8px;
    max-width: 122%;
    height: calc(100% + 12px);
    transform: translateX(-50%) scale(1.12);
  }

  .realm-info strong {
    font-size: 16px;
  }
}
</style>
