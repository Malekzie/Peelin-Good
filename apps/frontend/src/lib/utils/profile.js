export function isProfileComplete(profile) {
	return !!(
		profile?.phone &&
		profile?.address?.line1 &&
		profile?.address?.city &&
		profile?.address?.province &&
		profile?.address?.postalCode
	);
}
