---
name: design-guide
description: Offline project snapshot of Paperclip's component-driven design-guide principles, adapted for Retra's Android stack.
source: https://github.com/paperclipai/paperclip
---
# Design Guide — Retra Adaptation

Use dense-but-scannable composition, semantic design tokens, minimal elevation, reusable domain components, clear typography roles, consistent status colors, and a living component showcase. Retra translates Paperclip's web-specific primitives into Material 3 Compose primitives and composites rather than copying React/Tailwind APIs.

Create a reusable component when a pattern repeats, contains interaction, or encodes domain semantics. Avoid one-off visual variants, raw colors, ad-hoc typography, heavy shadows, and untracked components.
