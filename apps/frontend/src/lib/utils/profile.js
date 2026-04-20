export function isProfileComplete(profile) {
	return !!(
		profile?.firstName &&
		profile?.lastName &&
		profile?.phone &&
		!profile.phone.toUpperCase().startsWith('OAUTH-') &&
		profile?.address?.line1 &&
		profile?.address?.city &&
		profile?.address?.province &&
		profile?.address?.postalCode
	);
}
