# UI Redesign — Modern, Sleek, Minimalist

**Date:** 2026-06-27  
**Scope:** CSS-only overhaul of `styles.css`. No FXML structural changes.  
**Goal:** Make ImageSorterPro feel like a native macOS app — clean, fast, and focused on the image.

---

## Approach

Option A: Refined Evolution. Replace the Bootstrap-era color palette with a macOS-native system palette. Remove hard borders; separate panels through subtle background tone differences. Tighten spacing to an 8px grid. Switch font to SF Pro (via `-apple-system`). Blue accent `#3B82F6`. Fully system-adaptive (light + dark mode).

---

## 1. Color Palette

All colors defined as CSS variables on `.root` with separate dark-mode overrides via `@media (prefers-color-scheme: dark)`.

| Token | Light | Dark |
|---|---|---|
| `--canvas` | `#F5F5F7` | `#1C1C1E` |
| `--surface` | `#FFFFFF` | `#2C2C2E` |
| `--surface-raised` | `#FFFFFF` | `#3A3A3C` |
| `--border` | `#E5E5EA` | `#3A3A3C` |
| `--text-primary` | `#1D1D1F` | `#F5F5F7` |
| `--text-secondary` | `#6E6E73` | `#98989D` |
| `--text-muted` | `#AEAEB2` | `#636366` |
| `--accent` | `#3B82F6` | `#3B82F6` |
| `--accent-hover` | `#2563EB` | `#60A5FA` |
| `--accent-pressed` | `#1D4ED8` | `#3B82F6` |
| `--accent-surface` | `#EFF6FF` | `#1E3A5F` |
| `--accent-surface-strong` | `#DBEAFE` | `#1E40AF` |
| `--danger` | `#EF4444` | `#F87171` |
| `--success` | `#22C55E` | `#4ADE80` |

---

## 2. Typography & Spacing

**Font stack:** `-apple-system, "SF Pro Text", "Helvetica Neue", Arial, sans-serif`  
Renders as SF Pro on macOS — gives native feel at zero cost.

| Role | Size | Weight | Color |
|---|---|---|---|
| Section title | 13px | 600 | `--text-primary` |
| Body / list items | 13px | 400 | `--text-primary` |
| Metadata key | 12px | 500 | `--text-secondary` |
| Metadata value | 12px | 400 | `--text-primary` |
| Status bar | 11px | 400 | `--text-secondary` |
| Help text / muted | 11px | 400 | `--text-muted` |
| Dialog title | 15px | 600 | `--text-primary` |
| Dialog description | 12px | 400 | `--text-secondary` |

**Spacing grid:** All padding/gap values are multiples of 4px.  
**Panel internal padding:** `12px`  
**List cell padding:** `6px 10px`  
**Border radius:** `6px` for controls, `5px` for thumbnails, `3px` for badges

---

## 3. Panel Surfaces & Dividers

**Root / canvas:** Background `--canvas` (`#F5F5F7` / `#1C1C1E`).

**Left panel (hotkey list):**
- Background: `--surface`
- No border on the list itself — SplitPane divider only
- SplitPane divider: `1px --border`, no grabber chrome

**Center panel (image viewer):**
- Background: `--canvas` (recessed vs panels)
- `ScrollPane` border removed entirely
- Image sits cleanly in viewport; `dropshadow` on `.main-image` kept but softened to `rgba(0,0,0,0.08)`

**Right panel (metadata):**
- Background: `--surface`
- Metadata rows: alternating tint `transparent` / `rgba(0,0,0,0.02)` in light, `rgba(255,255,255,0.02)` in dark

**Thumbnail bar:**
- Background: `--surface`
- Bottom border: `1px --border` only (no top border)
- Thumbnail images: no border, `5px` radius, shadow `rgba(0,0,0,0.08)`
- Selected thumbnail: `2px solid --accent`, shadow `rgba(59,130,246,0.25)`

**Menu bar:**
- Background: `--surface`
- Bottom border: `1px --border`

**Status bar:**
- Top border: `1px --border` only
- Reduced vertical padding (`4px` top/bottom)
- All text in `--text-secondary`

---

## 4. Buttons & Interactive Controls

**Primary button** (Save, Commit Staged):
- Fill: `--accent`, text: `#FFFFFF`, radius: `6px`, padding: `7px 14px`
- Hover: `--accent-hover`
- Pressed: `--accent-pressed`
- No border, no shadow

**Secondary / default button** (Cancel, Clear Batch, Browse):
- Fill: `transparent`, border: `1px --border`, text: `--text-primary`
- Hover: fill `--canvas`
- Replaces current heavy gray fill — all Browse buttons also use this style (green `#28a745` removed)

**Text fields / inputs:**
- Border: `1px --border`, radius: `6px`, padding: `7px 10px`
- Focus: border `--accent`, no glow effect
- Background: `--surface`

**Checkboxes:**
- Label text: `--text-secondary` (de-emphasised)

**Pin button (📌):**
- Idle: opacity `0.3`, background `transparent`
- Hover: opacity `0.7`, background `rgba(0,0,0,0.06)`
- Selected: opacity `1.0`, background `rgba(59,130,246,0.12)`

**Hotkey list cells:**
- Idle: `transparent`, `--text-primary`
- Hover: `--accent-surface`
- Selected: `--accent-surface-strong`, text `#1D4ED8` / `#93C5FD`, weight `500`

**Scrollbars (all):**
- Width: `4px`
- Track: `transparent`
- Thumb: `rgba(0,0,0,0.2)` light / `rgba(255,255,255,0.2)` dark
- Thumb hover: `rgba(0,0,0,0.35)` / `rgba(255,255,255,0.35)`
- Visible only on hover (opacity transition)

**Progress bar:**
- Accent: `--accent`
- Track: `--border`
- Radius: `2px`

---

## 5. Dialogs & Alerts

**Configure Folders / Rules:**
- Background: `--surface-raised`
- Title: 15px 600 `--text-primary`
- Description: 12px `--text-secondary`
- Tabs: bottom-border `2px --accent` on active, inactive text `--text-muted`, no background fill
- Input row gap: `6px`
- Label column: `--text-secondary` 12px 500

**Tooltips:**
- Background: `rgba(29,29,31,0.92)` light / `rgba(58,58,60,0.95)` dark
- Radius: `6px`, padding: `7px 10px`, font: `11px`

**Alert dialogs:**
- Header: `--surface` background, `1px --border` bottom only
- Header label: 13px 600

---

## Out of Scope

- No FXML structural changes
- No new panels, controls, or features
- No third-party theme library changes (AtlantaFX themes remain selectable via the Theme menu; `styles.css` is always loaded as the base layer and AtlantaFX themes override it — this redesign only affects the base layer)
- No icon set changes

---

## Files Changed

| File | Change |
|---|---|
| `src/main/resources/com/imagesorter/css/styles.css` | Full replacement |
