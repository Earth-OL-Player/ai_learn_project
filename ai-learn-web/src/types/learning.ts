export interface RoadmapSection {
  title: string;
  summary: string;
  items: string[];
}

// 学习资料条目用于资料区卡片展示。
export interface ResourceItem {
  title: string;
  description: string;
  url: string;
}

// 学习路线响应数据与后端 DTO 字段保持一致。
export interface LearningRoadmap {
  title: string;
  description: string;
  platformIntro: string;
  overview: string;
  sections: RoadmapSection[];
  resources: ResourceItem[];
  suggestions: string[];
}
