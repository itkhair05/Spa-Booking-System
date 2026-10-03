# Design System - Spa Booking Management App

## 1. Design Philosophy
A modern, premium spa SaaS management application. The UI aims to feel calm, clean, trustworthy, professional, and easy to operate. It avoids clichés (like excessive floral patterns or lotus graphics) and instead focuses on strong information hierarchy, clear states, good contrast, and fast operation.

## 2. Color Palette
The color system relies on CSS variables defined in `src/index.css`.

### Brand Colors (Ocean/Sky theme for calm professionalism)
- `--color-brand-50`: `#f0f9ff` (Soft highlights, hover states)
- `--color-brand-100`: `#e0f2fe` (Subtle backgrounds)
- `--color-brand-500`: `#0ea5e9` (Primary accents, active states)
- `--color-brand-600`: `#0284c7` (Primary buttons, strong emphasis)
- `--color-brand-700`: `#0369a1` (Hover states on primary elements)

### Neutral Colors (Slate)
- `--color-neutral-50`: `#f8fafc` (App background)
- `--color-neutral-100`: `#f1f5f9` (Card/surface backgrounds)
- `--color-neutral-200`: `#e2e8f0` (Borders, dividers)
- `--color-neutral-400`: `#94a3b8` (Muted text, inactive icons)
- `--color-neutral-500`: `#64748b` (Secondary text)
- `--color-neutral-700`: `#334155` (Primary text, headings)
- `--color-neutral-800`: `#1e293b` (Sidebar hover backgrounds)
- `--color-neutral-900`: `#0f172a` (Sidebar background, strongest text)

### Semantic Colors
- **Error**: `--color-error` (`#dc2626`), `--color-error-bg` (`#fef2f2`), `--color-error-border` (`#fecaca`)

## 3. Typography
- **Font Family**: Inter, system-ui, sans-serif. (`--font-sans`)
- **Hierarchy**:
  - App Logo: `800` weight, `-0.01em` tracking.
  - Page Titles (H1): `1.25rem` to `1.5rem`, `700` weight.
  - Section Titles (H2): `1.125rem`, `600` weight.
  - Body Text: `0.9375rem`, `400` weight.
  - Small Text / Labels: `0.8125rem` to `0.875rem`, `500` or `600` weight.

## 4. Spacing & Sizing
- **Sidebar Width**: `240px` on desktop, hidden on small screens behind a hamburger menu.
- **Header Height**: `60px`.
- **Page Padding**: `24px` on desktop, `16px` on mobile.

## 5. Border Radius & Shadows
- **Radius**: `8px` (`0.5rem`) for standard elements (inputs, buttons, cards). `16px` for large containers (login card). `9999px` (pill) reserved for badges or avatars.
- **Shadows**:
  - `--shadow-sm`: Subtle elevation for sticky headers.
  - `--shadow-md`: Prominent elevation for standalone cards (like Login).

## 6. Component Foundation
- **Button**: Clear primary/secondary variants with focus-visible rings and disabled states.
- **Input**: Distinct borders, prominent focus rings (`box-shadow`), muted disabled states.
- **Card**: Clean white surface with subtle borders and shadows.
- **AppShell**: Responsive sidebar layout with sticky header.

## 7. Accessibility
- Semantic HTML tags (`<aside>`, `<main>`, `<header>`, `<nav>`).
- Explicit focus states (`:focus-visible`).
- Contrast ratios meeting WCAG AA standards.
- Form inputs always paired with `id` and `<label>`.
