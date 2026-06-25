package rpg.i18n

/**
 * Supported UI languages. Spoken voice barks and songs stay English regardless
 * of this setting (they are audio, not text) -- see docs/SONGBOOK.md and the
 * bark audio system.
 *
 * German is a *complete* translation. The other languages cover the
 * high-visibility "structural" set ([Localizer.requiredKeys]: chapter/boss/
 * enemy/page names, quest-pressure labels, and the headline Questbook lines);
 * any untranslated long-tail line falls back to English. See
 * .kiro/steering/localization.md.
 */
enum class Locale { EN, DE, ES, FR, IT, PT, RU, ZH, JA }

/**
 * The currently selected UI language. The renderer (Compose today, KorGE next)
 * sets this; pure logic stays renderer-agnostic and reads it only when turning
 * a canonical English string into display text.
 */
object GameLocale {
    var current: Locale = Locale.EN
}

/**
 * Localization of the game's user-facing text.
 *
 * Design: the canonical English string (as produced by [rpg.questbook.QuestbookProcessor]
 * and the various `displayName`/`title` properties) is the lookup key. [localize]
 * returns the translation for the target [Locale], or the original English when
 * a key is missing -- so a missing entry degrades gracefully instead of
 * crashing. [LocalizationTest] enforces that every supported locale covers
 * [requiredKeys] (the structural, high-visibility set) and that German covers
 * everything.
 *
 * Voice/song audio is intentionally NOT localized.
 */
object Localizer {

    fun localize(text: String, locale: Locale = GameLocale.current): String =
        catalogs[locale]?.get(text) ?: text

    /** True if [text] has an explicit German translation (German is complete). */
    fun hasGerman(text: String): Boolean = de.containsKey(text)

    /** True if [text] is translated in [locale]. */
    fun hasTranslation(text: String, locale: Locale): Boolean =
        locale == Locale.EN || catalogs[locale]?.containsKey(text) == true

    /** Localized LOW/MEDIUM/HIGH label for the quest-pressure meter. */
    fun pressureLabel(level: String, locale: Locale = GameLocale.current): String =
        pressure[locale]?.get(level.uppercase()) ?: level

    /** All English keys that have a German translation (for tests/tools). */
    val germanKeys: Set<String> get() = de.keys

    /** Every non-English UI language the game ships. */
    val translatedLocales: List<Locale> get() = Locale.entries.filter { it != Locale.EN }

    /**
     * The structural, high-visibility strings every supported locale must
     * translate (chapter/boss/enemy/page names, generic headers, and the
     * headline Questbook lines). The long tail of Questbook reactions is fully
     * translated in German and falls back to English elsewhere.
     */
    val requiredKeys: Set<String> = setOf(
        "QUEST PRESSURE", "QUEST ACCEPTED", "GAME OVER",
        "The Four-Armed Bartender",
        "The Sewers of Bad Decisions",
        "The Market of Mandatory Commerce",
        "The Woods That Had Opinions",
        "The Ship That Was Technically Seaworthy",
        "The Dragon That Was Accidentally Summoned",
        "Done Enough",
        "The Rat Accountant",
        "The Guard Captain Who Cannot Legally Move",
        "The Helpful Tree",
        "Captain Formbeard",
        "The Administragon",
        "Sewer Rat",
        "Sludge Blob",
        "Forest Wolf",
        "The Tax Collector Badger",
        "Kobold Scout",
        "Quest Wisp",
        "Pirate Clerk",
        "Barrel Mimic",
        "The Page of Beginnings",
        "The Page of Terms and Conditions",
        "The Page of Directions",
        "The Page of Claims and Rewards",
        "URGENT QUEST ACCEPTED: DEFEAT THE DRAGON",
        "QUEST ACCEPTED: IDENTIFY THE HORSE",
        "QUEST ACCEPTED: APPRAISE THE GOLD",
        "QUEST ACCEPTED: OPEN THE DOOR",
        "Atmospheric observation noted",
        "Official Quest Registered: Locate subterranean valuables (Priority: Mandatory)",
        "Name Registration Complete: 'Everything Changes' -- Official Party Name Locked"
    )

    /** Per-locale catalogs. English has none (identity). Lazy so all maps
     *  (incl. the large German map below and the per-language files) are
     *  initialised before they are referenced. */
    private val catalogs: Map<Locale, Map<String, String>> by lazy {
        mapOf(
            Locale.DE to de,
            Locale.ES to es,
            Locale.FR to fr,
            Locale.IT to it,
            Locale.PT to pt,
            Locale.RU to ru,
            Locale.ZH to zh,
            Locale.JA to ja
        )
    }

    /** Per-locale quest-pressure labels. */
    private val pressure: Map<Locale, Map<String, String>> = mapOf(
        Locale.DE to mapOf("LOW" to "NIEDRIG", "MEDIUM" to "MITTEL", "HIGH" to "HOCH"),
        Locale.ES to mapOf("LOW" to "BAJO", "MEDIUM" to "MEDIO", "HIGH" to "ALTO"),
        Locale.FR to mapOf("LOW" to "FAIBLE", "MEDIUM" to "MOYEN", "HIGH" to "ÉLEVÉ"),
        Locale.IT to mapOf("LOW" to "BASSO", "MEDIUM" to "MEDIO", "HIGH" to "ALTO"),
        Locale.PT to mapOf("LOW" to "BAIXO", "MEDIUM" to "MÉDIO", "HIGH" to "ALTO"),
        Locale.RU to mapOf("LOW" to "НИЗКИЙ", "MEDIUM" to "СРЕДНИЙ", "HIGH" to "ВЫСОКИЙ"),
        Locale.ZH to mapOf("LOW" to "低", "MEDIUM" to "中", "HIGH" to "高"),
        Locale.JA to mapOf("LOW" to "低", "MEDIUM" to "中", "HIGH" to "高")
    )

    private val de: Map<String, String> = mapOf(
        // ─── Quest pressure / generic UI ─────────────────────────────────
        "QUEST PRESSURE" to "QUEST-DRUCK",
        "QUEST ACCEPTED" to "QUEST ANGENOMMEN",
        "GAME OVER" to "SPIEL VORBEI",
        "Quest Pressure" to "Quest-Druck",

        // ─── Chapter titles ──────────────────────────────────────────────
        "The Four-Armed Bartender" to "Der vierarmige Wirt",
        "The Sewers of Bad Decisions" to "Die Kanalisation schlechter Entscheidungen",
        "The Market of Mandatory Commerce" to "Der Markt des verpflichtenden Handels",
        "The Woods That Had Opinions" to "Der Wald, der Meinungen hatte",
        "The Ship That Was Technically Seaworthy" to "Das Schiff, das technisch gesehen seetüchtig war",
        "The Dragon That Was Accidentally Summoned" to "Der Drache, der versehentlich beschworen wurde",
        "Done Enough" to "Genug getan",

        // ─── Bosses / enemies (displayName) ──────────────────────────────
        "The Rat Accountant" to "Der Ratten-Buchhalter",
        "The Guard Captain Who Cannot Legally Move" to "Der Wachhauptmann, der sich rechtlich nicht bewegen darf",
        "The Helpful Tree" to "Der hilfsbereite Baum",
        "Captain Formbeard" to "Kapitän Formbart",
        "The Administragon" to "Der Administragon",
        "Sewer Rat" to "Kanalratte",
        "Sludge Blob" to "Schlammklumpen",
        "Forest Wolf" to "Waldwolf",
        "The Tax Collector Badger" to "Der Steuereintreiber-Dachs",
        "Kobold Scout" to "Kobold-Späher",
        "Quest Wisp" to "Quest-Irrlicht",
        "Pirate Clerk" to "Piraten-Sachbearbeiter",
        "Barrel Mimic" to "Fass-Mimik",

        // ─── Questbook pages ─────────────────────────────────────────────
        "The Page of Beginnings" to "Die Seite der Anfänge",
        "The Page of Terms and Conditions" to "Die Seite der Allgemeinen Geschäftsbedingungen",
        "The Page of Directions" to "Die Seite der Wegbeschreibungen",
        "The Page of Claims and Rewards" to "Die Seite der Ansprüche und Belohnungen",

        // ─── Questbook reactions: Prologue / Chapter 1 ───────────────────
        "Valuables Located: Filing Cabinet (Contents: Rats)" to "Wertgegenstände lokalisiert: Aktenschrank (Inhalt: Ratten)",
        "Official Quest Registered: Locate subterranean valuables (Priority: Mandatory)" to "Offizielle Quest registriert: Unterirdische Wertgegenstände lokalisieren (Priorität: Verpflichtend)",
        "Official Quest Registered: Locate subterranean valuables (Source: Self)" to "Offizielle Quest registriert: Unterirdische Wertgegenstände lokalisieren (Quelle: Selbst)",
        "Incident Report Filed: Denial of Involvement (Case #0001) -- Noted for Records" to "Vorfallsbericht eingereicht: Bestreiten der Beteiligung (Fall #0001) -- Aktenkundig vermerkt",
        "Amendment Filed: Structural Assessment of Municipal Underground -- Status: Satisfactory" to "Nachtrag eingereicht: Bauliche Bewertung der städtischen Unterwelt -- Status: Zufriedenstellend",
        "Academic Grant Approved: Research Into Obstruction Removal (Budget: 0 Gold)" to "Forschungsstipendium bewilligt: Untersuchung zur Hindernisbeseitigung (Budget: 0 Gold)",
        "Research complete: No findings" to "Forschung abgeschlossen: Keine Erkenntnisse",
        "Demolition Permit Issued: Immediate Effect" to "Abrissgenehmigung erteilt: Mit sofortiger Wirkung",
        "Aggression: Unfocused" to "Aggression: Unfokussiert",
        "Controlled Burn Authorization: Filed" to "Genehmigung für kontrolliertes Abbrennen: Eingereicht",
        "Burn permit: No valid target" to "Brenngenehmigung: Kein gültiges Ziel",
        "Name Registration Complete: 'Everything Changes' -- Official Party Name Locked" to "Namensregistrierung abgeschlossen: 'Alles ändert sich' -- Offizieller Gruppenname festgelegt",
        "Major Amendment Filed: Reality Reassessment In Progress" to "Großer Nachtrag eingereicht: Neubewertung der Realität in Bearbeitung",
        "Olfactory hazard documented" to "Geruchsgefahr dokumentiert",
        "Route Optimisation Request: Approved" to "Antrag auf Routenoptimierung: Genehmigt",
        "Shortcut: Go Back" to "Abkürzung: Kehren Sie um",
        "Asset Inspection Authorised (Subjects: Alerted)" to "Inventarinspektion genehmigt (Subjekte: Alarmiert)",
        "No pockets found in vicinity" to "Keine Taschen in der Umgebung gefunden",
        "Ambient noise: classified as non-threatening" to "Umgebungsgeräusch: Als ungefährlich eingestuft",
        "Treasure Audit Initiated (Hazard: Possible)" to "Schatzprüfung eingeleitet (Gefahr: Möglich)",
        "Treasure audit: Inconclusive" to "Schatzprüfung: Ergebnislos",
        "Tactical Withdrawal Logged (Direction: Approximate)" to "Taktischer Rückzug protokolliert (Richtung: Ungefähr)",
        "Sub-Objective Generated: Await Further Instruction" to "Teilziel generiert: Weitere Anweisungen abwarten",
        "Provision request filed (Priority: Low)" to "Verpflegungsantrag eingereicht (Priorität: Niedrig)",
        "Perimeter Defence Filed (Aggro: Concentrated)" to "Perimeterverteidigung eingereicht (Aggro: Konzentriert)",
        "Perimeter: Secure" to "Perimeter: Gesichert",
        "Mass Demolition Permit Issued (Scope: Indiscriminate)" to "Massenabrissgenehmigung erteilt (Umfang: Wahllos)",
        "Destruction permit: Void (nothing to demolish)" to "Abrissgenehmigung: Ungültig (nichts abzureißen)",
        "Cryogenic Works Permit: Approved" to "Genehmigung für Kryo-Arbeiten: Erteilt",
        "Cooling request: Denied (no substrate)" to "Kühlungsantrag: Abgelehnt (kein Substrat)",
        "Accuracy confirmed. Filed under: Pedantry" to "Korrektheit bestätigt. Abgelegt unter: Pedanterie",
        "Disclosure Request Granted: Fine Print Revealed" to "Offenlegungsantrag bewilligt: Kleingedrucktes enthüllt",
        "No fine print located (font size: adequate)" to "Kein Kleingedrucktes gefunden (Schriftgröße: angemessen)",
        "Atmospheric observation noted" to "Atmosphärische Beobachtung vermerkt",
        "Amendment: Already on record" to "Nachtrag: Bereits aktenkundig",
        "Orders: Survive" to "Befehle: Überleben",
        "Filed in triplicate. No further action at maximum pressure." to "In dreifacher Ausfertigung abgelegt. Keine weitere Maßnahme bei Maximaldruck.",

        // ─── Questbook reactions: Chapter 2 ──────────────────────────────
        "Surrender Filing: Rejected (Reason: Insufficient Paperwork)" to "Kapitulationsantrag: Abgelehnt (Grund: Unzureichender Papierkram)",
        "Commercial Survey Initiated: Catalogue all vendor-adjacent valuables (Mandatory)" to "Handelserhebung eingeleitet: Alle händlernahen Wertgegenstände katalogisieren (Verpflichtend)",
        "Gold detected: Source unverified" to "Gold erkannt: Quelle ungeprüft",
        "Municipal Inquiry Filed: Authority Structure Report (Budget: 0 Gold)" to "Städtische Anfrage eingereicht: Bericht zur Behördenstruktur (Budget: 0 Gold)",
        "Wildlife Census Ordered: Document all woodland fauna (Deadline: Immediate)" to "Wildtierzählung angeordnet: Gesamte Waldfauna dokumentieren (Frist: Sofort)",
        "Elemental Claim Registered: Command over elements noted (Scope: Undefined)" to "Elementaranspruch registriert: Befehlsgewalt über Elemente vermerkt (Umfang: Undefiniert)",
        "Electrical Works Permit: Granted (Safety Notice: None)" to "Genehmigung für Elektroarbeiten: Erteilt (Sicherheitshinweis: Keiner)",
        "Lightning request: No valid conductor" to "Blitzantrag: Kein gültiger Leiter",
        "Existential Risk Assessment: Filed (Priority: Philosophical)" to "Existenzielle Risikobewertung: Eingereicht (Priorität: Philosophisch)",

        // ─── Questbook reactions: Chapters 3-5 + Finale ──────────────────
        "Objective Logged: Closed (Satisfaction: Assumed)" to "Ziel protokolliert: Geschlossen (Zufriedenheit: Angenommen)",
        "Knowledge Filed (Effort: Noted, Reward: Pending)" to "Wissen abgelegt (Aufwand: Vermerkt, Belohnung: Ausstehend)",
        "Navigation Quest Reissued: Destination Recalculated" to "Navigations-Quest neu ausgestellt: Ziel neu berechnet",
        "Direction noted. Map unchanged (for now)" to "Richtung vermerkt. Karte unverändert (vorerst)",
        "Cartographic Asset Catalogued" to "Kartografisches Gut katalogisiert",
        "No cartographic asset detected" to "Kein kartografisches Gut erkannt",
        "Seaworthiness Assessment: Technically" to "Seetüchtigkeitsbewertung: Technisch gesehen",
        "Anchorage Released (Direction: Approximate)" to "Ankerplatz freigegeben (Richtung: Ungefähr)",
        "Anchor status: ambiguous" to "Ankerstatus: Mehrdeutig",
        "Mooring Logged (Permanence: Doubtful)" to "Vertäuung protokolliert (Dauerhaftigkeit: Zweifelhaft)",
        "Canvas Deployment Filed" to "Segelsetzung eingereicht",
        "Departure Authorised (Heading: Reverse)" to "Abfahrt genehmigt (Kurs: Rückwärts)",
        "Tactical Disadvantage Filed (Blame: Pending)" to "Taktischer Nachteil eingereicht (Schuld: Ausstehend)",
        "Retreat denied by paperwork." to "Rückzug durch Papierkram verweigert.",
        "Tactical Withdrawal Logged (Direction: Away)" to "Taktischer Rückzug protokolliert (Richtung: Weg)",
        "URGENT QUEST ACCEPTED: DEFEAT THE DRAGON" to "DRINGENDE QUEST ANGENOMMEN: BESIEGE DEN DRACHEN",
        "Threat Acknowledgement Filed: Monsters Marked" to "Bedrohungsbestätigung eingereicht: Monster markiert",
        "No monsters in vicinity (regrettably)" to "Keine Monster in der Umgebung (bedauerlicherweise)",
        "Defensive Posture Mandated (Line: Imaginary)" to "Defensivhaltung angeordnet (Linie: Eingebildet)",
        "Asset Protection Order Filed (Asset: Undefined)" to "Anordnung zum Objektschutz eingereicht (Objekt: Undefiniert)",
        "QUEST ACCEPTED: IDENTIFY THE HORSE" to "QUEST ANGENOMMEN: IDENTIFIZIERE DAS PFERD",
        "QUEST ACCEPTED: APPRAISE THE GOLD" to "QUEST ANGENOMMEN: SCHÄTZE DAS GOLD",
        "QUEST ACCEPTED: OPEN THE DOOR" to "QUEST ANGENOMMEN: ÖFFNE DIE TÜR",
        "Entry Reclassified as Pre-Authorised" to "Zutritt neu eingestuft als Vorab-Genehmigt",

        // ─── Quest marker hints (shown on markers) ───────────────────────
        "nearest interactable" to "nächstes interaktives Objekt",
        "Investigate Reported Innocence" to "Gemeldete Unschuld untersuchen",
        "entrance" to "Eingang",
        "objective" to "Ziel",
        "market valuables" to "Marktwertgegenstände",
        "woodland fauna" to "Waldfauna",
        "Recalculated Route" to "Neu berechnete Route",
        "dragon (to be generated)" to "Drache (zu generieren)",
        "nearest monster" to "nächstes Monster",
        "horse" to "Pferd",
        "gold" to "Gold",
        "door" to "Tür"
    )
}

/** Convenience: localize this canonical English string for [locale]. */
fun String.localized(locale: Locale = GameLocale.current): String = Localizer.localize(this, locale)


/** The Questbook reaction's display text, localized for [locale]. */
fun rpg.questbook.QuestbookReaction.displayText(locale: Locale = GameLocale.current): String =
    Localizer.localize(questbookText, locale)
