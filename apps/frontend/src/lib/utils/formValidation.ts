export class FormValidationUtil {
	private static readonly emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
	private static readonly phoneRegex = /^\(\d{3}\) \d{3}-\d{4}$/;
	private static readonly canadianPostalRegex = /^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/;

	static isValidEmail(value: string): boolean {
		return this.emailRegex.test(value.trim());
	}

	static isValidPhone(value: string): boolean {
		return this.phoneRegex.test(value.trim());
	}

	static isValidCanadianPostalCode(value: string): boolean {
		return this.canadianPostalRegex.test(value.trim());
	}

	static formatPhone(raw: string): string {
		const digits = raw.replace(/\D/g, '').substring(0, 10);
		const parts: string[] = [];
		if (digits.length > 0) parts.push('(' + digits.substring(0, 3));
		if (digits.length >= 4) parts.push(') ' + digits.substring(3, 6));
		if (digits.length >= 7) parts.push('-' + digits.substring(6, 10));
		return parts.join('');
	}
}
