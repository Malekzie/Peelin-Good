# Staff Portal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a staff-facing management portal at `/staff/*` for admin and employee roles, exposing all existing backend admin/employee APIs through a clean, role-aware SvelteKit interface.

**Architecture:** A dedicated `/staff` route prefix with its own `+layout.svelte` (no customer Navbar/Footer), a `StaffSidebar.svelte` with role-filtered nav links, and `hooks.server.ts` guarding `/staff` for `ADMIN`/`EMPLOYEE` roles only. All data fetching follows the existing `onMount()` + three-state (loading/error/data) pattern used throughout the customer portal. Admin-only pages redirect employees away on mount using the `$user` store.

**Tech Stack:** SvelteKit 5, Svelte 5 runes (`$state`, `$derived`, `$props`), Tailwind CSS v4, TypeScript, `@lucide/svelte`, shadcn-style UI components (`Badge`, `Button`, `Card`, `Input`, `Skeleton`, `Avatar`, `Separator`), Chart.js (new — analytics page only).

---

## Role Matrix

| Route | Employee | Admin |
|-------|----------|-------|
| `/staff/dashboard` | Read | Read |
| `/staff/orders` | Read + status update | Read + status update |
| `/staff/reviews` | Approve/reject | Approve/reject |
| `/staff/customers` | Read + photo moderate | Read + photo moderate |
| `/staff/customers/[id]` | Read only | Read + edit |
| `/staff/analytics` | Redirect to dashboard | Full access |
| `/staff/staff` | Redirect to dashboard | Full CRUD |
| `/staff/products` | Redirect to dashboard | Full CRUD |
| `/staff/users` | Redirect to dashboard | Read + toggle active |
| `/staff/profile` | Edit own | Edit own |

---

## File Map

### New Files

| File | Purpose |
|------|---------|
| `src/routes/staff/+layout.svelte` | Staff portal shell — sidebar + main slot, no Navbar/Footer |
| `src/routes/staff/+page.svelte` | Redirect to `/staff/dashboard` |
| `src/routes/staff/dashboard/+page.svelte` | KPI summary + recent orders |
| `src/routes/staff/orders/+page.svelte` | Order queue + inline status updates |
| `src/routes/staff/reviews/+page.svelte` | Pending review queue — approve/reject |
| `src/routes/staff/customers/+page.svelte` | Customer list + search + photo moderation tab |
| `src/routes/staff/customers/[id]/+page.svelte` | Customer detail, admin-only edit |
| `src/routes/staff/analytics/+page.svelte` | Analytics charts — admin only |
| `src/routes/staff/staff/+page.svelte` | Employee management — admin only |
| `src/routes/staff/products/+page.svelte` | Product management — admin only |
| `src/routes/staff/users/+page.svelte` | User account management — admin only |
| `src/routes/staff/profile/+page.svelte` | Own employee profile |
| `src/lib/components/staff/StaffSidebar.svelte` | Role-filtered sidebar nav |
| `src/lib/components/staff/KpiCard.svelte` | Reusable metric card (label + value + subtext) |
| `src/lib/services/dashboard.js` | `getDashboardSummary()` |
| `src/lib/services/analytics.js` | All analytics API calls |
| `src/lib/services/staff-orders.js` | `updateOrderStatus()`, `markDelivered()` |
| `src/lib/services/staff-reviews.js` | `getPendingReviews()`, `updateReviewStatus()` |
| `src/lib/services/staff-customers.js` | `listCustomers()`, `getCustomer()`, `patchCustomer()`, `getPendingPhotos()`, `approvePhoto()`, `rejectPhoto()` |
| `src/lib/services/staff-employees.js` | `listStaff()`, `createEmployee()`, `updateEmployee()`, `deleteEmployee()` |
| `src/lib/services/staff-products.js` | `listProducts()`, `createProduct()`, `updateProduct()`, `deleteProduct()` |
| `src/lib/services/staff-users.js` | `listUsers()`, `setUserActive()` |

### Modified Files

| File | Change |
|------|--------|
| `src/hooks.server.ts` | Add `/staff` guard — requires ADMIN or EMPLOYEE |
| `src/lib/components/layout/Navbar.svelte` | Add "Staff Portal" link for admin/employee users |

---

## Task 1: Auth Guard + Navbar

**Files:**
- Modify: `src/hooks.server.ts`
- Modify: `src/lib/components/layout/Navbar.svelte`

### hooks.server.ts

Add `/staff` protection before the existing employee/admin blocks:

```typescript
const staffRoutes = ['/staff'];
const isStaff = staffRoutes.some((r) => pathname === r || pathname.startsWith(r + '/'));

if (isStaff) {
    if (!user) throw redirect(303, `/login?redirectTo=${encodeURIComponent(pathname)}`);
    if (user.role !== 'employee' && user.role !== 'admin') {
        throw redirect(303, '/?error=forbidden');
    }
}
```

Insert this block after the `isProtected` block and before the `/employee` block. Full updated guard section:

```typescript
const customerRoutes = ['/cart', '/checkout', '/orders', '/profile'];
const isProtected = customerRoutes.some((r) => pathname === r || pathname.startsWith(r + '/'));

const staffRoutes = ['/staff'];
const isStaff = staffRoutes.some((r) => pathname === r || pathname.startsWith(r + '/'));

if (isProtected && !user) {
    throw redirect(303, `/login?redirectTo=${encodeURIComponent(pathname)}`);
} else if (isStaff) {
    if (!user) throw redirect(303, `/login?redirectTo=${encodeURIComponent(pathname)}`);
    if (user.role !== 'employee' && user.role !== 'admin') {
        throw redirect(303, '/?error=forbidden');
    }
} else if (pathname.startsWith('/employee')) {
    if (!user || (user.role !== 'employee' && user.role !== 'admin')) {
        throw redirect(303, '/?error=forbidden');
    }
} else if (pathname.startsWith('/admin')) {
    if (!user || user.role !== 'admin') {
        throw redirect(303, '/?error=forbidden');
    }
}
```

### Navbar.svelte

In the desktop nav links section, add a Staff Portal link that only shows for admin/employee. Import `user` from authStore:

```svelte
<script>
    // add to existing imports:
    import { isLoggedIn, user } from '$lib/stores/authStore';
</script>
```

In the desktop nav (after the About link, before the Orders link):

```svelte
{#if $user?.role === 'admin' || $user?.role === 'employee'}
    <a
        href={resolve('/staff/dashboard')}
        class="text-sm font-medium text-foreground transition-colors hover:text-primary"
    >
        Staff Portal
    </a>
{/if}
```

In the mobile nav (same condition):

```svelte
{#if $user?.role === 'admin' || $user?.role === 'employee'}
    <a href={resolve('/staff/dashboard')} class="text-sm text-foreground hover:text-primary">
        Staff Portal
    </a>
{/if}
```

- [ ] Update `hooks.server.ts` with the staff route guard
- [ ] Update `Navbar.svelte` to add conditional Staff Portal link
- [ ] Verify: log in as admin, confirm "Staff Portal" appears in nav; log in as customer, confirm it does not

---

## Task 2: Staff Layout + Sidebar

**Files:**
- Create: `src/lib/components/staff/StaffSidebar.svelte`
- Create: `src/routes/staff/+layout.svelte`
- Create: `src/routes/staff/+page.svelte`

### StaffSidebar.svelte

Mirrors `ProfileSidebar.svelte` structure. Role-filtered nav links — admin sees all, employee sees a subset.

```svelte
<script>
    import { page } from '$app/state';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { user } from '$lib/stores/authStore';
    import { logoutUser } from '$lib/services/auth.js';
    import { Avatar, AvatarFallback } from '$lib/components/ui/avatar';
    import { Separator } from '$lib/components/ui/separator';
    import { Button } from '$lib/components/ui/button';
    import {
        LayoutDashboard,
        ShoppingBag,
        Star,
        Users,
        BarChart2,
        Package,
        UserCog,
        Shield,
        User,
        LogOut
    } from '@lucide/svelte';

    const allNavLinks = [
        { label: 'Dashboard', href: '/staff/dashboard', icon: LayoutDashboard, roles: null },
        { label: 'Orders', href: '/staff/orders', icon: ShoppingBag, roles: null },
        { label: 'Reviews', href: '/staff/reviews', icon: Star, roles: null },
        { label: 'Customers', href: '/staff/customers', icon: Users, roles: null },
        { label: 'Analytics', href: '/staff/analytics', icon: BarChart2, roles: ['admin'] },
        { label: 'Products', href: '/staff/products', icon: Package, roles: ['admin'] },
        { label: 'Employees', href: '/staff/staff', icon: UserCog, roles: ['admin'] },
        { label: 'Users', href: '/staff/users', icon: Shield, roles: ['admin'] },
        { label: 'My Profile', href: '/staff/profile', icon: User, roles: null },
    ];

    const navLinks = $derived(
        allNavLinks.filter((l) => l.roles === null || l.roles.includes($user?.role ?? ''))
    );

    const initials = $derived(
        ($user?.username?.[0] ?? '?').toUpperCase()
    );

    async function handleLogout() {
        await logoutUser();
        goto(resolve('/'));
    }
</script>

<aside class="hidden w-64 flex-col border-r border-border bg-card md:flex">
    <div class="flex flex-col gap-6 p-6 pt-8">
        <div class="flex items-center gap-3">
            <Avatar class="h-10 w-10">
                <AvatarFallback class="bg-primary text-sm font-semibold text-primary-foreground">
                    {initials}
                </AvatarFallback>
            </Avatar>
            <div class="min-w-0">
                <p class="truncate text-sm font-semibold text-foreground">{$user?.username ?? 'Staff'}</p>
                <p class="text-xs text-muted-foreground capitalize">{$user?.role ?? ''}</p>
            </div>
        </div>

        <Separator />

        <nav class="flex flex-col gap-1">
            {#each navLinks as link (link.href)}
                {@const active = $page.url.pathname === link.href || $page.url.pathname.startsWith(link.href + '/')}
                <a
                    href={resolve(link.href)}
                    class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors
                        {active
                        ? 'bg-primary text-primary-foreground'
                        : 'text-muted-foreground hover:bg-muted hover:text-foreground'}"
                >
                    <link.icon class="h-4 w-4 shrink-0" />
                    {link.label}
                </a>
            {/each}
        </nav>
    </div>

    <div class="mt-auto border-t border-border p-6">
        <Button
            variant="ghost"
            onclick={handleLogout}
            class="w-full justify-start gap-2 text-muted-foreground hover:text-destructive"
        >
            <LogOut class="h-4 w-4" />
            Log out
        </Button>
    </div>
</aside>
```

### +layout.svelte

Staff portal has no Navbar or Footer — it's a standalone management interface.

```svelte
<script lang="ts">
    import StaffSidebar from '$lib/components/staff/StaffSidebar.svelte';

    let { children } = $props();
</script>

<div class="flex min-h-screen bg-background">
    <StaffSidebar />
    <div class="flex flex-1 flex-col overflow-hidden">
        {@render children()}
    </div>
</div>
```

### +page.svelte (redirect)

```svelte
<script>
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { onMount } from 'svelte';

    onMount(() => {
        goto(resolve('/staff/dashboard'), { replaceState: true });
    });
</script>
```

- [ ] Create `StaffSidebar.svelte`
- [ ] Create `src/routes/staff/+layout.svelte`
- [ ] Create `src/routes/staff/+page.svelte`
- [ ] Verify: navigate to `/staff` as admin → redirects to `/staff/dashboard`; sidebar renders with all links

---

## Task 3: Dashboard Page

**Files:**
- Create: `src/lib/services/dashboard.js`
- Create: `src/lib/components/staff/KpiCard.svelte`
- Create: `src/routes/staff/dashboard/+page.svelte`

### dashboard.js

```javascript
const API = '/api/v1';

export async function getDashboardSummary() {
    const res = await fetch(`${API}/admin/dashboard/summary`, {
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to fetch dashboard summary');
    return res.json();
}
```

Response shape:
```json
{
  "totalRevenue": 12345.67,
  "totalOrders": 142,
  "totalCustomers": 38,
  "totalProducts": 24,
  "recentOrders": [{ ...OrderDto }]
}
```

### KpiCard.svelte

```svelte
<script>
    let { label, value, subtext = null } = $props();
</script>

<div class="rounded-xl border border-border bg-card p-6 shadow-sm">
    <p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">{label}</p>
    <p class="mt-2 text-3xl font-bold text-foreground">{value}</p>
    {#if subtext}
        <p class="mt-1 text-xs text-muted-foreground">{subtext}</p>
    {/if}
</div>
```

### dashboard/+page.svelte

```svelte
<script>
    import { onMount } from 'svelte';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import { Badge } from '$lib/components/ui/badge';
    import KpiCard from '$lib/components/staff/KpiCard.svelte';
    import { getDashboardSummary } from '$lib/services/dashboard.js';

    let summary = $state(null);
    let loading = $state(true);
    let error = $state(null);

    onMount(async () => {
        try {
            summary = await getDashboardSummary();
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    });

    function statusVariant(status) {
        if (!status) return 'secondary';
        if (['completed', 'delivered', 'picked_up'].includes(status)) return 'default';
        if (status === 'cancelled') return 'destructive';
        if (status === 'ready') return 'default';
        return 'secondary';
    }

    function formatCurrency(val) {
        if (val == null) return '—';
        return `$${Number(val).toFixed(2)}`;
    }

    function formatDate(dt) {
        if (!dt) return '—';
        return new Date(dt).toLocaleDateString('en-CA', { month: 'short', day: 'numeric', year: 'numeric' });
    }
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
    <div class="mx-auto max-w-5xl space-y-8">
        <div>
            <h1 class="text-2xl font-bold tracking-tight text-foreground">Dashboard</h1>
            <p class="mt-1 text-sm text-muted-foreground">Overview of bakery operations</p>
        </div>

        {#if loading}
            <div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
                {#each Array(4) as _, i (i)}
                    <Skeleton class="h-28 rounded-xl" />
                {/each}
            </div>
            <Skeleton class="h-64 rounded-xl" />
        {:else if error}
            <p class="text-sm text-destructive">Failed to load dashboard. Please try again.</p>
        {:else}
            <div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
                <KpiCard label="Total Revenue" value={formatCurrency(summary.totalRevenue)} />
                <KpiCard label="Total Orders" value={summary.totalOrders} />
                <KpiCard label="Customers" value={summary.totalCustomers} />
                <KpiCard label="Products" value={summary.totalProducts} />
            </div>

            <div class="rounded-xl border border-border bg-card shadow-sm">
                <div class="border-b border-border px-6 py-4">
                    <h2 class="text-sm font-semibold text-foreground">Recent Orders</h2>
                </div>
                <div class="divide-y divide-border">
                    {#if summary.recentOrders?.length === 0}
                        <p class="px-6 py-8 text-center text-sm text-muted-foreground">No recent orders</p>
                    {:else}
                        {#each summary.recentOrders ?? [] as order (order.id)}
                            <div class="flex items-center justify-between px-6 py-4">
                                <div class="space-y-0.5">
                                    <p class="text-sm font-semibold text-foreground">
                                        #{order.orderNumber}
                                    </p>
                                    <p class="text-xs text-muted-foreground">
                                        {order.bakeryName ?? 'Unknown'} · {formatDate(order.placedAt)}
                                    </p>
                                </div>
                                <div class="flex items-center gap-3">
                                    <Badge variant={statusVariant(order.status)}>
                                        {order.status?.replace(/_/g, ' ') ?? '—'}
                                    </Badge>
                                    <p class="text-sm font-bold text-foreground">
                                        {formatCurrency(order.orderGrandTotal)}
                                    </p>
                                </div>
                            </div>
                        {/each}
                    {/if}
                </div>
            </div>
        {/if}
    </div>
</main>
```

- [ ] Create `src/lib/services/dashboard.js`
- [ ] Create `src/lib/components/staff/KpiCard.svelte`
- [ ] Create `src/routes/staff/dashboard/+page.svelte`
- [ ] Verify: navigate to `/staff/dashboard` — KPI cards and recent orders render

---

## Task 4: Orders Page

**Note:** `GET /api/v1/orders` returns the current user's orders only. For staff, the dashboard summary provides recent orders. This page uses recent orders from the dashboard + allows staff to update order statuses. A dedicated "all orders for staff" endpoint does not currently exist in the backend.

**Files:**
- Create: `src/lib/services/staff-orders.js`
- Create: `src/routes/staff/orders/+page.svelte`

### staff-orders.js

```javascript
const API = '/api/v1';

export async function updateOrderStatus(orderId, status) {
    const res = await fetch(`${API}/orders/${orderId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ status })
    });
    if (!res.ok) throw new Error('Failed to update order status');
    return res.json();
}

export async function markDelivered(orderId) {
    const res = await fetch(`${API}/orders/${orderId}/delivered`, {
        method: 'PATCH',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to mark order delivered');
    return res.json();
}
```

### orders/+page.svelte

Shows recent orders from the dashboard summary. Staff can advance each order's status inline.

```svelte
<script>
    import { onMount } from 'svelte';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import { Badge } from '$lib/components/ui/badge';
    import { Button } from '$lib/components/ui/button';
    import { getDashboardSummary } from '$lib/services/dashboard.js';
    import { updateOrderStatus, markDelivered } from '$lib/services/staff-orders.js';

    let orders = $state([]);
    let loading = $state(true);
    let error = $state(null);
    let updating = $state({});

    onMount(async () => {
        try {
            const summary = await getDashboardSummary();
            orders = summary.recentOrders ?? [];
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    });

    const STATUS_FLOW = {
        placed: 'pending_payment',
        pending_payment: 'paid',
        paid: 'preparing',
        preparing: 'ready',
        ready: 'picked_up'
    };

    function nextStatus(current) {
        return STATUS_FLOW[current] ?? null;
    }

    function nextLabel(current) {
        const next = nextStatus(current);
        if (!next) return null;
        return next.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
    }

    async function advance(order) {
        const next = nextStatus(order.status);
        if (!next) return;
        updating[order.id] = true;
        try {
            if (next === 'delivered') {
                const updated = await markDelivered(order.id);
                orders = orders.map((o) => (o.id === order.id ? updated : o));
            } else {
                const updated = await updateOrderStatus(order.id, next);
                orders = orders.map((o) => (o.id === order.id ? updated : o));
            }
        } catch {
            // silent — badge will not change
        } finally {
            updating[order.id] = false;
        }
    }

    function statusVariant(status) {
        if (['completed', 'delivered', 'picked_up'].includes(status)) return 'default';
        if (status === 'cancelled') return 'destructive';
        if (status === 'ready') return 'default';
        return 'secondary';
    }

    function formatDate(dt) {
        if (!dt) return '—';
        return new Date(dt).toLocaleString('en-CA', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    }

    function formatCurrency(val) {
        if (val == null) return '—';
        return `$${Number(val).toFixed(2)}`;
    }
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
    <div class="mx-auto max-w-5xl space-y-6">
        <div>
            <h1 class="text-2xl font-bold tracking-tight text-foreground">Orders</h1>
            <p class="mt-1 text-sm text-muted-foreground">Recent orders — update status as work progresses</p>
        </div>

        {#if loading}
            <div class="space-y-3">
                {#each Array(5) as _, i (i)}
                    <Skeleton class="h-20 rounded-xl" />
                {/each}
            </div>
        {:else if error}
            <p class="text-sm text-destructive">Failed to load orders.</p>
        {:else if orders.length === 0}
            <div class="rounded-xl border border-border bg-card p-10 text-center">
                <p class="text-sm text-muted-foreground">No recent orders</p>
            </div>
        {:else}
            <div class="space-y-3">
                {#each orders as order (order.id)}
                    <div class="flex items-center justify-between rounded-xl border border-border bg-card px-5 py-4">
                        <div class="space-y-0.5">
                            <p class="text-sm font-semibold text-foreground">#{order.orderNumber}</p>
                            <p class="text-xs text-muted-foreground">
                                {order.bakeryName ?? '—'} · {order.orderMethod} · {formatDate(order.placedAt)}
                            </p>
                            <p class="text-xs font-medium text-foreground">{formatCurrency(order.orderGrandTotal)}</p>
                        </div>
                        <div class="flex items-center gap-3">
                            <Badge variant={statusVariant(order.status)}>
                                {order.status?.replace(/_/g, ' ') ?? '—'}
                            </Badge>
                            {#if nextStatus(order.status)}
                                <Button
                                    size="sm"
                                    variant="outline"
                                    onclick={() => advance(order)}
                                    disabled={!!updating[order.id]}
                                >
                                    {updating[order.id] ? 'Updating...' : `Mark ${nextLabel(order.status)}`}
                                </Button>
                            {/if}
                        </div>
                    </div>
                {/each}
            </div>
        {/if}
    </div>
</main>
```

- [ ] Create `src/lib/services/staff-orders.js`
- [ ] Create `src/routes/staff/orders/+page.svelte`
- [ ] Verify: orders list renders; clicking "Mark Preparing" on a paid order updates the status badge

---

## Task 5: Reviews Page

**Files:**
- Create: `src/lib/services/staff-reviews.js`
- Create: `src/routes/staff/reviews/+page.svelte`

### staff-reviews.js

```javascript
const API = '/api/v1';

export async function getPendingReviews() {
    const res = await fetch(`${API}/reviews/pending`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch pending reviews');
    return res.json();
}

export async function updateReviewStatus(reviewId, status) {
    const res = await fetch(`${API}/reviews/${reviewId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ status })
    });
    if (!res.ok) throw new Error('Failed to update review status');
    return res.json();
}
```

### reviews/+page.svelte

```svelte
<script>
    import { onMount } from 'svelte';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import { Button } from '$lib/components/ui/button';
    import { getPendingReviews, updateReviewStatus } from '$lib/services/staff-reviews.js';

    let reviews = $state([]);
    let loading = $state(true);
    let error = $state(null);
    let actioning = $state({});

    onMount(async () => {
        try {
            reviews = await getPendingReviews();
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    });

    async function moderate(reviewId, status) {
        actioning[reviewId] = status;
        try {
            await updateReviewStatus(reviewId, status);
            reviews = reviews.filter((r) => r.id !== reviewId);
        } catch {
            // leave in list on failure
        } finally {
            actioning[reviewId] = null;
        }
    }

    function renderStars(rating) {
        return '★'.repeat(rating) + '☆'.repeat(5 - rating);
    }

    function formatDate(dt) {
        if (!dt) return '—';
        return new Date(dt).toLocaleDateString('en-CA', { year: 'numeric', month: 'short', day: 'numeric' });
    }
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
    <div class="mx-auto max-w-4xl space-y-6">
        <div class="flex items-center justify-between">
            <div>
                <h1 class="text-2xl font-bold tracking-tight text-foreground">Reviews</h1>
                <p class="mt-1 text-sm text-muted-foreground">Pending reviews awaiting moderation</p>
            </div>
            {#if !loading && !error}
                <span class="rounded-full bg-muted px-3 py-1 text-xs font-semibold text-foreground">
                    {reviews.length} pending
                </span>
            {/if}
        </div>

        {#if loading}
            <div class="space-y-3">
                {#each Array(4) as _, i (i)}
                    <Skeleton class="h-28 rounded-xl" />
                {/each}
            </div>
        {:else if error}
            <p class="text-sm text-destructive">Failed to load reviews.</p>
        {:else if reviews.length === 0}
            <div class="rounded-xl border border-border bg-card p-10 text-center">
                <p class="text-sm text-muted-foreground">No pending reviews</p>
            </div>
        {:else}
            <div class="space-y-3">
                {#each reviews as review (review.id)}
                    <div class="rounded-xl border border-border bg-card p-5">
                        <div class="flex items-start justify-between gap-4">
                            <div class="space-y-1">
                                <div class="flex items-center gap-2">
                                    <span class="text-sm font-semibold text-foreground">
                                        {review.reviewerDisplayName ?? 'Anonymous'}
                                    </span>
                                    <span class="text-sm text-amber-500">{renderStars(review.rating)}</span>
                                </div>
                                <p class="text-xs text-muted-foreground">
                                    {review.bakeryName ?? '—'} · {formatDate(review.submittedAt)}
                                </p>
                                {#if review.comment}
                                    <p class="mt-2 text-sm text-foreground">{review.comment}</p>
                                {/if}
                            </div>
                            <div class="flex shrink-0 gap-2">
                                <Button
                                    size="sm"
                                    variant="outline"
                                    onclick={() => moderate(review.id, 'approved')}
                                    disabled={!!actioning[review.id]}
                                >
                                    {actioning[review.id] === 'approved' ? '...' : 'Approve'}
                                </Button>
                                <Button
                                    size="sm"
                                    variant="destructive"
                                    onclick={() => moderate(review.id, 'rejected')}
                                    disabled={!!actioning[review.id]}
                                >
                                    {actioning[review.id] === 'rejected' ? '...' : 'Reject'}
                                </Button>
                            </div>
                        </div>
                    </div>
                {/each}
            </div>
        {/if}
    </div>
</main>
```

- [ ] Create `src/lib/services/staff-reviews.js`
- [ ] Create `src/routes/staff/reviews/+page.svelte`
- [ ] Verify: pending reviews list renders; Approve removes the item; Reject removes the item

---

## Task 6: Customers Page + Detail

**Files:**
- Create: `src/lib/services/staff-customers.js`
- Create: `src/routes/staff/customers/+page.svelte`
- Create: `src/routes/staff/customers/[id]/+page.svelte`

### staff-customers.js

```javascript
const API = '/api/v1/admin/customers';

export async function listCustomers(search = '') {
    const url = search ? `${API}?search=${encodeURIComponent(search)}` : API;
    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch customers');
    return res.json();
}

export async function getCustomer(id) {
    const res = await fetch(`${API}/${id}`, { credentials: 'include' });
    if (!res.ok) throw new Error('Customer not found');
    return res.json();
}

export async function patchCustomer(id, data) {
    const res = await fetch(`${API}/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Failed to update customer');
    return res.json();
}

export async function getPendingPhotos() {
    const res = await fetch(`${API}/pending-photos`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch pending photos');
    return res.json();
}

export async function approvePhoto(id) {
    const res = await fetch(`${API}/${id}/approve-photo`, {
        method: 'POST',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to approve photo');
}

export async function rejectPhoto(id) {
    const res = await fetch(`${API}/${id}/reject-photo`, {
        method: 'POST',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to reject photo');
}
```

### customers/+page.svelte

Two tabs: "All Customers" (searchable list) and "Pending Photos" (moderation queue).

```svelte
<script>
    import { onMount } from 'svelte';
    import { resolve } from '$app/paths';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import { Input } from '$lib/components/ui/input';
    import { Button } from '$lib/components/ui/button';
    import { Badge } from '$lib/components/ui/badge';
    import {
        listCustomers,
        getPendingPhotos,
        approvePhoto,
        rejectPhoto
    } from '$lib/services/staff-customers.js';

    let tab = $state('all');
    let customers = $state([]);
    let pendingPhotos = $state([]);
    let search = $state('');
    let loading = $state(true);
    let error = $state(null);
    let actioning = $state({});

    onMount(async () => {
        await loadAll();
    });

    async function loadAll() {
        loading = true;
        error = null;
        try {
            [customers, pendingPhotos] = await Promise.all([
                listCustomers(),
                getPendingPhotos()
            ]);
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    }

    async function handleSearch() {
        loading = true;
        try {
            customers = await listCustomers(search);
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    }

    async function handleApprove(id) {
        actioning[id] = 'approve';
        try {
            await approvePhoto(id);
            pendingPhotos = pendingPhotos.filter((c) => c.id !== id);
        } finally {
            actioning[id] = null;
        }
    }

    async function handleReject(id) {
        actioning[id] = 'reject';
        try {
            await rejectPhoto(id);
            pendingPhotos = pendingPhotos.filter((c) => c.id !== id);
        } finally {
            actioning[id] = null;
        }
    }

    const filteredCustomers = $derived(
        search
            ? customers.filter(
                  (c) =>
                      c.firstName?.toLowerCase().includes(search.toLowerCase()) ||
                      c.lastName?.toLowerCase().includes(search.toLowerCase()) ||
                      c.email?.toLowerCase().includes(search.toLowerCase())
              )
            : customers
    );
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
    <div class="mx-auto max-w-5xl space-y-6">
        <div class="flex items-center justify-between">
            <div>
                <h1 class="text-2xl font-bold tracking-tight text-foreground">Customers</h1>
                <p class="mt-1 text-sm text-muted-foreground">Manage customer accounts and photos</p>
            </div>
            {#if pendingPhotos.length > 0}
                <Badge variant="destructive">{pendingPhotos.length} photos pending</Badge>
            {/if}
        </div>

        <!-- Tabs -->
        <div class="flex gap-1 border-b border-border">
            {#each [['all', 'All Customers'], ['photos', 'Pending Photos']] as [key, label] (key)}
                <button
                    onclick={() => (tab = key)}
                    class="px-4 py-2 text-sm font-medium transition-colors
                        {tab === key
                        ? 'border-b-2 border-primary text-foreground'
                        : 'text-muted-foreground hover:text-foreground'}"
                >
                    {label}
                    {#if key === 'photos' && pendingPhotos.length > 0}
                        <span class="ml-1 rounded-full bg-destructive px-1.5 py-0.5 text-xs text-white">
                            {pendingPhotos.length}
                        </span>
                    {/if}
                </button>
            {/each}
        </div>

        {#if loading}
            <div class="space-y-3">
                {#each Array(5) as _, i (i)}
                    <Skeleton class="h-16 rounded-xl" />
                {/each}
            </div>
        {:else if error}
            <p class="text-sm text-destructive">Failed to load customers.</p>
        {:else if tab === 'all'}
            <div class="flex gap-3">
                <Input
                    placeholder="Search by name or email..."
                    bind:value={search}
                    class="max-w-sm"
                />
            </div>

            <div class="rounded-xl border border-border bg-card">
                <div class="divide-y divide-border">
                    {#if filteredCustomers.length === 0}
                        <p class="px-6 py-8 text-center text-sm text-muted-foreground">No customers found</p>
                    {:else}
                        {#each filteredCustomers as c (c.id)}
                            <div class="flex items-center justify-between px-5 py-3">
                                <div>
                                    <p class="text-sm font-medium text-foreground">
                                        {c.firstName ?? ''} {c.lastName ?? ''}
                                    </p>
                                    <p class="text-xs text-muted-foreground">{c.email ?? '—'}</p>
                                </div>
                                <div class="flex items-center gap-3">
                                    {#if c.photoApprovalPending}
                                        <Badge variant="destructive" class="text-xs">Photo pending</Badge>
                                    {/if}
                                    <a
                                        href={resolve(`/staff/customers/${c.id}`)}
                                        class="text-xs font-medium text-primary hover:underline"
                                    >
                                        View
                                    </a>
                                </div>
                            </div>
                        {/each}
                    {/if}
                </div>
            </div>
        {:else}
            <!-- Pending photos tab -->
            {#if pendingPhotos.length === 0}
                <div class="rounded-xl border border-border bg-card p-10 text-center">
                    <p class="text-sm text-muted-foreground">No pending photos</p>
                </div>
            {:else}
                <div class="space-y-3">
                    {#each pendingPhotos as c (c.id)}
                        <div class="flex items-center justify-between rounded-xl border border-border bg-card px-5 py-4">
                            <div class="flex items-center gap-4">
                                {#if c.profilePhotoPath}
                                    <img
                                        src={c.profilePhotoPath}
                                        alt="Profile"
                                        class="h-12 w-12 rounded-full object-cover border border-border"
                                    />
                                {/if}
                                <div>
                                    <p class="text-sm font-medium text-foreground">
                                        {c.firstName ?? ''} {c.lastName ?? ''}
                                    </p>
                                    <p class="text-xs text-muted-foreground">{c.email ?? '—'}</p>
                                </div>
                            </div>
                            <div class="flex gap-2">
                                <Button
                                    size="sm"
                                    variant="outline"
                                    onclick={() => handleApprove(c.id)}
                                    disabled={!!actioning[c.id]}
                                >
                                    Approve
                                </Button>
                                <Button
                                    size="sm"
                                    variant="destructive"
                                    onclick={() => handleReject(c.id)}
                                    disabled={!!actioning[c.id]}
                                >
                                    Reject
                                </Button>
                            </div>
                        </div>
                    {/each}
                </div>
            {/if}
        {/if}
    </div>
</main>
```

### customers/[id]/+page.svelte

View-only for employees; full edit for admins.

```svelte
<script>
    import { onMount } from 'svelte';
    import { page } from '$app/state';
    import { resolve } from '$app/paths';
    import { user } from '$lib/stores/authStore';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import { Button } from '$lib/components/ui/button';
    import { Input } from '$lib/components/ui/input';
    import { getCustomer, patchCustomer, approvePhoto, rejectPhoto } from '$lib/services/staff-customers.js';

    const id = page.params.id;
    const isAdmin = $derived($user?.role === 'admin');

    let customer = $state(null);
    let loading = $state(true);
    let error = $state(null);
    let editing = $state(false);
    let saving = $state(false);

    // Draft for edit form
    let draft = $state({});

    onMount(async () => {
        try {
            customer = await getCustomer(id);
            resetDraft();
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    });

    function resetDraft() {
        draft = {
            firstName: customer.firstName ?? '',
            lastName: customer.lastName ?? '',
            phone: customer.phone ?? '',
            email: customer.email ?? ''
        };
    }

    async function handleSave() {
        saving = true;
        try {
            customer = await patchCustomer(id, draft);
            editing = false;
        } catch {
            // leave form open on error
        } finally {
            saving = false;
        }
    }

    async function handleApprovePhoto() {
        await approvePhoto(id);
        customer = { ...customer, photoApprovalPending: false };
    }

    async function handleRejectPhoto() {
        await rejectPhoto(id);
        customer = { ...customer, photoApprovalPending: false, profilePhotoPath: null };
    }
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
    <div class="mx-auto max-w-3xl space-y-6">
        <div class="flex items-center gap-4">
            <a href={resolve('/staff/customers')} class="text-sm text-primary hover:underline">
                Customers
            </a>
            <span class="text-muted-foreground">/</span>
            <span class="text-sm text-muted-foreground">Detail</span>
        </div>

        {#if loading}
            <Skeleton class="h-48 rounded-xl" />
        {:else if error}
            <p class="text-sm text-destructive">Customer not found.</p>
        {:else}
            <div class="rounded-xl border border-border bg-card p-6 space-y-4">
                <div class="flex items-start justify-between">
                    <h1 class="text-xl font-bold text-foreground">
                        {customer.firstName ?? ''} {customer.lastName ?? ''}
                    </h1>
                    {#if isAdmin && !editing}
                        <Button size="sm" variant="outline" onclick={() => (editing = true)}>Edit</Button>
                    {/if}
                </div>

                {#if editing}
                    <form class="space-y-3" onsubmit={(e) => { e.preventDefault(); handleSave(); }}>
                        <div class="grid grid-cols-2 gap-3">
                            <Input bind:value={draft.firstName} placeholder="First name" />
                            <Input bind:value={draft.lastName} placeholder="Last name" />
                            <Input bind:value={draft.phone} placeholder="Phone" />
                            <Input bind:value={draft.email} placeholder="Email" type="email" />
                        </div>
                        <div class="flex gap-2">
                            <Button type="submit" size="sm" disabled={saving}>
                                {saving ? 'Saving...' : 'Save'}
                            </Button>
                            <Button
                                type="button"
                                size="sm"
                                variant="ghost"
                                onclick={() => { editing = false; resetDraft(); }}
                            >
                                Cancel
                            </Button>
                        </div>
                    </form>
                {:else}
                    <dl class="grid grid-cols-2 gap-3 text-sm">
                        <div>
                            <dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Email</dt>
                            <dd class="mt-1 text-foreground">{customer.email ?? '—'}</dd>
                        </div>
                        <div>
                            <dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Phone</dt>
                            <dd class="mt-1 text-foreground">{customer.phone ?? '—'}</dd>
                        </div>
                        <div>
                            <dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Reward Balance</dt>
                            <dd class="mt-1 text-foreground">{customer.rewardBalance ?? 0} pts</dd>
                        </div>
                        <div>
                            <dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Address</dt>
                            <dd class="mt-1 text-foreground">
                                {#if customer.address}
                                    {customer.address.addressLine1}, {customer.address.addressCity}
                                {:else}
                                    —
                                {/if}
                            </dd>
                        </div>
                    </dl>
                {/if}
            </div>

            {#if customer.photoApprovalPending}
                <div class="rounded-xl border border-destructive bg-destructive/5 p-5 space-y-3">
                    <p class="text-sm font-semibold text-foreground">Photo Pending Approval</p>
                    {#if customer.profilePhotoPath}
                        <img
                            src={customer.profilePhotoPath}
                            alt="Pending profile photo"
                            class="h-24 w-24 rounded-full object-cover border border-border"
                        />
                    {/if}
                    <div class="flex gap-2">
                        <Button size="sm" variant="outline" onclick={handleApprovePhoto}>Approve</Button>
                        <Button size="sm" variant="destructive" onclick={handleRejectPhoto}>Reject</Button>
                    </div>
                </div>
            {/if}
        {/if}
    </div>
</main>
```

- [ ] Create `src/lib/services/staff-customers.js`
- [ ] Create `src/routes/staff/customers/+page.svelte`
- [ ] Create `src/routes/staff/customers/[id]/+page.svelte`
- [ ] Verify: customer list renders, search filters, photo tab shows pending; detail page shows edit form for admin, read-only for employee

---

## Task 7: Analytics Page (Admin Only)

**Dependencies:** Install Chart.js before starting this task:
```bash
npm install chart.js
```

**Files:**
- Create: `src/lib/services/analytics.js`
- Create: `src/routes/staff/analytics/+page.svelte`

### analytics.js

```javascript
const API = '/api/v1/admin/analytics';

function dateParam(d) {
    return d.toISOString().split('T')[0];
}

function buildUrl(path, start, end, bakery) {
    const params = new URLSearchParams({
        start: dateParam(start),
        end: dateParam(end)
    });
    if (bakery) params.set('bakerySelection', bakery);
    return `${API}${path}?${params}`;
}

async function get(url) {
    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) throw new Error('Analytics request failed');
    return res.json();
}

export async function getBakeryNames() {
    return get(`${API}/meta/bakery-names`);
}

export async function getTotalRevenue(start, end, bakery) {
    return get(buildUrl('/metrics/total-revenue', start, end, bakery));
}

export async function getAverageOrderValue(start, end, bakery) {
    return get(buildUrl('/metrics/average-order-value', start, end, bakery));
}

export async function getCompletionRate(start, end, bakery) {
    return get(buildUrl('/metrics/completion-rate', start, end, bakery));
}

export async function getRevenueOverTime(start, end, bakery) {
    return get(buildUrl('/revenue-over-time', start, end, bakery));
}

export async function getRevenueByBakery(start, end) {
    return get(buildUrl('/revenue-by-bakery', start, end, null));
}

export async function getTopProducts(start, end, bakery) {
    return get(buildUrl('/series/top-products', start, end, bakery));
}

export async function getSalesByEmployee(start, end, bakery) {
    return get(buildUrl('/series/sales-by-employee', start, end, bakery));
}
```

### analytics/+page.svelte

Date range defaulting to last 30 days. Bakery selector from `/meta/bakery-names`. Charts use Chart.js via canvas elements. Admin-only: employees are redirected to dashboard on mount.

```svelte
<script>
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { user } from '$lib/stores/authStore';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import KpiCard from '$lib/components/staff/KpiCard.svelte';
    import {
        getBakeryNames,
        getTotalRevenue,
        getAverageOrderValue,
        getCompletionRate,
        getRevenueOverTime,
        getRevenueByBakery,
        getTopProducts,
        getSalesByEmployee
    } from '$lib/services/analytics.js';

    // Admin guard
    if (typeof window !== 'undefined' && $user?.role !== 'admin') {
        goto(resolve('/staff/dashboard'), { replaceState: true });
    }

    const today = new Date();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(today.getDate() - 30);

    let startDate = $state(thirtyDaysAgo.toISOString().split('T')[0]);
    let endDate = $state(today.toISOString().split('T')[0]);
    let selectedBakery = $state('');
    let bakeryNames = $state([]);

    let metrics = $state({ revenue: null, aov: null, completionRate: null });
    let revenueOverTime = $state([]);
    let revenueByBakery = $state([]);
    let topProducts = $state([]);
    let salesByEmployee = $state([]);

    let loading = $state(true);
    let error = $state(null);

    // Chart canvas refs
    let revenueCanvas;
    let productsCanvas;
    let employeeCanvas;
    let bakeryCanvas;

    // Chart instances (for destroy/recreate on data change)
    let revenueChart;
    let productsChart;
    let employeeChart;
    let bakeryChart;

    onMount(async () => {
        const { Chart, registerables } = await import('chart.js');
        Chart.register(...registerables);

        await loadBakeries();
        await loadData();

        function makeChart(canvas, type, labels, data, label, color) {
            return new Chart(canvas, {
                type,
                data: {
                    labels,
                    datasets: [{
                        label,
                        data,
                        backgroundColor: color + '33',
                        borderColor: color,
                        borderWidth: 2,
                        tension: 0.3,
                        fill: type === 'line'
                    }]
                },
                options: {
                    responsive: true,
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: true } }
                }
            });
        }

        revenueChart = makeChart(
            revenueCanvas, 'line',
            revenueOverTime.map((d) => d.label),
            revenueOverTime.map((d) => d.value),
            'Revenue', '#C4714A'
        );
        productsChart = makeChart(
            productsCanvas, 'bar',
            topProducts.map((d) => d.label),
            topProducts.map((d) => d.value),
            'Units Sold', '#8A9E7F'
        );
        employeeChart = makeChart(
            employeeCanvas, 'bar',
            salesByEmployee.map((d) => d.label),
            salesByEmployee.map((d) => d.value),
            'Sales ($)', '#2C1A0E'
        );
        bakeryChart = makeChart(
            bakeryCanvas, 'bar',
            revenueByBakery.map((d) => d.label),
            revenueByBakery.map((d) => d.value),
            'Revenue ($)', '#C4714A'
        );

        return () => {
            revenueChart?.destroy();
            productsChart?.destroy();
            employeeChart?.destroy();
            bakeryChart?.destroy();
        };
    });

    async function loadBakeries() {
        bakeryNames = await getBakeryNames();
    }

    async function loadData() {
        loading = true;
        error = null;
        const start = new Date(startDate);
        const end = new Date(endDate);
        const bakery = selectedBakery || undefined;
        try {
            [
                metrics.revenue,
                metrics.aov,
                metrics.completionRate,
                revenueOverTime,
                revenueByBakery,
                topProducts,
                salesByEmployee
            ] = await Promise.all([
                getTotalRevenue(start, end, bakery),
                getAverageOrderValue(start, end, bakery),
                getCompletionRate(start, end, bakery),
                getRevenueOverTime(start, end, bakery),
                getRevenueByBakery(start, end),
                getTopProducts(start, end, bakery),
                getSalesByEmployee(start, end, bakery)
            ]);
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    }

    function formatCurrency(val) {
        if (val == null) return '—';
        return `$${Number(val).toFixed(2)}`;
    }

    function formatPercent(val) {
        if (val == null) return '—';
        return `${(Number(val) * 100).toFixed(1)}%`;
    }
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
    <div class="mx-auto max-w-5xl space-y-8">
        <div>
            <h1 class="text-2xl font-bold tracking-tight text-foreground">Analytics</h1>
            <p class="mt-1 text-sm text-muted-foreground">Revenue and performance metrics</p>
        </div>

        <!-- Filters -->
        <div class="flex flex-wrap gap-3">
            <div class="flex items-center gap-2">
                <label class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">From</label>
                <input
                    type="date"
                    bind:value={startDate}
                    class="rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground"
                />
            </div>
            <div class="flex items-center gap-2">
                <label class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">To</label>
                <input
                    type="date"
                    bind:value={endDate}
                    class="rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground"
                />
            </div>
            <select
                bind:value={selectedBakery}
                class="rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground"
            >
                <option value="">All Bakeries</option>
                {#each bakeryNames as name (name)}
                    <option value={name}>{name}</option>
                {/each}
            </select>
            <button
                onclick={loadData}
                class="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
            >
                Apply
            </button>
        </div>

        {#if loading}
            <div class="grid grid-cols-3 gap-4">
                {#each Array(3) as _, i (i)}<Skeleton class="h-28 rounded-xl" />{/each}
            </div>
            <Skeleton class="h-64 rounded-xl" />
        {:else if error}
            <p class="text-sm text-destructive">Failed to load analytics.</p>
        {:else}
            <!-- KPI row -->
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <KpiCard label="Total Revenue" value={formatCurrency(metrics.revenue)} />
                <KpiCard label="Avg Order Value" value={formatCurrency(metrics.aov)} />
                <KpiCard label="Completion Rate" value={formatPercent(metrics.completionRate)} />
            </div>

            <!-- Charts -->
            <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
                <div class="rounded-xl border border-border bg-card p-5">
                    <p class="mb-4 text-sm font-semibold text-foreground">Revenue Over Time</p>
                    <canvas bind:this={revenueCanvas}></canvas>
                </div>
                <div class="rounded-xl border border-border bg-card p-5">
                    <p class="mb-4 text-sm font-semibold text-foreground">Revenue by Bakery</p>
                    <canvas bind:this={bakeryCanvas}></canvas>
                </div>
                <div class="rounded-xl border border-border bg-card p-5">
                    <p class="mb-4 text-sm font-semibold text-foreground">Top Products</p>
                    <canvas bind:this={productsCanvas}></canvas>
                </div>
                <div class="rounded-xl border border-border bg-card p-5">
                    <p class="mb-4 text-sm font-semibold text-foreground">Sales by Employee</p>
                    <canvas bind:this={employeeCanvas}></canvas>
                </div>
            </div>
        {/if}
    </div>
</main>
```

- [ ] Run `npm install chart.js` in `Workshop7/apps/frontend/`
- [ ] Create `src/lib/services/analytics.js`
- [ ] Create `src/routes/staff/analytics/+page.svelte`
- [ ] Verify: admin sees 3 KPI cards and 4 charts; employee navigating to `/staff/analytics` is redirected to `/staff/dashboard`

---

## Task 8: Employee Management (Admin Only)

**Files:**
- Create: `src/lib/services/staff-employees.js`
- Create: `src/routes/staff/staff/+page.svelte`

### staff-employees.js

```javascript
const API = '/api/v1';

export async function listStaff() {
    const res = await fetch(`${API}/employee/staff`, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch staff');
    return res.json();
}

export async function createEmployee(data) {
    const res = await fetch(`${API}/admin/employees`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Failed to create employee');
    return res.json();
}

export async function updateEmployee(id, data) {
    const res = await fetch(`${API}/admin/employees/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Failed to update employee');
    return res.json();
}

export async function deleteEmployee(id) {
    const res = await fetch(`${API}/admin/employees/${id}`, {
        method: 'DELETE',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to delete employee');
}
```

### staff/+page.svelte

List of employees with position and bakery. Admin-only: redirect employee role to dashboard on mount.

```svelte
<script>
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { user } from '$lib/stores/authStore';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import { Button } from '$lib/components/ui/button';
    import { listStaff, deleteEmployee } from '$lib/services/staff-employees.js';

    onMount(async () => {
        if ($user?.role !== 'admin') {
            goto(resolve('/staff/dashboard'), { replaceState: true });
            return;
        }
        try {
            employees = await listStaff();
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    });

    let employees = $state([]);
    let loading = $state(true);
    let error = $state(null);
    let deleting = $state({});

    async function handleDelete(id) {
        if (!confirm('Delete this employee? This cannot be undone.')) return;
        deleting[id] = true;
        try {
            await deleteEmployee(id);
            employees = employees.filter((e) => e.id !== id);
        } catch {
            // show nothing — employee stays in list
        } finally {
            deleting[id] = false;
        }
    }
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
    <div class="mx-auto max-w-5xl space-y-6">
        <div class="flex items-center justify-between">
            <div>
                <h1 class="text-2xl font-bold tracking-tight text-foreground">Employees</h1>
                <p class="mt-1 text-sm text-muted-foreground">Manage staff accounts</p>
            </div>
        </div>

        {#if loading}
            <div class="space-y-3">
                {#each Array(4) as _, i (i)}<Skeleton class="h-16 rounded-xl" />{/each}
            </div>
        {:else if error}
            <p class="text-sm text-destructive">Failed to load employees.</p>
        {:else if employees.length === 0}
            <p class="text-sm text-muted-foreground">No employees found.</p>
        {:else}
            <div class="rounded-xl border border-border bg-card">
                <div class="divide-y divide-border">
                    {#each employees as emp (emp.id)}
                        <div class="flex items-center justify-between px-5 py-3">
                            <div>
                                <p class="text-sm font-medium text-foreground">
                                    {emp.firstName} {emp.lastName}
                                </p>
                                <p class="text-xs text-muted-foreground">
                                    {emp.position ?? '—'} · {emp.workEmail ?? '—'}
                                </p>
                            </div>
                            <Button
                                size="sm"
                                variant="destructive"
                                onclick={() => handleDelete(emp.id)}
                                disabled={!!deleting[emp.id]}
                            >
                                {deleting[emp.id] ? '...' : 'Remove'}
                            </Button>
                        </div>
                    {/each}
                </div>
            </div>
        {/if}
    </div>
</main>
```

> **Note:** Employee creation (`POST /admin/employees`) requires an existing `userId` (UUID of a user account), `bakeryId`, `addressId`, and full contact info. Creating employees via the web portal would require a separate user-creation flow. This page covers listing and deletion only; creation is handled via the desktop app (Workshop5) or directly through the backend.

- [ ] Create `src/lib/services/staff-employees.js`
- [ ] Create `src/routes/staff/staff/+page.svelte`
- [ ] Verify: employee list renders for admin; employee role is redirected to dashboard

---

## Task 9: Products Management (Admin Only)

**Files:**
- Create: `src/lib/services/staff-products.js`
- Create: `src/routes/staff/products/+page.svelte`

### staff-products.js

```javascript
const API = '/api/v1/products';

export async function listProducts() {
    const res = await fetch(API, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch products');
    return res.json();
}

export async function createProduct(data) {
    const res = await fetch(API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Failed to create product');
    return res.json();
}

export async function updateProduct(id, data) {
    const res = await fetch(`${API}/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('Failed to update product');
    return res.json();
}

export async function deleteProduct(id) {
    const res = await fetch(`${API}/${id}`, {
        method: 'DELETE',
        credentials: 'include'
    });
    if (!res.ok) throw new Error('Failed to delete product');
}
```

### products/+page.svelte

Product list with inline create form and delete. Edit opens an inline form per row.

```svelte
<script>
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { user } from '$lib/stores/authStore';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import { Button } from '$lib/components/ui/button';
    import { Input } from '$lib/components/ui/input';
    import { listProducts, createProduct, updateProduct, deleteProduct } from '$lib/services/staff-products.js';

    onMount(async () => {
        if ($user?.role !== 'admin') {
            goto(resolve('/staff/dashboard'), { replaceState: true });
            return;
        }
        try {
            products = await listProducts();
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    });

    let products = $state([]);
    let loading = $state(true);
    let error = $state(null);
    let editingId = $state(null);
    let deleting = $state({});
    let showCreate = $state(false);
    let saving = $state(false);

    let editDraft = $state({ name: '', description: '', basePrice: '' });
    let createDraft = $state({ name: '', description: '', basePrice: '' });

    function startEdit(product) {
        editingId = product.id;
        editDraft = { name: product.name, description: product.description ?? '', basePrice: String(product.basePrice) };
    }

    async function handleUpdate(id) {
        saving = true;
        try {
            const updated = await updateProduct(id, {
                name: editDraft.name,
                description: editDraft.description,
                basePrice: parseFloat(editDraft.basePrice)
            });
            products = products.map((p) => (p.id === id ? updated : p));
            editingId = null;
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
                basePrice: parseFloat(createDraft.basePrice)
            });
            products = [created, ...products];
            showCreate = false;
            createDraft = { name: '', description: '', basePrice: '' };
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
                class="rounded-xl border border-border bg-card p-5 space-y-3"
                onsubmit={(e) => { e.preventDefault(); handleCreate(); }}
            >
                <p class="text-sm font-semibold text-foreground">New Product</p>
                <div class="grid grid-cols-2 gap-3">
                    <Input bind:value={createDraft.name} placeholder="Name" required />
                    <Input bind:value={createDraft.basePrice} placeholder="Price (e.g. 4.99)" type="number" step="0.01" min="0" required />
                </div>
                <Input bind:value={createDraft.description} placeholder="Description (optional)" />
                <Button type="submit" size="sm" disabled={saving}>{saving ? 'Creating...' : 'Create'}</Button>
            </form>
        {/if}

        {#if loading}
            <div class="space-y-3">
                {#each Array(5) as _, i (i)}<Skeleton class="h-16 rounded-xl" />{/each}
            </div>
        {:else if error}
            <p class="text-sm text-destructive">Failed to load products.</p>
        {:else}
            <div class="rounded-xl border border-border bg-card">
                <div class="divide-y divide-border">
                    {#each products as product (product.id)}
                        <div class="px-5 py-3">
                            {#if editingId === product.id}
                                <form
                                    class="space-y-2"
                                    onsubmit={(e) => { e.preventDefault(); handleUpdate(product.id); }}
                                >
                                    <div class="grid grid-cols-2 gap-2">
                                        <Input bind:value={editDraft.name} placeholder="Name" />
                                        <Input bind:value={editDraft.basePrice} placeholder="Price" type="number" step="0.01" min="0" />
                                    </div>
                                    <Input bind:value={editDraft.description} placeholder="Description" />
                                    <div class="flex gap-2">
                                        <Button type="submit" size="sm" disabled={saving}>{saving ? '...' : 'Save'}</Button>
                                        <Button type="button" size="sm" variant="ghost" onclick={() => (editingId = null)}>Cancel</Button>
                                    </div>
                                </form>
                            {:else}
                                <div class="flex items-center justify-between">
                                    <div>
                                        <p class="text-sm font-medium text-foreground">{product.name}</p>
                                        <p class="text-xs text-muted-foreground">
                                            ${Number(product.basePrice).toFixed(2)}
                                            {#if product.description}· {product.description.slice(0, 60)}{product.description.length > 60 ? '…' : ''}{/if}
                                        </p>
                                    </div>
                                    <div class="flex gap-2">
                                        <Button size="sm" variant="outline" onclick={() => startEdit(product)}>Edit</Button>
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
```

- [ ] Create `src/lib/services/staff-products.js`
- [ ] Create `src/routes/staff/products/+page.svelte`
- [ ] Verify: product list renders; "New Product" form creates a product; Edit updates inline; Delete removes from list

---

## Task 10: Users Page (Admin Only)

**Files:**
- Create: `src/lib/services/staff-users.js`
- Create: `src/routes/staff/users/+page.svelte`

### staff-users.js

```javascript
const API = '/api/v1/admin/users';

export async function listUsers() {
    const res = await fetch(API, { credentials: 'include' });
    if (!res.ok) throw new Error('Failed to fetch users');
    return res.json();
}

export async function setUserActive(id, active) {
    const res = await fetch(`${API}/${id}/active`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ active })
    });
    if (!res.ok) throw new Error('Failed to update user');
    return res.json();
}
```

### users/+page.svelte

```svelte
<script>
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { user } from '$lib/stores/authStore';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import { Badge } from '$lib/components/ui/badge';
    import { Button } from '$lib/components/ui/button';
    import { listUsers, setUserActive } from '$lib/services/staff-users.js';

    onMount(async () => {
        if ($user?.role !== 'admin') {
            goto(resolve('/staff/dashboard'), { replaceState: true });
            return;
        }
        try {
            users = await listUsers();
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    });

    let users = $state([]);
    let loading = $state(true);
    let error = $state(null);
    let toggling = $state({});

    async function handleToggle(u) {
        toggling[u.id] = true;
        try {
            const updated = await setUserActive(u.id, !u.active);
            users = users.map((x) => (x.id === u.id ? updated : x));
        } finally {
            toggling[u.id] = false;
        }
    }
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
    <div class="mx-auto max-w-4xl space-y-6">
        <div>
            <h1 class="text-2xl font-bold tracking-tight text-foreground">Users</h1>
            <p class="mt-1 text-sm text-muted-foreground">All registered accounts — enable or disable access</p>
        </div>

        {#if loading}
            <div class="space-y-3">
                {#each Array(6) as _, i (i)}<Skeleton class="h-14 rounded-xl" />{/each}
            </div>
        {:else if error}
            <p class="text-sm text-destructive">Failed to load users.</p>
        {:else}
            <div class="rounded-xl border border-border bg-card">
                <div class="divide-y divide-border">
                    {#each users as u (u.id)}
                        <div class="flex items-center justify-between px-5 py-3">
                            <div>
                                <p class="text-sm font-medium text-foreground">{u.username}</p>
                                <p class="text-xs text-muted-foreground">{u.email} · <span class="capitalize">{u.role}</span></p>
                            </div>
                            <div class="flex items-center gap-3">
                                <Badge variant={u.active ? 'default' : 'secondary'}>
                                    {u.active ? 'Active' : 'Disabled'}
                                </Badge>
                                <Button
                                    size="sm"
                                    variant={u.active ? 'destructive' : 'outline'}
                                    onclick={() => handleToggle(u)}
                                    disabled={!!toggling[u.id]}
                                >
                                    {toggling[u.id] ? '...' : u.active ? 'Disable' : 'Enable'}
                                </Button>
                            </div>
                        </div>
                    {/each}
                </div>
            </div>
        {/if}
    </div>
</main>
```

- [ ] Create `src/lib/services/staff-users.js`
- [ ] Create `src/routes/staff/users/+page.svelte`
- [ ] Verify: user list renders; toggling active state updates the badge; employee is redirected to dashboard

---

## Task 11: Staff Profile Page

**Files:**
- Create: `src/routes/staff/profile/+page.svelte`

Uses existing `src/lib/services/profile.js` — `getProfile()` already calls `GET /employee/me` for admin/employee roles and `patchProfile()` → `PATCH /employee/me`.

```svelte
<script>
    import { onMount } from 'svelte';
    import { Skeleton } from '$lib/components/ui/skeleton';
    import { Button } from '$lib/components/ui/button';
    import { Input } from '$lib/components/ui/input';
    import { Avatar, AvatarFallback } from '$lib/components/ui/avatar';
    import { Separator } from '$lib/components/ui/separator';
    import { getProfile } from '$lib/services/profile.js';

    let profile = $state(null);
    let loading = $state(true);
    let error = $state(null);
    let editing = $state(false);
    let saving = $state(false);
    let draft = $state({});

    const initials = $derived(
        profile ? `${profile.firstName?.[0] ?? ''}${profile.lastName?.[0] ?? ''}`.toUpperCase() : '?'
    );

    onMount(async () => {
        try {
            profile = await getProfile();
            resetDraft();
        } catch {
            error = true;
        } finally {
            loading = false;
        }
    });

    function resetDraft() {
        draft = {
            firstName: profile.firstName ?? '',
            lastName: profile.lastName ?? '',
            phone: profile.phone ?? '',
            workEmail: profile.workEmail ?? ''
        };
    }

    async function handleSave() {
        saving = true;
        try {
            const res = await fetch('/api/v1/employee/me', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify(draft)
            });
            if (!res.ok) throw new Error('Failed to save');
            profile = await res.json();
            editing = false;
        } finally {
            saving = false;
        }
    }
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
    <div class="mx-auto max-w-3xl space-y-6">
        <div>
            <h1 class="text-2xl font-bold tracking-tight text-foreground">My Profile</h1>
            <p class="mt-1 text-sm text-muted-foreground">Your staff account details</p>
        </div>

        {#if loading}
            <Skeleton class="h-40 rounded-xl" />
        {:else if error}
            <p class="text-sm text-destructive">Failed to load profile.</p>
        {:else}
            <div class="rounded-xl border border-border bg-card p-6 space-y-6">
                <div class="flex items-center gap-5">
                    <Avatar class="h-16 w-16">
                        <AvatarFallback class="bg-primary text-xl font-bold text-primary-foreground">
                            {initials}
                        </AvatarFallback>
                    </Avatar>
                    <div>
                        <p class="text-xl font-bold text-foreground">
                            {profile.firstName} {profile.lastName}
                        </p>
                        <p class="text-sm text-muted-foreground capitalize">{profile.position ?? '—'}</p>
                    </div>
                </div>

                <Separator />

                {#if editing}
                    <form class="space-y-3" onsubmit={(e) => { e.preventDefault(); handleSave(); }}>
                        <div class="grid grid-cols-2 gap-3">
                            <Input bind:value={draft.firstName} placeholder="First name" />
                            <Input bind:value={draft.lastName} placeholder="Last name" />
                            <Input bind:value={draft.phone} placeholder="Phone" />
                            <Input bind:value={draft.workEmail} placeholder="Work email" type="email" />
                        </div>
                        <div class="flex gap-2">
                            <Button type="submit" size="sm" disabled={saving}>{saving ? 'Saving...' : 'Save'}</Button>
                            <Button type="button" size="sm" variant="ghost" onclick={() => { editing = false; resetDraft(); }}>
                                Cancel
                            </Button>
                        </div>
                    </form>
                {:else}
                    <dl class="grid grid-cols-2 gap-4 text-sm">
                        <div>
                            <dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Work Email</dt>
                            <dd class="mt-1 text-foreground">{profile.workEmail ?? '—'}</dd>
                        </div>
                        <div>
                            <dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Phone</dt>
                            <dd class="mt-1 text-foreground">{profile.phone ?? '—'}</dd>
                        </div>
                        <div>
                            <dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Position</dt>
                            <dd class="mt-1 text-foreground">{profile.position ?? '—'}</dd>
                        </div>
                    </dl>
                    <Button size="sm" variant="outline" onclick={() => (editing = true)}>Edit</Button>
                {/if}
            </div>
        {/if}
    </div>
</main>
```

- [ ] Create `src/routes/staff/profile/+page.svelte`
- [ ] Verify: profile renders with employee name and position; edit form saves and reflects changes

---

## Verification: End-to-End

1. Log in as **admin** (`alicia.nguyen` / `Admin123!` per seed data)
2. Confirm "Staff Portal" appears in Navbar
3. Navigate to `/staff` → redirects to `/staff/dashboard`
4. Confirm KPI cards show non-zero data
5. Navigate to `/staff/orders` → recent orders list with status buttons
6. Navigate to `/staff/reviews` → pending review queue
7. Navigate to `/staff/customers` → searchable list; Pending Photos tab
8. Navigate to `/staff/analytics` → date filters + 4 charts
9. Navigate to `/staff/products` → list with create/edit/delete
10. Navigate to `/staff/staff` → employee list
11. Navigate to `/staff/users` → user list with enable/disable
12. Navigate to `/staff/profile` → employee profile edit
13. Log out, log in as **employee** (`mason.clark` / `Emp123!`)
14. Confirm "Staff Portal" appears
15. Confirm `/staff/analytics` redirects to `/staff/dashboard`
16. Confirm `/staff/products` redirects to `/staff/dashboard`
17. Confirm `/staff/staff` redirects to `/staff/dashboard`
18. Confirm `/staff/users` redirects to `/staff/dashboard`
19. Log in as **customer** → confirm "Staff Portal" does NOT appear in Navbar
20. Confirm `/staff/dashboard` redirects to `/login?redirectTo=/staff/dashboard`

---

## Known Limitations / Future Work

- **Order queue is limited** to the recent orders returned by the dashboard summary. A dedicated `GET /api/v1/admin/orders` endpoint with filtering/pagination does not exist yet and would need to be added to the backend for a proper live order queue.
- **Employee creation** via the web requires an existing user account UUID — the creation form is not planned here as it needs a user-creation flow first. Deletion is included.
- **Product images** — `POST /api/v1/products/{id}/image` (multipart upload) is not covered in this plan. Image URLs must be set manually via the `imageUrl` field for now.
- **Inventory/batch viewing** — `GET /bakeries/{bakeryId}/batches` is available but not included in this plan. Could be added as a `/staff/inventory` page if needed.
