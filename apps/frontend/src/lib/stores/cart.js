import { writable } from 'svelte/store';

function createCart() {
	const { subscribe, update, set } = writable({
		items:
			/** @type {{ productId: number, productName: string, productImageUrl: string | null, unitPrice: number, quantity: number, lineTotal: number }[]} */ ([]),
		subtotal: 0,
		discount: 0,
		total: 0
	});

	return {
		subscribe,
		addItem(product, quantity = 1) {
			update((c) => {
				const existing = c.items.find((i) => i.productId === product.id);
				if (existing) {
					existing.quantity += quantity;
					existing.lineTotal = existing.unitPrice * existing.quantity;
				} else {
					c.items = [
						...c.items,
						{
							productId: product.id,
							productName: product.name,
							productImageUrl: product.imageUrl ?? null,
							unitPrice: product.basePrice,
							quantity,
							lineTotal: product.basePrice * quantity
						}
					];
				}
				c.subtotal = c.items.reduce((sum, i) => sum + i.lineTotal, 0);
				c.total = c.subtotal - c.discount;
				return c;
			});
		},
		updateQuantity(productId, quantity) {
			update((cart) => {
				if (quantity <= 0) {
					cart.items = cart.items.filter((i) => i.productId !== productId);
				} else {
					const item = cart.items.find((i) => i.productId === productId);
					if (item) {
						item.quantity = quantity;
						item.lineTotal = +(item.unitPrice * quantity).toFixed(2);
					}
				}
				cart.subtotal = cart.items.reduce((sum, i) => sum + i.lineTotal, 0);
				cart.total = cart.subtotal - cart.discount;
				return { ...cart };
			});
		},
		removeItem(productId) {
			update((cart) => {
				cart.items = cart.items.filter((i) => i.productId !== productId);
				cart.subtotal = cart.items.reduce((sum, i) => sum + i.lineTotal, 0);
				cart.total = cart.subtotal - cart.discount;
				return { ...cart };
			});
		},
		clear() {
			set({ items: [], subtotal: 0, discount: 0, total: 0 });
		}
	};
}

export const cart = createCart();
