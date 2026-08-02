import { API_ORIGIN, apiRequest } from './apiClient';

export interface UploadedPhoto {
  id: number;
  originalName: string;
  contentType: string;
  sizeBytes: number;
}

/** 사진을 먼저 올리고 받은 id를 글·기록에 연결한다. */
export const uploadPhoto = async (
  file: File,
  purpose: 'COMMUNITY_PHOTO' | 'ACTIVITY_PHOTO',
): Promise<UploadedPhoto> => {
  const form = new FormData();
  form.append('file', file);
  return apiRequest<UploadedPhoto>(`/api/v1/files?purpose=${purpose}`, {
    method: 'POST',
    body: form,
  });
};

/** 업로드된 사진의 공개 조회 주소. */
export const photoUrl = (fileId: number) => `${API_ORIGIN}/api/v1/files/${fileId}/image`;

/** 상대가 받은 사진을 그대로 저장할 수 있는 내려받기 주소. */
export const photoDownloadUrl = (fileId: number) =>
  `${API_ORIGIN}/api/v1/files/${fileId}/image?download=true`;
