import { building } from '$app/environment';

/**
 * Fail-loud env-var validation for the SvelteKit node server. Mirrors the backend
 * EnvValidator: if any required variable is missing, we intercept every request and
 * return a full-page red overlay listing the offenders so the deployment can never
 * silently run in a half-configured state.
 *
 * Profile gating follows NODE_ENV — dev starts in a degraded mode (warnings only) so
 * developers can iterate without the full env set; production refuses to serve traffic.
 */

type RequiredVar = { name: string; description: string };

const ALWAYS_REQUIRED: RequiredVar[] = [];

const PROD_REQUIRED: RequiredVar[] = [
	{
		name: 'PUBLIC_SENTRY_DSN',
		description:
			'Client-side Sentry DSN. Without it the browser SDK is disabled silently — you lose FE error observability.'
	},
	{
		name: 'SENTRY_DSN',
		description:
			'Server-side Sentry DSN for the SvelteKit node adapter. Without it backend render errors never reach Sentry.'
	}
];

export type EnvValidationResult = {
	missing: RequiredVar[];
	isProd: boolean;
};

export function validateEnv(): EnvValidationResult {
	const isProd = process.env.NODE_ENV === 'production';
	const required = [...ALWAYS_REQUIRED, ...(isProd ? PROD_REQUIRED : [])];

	const missing = required.filter((r) => {
		const v = process.env[r.name];
		return !v || v.trim() === '';
	});

	if (!building && missing.length > 0) {
		const header = '!'.repeat(80);
		const body = missing.map((m) => `  * ${m.name}\n        ${m.description}`).join('\n\n');
		const msg = `\n\n${header}\n  MISSING REQUIRED ENVIRONMENT VARIABLES (frontend, NODE_ENV=${process.env.NODE_ENV})\n${header}\n\n${body}\n\n${header}\n`;
		if (isProd) console.error(msg);
		else console.warn(msg);
	}

	return { missing, isProd };
}

export function renderMissingEnvPage(result: EnvValidationResult): string {
	const rows = result.missing
		.map(
			(m) => `
        <li>
          <code>${escape(m.name)}</code>
          <p>${escape(m.description)}</p>
        </li>`
		)
		.join('');

	return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Configuration error — missing environment variables</title>
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <style>
    :root { color-scheme: dark; }
    html, body { margin: 0; padding: 0; min-height: 100%; background: #7a0b0b; color: #fff6f6;
      font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, sans-serif; }
    main { max-width: 860px; margin: 0 auto; padding: 48px 24px; }
    h1 { font-size: 2rem; margin: 0 0 0.25em; letter-spacing: -0.01em; }
    .sub { opacity: 0.85; margin: 0 0 32px; }
    .card { background: #5a0808; border: 2px solid #ffb4b4; border-radius: 12px; padding: 24px 28px; }
    ul { list-style: none; padding: 0; margin: 0; }
    li { padding: 14px 0; border-bottom: 1px solid rgba(255,255,255,0.18); }
    li:last-child { border-bottom: 0; }
    code { font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      background: #2d0303; padding: 2px 8px; border-radius: 4px; font-size: 1rem; }
    p { margin: 6px 0 0; opacity: 0.9; font-size: 0.95rem; }
    footer { margin-top: 32px; font-size: 0.9rem; opacity: 0.75; }
  </style>
</head>
<body>
  <main>
    <h1>Missing required environment variables</h1>
    <p class="sub">The frontend refuses to serve traffic until every variable below is set. This screen
      replaces a silent failure where pages load but features break at runtime.</p>
    <div class="card">
      <ul>${rows}
      </ul>
    </div>
    <footer>
      NODE_ENV=${escape(process.env.NODE_ENV ?? '(unset)')} — set these in your deployment
      platform's environment panel (e.g. DigitalOcean App Platform → Settings → Environment).
    </footer>
  </main>
</body>
</html>`;
}

function escape(s: string): string {
	return s
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;');
}
