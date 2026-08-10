# Retra Redesign Changelog

## Retra 3.0 Complete Product Design & Architecture

### Brand Identity & Visual System
- Replaced legacy geometric mark with original **Retra Sprite** memory-creature identity in SVG, vector XML, and high-resolution raster assets.
- Implemented multi-tiered **Liquid Glass System** (`GlassTier.THIN`, `REGULAR`, `ELEVATED`, `STRONG`, `OPAQUE`) with background light pools, rim highlights, and zero-compromise accessibility fallbacks.
- Transitioned default color palette to deep near-black aubergine (`#09070D`), deep plum-charcoal (`#13101A`), electric lilac (`#C7ACFC`), raspberry pink (`#FF6B8B`), mint aqua (`#5EEAD4`), and warm coral (`#FF9376`).

### Navigation & Shell
- Replaced old bottom navigation with a **Floating Liquid-Glass Dock** on phones and an **Adaptive Rail** on tablets/wide screens.
- Added elevated center **Add Action Sheet** covering File Import, Folder Scan, Patch Studio, Retra Drift Demo, and Vault Restore.
- Restructured navigation flow: Home, Library, Add Sheet, Profile, Settings.

### Onboarding Experience
- Created 5 interactive, animated onboarding screens:
  1. Emotional Introduction (Retra Sprite with spring physics drag).
  2. Your Games, Your Library (Parallax floating game cards).
  3. ROM Hacks & Patches Made Simple (Visual patch pairing metaphor).
  4. Make The Handheld Yours (Interactive live controller preview).
  5. Identity & Sign-In (Google sign-in + offline continuation).

### Screens & Components
- **Home**: Asymmetrical "Continue" hero card, recently played shelf, quick feature banners, on-device privacy guarantee.
- **Library**: Visual-first poster grid, floating glass search, comprehensive filter chips, detailed list view toggle.
- **Game Details**: Modal bottom sheet over artwork, 52dp primary action, favorite toggle, cheats installer, expandable ROM identity & provenance.
- **Profile**: Local-first profile, optional Google identity link, save health monitor, milestone achievements.
- **Settings**: Deeply organized categories (Appearance, Player, Controls, Saves, Privacy, Diagnostics, About & Developer with Prashant Chataut profile links).
- **Player UI**: Liquid glass touch controls, 1-tap screen rotation toggle, prioritized pause overlay.
