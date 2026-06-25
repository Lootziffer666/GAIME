# Localization Policy

The game ships a **complete German localization** of all user-facing **text**.
**Audio is never localized** — spoken voice barks and the songs stay English
(see docs/SONGBOOK.md and the bark audio system).

## How it works

- Localization lives in `:core` (`rpg.i18n`): `Locale { EN, DE }`, a `GameLocale`
  holder the renderer sets, and `Localizer` — a catalog keyed by the **canonical
  English string** with a German value and graceful English fallback.
- Pure logic (e.g. `QuestbookProcessor`) keeps producing canonical English; the
  renderer turns it into display text via `Localizer.localize(text, locale)` or
  `QuestbookReaction.displayText(locale)` / `String.localized()`.
- Quest-pressure labels use `Localizer.pressureLabel(...)`.

## Rules for new content

1. Any new user-facing string (Questbook reaction, chapter/boss/enemy/page name,
   UI label) MUST get a German entry in `Localizer.de`.
2. `LocalizationTest` enforces coverage: every `Chapter.title`,
   `CampaignBoss.displayName`, `EnemyArchetype.displayName`,
   `QuestbookPage.displayTitle`, and the `QuestPressure` labels must have a German
   translation. Add the translation in the same change that adds the string.
3. Keep the dry, bureaucratic tone in German (forms, permits, filings).
4. Do NOT add translations for bark `audioText` or song lyrics — those are audio.

## Renderer wiring

- The renderer sets `GameLocale.current` from a language setting and renders all
  text through the localizer. Compose UI chrome strings are being retired with
  the KorGE migration (`.kiro/steering/rendering-engine.md`); the KorGE `:game`
  module reads everything through `:core`'s `Localizer`.
