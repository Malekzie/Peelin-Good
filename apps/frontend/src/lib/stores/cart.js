import { writable } from 'svelte/store';

// Placeholder - replace with full cart implementation
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
		updateQuantity(productId, quantity) {
			update((cart) => {
				if (quantity <= 0) {
					cart.items = cart.items.filter((i) => i.productId !== productId);
				} else {
					const item = cart.items.find((i) => i.productId === productId);
					if (item) item.quantity = quantity;
				}
				return cart;
			});
		},
		removeItem(productId) {
			update((cart) => {
				cart.items = cart.items.filter((i) => i.productId !== productId);
				return cart;
			});
		},
		clear() {
			set({ items: [], subtotal: 0, discount: 0, total: 0 });
		}
	};
}

export const cart = createCart();
