/** Keep in sync with ReviewModerationService.MAX_REJECTION_REASON_CHARS (backend). */
const DEFAULT_MAX = 80;

/**
 * Shortens API moderation text for toasts and inline errors on small screens.
 * @param {string | null | undefined} text
 * @param {number} [maxLen]
 * @returns {string}
 */
export function truncateModerationMessage(text, maxLen = DEFAULT_MAX) {
	const t = (text ?? '').trim();
	if (!t) return '';
	if (t.length <= maxLen) return t;
	return `${t.slice(0, maxLen - 1).trim()}…`;
}
