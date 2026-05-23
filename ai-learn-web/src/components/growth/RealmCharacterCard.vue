<template>
  <section class="realm-card" :class="{ compact }">
    <div class="realm-figure-wrap">
      <img class="realm-figure" :src="figureSrc" :alt="figureAlt" loading="lazy" decoding="async" />
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
  // 角色图已处理为透明柔边 PNG，此处只负责稳定展示尺寸。
  display: flex;
  align-items: flex-end;
  justify-content: center;
  min-height: 288px;
  padding: 4px 0 36px;
}

.realm-card.compact .realm-figure-wrap {
  min-height: 260px;
}

.realm-figure {
  display: block;
  width: auto;
  max-width: 90%;
  height: 218px;
  object-fit: contain;
  filter: drop-shadow(0 18px 20px rgba(70, 91, 128, 0.14));
  // 放大后向下微调，避免卡片顶部裁掉角色头饰和发髻。
  transform: translateY(36px) scale(1.5);
  transform-origin: center bottom;
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
