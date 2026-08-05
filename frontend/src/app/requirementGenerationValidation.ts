const allowedExtensions = new Set(['pdf', 'docx', 'doc', 'csv']);
const maximumFileSize = 25 * 1024 * 1024;

export function validateRequirementDocument(file: File): string | null {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
  if (!allowedExtensions.has(extension)) {
    return 'Choose a PDF, DOCX, DOC, or CSV document.';
  }
  if (file.size === 0) {
    return 'The selected document is empty.';
  }
  if (file.size > maximumFileSize) {
    return 'The selected document is larger than the 25 MB upload limit.';
  }
  return null;
}
