// 픽셀 케어 (Pixel Care) 공통 타입 정의

export interface VolunteerItem {
  id: number;
  title: string;
  category: 'VOLUNTEER' | 'DONATION';
  location: string;
  organizer: string;
  targetAmount?: number;
  currentAmount?: number;
  tags: string[];
  link1365?: string;
}

export interface PixelDiaryItem {
  id: number;
  author: string;
  date: string;
  emotion: string; // e.g., '😊', '🥰', '🔥', '🌱'
  content: string;
  hearts: number;
  imageUrl?: string;
}

export interface UserBadge {
  id: number;
  level: number;
  name: string;
  description: string;
  iconUrl: string;
  acquiredAt?: string;
  isUnlocked: boolean;
}

export interface AiChatMessage {
  id: string;
  sender: 'USER' | 'AI';
  text: string;
  recommendedCard?: VolunteerItem;
  createdAt: string;
}
