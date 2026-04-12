<script>
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { user } from '$lib/stores/authStore';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import {
		listProducts,
		createProduct,
		updateProduct,
		deleteProduct,
		uploadProductImage
	} from '$lib/services/staff-products.js';
	import { getTags } from '$lib/services/tags';
	import { formatPriceCad } from '$lib/utils/money';

	let products = $state([]);
	let loading = $state(true);
	let error = $state(null);
	let editingId = $state(null);
	let showCreate = $state(false);
	let saving = $state(false);
	let deleting = $state({});
	let createImageFile = $state(null);
	let editImageFile = $state(null);
	let uploadingImage = $state(false);
	let tags = $state([]);
	let createTagIds = $state([]);
	let editTagIds = $state([]);

	let editDraft = $state({ name: '', description: '', basePrice: '' });
	let createDraft = $state({ name: '', description: '', basePrice: '' });

	onMount(async () => {
		if ($user?.role !== 'admin') {
			goto(resolve('/staff/dashboard'), { replaceState: true });
			return;
		}
		try {
			[products, tags] = await Promise.all([listProducts(), getTags()]);
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function startEdit(product) {
		editingId = product.id;
		editDraft = {
			name: product.name,
			description: product.description ?? '',
			basePrice: String(product.basePrice)
		};
		editTagIds = product.tagIds ?? [];
	}

	async function handleUpdate(id) {
		saving = true;
		try {
			let updated = await updateProduct(id, {
				name: editDraft.name,
				description: editDraft.description,
				basePrice: parseFloat(editDraft.basePrice),
				tagIds: editTagIds
			});
			if (editImageFile) {
				updated = await uploadProductImage(id, editImageFile);
				editImageFile = null;
			}
			products = products.map((p) => (p.id === id ? updated : p));
			editingId = null;
		} catch {
			// leave form open
		} finally {
			saving = false;
		}
	}

	async function handleCreate() {
		saving = true;
		try {
			const created = await createProduct({
				name: createDraft.name,
				description: createDraft.description,
				basePrice: parseFloat(createDraft.basePrice),
				tagIds: createTagIds
			});
			if (createImageFile) {
				const withImage = await uploadProductImage(created.id, createImageFile);
				products = [withImage, ...products];
			} else {
				products = [created, ...products];
			}
			showCreate = false;
			createDraft = { name: '', description: '', basePrice: '' };
			createImageFile = null;
			createTagIds = [];
		} catch {
			// leave form open
		} finally {
			saving = false;
		}
	}

	async function handleDelete(id) {
		if (!confirm('Delete this product?')) return;
		deleting[id] = true;
		try {
			await deleteProduct(id);
			products = products.filter((p) => p.id !== id);
		} catch {
			// leave in list
		} finally {
			deleting[id] = false;
		}
	}
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-5xl space-y-6">
		<div class="flex items-center justify-between">
			<div>
				<h1 class="text-2xl font-bold tracking-tight text-foreground">Products</h1>
				<p class="mt-1 text-sm text-muted-foreground">Create and manage bakery products</p>
			</div>
			<Button size="sm" onclick={() => (showCreate = !showCreate)}>
				{showCreate ? 'Cancel' : 'New Product'}
			</Button>
		</div>

		{#if showCreate}
			<form
				class="space-y-3 rounded-xl border border-border bg-card p-5"
				onsubmit={(e) => {
					e.preventDefault();
					handleCreate();
				}}
			>
				<p class="text-sm font-semibold text-foreground">New Product</p>
				<div class="grid grid-cols-2 gap-3">
					<Input bind:value={createDraft.name} placeholder="Name" required />
					<Input
						bind:value={createDraft.basePrice}
						placeholder="Price (e.g. 4.99)"
						type="number"
						step="0.01"
						min="0"
						required
					/>
				</div>
				<Input bind:value={createDraft.description} placeholder="Description (optional)" />

				<div class="flex flex-col gap-1">
					<label class="text-xs font-medium text-muted-foreground">
						Product Image <span class="font-normal">(optional)</span>
					</label>
					<input
						type="file"
						accept="image/jpeg,image/png,image/webp"
						onchange={(e) => (createImageFile = e.target.files?.[0] ?? null)}
						class="text-sm text-muted-foreground file:mr-3 file:rounded-md file:border file:border-border file:bg-background file:px-3 file:py-1 file:text-xs file:font-medium"
					/>
				</div>

				{#if tags.length > 0}
					<div class="flex flex-col gap-1">
						<label class="text-xs font-medium text-muted-foreground">Categories</label>
						<div class="flex flex-wrap gap-2">
							{#each tags as tag (tag.id)}
								<label
									class="flex cursor-pointer items-center gap-1.5 rounded-full border border-border px-3 py-1 text-xs font-medium transition-colors
                    {createTagIds.includes(tag.id)
										? 'border-primary bg-primary text-primary-foreground'
										: 'bg-background text-muted-foreground hover:bg-muted'}"
								>
									<input
										type="checkbox"
										class="hidden"
										checked={createTagIds.includes(tag.id)}
										onchange={() => {
											createTagIds = createTagIds.includes(tag.id)
												? createTagIds.filter((id) => id !== tag.id)
												: [...createTagIds, tag.id];
										}}
									/>
									{tag.name}
								</label>
							{/each}
						</div>
					</div>
				{/if}

				<Button type="submit" size="sm" disabled={saving}>
					{saving ? 'Creating...' : 'Create'}
				</Button>
			</form>
		{/if}

		{#if loading}
			<div class="space-y-3">
				{#each Array(5) as _, i (i)}
					<Skeleton class="h-16 rounded-xl" />
				{/each}
			</div>
		{:else if error}
			<p class="text-sm text-destructive">Failed to load products.</p>
		{:else if products.length === 0}
			<div class="rounded-xl border border-border bg-card p-10 text-center">
				<p class="text-sm text-muted-foreground">No products yet.</p>
			</div>
		{:else}
			<div class="rounded-xl border border-border bg-card">
				<div class="divide-y divide-border">
					{#each products as product (product.id)}
						<div class="px-5 py-4">
							{#if editingId === product.id}
								<form
									class="space-y-2"
									onsubmit={(e) => {
										e.preventDefault();
										handleUpdate(product.id);
									}}
								>
									<div class="grid grid-cols-2 gap-2">
										<Input bind:value={editDraft.name} placeholder="Name" required />
										<Input
											bind:value={editDraft.basePrice}
											placeholder="Price"
											type="number"
											step="0.01"
											min="0"
											required
										/>
									</div>
									<Input bind:value={editDraft.description} placeholder="Description" />

									<div class="flex flex-col gap-1">
										<label class="text-xs font-medium text-muted-foreground">
											Replace Image <span class="font-normal">(optional)</span>
										</label>
										<input
											type="file"
											accept="image/jpeg,image/png,image/webp"
											onchange={(e) => (editImageFile = e.target.files?.[0] ?? null)}
											class="text-sm text-muted-foreground file:mr-3 file:rounded-md file:border file:border-border file:bg-background file:px-3 file:py-1 file:text-xs file:font-medium"
										/>
									</div>

									{#if tags.length > 0}
										<div class="flex flex-col gap-1">
											<label class="text-xs font-medium text-muted-foreground">Categories</label>
											<div class="flex flex-wrap gap-2">
												{#each tags as tag (tag.id)}
													<label
														class="flex cursor-pointer items-center gap-1.5 rounded-full border border-border px-3 py-1 text-xs font-medium transition-colors
                    {editTagIds.includes(tag.id)
															? 'border-primary bg-primary text-primary-foreground'
															: 'bg-background text-muted-foreground hover:bg-muted'}"
													>
														<input
															type="checkbox"
															class="hidden"
															checked={editTagIds.includes(tag.id)}
															onchange={() => {
																editTagIds = editTagIds.includes(tag.id)
																	? editTagIds.filter((id) => id !== tag.id)
																	: [...editTagIds, tag.id];
															}}
														/>
														{tag.name}
													</label>
												{/each}
											</div>
										</div>
									{/if}

									<div class="flex gap-2">
										<Button type="submit" size="sm" disabled={saving}>
											{saving ? '...' : 'Save'}
										</Button>
										<Button
											type="button"
											size="sm"
											variant="ghost"
											onclick={() => (editingId = null)}
										>
											Cancel
										</Button>
									</div>
								</form>
							{:else}
								<div class="flex items-center justify-between">
									<div>
										<p class="text-sm font-medium text-foreground">{product.name}</p>
										<p class="text-xs text-muted-foreground">
											{formatPriceCad(product.basePrice)}
											{#if product.description}
												· {product.description.slice(0, 60)}{product.description.length > 60
													? '…'
													: ''}
											{/if}
										</p>
									</div>
									<div class="flex gap-2">
										<Button size="sm" variant="outline" onclick={() => startEdit(product)}>
											Edit
										</Button>
										<Button
											size="sm"
											variant="destructive"
											onclick={() => handleDelete(product.id)}
											disabled={!!deleting[product.id]}
										>
											{deleting[product.id] ? '...' : 'Delete'}
										</Button>
									</div>
								</div>
							{/if}
						</div>
					{/each}
				</div>
			</div>
		{/if}
	</div>
</main>
