/**
 * Returns `raw` only if it is a safe same-origin relative path.
 * Anything that could resolve to a different origin (`//evil.com`,
 * `https://evil.com`, `\\evil.com`, `javascript:` URIs, etc.) falls back.
 */
export function safeRedirectPath(raw: string | null | undefined, fallback = '/'): string {
	if (!raw) return fallback;
	// Must start with a single "/" followed by a path character that is not
	// another "/" or a backslash (which some browsers treat as a path separator).
	if (!/^\/[^/\\]/.test(raw)) return fallback;
	// Defensive: reject anything containing an explicit scheme separator.
	if (raw.includes('://')) return fallback;
	return raw;
}
