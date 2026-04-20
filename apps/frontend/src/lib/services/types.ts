export type ApiId = number | string;

export interface ApiRecord {
	id?: ApiId;
	[key: string]: unknown;
}

export interface NamedRecord extends ApiRecord {
	name: string;
}

export interface TagRecord extends NamedRecord {
	description?: string | null;
	isFeatured?: boolean;
}

export interface ProductRecord extends ApiRecord {
	name: string;
	description?: string | null;
	imageUrl?: string | null;
	basePrice: number | string;
	tagIds?: ApiId[];
	createdAt?: string;
	updatedAt?: string;
}

export interface ReviewRecord extends ApiRecord {
	reviewerDisplayName?: string;
	verifiedAccount?: boolean;
	verifiedPurchase?: boolean;
	rating: number;
	comment?: string | null;
	status?: string;
	moderationMessage?: string | null;
	submittedAt?: string | null;
	reviewerPhotoUrl?: string | null;
	reviewerPhotoApprovalPending?: boolean;
	reviewerPhotoApproved?: boolean;
}

export interface OrderItemRecord extends ApiRecord {
	productId?: ApiId;
	productName?: string;
	productImageUrl?: string | null;
	unitPrice: number;
	quantity: number;
	lineTotal: number;
}

export interface OrderRecord extends ApiRecord {
	orderNumber?: string | number;
	status?: string;
	items?: OrderItemRecord[];
	bakeryName?: string;
	orderMethod?: string;
	placedAt?: string;
	orderGrandTotal?: number;
}

export interface CustomerRecord extends ApiRecord {
	firstName?: string | null;
	lastName?: string | null;
	username?: string;
	name?: string | null;
	email?: string | null;
	photoApprovalPending?: boolean;
	profilePhotoUrl?: string | null;
	photoUrl?: string | null;
	profilePhotoPath?: string | null;
	workEmail?: string | null;
	rewardTierName?: string | null;
	rewardBalance?: number | null;
}

export interface UserRecord extends ApiRecord {
	firstName?: string | null;
	lastName?: string | null;
	email?: string | null;
	username: string;
	role?: string;
	active?: boolean;
}

export interface AuthSession {
	userId: ApiId;
	username: string;
	role?: string;
}

export interface AuthResponse extends AuthSession {
	employeeDiscountLinkEstablished?: boolean;
	employeeDiscountLinkMessage?: string | null;
}

export interface LoginSuccess {
	ok: true;
}

export interface LoginFailure {
	ok: false;
	message: string;
	roleChoiceRequired?: boolean;
	choices?: unknown[];
}

export type LoginResult = LoginSuccess | LoginFailure;

export interface RegisterSuccess {
	ok: true;
	employeeDiscountLinkEstablished: boolean;
	employeeDiscountLinkMessage: string | null;
}

export interface RegisterFailure {
	ok: false;
	message: string;
	/** Present when the server rejected the request (e.g. 401 employee link password). */
	status?: number;
}

export type RegisterResult = RegisterSuccess | RegisterFailure;

export interface ReviewSubmissionResult {
	status?: string;
	moderationMessage?: string | null;
}

export type ErrorWithStatus = Error & { status?: number };

export interface AnalyticsPoint {
	label: string;
	value: number;
}

export interface ChatThread {
	id: number;
	customerUserId: string;
	customerDisplayName: string | null;
	customerUsername: string;
	customerEmail: string | null;
	customerProfilePhotoPath: string | null;
	customerPhotoApprovalPending: boolean;
	employeeUserId: string | null;
	employeeDisplayName: string | null;
	employeeUsername: string | null;
	employeeProfilePhotoPath: string | null;
	status: string;
	category: string;
	createdAt: string;
	updatedAt: string;
	closedAt: string | null;
}

export interface ChatMessage {
	id: number;
	threadId: number;
	senderUserId: string;
	text: string;
	sentAt: string;
	read: boolean;
	isSystem?: boolean;
	staffOnly?: boolean;
}

export interface StaffConversation {
	id: number;
	otherUserId: string;
	otherUsername: string;
	otherProfilePhotoPath: string | null;
	otherRole: string | null;
	updatedAt: string;
	unreadCount: number;
}

export interface StaffMessage {
	id: number;
	conversationId: number;
	senderUserId: string;
	text: string;
	sentAt: string;
	read: boolean;
}

export interface TypingPayload {
	userId: string;
	typing: boolean;
}
