# UI/UX Audit — Retra 0.6

## Problems addressed

The previous interface had too many visually independent cards, saturated containers, mixed surface treatments, and an excessively long settings screen. Feature density was high but hierarchy was weak.

## 0.6 design direction

**Minimal premium emulator, not a gaming dashboard.**

- One deep neutral backdrop.
- One restrained translucent panel language.
- Accent color reserved for state, focus, and primary action.
- Glass expressed through transparency, edge light, and atmosphere—not blurred text.
- Rounded geometry is consistent but not bubble-like.
- Typography favors calm semibold headings and readable body text.
- Technical provenance remains visible but is moved into secondary copy.

## UX changes

- Settings now expose one category at a time.
- Navigation uses the same glass grammar on phone and tablet.
- Profile and community surfaces are shorter, clearer, and private-first.
- Catalog import explains the security requirement next to the hash field.
- Notifications are requested only from the Alerts category.
- Sound and haptic previews sit beside their controls.
- Player diagnostic status is explicit but visually quiet.
- Reduced transparency converts glass panels to opaque surfaces.

## Accessibility constraints

- Material color scheme remains opaque for text fields and dialogs.
- Decorative blur is outside content and ignored safely on unsupported systems.
- Controls retain semantic labels and large touch regions.
- Color is not the only status signal.
- Haptics and sounds are separately disableable.
- Reduced motion and high contrast remain first-class preferences.

## Device QA checklist

- 320dp compact phone through tablet/foldable widths;
- font scales 1.0, 1.3, and system maximum;
- TalkBack and controller/D-pad focus order;
- light, dark, OLED, dynamic color, high contrast;
- reduced motion and reduced transparency;
- landscape player with cutouts and gesture navigation;
- 60/90/120/144Hz display modes.
