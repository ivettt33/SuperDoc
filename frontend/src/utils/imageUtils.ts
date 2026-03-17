import { FileApi } from "../api";

/**
 * Converts a file URL/path to a full accessible URL
 * Handles both absolute URLs and relative file paths
 */
export function getImageUrl(url: string | undefined | null): string | null {
  if (!url) return null;
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  if (url.includes('/')) {
    const fileName = url.split('/').pop() || url;
    return FileApi.getFileUrl(fileName);
  }
  return FileApi.getFileUrl(url);
}

