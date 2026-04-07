# Locations Tab Redesign — Mobile

**Platform:** Android (Workshop06, Kotlin)  
**Status:** Planning

---

## What it does

Replace the current map-centric locations tab with a list/card layout. Add a Directions button directly on each location card. Improve the bakery search to filter on more than just name.

Covers two meeting items:
- "Locations tab changed from map"
- "Directions button directly on the locations"
- "Search field for bakeries changed to encompass more criteria"

---

## Scope

- Replace map view with a scrollable list of bakery cards as the default view
- Optional: keep map as a secondary toggle ("List | Map")
- Each card shows: name, address, phone, hours (today's), status (open/closed)
- "Directions" button on each card — deep links to Google Maps or device default navigation app
- Search/filter bar at top — filters on name, city, postal code, and open-now status

---

## Backend changes

`GET /api/v1/bakeries?search=` currently only filters by name. Extend to support:
- `?search=` matches name, city, or postal code (service-layer change in `BakeryService.list()`)
- `?openNow=true` filter (requires comparing current time against `BakeryHour` records)

No new endpoints needed — extend existing query params.

---

## Android changes

**UI:**
- Replace `MapFragment` (or equivalent) with `LocationsFragment` using a `RecyclerView`
- `BakeryCardViewHolder` — name, address, open/closed badge, today's hours, phone, Directions button
- Search bar with debounce — calls `GET /api/v1/bakeries?search=` on input change
- Optional "Open Now" chip filter

**Directions button:**
- Construct `geo:lat,lng?q=address` URI or `google.navigation:q=lat,lng`
- `startActivity(Intent(Intent.ACTION_VIEW, uri))` — falls back to any navigation app

**ViewModel:**
- `LocationsViewModel` — holds bakery list, search query state, loading/error state

---

## Open questions

- Keep map view at all, or full replacement?
- Should "open now" filtering be done client-side (from hours data already fetched) or server-side?
