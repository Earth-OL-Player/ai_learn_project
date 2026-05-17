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
  recentEvents: GrowthEventInfo[];
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

export interface GrowthEventInfo {
  id: string;
  eventType: string;
  title: string;
  description: string | null;
  experienceDelta: number;
  createdAt: string;
}
