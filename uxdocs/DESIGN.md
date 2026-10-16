---
name: Warm Culinary Craft
colors:
  surface: '#fbf9f5'
  surface-dim: '#dbdad6'
  surface-bright: '#fbf9f5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3ef'
  surface-container: '#efeeea'
  surface-container-high: '#eae8e4'
  surface-container-highest: '#e4e2de'
  on-surface: '#1b1c1a'
  on-surface-variant: '#59413b'
  inverse-surface: '#30312e'
  inverse-on-surface: '#f2f0ed'
  outline: '#8c716a'
  outline-variant: '#e0bfb7'
  surface-tint: '#ab3514'
  primary: '#a83212'
  on-primary: '#ffffff'
  primary-container: '#ca4a29'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb4a1'
  secondary: '#006c4a'
  on-secondary: '#ffffff'
  secondary-container: '#82f5c1'
  on-secondary-container: '#00714e'
  tertiary: '#8d4b00'
  on-tertiary: '#ffffff'
  tertiary-container: '#b15f00'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbd2'
  primary-fixed-dim: '#ffb4a1'
  on-primary-fixed: '#3c0800'
  on-primary-fixed-variant: '#891e00'
  secondary-fixed: '#85f8c4'
  secondary-fixed-dim: '#68dba9'
  on-secondary-fixed: '#002114'
  on-secondary-fixed-variant: '#005137'
  tertiary-fixed: '#ffdcc3'
  tertiary-fixed-dim: '#ffb77d'
  on-tertiary-fixed: '#2f1500'
  on-tertiary-fixed-variant: '#6e3900'
  background: '#fbf9f5'
  on-background: '#1b1c1a'
  surface-variant: '#e4e2de'
typography:
  display:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 30px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 2rem
  margin-desktop: 3rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system expresses the warmth, generosity, and grounded craftsmanship of home cooking, paired with modern, unobtrusive AI intelligence. It blends organic warmth with contemporary digital clarity—reminiscent of natural ceramics, sunlit pantries, and fresh aromatic herbs. 

The aesthetic is Tactile Modern: interfaces prioritize soft warmth, inviting tactile feedback, and highly readable typography over sterile tech minimalism. Visual elements use generous touch targets, comforting curved corners, and organic hues that stimulate appetite while reducing cognitive friction during dynamic, hands-on cooking sessions.

## Colors

The palette establishes an appetizing, domestic atmosphere anchored in nature and culinary craft:

- **Primary (`#E05A37` - Terracotta Red-Orange):** Used for primary actions, active cooking states, and appetite-stimulating focal points. On light backgrounds, it conveys energy and heat without the clinical aggression of pure scarlet.
- **Secondary (`#059669` - Sage Emerald):** Communicates freshness, pantry abundance, healthy nutritional markers, and verified AI ingredient recognitions.
- **Tertiary (`#D97706` - Warm Amber):** Reserved for timer notifications, culinary warnings, chef tips, and simmering state indicators.
- **Neutral Base (`#FDFBF7` - Warm Cream Alabaster):** Replaces clinical pure white with an eggshell-ceramic softness to minimize glare and visual fatigue on countertop-mounted screens.
- **Text & Structure (`#1E293B` - Soft Charcoal Slate):** Delivers WCAG AAA contrast against the cream ground while remaining noticeably warmer and gentler than raw `#000000`.

## Typography

Typography prioritizes effortless glanceability at arm's length (for hands-on countertop kitchen situations).

- **Headlines & Expressive UI (`Plus Jakarta Sans`):** Selected for its balanced geometric structure, open apertures, and friendly humanist inflections. It creates an approachable, contemporary culinary atmosphere.
- **Body & Instructional Flow (`Inter`):** Selected for its neutral legibility, tall x-height, and superior performance across step-by-step cooking procedures, ingredient proportions, and conversational AI transcripts.
- **Arm’s-Length Sizing:** Step-by-step instructions in Cook Mode must default to `body-lg` (18px) or larger to ensure steps can be read without leaning over the kitchen counter.

## Layout & Spacing

The layout is built upon a fluid responsive grid designed for single-hand mobile utility, countertop tablet viewing, and wide desktop prep management.

- **Mobile (<768px):** 4 columns, `margin` of `1rem`, `gutter` of `1rem`. All interactive elements must adhere to a strict minimum hit target of 48×48px to accommodate wet or messy cooking hands.
- **Tablet (768px–1024px):** 8 columns, `margin-tablet` of `2rem`. Optimized for landscape kickstand viewing in kitchen docks, with persistent side-by-side splits (steps alongside timer/AI assistant).
- **Desktop (>1024px):** 12 columns, max-width `1280px` centered container with `margin-desktop` of `3rem`.
- **Rhythm & Padding:** Spacing tokens (`space-xs` through `space-xl`) scale linearly in multiples of 4px/8px to sustain consistent visual cadence between cards, recipe metadata badges, and media banners.

## Elevation & Depth

Visual depth is achieved through warm-toned ambient shadows and surface stacking rather than cold grayscale blurs.

- **Surface Tiers:**
  - Base canvas: `#FDFBF7` (Ceramic Cream)
  - Card / Panel layer: `#FFFFFF` (Pure Eggshell)
  - Floating controls / Tooltips: `#FFFDFB` with amber-charcoal tinted drop shadows
- **Ambient Shadow Character:** Drop shadows carry an amber-charcoal tint (`rgba(30, 41, 59, 0.06)` combined with a faint warm spread of `rgba(224, 90, 55, 0.04)`), creating natural sunlight ambient occlusion.
- **Floating Kitchen Modals:** Cooking timers, persistent voice control triggers, and AI camera overlays use soft, diffuse multi-stop shadows (`0 8px 30px rgba(30, 41, 59, 0.10)`) to cleanly detach from busy photo feeds.
- **Borders:** Thin, soft borders (`1px solid rgba(30, 41, 59, 0.08)`) are used instead of heavy lines to preserve visual lightness.

## Shapes

The interface employs a rounded shape language (Level 2: `0.5rem` base, `1rem` on cards and sheets, and `1.5rem` on prominent hero units) to echo the natural curves of culinary pottery, smooth ingredients, and kitchen tools. 

Interactive micro-elements such as tags, dietary badges, and step counters lean toward pill profiles (`rounded-full`) to invite touch and denote active state selections.

## Components

### Buttons
- **Primary:** Background in Terracotta `#E05A37`, text in `#FFFFFF`, with `0.75rem` rounded corners. Hover transitions to `#C94A29`. Minimum touch height is 52px for primary mobile actions.
- **Secondary:** Surface in `#F2EDE4` with `#1E293B` text, shifting to subtle terracotta tint on focus.
- **Icon / Voice Trigger:** Circular pill container with sage green `#059669` accents for active listening and camera triggers.

### Chips & Badges
- **Ingredient & Tag Chips:** Soft neutral background `#F4EFE6`, text `#1E293B`, rounded-full with 12px horizontal padding.
- **Confirmed AI Detection Chips:** Sage background (`rgba(5, 150, 105, 0.12)`), text `#059669`, bordered with a fine `1px` sage stroke.

### Recipe & Ingredient Cards
- White `#FFFFFF` surface mounted on `#FDFBF7`, framed with a `1px` whisper border (`rgba(30, 41, 59, 0.06)`), `rounded-lg` (16px), and soft amber-charcoal shadow.
- Imagery is treated with top-corner clipping matching the card curvature, overlaid with dietary badges in the top right.

### Input Fields & Search
- Inputs use `#FFFFFF` background, `0.75rem` border radius, and inset padding of `0.875rem` by `1rem`. Active focus replaces the light border with a 2px Terracotta `#E05A37` ring and zero sharp outline.

### Checkboxes & Step Radios
- Checkboxes feature rounded corners (6px), transitioning from an unfilled ceramic ring to full Sage `#059669` fill with a white checkmark when an ingredient or step is completed.

### Domain-Specific Components
- **Camera Ingredient HUD:** Semi-transparent viewfinder with animated emerald-tinted bounding boxes and floating recognized tags.
- **Cook Mode Step Banner:** Sticky, high-contrast bottom navigation bar featuring an oversized active step counter, built-in active timer chip with pause/play, and a prominent "Next Step" tap zone.
- **Culinary Chatbot (Sous Chef Drawer):** Conversational slide-up drawer styled with warm cream speech bubbles, suggestion quick-chips, and an ambient amber pulsing mic indicator.