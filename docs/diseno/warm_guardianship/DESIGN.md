---
name: Warm Guardianship
colors:
  surface: '#f6faff'
  surface-dim: '#d6dae0'
  surface-bright: '#f6faff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f0f4fa'
  surface-container: '#eaeef4'
  surface-container-high: '#e4e8ee'
  surface-container-highest: '#dee3e9'
  on-surface: '#171c20'
  on-surface-variant: '#3f4947'
  inverse-surface: '#2c3135'
  inverse-on-surface: '#edf1f7'
  outline: '#6f7977'
  outline-variant: '#bec9c6'
  surface-tint: '#136a61'
  primary: '#05645c'
  on-primary: '#ffffff'
  primary-container: '#2e7d74'
  on-primary-container: '#d7fff8'
  inverse-primary: '#88d4c9'
  secondary: '#974727'
  on-secondary: '#ffffff'
  secondary-container: '#ff9972'
  on-secondary-container: '#772f11'
  tertiary: '#4f5b5a'
  on-tertiary: '#ffffff'
  tertiary-container: '#677472'
  on-tertiary-container: '#edfaf8'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#a4f1e5'
  primary-fixed-dim: '#88d4c9'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#005049'
  secondary-fixed: '#ffdbcf'
  secondary-fixed-dim: '#ffb59b'
  on-secondary-fixed: '#380d00'
  on-secondary-fixed-variant: '#793012'
  tertiary-fixed: '#d8e5e3'
  tertiary-fixed-dim: '#bcc9c7'
  on-tertiary-fixed: '#121e1d'
  on-tertiary-fixed-variant: '#3d4948'
  background: '#f6faff'
  on-background: '#171c20'
  surface-variant: '#dee3e9'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style

This design system expresses safety, affectionate care, and structured institutional reliability for childcare communication. The visual language blends Material 3 clarity with warm, domestic warmth tailored to parent-educator interactions. 

The aesthetic is clean and organized to reduce parental anxiety while remaining approachable through soft contours and comforting tones. It rejects sterile institutional tropes in favor of grounded, sunlit stability: thoughtful spacing, organic tactile containers, and immediate clarity for urgent updates, daily milestones, and media sharing.

## Colors

The palette balances tranquil teals with earth-toned terracotta to reflect nurturing safety and warm attentiveness:

- **Primary (`#2E7D74`)**: Anchor teal used for high-emphasis interactive states, top app bars, primary action buttons, outgoing parent message bubbles, and verified educator identity markers.
- **Primary Container (`#E6F3F1`)**: Soft sage tint utilized for incoming chat containers, active filter chips, day log backgrounds, and calm notification banners.
- **Secondary (`#D97A56`)**: Terracotta accent reserved for timely alerts, incident flags, active attendance notifications, and joyful accomplishment highlights.
- **Background (`#F5F6F8`)**: Soft, desaturated neutral canvas mitigating harsh screen glare for daily mobile reading.
- **Surface & Cards (`#FFFFFF`)**: Crisp, elevated pure white containers providing distinct reading contrast.
- **Text Primary (`#1A1C1E`)**: High-contrast, near-black neutral ensuring legibility under variable outdoor and ambient lighting.
- **Text Secondary (`#5E6368`)**: Calibrated slate neutral for secondary metadata, timestamps, read receipts, and input labels.
- **Divider & Outlines (`#E0E3E7`)**: Subdued perimeter definition for list items, media enclosures, and structured input strokes.

## Typography

The typography scale utilizes **Inter** across all roles to ensure maximum readability in dense conversational interfaces and variable handheld settings. 

- **Headlines & Titles**: Tuned with medium and semi-bold weights for student profile headers, educator identifiers, and channel groups without feeling aggressive.
- **Body Text**: Maintains generous line heights (`20px` on `14px` body, `24px` on `16px` body) to optimize readability for multi-line daily caregiver logs, feeding routines, and educator voice transcriptions.
- **Labels & Metadata**: Set with tightened proportions and medium weight (`font-weight: 500`) for chat timestamps, read ticks, status badges, and input counters.

## Layout & Spacing

The layout is built primarily around a compact mobile-first single-column framework, scaling smoothly to a disciplined 4-column structure on tablets or split-view folded displays.

- **Margins & Gutters**: Outer canvas margins use `1rem` (16px) on mobile viewports to preserve horizontal message bubble capacity, expanding to `1.5rem` (24px) on tablet interfaces.
- **Rhythm**: Spacing adheres strictly to an 8dp baseline grid with a 4dp micro-step for tight contextual associations (such as user avatars aligned against incoming message stems, or timestamps tethered below message text).
- **Safe Zones**: Pinned bottom chat composer docks and sticky calendar bars integrate native device safe areas with internal padding of `space-md` vertically.

## Elevation & Depth

This system avoids heavy drop shadows, relying instead on Material 3 tonal layering paired with soft ambient diffusion:

- **Level 0 (Flat Canvas)**: `#F5F6F8` background surface for root conversational threads and activity lists.
- **Level 1 (Cards & Message Surfaces)**: Incoming chat cards and summary panels sit on `#FFFFFF` with a faint perimeter boundary (`1px solid #E0E3E7`) and an ambient tinted drop shadow: `0 1px 3px rgba(26, 28, 30, 0.05)`.
- **Level 2 (Floating Action Buttons & Dynamic Bars)**: Chat input composer dock and floating quick-action buttons utilize `0 4px 12px rgba(46, 125, 116, 0.12)`, grounding them above scrolling message streams.
- **Level 3 (Modals & Media Previews)**: Child check-in confirmations, diaper/sleep log sheets, and high-res media lightboxes employ an overlay scrim of `rgba(26, 28, 30, 0.4)` and a high-blur elevation of `0 8px 24px rgba(26, 28, 30, 0.16)`.

## Shapes

The interface embraces approachable, protective geometry defined by three tiers of corner rounding:

- **12dp (`rounded-md`)**: Applied to compact controls, daily schedule items, form inputs, and inline message attachments.
- **16dp (`rounded-lg`)**: Applied to primary content cards, classroom announcement banners, and message speech bubbles. Outgoing and incoming message bubbles taper one corner to 4dp to create directional chat tails.
- **20dp to Full Pill (`rounded-xl` / `rounded-full`)**: Applied to status pills, tag filters, voice note recording containers, user presence indicators, and action buttons.

## Components

### Buttons
- **Primary**: Solid `#2E7D74` with white text, minimum height of 48px for thumb reach, rounded to full pill shape (9999px), subtle scale feedback on tap (`scale(0.98)`).
- **Secondary / Tonal**: Filled with `#E6F3F1` and text styled in `#2E7D74`, pill-shaped, used for auxiliary choices like "Ver Fotos" or "Llamar a Dirección".
- **Terracotta Accent**: Solid `#D97A56` for immediate confirmations (e.g., authorization of child pickup, medication logging).

### Chat Bubbles
- **Sent (Parent)**: Background `#2E7D74`, text `#FFFFFF`, border-radius: `16dp 16dp 4dp 16dp`. Timestamps rendered in semi-transparent white (`rgba(255, 255, 255, 0.7)`).
- **Received (Daycare / Educator)**: Background `#FFFFFF`, border `1px solid #E0E3E7`, text `#1A1C1E`, border-radius: `16dp 16dp 16dp 4dp`. Teacher name displayed in `label-sm` using primary teal `#2E7D74`.

### Message Composer
- Anchored to bottom safe area. `#FFFFFF` surface container, elevated with Level 2 ambient shadow.
- Pill-shaped input field (`#F5F6F8`) surrounded by rounded line icon actions for photo attachment, daily milestone tagging, and voice memos.

### Status Pills & Badges
- Height 24px to 28px, rounded pill geometry (`rounded-full`), padded horizontally by `space-sm` (8px).
- **Nap / Sleep**: Subtle indigo/gray tint.
- **Meals / Nutrition**: `#E6F3F1` container with `#2E7D74` text.
- **Incident / Alert**: `#FDF0EB` container with `#D97A56` text.

### Cards & Activity Logs
- `#FFFFFF` background, `16dp` corner radius, `1px solid #E0E3E7` border.
- Header contains child avatar (`40dp` circular) accompanied by teacher name and timestamp. Includes an inline status pill indicating activity type (e.g., "Almuerzo", "Siesta", "Actividad").

### Inputs & Selectors
- Background `#FFFFFF`, border `1px solid #E0E3E7`, `12dp` radius.
- On focus: border shifts to 2px solid `#2E7D74` with zero drop shadow to maintain clean readability. Helper text and error messages use Inter `body-sm`.