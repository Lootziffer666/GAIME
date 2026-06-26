# Localization Policy

The game ships localization of all user-facing **text** across the most-spoken
UI languages. **Audio is never localized** — spoken voice barks and the songs
stay English (see docs/SONGBOOK.md and the bark audio system).

Shipped languages: **EN, DE, ES, FR, IT, PT, RU, ZH (Simplified), JA**.
- **German** is a *complete* translation (every Questbook line included).
- The other languages cover the high-visibility **structural set**
  (`Localizer.requiredKeys`: chapter/boss/enemy/page names, generic headers, and
  the headline Questbook lines). The long tail of Questbook reactions falls back
  to English until translated — maximum reach for minimal effort.
- ES/FR/IT/PT are high-confidence; RU/ZH/JA are initial drafts for native review.
- Arabic and other RTL languages are deferred (need RTL rendering support).

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
   UI label) MUST get a German entry in `Localizer`'s `de` map. If it is a
   structural/high-visibility string, add it to `Localizer.requiredKeys` and
   translate it in every shipped language (`Translations.kt`).
2. `LocalizationTest` enforces coverage: German covers every enum display name;
   every shipped language covers `requiredKeys` and localizes the pressure
   labels. Add translations in the same change that adds the string.
3. Keep the dry, bureaucratic tone (forms, permits, filings) in every language.
4. Do NOT add translations for bark `audioText` or song lyrics — those are audio.

## Renderer wiring

- The renderer sets `GameLocale.current` from a language setting and renders all
  text through the localizer. Compose UI chrome strings are being retired with
  the KorGE migration (`.kiro/steering/rendering-engine.md`); the KorGE `:game`
  module reads everything through `:core`'s `Localizer`.
