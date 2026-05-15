export interface GrowthInfo {
  earnedExperience: number;
  currentExperience: number;
  level: string;
  levelName: string;
  rank: string;
  answeredCount: number;
  averageScore: number;
  nextLevelExperience: number;
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
