# UI Redesign — Modern, Sleek, Minimalist — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `styles.css` with a macOS-native, system-adaptive palette — clean surfaces, blue accent `#3B82F6`, SF Pro font, ghost scrollbars, flat buttons.

**Architecture:** Two CSS files — `styles.css` (complete light mode base) and `styles-dark.css` (dark mode overrides only). A small Java utility detects macOS dark mode at startup and conditionally adds `styles-dark.css` to the scene's stylesheet list. This is required because JavaFX CSS does **not** support `@media (prefers-color-scheme: dark)` — that construct is silently ignored by the JavaFX CSS engine.

**Tech Stack:** JavaFX 21 CSS, Java, Maven (`mvn javafx:run` to launch). CSS files live at `src/main/resources/com/imagesorter/css/`.

## Global Constraints

- JavaFX CSS does NOT support `@media` queries — never use them; use the two-file approach described above
- No FXML structural changes — only CSS files and the OS-theme-detection Java code are modified
- All CSS class names and `fx:id` selectors must remain identical to the originals (no renames)
- `styles.css` is always loaded first; `styles-dark.css` is loaded on top when dark mode is detected
- AtlantaFX themes (selectable via the Theme menu) override both CSS files — this base layer must look correct when no AtlantaFX theme is active
- Run command: `mvn javafx:run` from the project root
- macOS only — font stack targets `-apple-system` / SF Pro; dark mode detection uses `defaults read -g AppleInterfaceStyle`

---

## File Map

| File | Action |
|---|---|
| `src/main/resources/com/imagesorter/css/styles.css` | Full replacement — light mode only |
| `src/main/resources/com/imagesorter/css/styles-dark.css` | New file — dark mode overrides only |
| Java file that creates the `Scene` and loads stylesheets | Add `OsTheme.isDark()` check + conditional `styles-dark.css` load |

> **Find the Scene-creation file:** Before starting Task 9, run `grep -rn "getStylesheets\|\.css\|new Scene" src/main/java` to find the exact file and line where stylesheets are added. That is the file to modify in Task 9.

---

### Task 1: Root, Canvas & Typography

**Files:**
- Modify: `src/main/resources/com/imagesorter/css/styles.css` (replace `.root` block only)

**What this produces:** The app background shifts to `#F5F5F7` and the font switches to SF Pro. All other rules are unchanged from the original file until later tasks.

- [ ] **Step 1: Replace the `.root` block**

Open `src/main/resources/com/imagesorter/css/styles.css`. Replace the entire `.root` block (currently lines 1–6) with:

```css
/* ── Root & Typography ─────────────────────────────────────── */
.root {
    -fx-font-family: "-apple-system", "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
    -fx-font-size: 13px;
    -fx-background-color: #F5F5F7;
}
```

- [ ] **Step 2: Remove the existing `@media` block**

Delete the entire `@media (prefers-color-scheme: dark) { ... }` block at the bottom of `styles.css`. It is dead code — JavaFX ignores it. Dark mode will be handled by `styles-dark.css` (added in Task 8).

- [ ] **Step 3: Run and verify**

```bash
mvn javafx:run
```

Expected: App launches. Background is `#F5F5F7` (slightly cool light gray). Font renders as SF Pro on macOS. No layout breakage.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/com/imagesorter/css/styles.css
git commit -m "style: update root canvas color and switch to SF Pro font stack"
```

---

### Task 2: Menu Bar

**Files:**
- Modify: `src/main/resources/com/imagesorter/css/styles.css` (menu bar section)

**What this produces:** Menu bar uses pure white with a single `#E5E5EA` bottom divider. Menu item hover is a blue tint instead of gray.

- [ ] **Step 1: Replace the menu bar section**

Replace the entire `/* Menu Bar */` section with:

```css
/* ── Menu Bar ──────────────────────────────────────────────── */
.menu-bar {
    -fx-background-color: #FFFFFF;
    -fx-border-width: 0 0 1 0;
    -fx-border-color: #E5E5EA;
}

.menu-bar .menu .label {
    -fx-text-fill: #1D1D1F;
    -fx-font-size: 13px;
}

.menu-bar .menu:hover {
    -fx-background-color: #F5F5F7;
}

.menu-bar .menu:hover .label,
.menu-bar .menu:showing .label {
    -fx-text-fill: #1D1D1F;
}

.menu-item .label {
    -fx-text-fill: #1D1D1F;
    -fx-font-size: 13px;
}

.menu-item:hover {
    -fx-background-color: #EFF6FF;
}

.menu-item:hover .label {
    -fx-text-fill: #1D1D1F;
}
```

- [ ] **Step 2: Run and verify**

```bash
mvn javafx:run
```

Expected: Menu bar is white. Bottom border is a subtle `#E5E5EA` line. Hovering a menu item shows a light blue tint (`#EFF6FF`). Text is near-black `#1D1D1F`.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/com/imagesorter/css/styles.css
git commit -m "style: white menu bar, soft divider, blue menu item hover"
```

---

### Task 3: Panel Surfaces, Section Titles & Metadata

**Files:**
- Modify: `src/main/resources/com/imagesorter/css/styles.css`

**What this produces:** Section titles, metadata keys/values, and help text adopt the new palette. Hotkey list panel becomes a white surface with `6px` radius.

- [ ] **Step 1: Replace section titles, metadata, and help-text blocks**

Replace `/* Section Titles */`, `/* Metadata Panel Styles */`, and `/* Help Text */` with:

```css
/* ── Section Titles & Metadata ─────────────────────────────── */
.section-title {
    -fx-font-size: 13px;
    -fx-font-weight: bold;
    -fx-text-fill: #1D1D1F;
    -fx-padding: 0 0 8 0;
}

.metadata-key {
    -fx-font-size: 12px;
    -fx-font-weight: 500;
    -fx-text-fill: #6E6E73;
}

.metadata-value {
    -fx-font-size: 12px;
    -fx-text-fill: #1D1D1F;
}

.help-text {
    -fx-font-size: 11px;
    -fx-text-fill: #6E6E73;
    -fx-padding: 2 0 2 0;
}
```

- [ ] **Step 2: Replace the hotkey-list block**

Replace `/* Hotkey List */` section with:

```css
/* ── Hotkey List Panel ──────────────────────────────────────── */
.hotkey-list {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #E5E5EA;
    -fx-border-width: 1px;
    -fx-border-radius: 6px;
}

.hotkey-list .list-cell {
    -fx-padding: 6 10 6 10;
    -fx-background-color: transparent;
    -fx-border-width: 0 0 1 0;
    -fx-border-color: #F5F5F7;
    -fx-text-fill: #1D1D1F;
    -fx-font-family: "SF Mono", "Menlo", "Monaco", "Courier New", monospace;
    -fx-font-size: 11px;
}

.hotkey-list .list-cell:hover {
    -fx-background-color: #EFF6FF;
    -fx-text-fill: #2563EB;
}

.hotkey-list .list-cell:selected {
    -fx-background-color: #DBEAFE;
    -fx-text-fill: #1D4ED8;
    -fx-font-weight: bold;
}

.hotkey-list .list-cell:empty {
    -fx-background-color: transparent;
    -fx-text-fill: transparent;
}

.list-cell .text {
    -fx-fill: inherit;
}
```

- [ ] **Step 3: Run and verify**

```bash
mvn javafx:run
```

Expected: Left panel hotkey list has white background with `6px` rounded corners. Hovering a cell shows `#EFF6FF` blue tint. Selected cell is `#DBEAFE` with `#1D4ED8` text. Metadata keys are `#6E6E73` muted, values are `#1D1D1F`.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/com/imagesorter/css/styles.css
git commit -m "style: white hotkey list panel, blue cell hover/selection, muted metadata keys"
```

---

### Task 4: Thumbnail Bar & Status Bar

**Files:**
- Modify: `src/main/resources/com/imagesorter/css/styles.css`

**What this produces:** Thumbnail bar is white with a bottom-only divider. Selected thumbnail has a `2px` blue border (not `4px`). Status bar text is muted `11px`.

- [ ] **Step 1: Replace thumbnail bar block**

Replace `/* Thumbnail Bar */` section with:

```css
/* ── Thumbnail Bar ──────────────────────────────────────────── */
.thumbnail-bar {
    -fx-padding: 4px 8px;
    -fx-background-color: #FFFFFF;
    -fx-border-color: #E5E5EA;
    -fx-border-width: 0 0 1 0;
}

.thumbnail-image {
    -fx-border-width: 0;
    -fx-border-radius: 5px;
    -fx-background-radius: 5px;
    -fx-background-color: transparent;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0.3, 0, 1);
}

.thumbnail-selected {
    -fx-border-color: #3B82F6;
    -fx-border-width: 2px;
    -fx-border-radius: 5px;
    -fx-background-radius: 5px;
    -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.25), 8, 0.3, 0, 1);
}

.thumbnail-video-badge {
    -fx-background-color: rgba(0, 0, 0, 0.6);
    -fx-background-radius: 3px;
    -fx-padding: 3px 4px 3px 5px;
    -fx-max-width: 16px;
    -fx-max-height: 14px;
    -fx-min-width: 16px;
    -fx-min-height: 14px;
}
```

- [ ] **Step 2: Replace status bar block**

Replace `/* Status Bar */` section with:

```css
/* ── Status Bar ─────────────────────────────────────────────── */
.status-label {
    -fx-font-size: 11px;
    -fx-text-fill: #6E6E73;
    -fx-padding: 2 6 2 6;
}

.progress-bar {
    -fx-accent: #3B82F6;
}

.progress-bar .bar {
    -fx-background-color: #3B82F6;
    -fx-background-radius: 2px;
}

.progress-bar .track {
    -fx-background-color: #E5E5EA;
    -fx-background-radius: 2px;
}
```

- [ ] **Step 3: Run and verify**

```bash
mvn javafx:run
```

Open a folder with multiple images. Expected:
- Thumbnail bar: white, bottom border only, no top border
- Thumbnail images: no border at rest, soft shadow
- Selected thumbnail: thin `2px` blue border, blue shadow glow
- Status bar text: `#6E6E73` muted, `11px`

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/com/imagesorter/css/styles.css
git commit -m "style: white thumbnail bar, 2px blue selected ring, muted status bar, blue progress"
```

---

### Task 5: Image Viewer & Separators

**Files:**
- Modify: `src/main/resources/com/imagesorter/css/styles.css`

**What this produces:** Image scroll pane has no border. Image drop shadow is softened. Separator uses `#E5E5EA`.

- [ ] **Step 1: Replace image scroll pane block**

Replace `/* Image Scroll Pane */` section with:

```css
/* ── Image Viewer ───────────────────────────────────────────── */
.image-scroll-pane {
    -fx-background-color: transparent;
    -fx-border-width: 0;
    -fx-padding: 0;
}

.image-scroll-pane .viewport {
    -fx-background-color: transparent;
}

#imageScrollPane .viewport {
    -fx-background-color: #F5F5F7;
}

.main-image {
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.3, 0, 2);
}

.main-image:hover {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.14), 16, 0.3, 0, 3);
}
```

- [ ] **Step 2: Replace separator block**

Replace `/* Separator Styles */` section with:

```css
/* ── Separator ──────────────────────────────────────────────── */
.separator .line {
    -fx-border-color: #E5E5EA;
    -fx-border-width: 1px 0 0 0;
}
```

- [ ] **Step 3: Run and verify**

```bash
mvn javafx:run
```

Expected: Image area has no visible border frame. Image shadow is barely visible on the light canvas. Separators in the status bar and left panel are `#E5E5EA` — subtle.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/com/imagesorter/css/styles.css
git commit -m "style: borderless image viewer, softer shadow, updated separator color"
```

---

### Task 6: Buttons

**Files:**
- Modify: `src/main/resources/com/imagesorter/css/styles.css`

**What this produces:** Primary button is flat vivid blue. All other buttons (default, secondary, browse) become ghost buttons — transparent fill, `1px #E5E5EA` border. Green browse button is removed.

- [ ] **Step 1: Replace the entire buttons block**

Replace the `/* Button Styles */` section with:

```css
/* ── Buttons ────────────────────────────────────────────────── */
.button {
    -fx-background-color: transparent;
    -fx-text-fill: #1D1D1F;
    -fx-border-color: #E5E5EA;
    -fx-border-width: 1px;
    -fx-border-radius: 6px;
    -fx-background-radius: 6px;
    -fx-padding: 7 14 7 14;
    -fx-font-size: 13px;
    -fx-cursor: hand;
}

.button:hover {
    -fx-background-color: #F5F5F7;
}

.button:pressed {
    -fx-background-color: #E5E5EA;
}

.primary-button {
    -fx-background-color: #3B82F6;
    -fx-text-fill: #FFFFFF;
    -fx-border-width: 0;
}

.primary-button:hover {
    -fx-background-color: #2563EB;
}

.primary-button:pressed {
    -fx-background-color: #1D4ED8;
}

.secondary-button {
    -fx-background-color: transparent;
    -fx-text-fill: #1D1D1F;
    -fx-border-color: #E5E5EA;
    -fx-border-width: 1px;
}

.secondary-button:hover {
    -fx-background-color: #F5F5F7;
}

.secondary-button:pressed {
    -fx-background-color: #E5E5EA;
}

.browse-button {
    -fx-background-color: transparent;
    -fx-text-fill: #1D1D1F;
    -fx-border-color: #E5E5EA;
    -fx-border-width: 1px;
    -fx-min-width: 70px;
}

.browse-button:hover {
    -fx-background-color: #F5F5F7;
}

.browse-button:pressed {
    -fx-background-color: #E5E5EA;
}
```

- [ ] **Step 2: Run and verify**

```bash
mvn javafx:run
```

Open "Configure Folders..." dialog. Expected:
- Save button: solid `#3B82F6` blue, white text, no border
- Cancel button: transparent fill, `#E5E5EA` border, near-black text
- All Browse buttons: same ghost style — no green fill

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/com/imagesorter/css/styles.css
git commit -m "style: flat blue primary button, ghost secondary/browse buttons, remove green"
```

---

### Task 7: Text Fields, Inputs, Scrollbars & Pin Button

**Files:**
- Modify: `src/main/resources/com/imagesorter/css/styles.css`

**What this produces:** Text field focus shows a solid blue border (no glow ring). Scrollbars shrink to `4px` ghost strips. Pin button dims to `0.3` opacity at rest.

- [ ] **Step 1: Replace the text-field and focus indicator blocks**

Replace the `/* Configuration Dialog */` hotkey-label rule, `/* ... .text-field ... */` block, and `/* Focus Indicators */` section with:

```css
/* ── Dialog Labels ──────────────────────────────────────────── */
.hotkey-label {
    -fx-font-size: 12px;
    -fx-font-weight: 500;
    -fx-text-fill: #6E6E73;
    -fx-alignment: center-right;
}

/* ── Text Fields ────────────────────────────────────────────── */
.text-field {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #E5E5EA;
    -fx-border-width: 1px;
    -fx-border-radius: 6px;
    -fx-background-radius: 6px;
    -fx-padding: 7 10 7 10;
    -fx-font-size: 13px;
    -fx-text-fill: #1D1D1F;
}

.text-field:focused {
    -fx-border-color: #3B82F6;
    -fx-background-color: #FFFFFF;
    -fx-effect: null;
}

/* ── Focus Indicators ───────────────────────────────────────── */
.button:focused,
.text-field:focused,
.list-view:focused {
    -fx-effect: null;
}
```

- [ ] **Step 2: Replace the scroll bar block**

Replace `/* Scroll Bar Styling */` section with:

```css
/* ── Scrollbars ─────────────────────────────────────────────── */
.scroll-bar:horizontal,
.scroll-bar:vertical {
    -fx-background-color: transparent;
    -fx-pref-width: 4px;
    -fx-pref-height: 4px;
}

.scroll-bar .track {
    -fx-background-color: transparent;
    -fx-border-radius: 4px;
    -fx-background-radius: 4px;
}

.scroll-bar .thumb {
    -fx-background-color: rgba(0, 0, 0, 0.2);
    -fx-border-radius: 4px;
    -fx-background-radius: 4px;
}

.scroll-bar .thumb:hover {
    -fx-background-color: rgba(0, 0, 0, 0.35);
}

.scroll-bar .thumb:pressed {
    -fx-background-color: rgba(0, 0, 0, 0.5);
}

.scroll-bar .increment-button,
.scroll-bar .decrement-button {
    -fx-background-color: transparent;
    -fx-pref-width: 0;
    -fx-pref-height: 0;
    -fx-padding: 0;
}

.scroll-bar .increment-arrow,
.scroll-bar .decrement-arrow {
    -fx-shape: "";
    -fx-padding: 0;
}
```

- [ ] **Step 3: Replace pin button block**

Replace `/* Pin Button Styling */` section with:

```css
/* ── Pin Button ─────────────────────────────────────────────── */
.pin-button {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-border-width: 0;
    -fx-padding: 3 6 3 6;
    -fx-font-size: 13px;
    -fx-cursor: hand;
    -fx-opacity: 0.3;
    -fx-background-radius: 6px;
}

.pin-button:hover {
    -fx-opacity: 0.7;
    -fx-background-color: rgba(0, 0, 0, 0.06);
}

.pin-button:selected {
    -fx-opacity: 1.0;
    -fx-background-color: rgba(59, 130, 246, 0.12);
}
```

- [ ] **Step 4: Run and verify**

```bash
mvn javafx:run
```

Expected:
- Click a text field in "Configure Folders...": border becomes solid `#3B82F6`, no glow ring
- Scroll metadata panel: scrollbar is a thin `4px` strip, no arrows, barely visible at rest
- Pin button (📌): very dim at rest (`0.3` opacity), brightens on hover, blue tint when selected

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/com/imagesorter/css/styles.css
git commit -m "style: solid blue field focus, 4px ghost scrollbars, refined pin button"
```

---

### Task 8: Dialogs, Tooltips & Alerts

**Files:**
- Modify: `src/main/resources/com/imagesorter/css/styles.css`

**What this produces:** Dialog title reduced to `15px`. Notes text muted. Tooltips are compact with `6px` radius. Alert header uses surface color.

- [ ] **Step 1: Replace dialog, notes, alert, and tooltip blocks**

Replace `/* Dialog Styles */`, `/* Notes Section in Config Dialog */`, `/* Alert Dialog Styling */`, and `/* Tooltip Styling */` sections with:

```css
/* ── Dialogs ────────────────────────────────────────────────── */
.dialog-title {
    -fx-font-size: 15px;
    -fx-font-weight: bold;
    -fx-text-fill: #1D1D1F;
}

.dialog-description {
    -fx-font-size: 12px;
    -fx-text-fill: #6E6E73;
    -fx-wrap-text: true;
}

/* ── Config Dialog Notes ────────────────────────────────────── */
.notes-title {
    -fx-font-weight: bold;
    -fx-text-fill: #1D1D1F;
    -fx-font-size: 13px;
}

.notes-text {
    -fx-text-fill: #6E6E73;
    -fx-font-size: 11px;
    -fx-padding: 2 0 2 0;
}

/* ── Alerts ─────────────────────────────────────────────────── */
.alert {
    -fx-background-color: #FFFFFF;
}

.alert .header-panel {
    -fx-background-color: #FFFFFF;
    -fx-border-width: 0 0 1 0;
    -fx-border-color: #E5E5EA;
}

.alert .header-panel .label {
    -fx-font-size: 13px;
    -fx-font-weight: bold;
    -fx-text-fill: #1D1D1F;
}

.alert .content.label {
    -fx-text-fill: #6E6E73;
    -fx-wrap-text: true;
    -fx-font-size: 13px;
}

/* ── Tooltips ───────────────────────────────────────────────── */
.tooltip {
    -fx-background-color: rgba(29, 29, 31, 0.92);
    -fx-text-fill: #FFFFFF;
    -fx-background-radius: 6px;
    -fx-font-size: 11px;
    -fx-padding: 7 10 7 10;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0.3, 0, 1);
}
```

- [ ] **Step 2: Run and verify**

```bash
mvn javafx:run
```

Open "Configure Folders...". Expected:
- Dialog title is `15px` bold (not the oversized `18px`)
- Notes text at the bottom is `11px` muted gray
- Hover over a pin button — tooltip is compact, `6px` rounded, `11px` text
- Trigger a save: alert header is white with a single bottom line, not the gray tinted header

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/com/imagesorter/css/styles.css
git commit -m "style: proportional dialog title, compact tooltips, clean alert header"
```

---

### Task 9: Dark Mode — `styles-dark.css`

**Files:**
- Create: `src/main/resources/com/imagesorter/css/styles-dark.css`

**What this produces:** A complete dark mode override file containing only the rules that differ from light mode. Applied on top of `styles.css` when the OS is in dark mode.

- [ ] **Step 1: Create `styles-dark.css`**

Create `src/main/resources/com/imagesorter/css/styles-dark.css` with this content:

```css
/* Dark mode overrides — loaded on top of styles.css when OS is dark */

.root {
    -fx-background-color: #1C1C1E;
}

/* Menu Bar */
.menu-bar {
    -fx-background-color: #2C2C2E;
    -fx-border-color: #3A3A3C;
}

.menu-bar .menu .label,
.menu-item .label {
    -fx-text-fill: #F5F5F7;
}

.menu-bar .menu:hover {
    -fx-background-color: #3A3A3C;
}

.menu-bar .menu:hover .label,
.menu-bar .menu:showing .label,
.menu-item:hover .label {
    -fx-text-fill: #F5F5F7;
}

.menu-item:hover {
    -fx-background-color: #1E3A5F;
}

/* Section Titles & Metadata */
.section-title {
    -fx-text-fill: #F5F5F7;
}

.metadata-key {
    -fx-text-fill: #98989D;
}

.metadata-value {
    -fx-text-fill: #F5F5F7;
}

.help-text {
    -fx-text-fill: #98989D;
}

/* Hotkey List Panel */
.hotkey-list {
    -fx-background-color: #2C2C2E;
    -fx-border-color: #3A3A3C;
}

.hotkey-list .list-cell {
    -fx-border-color: #3A3A3C;
    -fx-text-fill: #F5F5F7;
}

.hotkey-list .list-cell:hover {
    -fx-background-color: #1E3A5F;
    -fx-text-fill: #60A5FA;
}

.hotkey-list .list-cell:selected {
    -fx-background-color: #1E40AF;
    -fx-text-fill: #93C5FD;
}

/* Thumbnail Bar */
.thumbnail-bar {
    -fx-background-color: #2C2C2E;
    -fx-border-color: #3A3A3C;
}

/* Status Bar */
.status-label {
    -fx-text-fill: #98989D;
}

/* Image Viewer */
#imageScrollPane .viewport {
    -fx-background-color: #1C1C1E;
}

/* Separator */
.separator .line {
    -fx-border-color: #3A3A3C;
}

/* Buttons */
.button {
    -fx-text-fill: #F5F5F7;
    -fx-border-color: #3A3A3C;
}

.button:hover {
    -fx-background-color: #3A3A3C;
}

.button:pressed {
    -fx-background-color: #48484A;
}

.secondary-button {
    -fx-text-fill: #F5F5F7;
    -fx-border-color: #3A3A3C;
}

.secondary-button:hover {
    -fx-background-color: #3A3A3C;
}

.browse-button {
    -fx-text-fill: #F5F5F7;
    -fx-border-color: #3A3A3C;
}

.browse-button:hover {
    -fx-background-color: #3A3A3C;
}

.primary-button:hover {
    -fx-background-color: #60A5FA;
}

.primary-button:pressed {
    -fx-background-color: #3B82F6;
}

/* Text Fields */
.text-field {
    -fx-background-color: #2C2C2E;
    -fx-border-color: #3A3A3C;
    -fx-text-fill: #F5F5F7;
}

.text-field:focused {
    -fx-border-color: #3B82F6;
    -fx-background-color: #2C2C2E;
}

.hotkey-label {
    -fx-text-fill: #98989D;
}

/* Scrollbars */
.scroll-bar .thumb {
    -fx-background-color: rgba(255, 255, 255, 0.2);
}

.scroll-bar .thumb:hover {
    -fx-background-color: rgba(255, 255, 255, 0.35);
}

.scroll-bar .thumb:pressed {
    -fx-background-color: rgba(255, 255, 255, 0.5);
}

/* Pin Button */
.pin-button:hover {
    -fx-background-color: rgba(255, 255, 255, 0.08);
}

/* Progress Bar */
.progress-bar .track {
    -fx-background-color: #3A3A3C;
}

/* Dialogs */
.dialog-title {
    -fx-text-fill: #F5F5F7;
}

.dialog-description {
    -fx-text-fill: #98989D;
}

.notes-title {
    -fx-text-fill: #F5F5F7;
}

.notes-text {
    -fx-text-fill: #98989D;
}

/* Alerts */
.alert {
    -fx-background-color: #2C2C2E;
}

.alert .header-panel {
    -fx-background-color: #2C2C2E;
    -fx-border-color: #3A3A3C;
}

.alert .header-panel .label {
    -fx-text-fill: #F5F5F7;
}

.alert .content.label {
    -fx-text-fill: #98989D;
}

/* Tooltips */
.tooltip {
    -fx-background-color: rgba(58, 58, 60, 0.95);
}
```

- [ ] **Step 2: Commit the dark CSS file**

```bash
git add src/main/resources/com/imagesorter/css/styles-dark.css
git commit -m "style: add styles-dark.css with complete dark mode overrides"
```

---

### Task 10: OS Theme Detection — Load Dark CSS Automatically

**Files:**
- Modify: the Java file that creates the `Scene` and loads stylesheets (find it first — see step 1)
- Create: `src/main/java/com/imagesorter/util/OsTheme.java`

**What this produces:** At startup, the app detects macOS dark mode and, if active, adds `styles-dark.css` to the scene's stylesheet list on top of `styles.css`.

- [ ] **Step 1: Find the stylesheet-loading file**

```bash
grep -rn "getStylesheets\|\.css\|new Scene" src/main/java --include="*.java"
```

Identify the file and line where `styles.css` is added to the scene (e.g. `scene.getStylesheets().add(...)`). That file is `TARGET_FILE` for step 4.

- [ ] **Step 2: Create `OsTheme.java`**

Create `src/main/java/com/imagesorter/util/OsTheme.java`:

```java
package com.imagesorter.util;

import java.util.concurrent.TimeUnit;

public final class OsTheme {

    private OsTheme() {}

    public static boolean isDark() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            return false;
        }
        try {
            Process p = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
                    .start();
            p.waitFor(1, TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 3: Verify `OsTheme` compiles**

```bash
mvn compile
```

Expected: `BUILD SUCCESS`. No errors in `OsTheme.java`.

- [ ] **Step 4: Add dark CSS loading to the Scene setup**

In `TARGET_FILE` (found in step 1), directly after the line that adds `styles.css` to the scene's stylesheets, add:

```java
if (OsTheme.isDark()) {
    scene.getStylesheets().add(
        getClass().getResource("/com/imagesorter/css/styles-dark.css").toExternalForm()
    );
}
```

Add the import at the top of `TARGET_FILE` if not already present:
```java
import com.imagesorter.util.OsTheme;
```

- [ ] **Step 5: Run in light mode and verify no regression**

Ensure macOS Appearance is set to **Light** (System Preferences → Appearance → Light).

```bash
mvn javafx:run
```

Expected: Identical to the light-mode results from previous tasks. `OsTheme.isDark()` returns `false`, `styles-dark.css` is not loaded.

- [ ] **Step 6: Switch to dark mode and verify**

Set macOS Appearance to **Dark** (System Preferences → Appearance → Dark). Quit and relaunch:

```bash
mvn javafx:run
```

Expected dark mode checklist:
- [ ] Root background: `#1C1C1E` (near-black)
- [ ] Menu bar: `#2C2C2E`, bottom border `#3A3A3C`
- [ ] Left/right panels: `#2C2C2E` surface
- [ ] Thumbnail bar: `#2C2C2E`, bottom border `#3A3A3C`
- [ ] Image viewer background: `#1C1C1E`
- [ ] Hotkey list hover: `#1E3A5F` blue tint
- [ ] Ghost buttons: `#3A3A3C` border, `#F5F5F7` text
- [ ] Primary (Save) button: still `#3B82F6`
- [ ] Text fields: `#2C2C2E` background, `#3A3A3C` border
- [ ] Scrollbar thumbs: `rgba(255,255,255,0.2)` (lighter strips)
- [ ] Status bar text: `#98989D`
- [ ] Tooltips: `rgba(58,58,60,0.95)` background

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/imagesorter/util/OsTheme.java TARGET_FILE
git commit -m "feat: auto-detect macOS dark mode at startup and load styles-dark.css"
```

---

### Task 11: Final Audit & Cleanup

**Files:**
- Read: both CSS files, verify no orphaned rules or regressions

**What this produces:** Confirmation that light and dark modes are visually complete and no rules from the original Bootstrap-era stylesheet remain active.

- [ ] **Step 1: Grep for any Bootstrap colors still in styles.css**

```bash
grep -n "#007bff\|#6c757d\|#28a745\|#212529\|#f8f9fa\|#dee2e6\|#ced4da\|#e9ecef" \
  src/main/resources/com/imagesorter/css/styles.css
```

Expected: no output. If any Bootstrap hex values appear, replace them with the equivalent new palette values from the spec (`#007bff` → `#3B82F6`, `#6c757d` → `#6E6E73`, `#28a745` removed, `#f8f9fa` → `#F5F5F7`, `#212529` → `#1D1D1F`, `#dee2e6`/`#ced4da`/`#e9ecef` → `#E5E5EA`).

- [ ] **Step 2: Full light-mode visual pass**

Set macOS to **Light**. Launch and check each screen:

```bash
mvn javafx:run
```

- [ ] Main window loads, canvas is `#F5F5F7`, panels are white
- [ ] Open a folder — thumbnails load, selected thumbnail has `2px` blue ring
- [ ] Open Configure Folders — title `15px`, ghost Browse buttons, blue Save
- [ ] Hover a Browse field — focus border is `#3B82F6`, no glow
- [ ] Hover a pin button — fades in, blue tint when selected
- [ ] Scroll metadata — `4px` ghost scrollbar strip visible

- [ ] **Step 3: Full dark-mode visual pass**

Set macOS to **Dark**. Launch and check:

```bash
mvn javafx:run
```

- [ ] Main window: `#1C1C1E` canvas, `#2C2C2E` panels
- [ ] Hotkey list cells: `#F5F5F7` text, `#1E3A5F` hover
- [ ] Configure Folders: `#2C2C2E` field backgrounds, blue focus border still works
- [ ] Tooltips: dark `rgba(58,58,60,0.95)` background
- [ ] Ghost buttons: `#3A3A3C` border, light text

- [ ] **Step 4: Final commit**

```bash
git add src/main/resources/com/imagesorter/css/styles.css \
        src/main/resources/com/imagesorter/css/styles-dark.css
git commit -m "style: final audit — remove Bootstrap remnants, complete light+dark verification"
```
