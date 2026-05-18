export interface GrowthInfo {
  earnedExperience: number;
  currentExperience: number;
  level: string;
  levelName: string;
  rank: string;
  levelValue: number;
  currentLevelExperience: number;
  nextLevelExperience: number;
  levelProgressText: string;
  answeredCount: number;
  averageScore: number;
  experienceToNextLevel: number;
  streakDays: number;
  badges: BadgeInfo[];
  newBadges: BadgeInfo[];
}

export interface BadgeInfo {
  id: string;
  name: string;
  description: string;
  icon: string;
  ruleCode: string;
  category: string;
  categoryName: string;
  hidden: boolean;
  acquired: boolean;
  acquiredAt: string | null;
}
