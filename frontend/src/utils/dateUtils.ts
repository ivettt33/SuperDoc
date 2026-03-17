/**
 * Utility functions for date operations
 */

/**
 * Checks if a date is today
 */
export function isToday(date: Date | string): boolean {
  const checkDate = typeof date === 'string' ? new Date(date) : date;
  const today = new Date();
  return checkDate.toDateString() === today.toDateString();
}

/**
 * Gets the start of today (00:00:00)
 */
export function getStartOfToday(): Date {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return today;
}

/**
 * Gets the end of a week from today (7 days from now, 23:59:59)
 */
export function getEndOfWeek(): Date {
  const endOfWeek = new Date();
  endOfWeek.setDate(endOfWeek.getDate() + 7);
  endOfWeek.setHours(23, 59, 59, 999);
  return endOfWeek;
}

/**
 * Gets the start of a date (00:00:00)
 */
export function getStartOfDate(date: Date | string): Date {
  const d = typeof date === 'string' ? new Date(date) : date;
  d.setHours(0, 0, 0, 0);
  return d;
}

