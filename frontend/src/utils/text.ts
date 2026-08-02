/**
 * 여러 문장이 이어진 안내문을 문장 단위로 나눈다.
 * 한 문단으로 흘리면 앞 문장의 끝부분이 다음 줄로 넘어가 읽기 흐름이 끊긴다.
 */
export const splitSentences = (text: string): string[] =>
  text
    .split(/(?<=[.!?])\s+/)
    .map((sentence) => sentence.trim())
    .filter(Boolean);
