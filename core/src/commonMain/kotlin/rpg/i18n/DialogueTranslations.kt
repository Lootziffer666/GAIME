package rpg.i18n

/**
 * German translations for all scripted dialogue lines and speaker names.
 * Merged into the DE catalog in [Localizer]. Other languages fall back to
 * English for dialogue (consistent with the long-tail fallback policy).
 *
 * Key = canonical English string. Value = German translation.
 */
internal val dialogueDe: Map<String, String> = mapOf(

    // ─── Speaker names ────────────────────────────────────────────────────
    "Barkeep"       to "Wirt",
    "Merchant"      to "Händler",
    "Guard"         to "Wächter",
    "Patron"        to "Gast",
    "Citizen"       to "Bürger",
    "Guildmaster"   to "Gildenmeister",
    "Elder"         to "Ältester",
    "Priest"        to "Priester",
    "Mage"          to "Magier",
    "Questbook"     to "Questbuch",

    // ─── UI strings ───────────────────────────────────────────────────────
    "— PRESS ANY KEY TO BEGIN —" to "— BELIEBIGE TASTE DRÜCKEN —",
    "▶ Continue"    to "▶ Weiter",

    // ─── INTRO_LINES ──────────────────────────────────────────────────────
    "Greetings, stranger."
        to "Gegrüßt sei, Fremder.",
    "You've been officially registered as a Hero Party. Don't ask how."
        to "Ihr wurdet offiziell als Heldengruppe registriert. Fragt nicht wie.",
    "...by who?"
        to "...von wem?",
    "The Questbook. It fell on the desk, opened to the right page, and made a noise like a judge swallowing a bell."
        to "Das Questbuch. Es fiel auf den Tresen, schlug die richtige Seite auf und machte ein Geräusch wie ein Richter, der eine Glocke verschluckt.",
    "Hmm, I wonder what this could be."
        to "Hm, ich frage mich, was das sein könnte.",
    "A mistake."
        to "Ein Fehler.",
    "Around here we call that paperwork."
        to "Hier nennen wir das Papierkram.",
    "So that's how it is then?"
        to "Also so ist das?",
    "Exactly. Congratulations. You're important now, which is generally how things get worse."
        to "Genau. Glückwunsch. Ihr seid jetzt wichtig, was im Allgemeinen der Beginn der Verschlimmerung ist.",

    // ─── FALLING_LINES ────────────────────────────────────────────────────
    "The cellar floor gives way."
        to "Der Kellerboden gibt nach.",
    "The party falls into the sewers below."
        to "Die Gruppe fällt in die Kanalisation darunter.",

    // ─── POST_BOSS_LINES ──────────────────────────────────────────────────
    "The Rat Accountant has been defeated."
        to "Der Ratten-Buchhalter wurde besiegt.",
    "A glowing page flutters from the remains of the filing cabinet."
        to "Eine leuchtende Seite flattert aus den Überresten des Aktenschranks.",
    "This... changes everything."
        to "Das... ändert alles.",

    // ─── RETURN_LINES ────────────────────────────────────────────────────
    "The party climbs back to The Limping Cockatrice."
        to "Die Gruppe klettert zurück zum Hinkenden Cockatrix.",
    "Back already? Smells like sewer."
        to "Schon zurück? Riecht nach Kanalisation.",
    "Quest Pressure: Reset. New quests pending. The Questbook is always listening."
        to "Quest-Druck: Zurückgesetzt. Neue Quests ausstehend. Das Questbuch hört immer zu.",

    // ─── CHAPTER2_MARKET_INTRO_LINES ─────────────────────────────────────
    "The party exits into the market square of Stokeport."
        to "Die Gruppe tritt auf den Marktplatz von Stokeport.",
    "Fresh air! And fresh pockets to pick."
        to "Frische Luft! Und frische Taschen zum Greifen.",
    "No."
        to "Nein.",
    "To observe academically."
        to "Zu wissenschaftlichen Beobachtungszwecken.",
    "The Questbook is restless. It demands a new page."
        to "Das Questbuch ist unruhig. Es verlangt eine neue Seite.",
    "The public quest board rattles. Several notices correct themselves badly."
        to "Das öffentliche Questbrett rasselt. Mehrere Bekanntmachungen korrigieren sich schlecht selbst.",
    "QUEST UPDATED: LOCATE RESPONSIBLE AUTHORITY."
        to "QUEST AKTUALISIERT: VERANTWORTLICHE BEHÖRDE FINDEN.",
    "That sounds expensive."
        to "Das klingt teuer.",
    "That sounds like the guard."
        to "Das klingt nach dem Wächter.",
    "That sounds like the beginning of a lawsuit."
        to "Das klingt nach dem Beginn eines Rechtsstreits.",

    // ─── CHAPTER2_MERCHANT_LINES ─────────────────────────────────────────
    "Greetings, friend."
        to "Gegrüßt, Freund.",
    "See if any of this strikes your fancy."
        to "Schaut, ob euch etwas davon gefällt.",
    "That depends. Is any of it valuable, cursed, or recently unattended?"
        to "Das hängt davon ab. Ist irgendwas davon wertvoll, verflucht oder kürzlich unbeaufsichtigt?",
    "Make me an offer."
        to "Macht mir ein Angebot.",
    "How much do you want for this?"
        to "Wie viel wollt Ihr dafür?",
    "Name your price."
        to "Nennt Euren Preis.",
    "One coin."
        to "Eine Münze.",
    "Insulting."
        to "Beleidigend.",
    "Two coins, but I say it with concern."
        to "Zwei Münzen, aber ich sage es mit Besorgnis.",
    "Might I interest you in this bauble?"
        to "Darf ich Euch für dieses Schmuckstück interessieren?",
    "It glows faintly."
        to "Es leuchtet schwach.",
    "Exactly. Premium faintness."
        to "Genau. Premium-Schwächlichkeit.",
    "What does it do?"
        to "Was macht es?",
    "It suggests importance. That is the backbone of commerce."
        to "Es suggeriert Wichtigkeit. Das ist das Rückgrat des Handels.",
    "I hate how much I respect that."
        to "Ich hasse, wie sehr ich das respektiere.",

    // ─── CHAPTER2_GUARD_LINES ────────────────────────────────────────────
    "Who goes there?"
        to "Wer da?",
    "Former guard. Current inconvenience."
        to "Ex-Wächter. Gegenwärtige Unannehmlichkeit.",
    "Drop your weapons and surrender."
        to "Legt Eure Waffen nieder und ergebt Euch.",
    "Can we surrender emotionally? My arms are tired."
        to "Können wir uns emotional ergeben? Meine Arme sind müde.",
    "Nothing to see here."
        to "Hier gibt es nichts zu sehen.",
    "That phrase has never once meant that."
        to "Dieser Satz hat das noch nie gemeint.",
    "The forest trail east of here has been overrun by wolves."
        to "Der Waldpfad östlich von hier wurde von Wölfen überrannt.",
    "If you're looking for trouble, you'll find it there."
        to "Wer Ärger sucht, wird ihn dort finden.",
    "Just keep to the trail."
        to "Bleibt einfach auf dem Pfad.",
    "Good advice. Last man left the trail and came back with a mushroom calling him father."
        to "Guter Rat. Der letzte Mann verließ den Pfad und kam mit einem Pilz zurück, der ihn Vater nannte.",
    "Was the mushroom rich?"
        to "War der Pilz reich?",
    "Emotionally."
        to "Emotional.",
    "Useless."
        to "Nutzlos.",

    // ─── CHAPTER2_POST_BOSS_LINES ────────────────────────────────────────
    "The Tax Collector Badger has been defeated."
        to "Der Steuereintreiber-Dachs wurde besiegt.",
    "A second glowing page flutters from its stamp collection."
        to "Eine zweite leuchtende Seite flattert aus seiner Stempelsammlung.",
    "So that's how it is then."
        to "Also so ist das dann.",

    // ─── CHAPTER2_RETURN_LINES ───────────────────────────────────────────
    "The party returns to Stokeport Market."
        to "Die Gruppe kehrt zum Stokeport-Markt zurück.",
    "Back already? Been playing in the sewers, have we?"
        to "Schon zurück? Haben wir in der Kanalisation gespielt?",
    "Quest Pressure: Reset. Page 2 secured. The Questbook grows heavier."
        to "Quest-Druck: Zurückgesetzt. Seite 2 gesichert. Das Questbuch wird schwerer.",

    // ─── BARKEEP_PRE_SEWER_LINES ─────────────────────────────────────────
    "Spend some coin or get out."
        to "Gebt Geld aus oder geht raus.",
    "I was considering a third option: standing here suspiciously."
        to "Ich erwog eine dritte Option: verdächtig hier stehenzubleiben.",
    "That option costs two copper."
        to "Diese Option kostet zwei Kupfermünzen.",
    "Barkeep! A flagon of ale!"
        to "Wirt! Einen Krug Bier!",
    "That's nothing a flagon of ale won't fix."
        to "Dafür gibt es kein Problem, das ein Krug Bier nicht löst.",
    "There are very few civic disasters that sentence has not worsened."
        to "Es gibt nur wenige städtische Katastrophen, die dieser Satz nicht verschlimmert hat.",
    "You've gotta try this roast cockatrice."
        to "Ihr müsst diesen gebratenen Cockatrix probieren.",
    "Is it legally food?"
        to "Ist es legal Essen?",
    "Legally adjacent."
        to "Rechtlich angrenzend.",

    // ─── BARKEEP_POST_SEWER_LINES ────────────────────────────────────────
    "Been playing in the sewers, have we?"
        to "Haben wir in der Kanalisation gespielt?",
    "Playing implies consent."
        to "Spielen impliziert Einverständnis.",
    "The floor gave way."
        to "Der Boden gab nach.",
    "Happens when the city outsources maintenance to destiny."
        to "Passiert, wenn die Stadt die Wartung an das Schicksal auslagert.",
    "We found a page from the Questbook."
        to "Wir haben eine Seite des Questbuchs gefunden.",
    "Ah. Wet, cursed, or stamped?"
        to "Ah. Nass, verflucht oder gestempelt?",
    "Yes."
        to "Ja.",
    "Then it's official."
        to "Dann ist es offiziell.",

    // ─── PATRON_LINES ────────────────────────────────────────────────────
    "He sure is slow for a four-armed bartender."
        to "Er ist ziemlich langsam für einen vierarmigen Wirt.",
    "Four arms, one work ethic. Tragic ratio."
        to "Vier Arme, eine Arbeitsmoral. Tragisches Verhältnis.",
    "I heard the Questbook once registered a sneeze as a holy expedition."
        to "Ich hörte, das Questbuch registrierte einmal ein Niesen als heilige Expedition.",
    "A common error. Many prophecies begin as respiratory events."
        to "Ein häufiger Fehler. Viele Prophezeiungen beginnen als Atemwegsereignisse.",
    "I hear the king likes to wear evening gowns."
        to "Ich höre, der König trägt gerne Abendkleider.",
    "Irrelevant."
        to "Irrelevant.",
    "Not if the gowns have pockets."
        to "Nicht, wenn die Kleider Taschen haben.",

    // ─── HEROES_HOME_EXT_LINES ───────────────────────────────────────────
    "The party steps outside into the morning air."
        to "Die Gruppe tritt in die Morgenluft hinaus.",
    "Fresh air. Suspicious. Usually costs extra."
        to "Frische Luft. Verdächtig. Kostet normalerweise extra.",
    "The guild hall is down the road."
        to "Die Gildenhalle ist die Straße runter.",
    "And the Questbook is still restless."
        to "Und das Questbuch ist noch immer unruhig.",
    "Are you the official heroes?"
        to "Seid Ihr die offiziellen Helden?",
    "Officially? Yes. Competently? Define your terms."
        to "Offiziell? Ja. Kompetent? Definiert Eure Begriffe.",
    "The last official hero tried to rescue a bucket because someone said it had fallen."
        to "Der letzte offizielle Held versuchte, einen Eimer zu retten, weil jemand sagte, er sei gefallen.",
    "Did it need rescuing?"
        to "Brauchte er Rettung?",
    "It was a bucket."
        to "Es war ein Eimer.",
    "A classic ambiguity. Container, victim, or metaphor."
        to "Eine klassische Mehrdeutigkeit. Behälter, Opfer oder Metapher.",
    "If the bucket had treasure, I support its recovery."
        to "Wenn der Eimer Schatz hatte, unterstütze ich seine Bergung.",

    // ─── GUILDMASTER_LINES ───────────────────────────────────────────────
    "Greetings, friends."
        to "Gegrüßt, Freunde.",
    "Registered heroes may pick up contracts at the board inside."
        to "Registrierte Helden dürfen Aufträge am Brett drinnen abholen.",
    "Non-registered adventurers are asked to leave or be fined."
        to "Nicht registrierte Abenteurer werden gebeten zu gehen oder werden mit einer Geldstrafe belegt.",
    "We're registered. The Questbook said so."
        to "Wir sind registriert. Das Questbuch hat es gesagt.",
    "The Questbook also once registered a sandwich as missing royalty."
        to "Das Questbuch hat auch einmal ein Sandwich als vermisstes Mitglied des Adels registriert.",
    "Was it?"
        to "War es das?",
    "No. But it had a crown-shaped bite mark, and the law is weaker than presentation."
        to "Nein. Aber es hatte einen kronenförmigen Biss, und das Gesetz ist schwächer als Erscheinung.",
    "Knowledge is the answer."
        to "Wissen ist die Antwort.",
    "Experience is how we grow."
        to "Erfahrung ist, wie wir wachsen.",
    "Pain is how we invoice."
        to "Schmerz ist, wie wir abrechnen.",
    "Grab your torch. There's work to be done."
        to "Nehmt Eure Fackel. Es gibt Arbeit zu erledigen.",

    // ─── CHAPEL_DEVOTEE_LINES ────────────────────────────────────────────
    "Our prayers will be answered."
        to "Unsere Gebete werden erhört.",
    "That's either comforting or a threat."
        to "Das ist entweder tröstlich oder eine Drohung.",
    "The chapel has been... quiet lately. Too quiet."
        to "Die Kapelle war... letztens still. Zu still.",
    "This is unusual."
        to "Das ist ungewöhnlich.",
    "Quiet chapels are rarely quiet for affordable reasons."
        to "Stille Kapellen sind selten aus erschwinglichen Gründen still.",
    "Something moved the pews. Something large."
        to "Etwas hat die Kirchenbänke verschoben. Etwas Großes.",
    "Animal?"
        to "Ein Tier?",
    "No. It organized them alphabetically by guilt."
        to "Nein. Es ordnete sie alphabetisch nach Schuld.",
    "I don't like furniture with judgment."
        to "Ich mag kein Mobiliar mit Urteilsvermögen.",
    "I need to speak to the town guard."
        to "Ich muss mit dem Stadtgarde sprechen.",
    "The town guard is currently arresting itself."
        to "Der Stadtgarde verhaftet sich gerade selbst.",
    "Again?"
        to "Schon wieder?",
    "Perfect. Let's go in."
        to "Perfekt. Gehen wir hinein.",

    // ─── TEMPLE_EXT_INTRO_LINES ──────────────────────────────────────────
    "The ruined temple exterior. Overgrown. Unsettled."
        to "Das verfallene Tempeläußere. Überwuchert. Unruhig.",
    "Wolves."
        to "Wölfe.",
    "Lots of wolves."
        to "Viele Wölfe.",
    "And markings on the stones."
        to "Und Markierungen auf den Steinen.",
    "Do the markings say treasure?"
        to "Sagen die Markierungen Schatz?",
    "They say warning."
        to "Sie sagen Warnung.",
    "Ancient people were terrible at marketing."
        to "Die alten Leute waren schrecklich im Marketing.",
    "There are all manner of creatures within these woods."
        to "In diesen Wäldern gibt es allerlei Kreaturen.",
    "Look sharp."
        to "Aufgepasst.",
    "I prefer looking profitable."
        to "Ich bevorzuge profitabel auszusehen.",
    "The deeper we go, the darker it gets."
        to "Je tiefer wir gehen, desto dunkler wird es.",
    "That is how depth works."
        to "So funktioniert Tiefe.",
    "Not always spiritually."
        to "Nicht immer spirituell.",
    "Especially spiritually."
        to "Besonders spirituell.",

    // ─── CHAPEL_INTERIOR_LINES ───────────────────────────────────────────
    "Blessings upon you, travelers."
        to "Segen über euch, Reisende.",
    "Are they refundable?"
        to "Sind sie erstattungsfähig?",
    "The chapel does not sell blessings."
        to "Die Kapelle verkauft keine Segen.",
    "That sounds exactly like something said before a donation box."
        to "Das klingt genau wie etwas, das vor einem Spendenkasten gesagt wird.",
    "We must live by the teachings of holy wisdom."
        to "Wir müssen nach den Lehren der heiligen Weisheit leben.",
    "Holy wisdom is rarely the issue. The issue is usually the people who laminate it."
        to "Heilige Weisheit ist selten das Problem. Das Problem sind gewöhnlich die Menschen, die sie laminieren.",
    "The Questbook passed through here once."
        to "Das Questbuch ist hier einmal durchgekommen.",
    "When?"
        to "Wann?",
    "Before the pews began confessing."
        to "Bevor die Kirchenbänke zu beichten begannen.",
    "Furniture shouldn't have interiority."
        to "Mobiliar sollte keine Innerlichkeit haben.",
    "It heard the words 'save all souls' and began sorting parishioners by theological compliance."
        to "Es hörte die Worte 'Rette alle Seelen' und begann, Gemeindemitglieder nach theologischer Konformität zu sortieren.",
    "A machine cannot understand mercy."
        to "Eine Maschine kann Gnade nicht verstehen.",
    "No. But it understands categories."
        to "Nein. Aber es versteht Kategorien.",
    "QUEST NOTED: REVIEW SOUL CLASSIFICATION."
        to "QUEST VERMERKT: SEELENKLASSIFIZIERUNG ÜBERPRÜFEN.",
    "Good. The book is getting religious. That always ends in architecture."
        to "Gut. Das Buch wird religiös. Das endet immer in Architektur.",

    // ─── CHAPTER2_GUILDHALL_INTERIOR_LINES ───────────────────────────────
    "Of all the arcane lore..."
        to "Von all dem arkanen Wissen...",
    "That is a dangerous way to start a sentence."
        to "Das ist eine gefährliche Art, einen Satz zu beginnen.",
    "The Questbook is not cursed."
        to "Das Questbuch ist nicht verflucht.",
    "It drops stamps from the ceiling."
        to "Es lässt Stempel von der Decke fallen.",
    "That is civic enchantment. Different smell."
        to "Das ist städtische Verzauberung. Anderer Geruch.",
    "Sometimes. Sometimes knowledge is the door, the trap, and the idiot holding the torch."
        to "Manchmal. Manchmal ist Wissen die Tür, die Falle und der Idiot, der die Fackel hält.",
    "Which one am I?"
        to "Welcher bin ich?",
    "You are the reason we label torches."
        to "Du bist der Grund, warum wir Fackeln beschriften.",
    "Now what was that incantation?"
        to "Wie war noch mal dieser Zauberspruch?",
    "Ah. The Questbook reacts to declared meaning, not truth."
        to "Ah. Das Questbuch reagiert auf erklärte Bedeutung, nicht auf Wahrheit.",
    "Plain words."
        to "Klare Worte.",
    "If someone says a thing like it matters, the book tries to make it matter."
        to "Wenn jemand etwas so sagt, als ob es wichtig wäre, versucht das Buch, es wichtig zu machen.",
    "So confidence is dangerous."
        to "Also ist Selbstvertrauen gefährlich.",
    "Historically, yes."
        to "Historisch gesehen, ja.",
    "QUEST UPDATED: SEEK NEXT PAGE."
        to "QUEST AKTUALISIERT: NÄCHSTE SEITE SUCHEN.",
    "There. It did it again. Self-important little library."
        to "Da. Es hat es wieder getan. Selbstgefällige kleine Bücherei.",

    // ─── CHAPTER3_BOSS_MEDUSA_INTRO_LINES ────────────────────────────────
    "The forest opens into a stone clearing. Statues stand in neat rows, each frozen mid-regret."
        to "Der Wald öffnet sich in eine steinerne Lichtung. Statuen stehen in ordentlichen Reihen, jede eingefroren mitten im Bedauern.",
    "Those are decorative, right?"
        to "Die sind dekorativ, oder?",
    "The balance of life and death sits on a knife's edge."
        to "Das Gleichgewicht von Leben und Tod steht auf Messers Schneide.",
    "I preferred decorative."
        to "Ich bevorzugte dekorativ.",
    "Another party. Another prophecy with boots."
        to "Eine weitere Gruppe. Eine weitere Prophezeiung mit Stiefeln.",
    "Stand aside."
        to "Tretet beiseite.",
    "I stood aside once. A hero called it destiny and built a statue park out of my afternoon."
        to "Ich trat einmal beiseite. Ein Held nannte es Schicksal und baute aus meinem Nachmittag einen Statuenpark.",
    "We do not seek violence."
        to "Wir suchen keine Gewalt.",
    "No one ever does. They seek glory, answers, treasure, closure, content. Violence is the delivery method."
        to "Niemand tut das je. Sie suchen Ruhm, Antworten, Schatz, Abschluss, Inhalt. Gewalt ist die Liefermethode.",
    "I only seek treasure."
        to "Ich suche nur Schatz.",
    "Honest. Repulsive, but honest."
        to "Ehrlich. Abstoßend, aber ehrlich.",
    "QUEST ACCEPTED: DEFEAT THE MONSTER."
        to "QUEST ANGENOMMEN: BESIEGE DAS MONSTER.",
    "There it is. The little book sees a woman with snakes and files paperwork for murder."
        to "Da ist es. Das kleine Buch sieht eine Frau mit Schlangen und reicht Papierkram für Mord ein.",
    "Then we break the paperwork."
        to "Dann zerbrechen wir den Papierkram.",
    "Or at least misfile it."
        to "Oder zumindest falsch ablegen.",

    // ─── CHAPTER3_BOSS_MEDUSA_POST_LINES ─────────────────────────────────
    "The stone light fades. The statues remain statues, but somehow look less blamed."
        to "Das Steinlicht erlischt. Die Statuen bleiben Statuen, sehen aber irgendwie weniger beschuldigt aus.",
    "You did not kill me."
        to "Ihr habt mich nicht getötet.",
    "Wasn't ordered."
        to "War kein Auftrag.",
    "Also, unclear loot table."
        to "Außerdem unklare Beutetabelle.",
    "The Questbook named you monster because the story needed one."
        to "Das Questbuch nannte dich Monster, weil die Geschichte eines brauchte.",
    "Stories always need monsters. It saves them from needing context."
        to "Geschichten brauchen immer Monster. Es bewahrt sie davor, Kontext zu brauchen.",
    "QUEST COMPLETE: MONSTER ENCOUNTER RESOLVED."
        to "QUEST ABGESCHLOSSEN: MONSTER-BEGEGNUNG GELÖST.",
    "Resolved is doing a lot of work there."
        to "'Gelöst' leistet da viel Arbeit.",
    "Take your page. And if the book asks who the villain was, tell it to look in a mirror."
        to "Nehmt Eure Seite. Und wenn das Buch fragt, wer der Schurke war, sagt ihm, es solle in einen Spiegel schauen.",
    "Hard-won knowledge."
        to "Mühsam erworbenes Wissen.",
    "The party receives a torn Questbook page, warm as if recently embarrassed."
        to "Die Gruppe erhält eine zerrissene Questbuch-Seite, warm als wäre sie kürzlich in Verlegenheit gebracht worden.",

    // ─── VILLAGE_ELDER_LINES ─────────────────────────────────────────────
    "Well met."
        to "Gut getroffen.",
    "That depends on whether this becomes unpaid advice."
        to "Das hängt davon ab, ob das zu unbezahltem Rat wird.",
    "The Questbook was built after the old wars, when heroes became too expensive and prophecies too vague."
        to "Das Questbuch wurde nach den alten Kriegen gebaut, als Helden zu teuer und Prophezeiungen zu vage wurden.",
    "So the city automated courage."
        to "Also automatisierte die Stadt Mut.",
    "The city automated liability."
        to "Die Stadt automatisierte Haftung.",
    "Knowledge was the first draft. Then came committees."
        to "Wissen war der erste Entwurf. Dann kamen Ausschüsse.",
    "And nobody stopped them?"
        to "Und niemand hielt sie auf?",
    "Everyone thought it would help the little man."
        to "Jeder dachte, es würde dem kleinen Mann helfen.",
    "Did it?"
        to "Tat es das?",
    "It helped the little man fill out forms explaining why help was unavailable."
        to "Es half dem kleinen Mann, Formulare auszufüllen, die erklärten, warum Hilfe nicht verfügbar war.",
    "That sounds like government."
        to "Das klingt nach Regierung.",
    "That sounds like everything, eventually."
        to "Das klingt irgendwann nach allem.",
    "Remember this: the Questbook does not hate people."
        to "Denkt daran: Das Questbuch hasst Menschen nicht.",
    "It merely turns their needs into obligations."
        to "Es verwandelt lediglich ihre Bedürfnisse in Verpflichtungen.",
    "That may be worse."
        to "Das könnte schlimmer sein.",

    // ─── CHAPTER2_BRIDGE_LINES ───────────────────────────────────────────
    "A narrow bridge crosses the muddy river east of Stokeport."
        to "Eine schmale Brücke überquert den schlammigen Fluss östlich von Stokeport.",
    "Careful crossing. The bridge has been asking travelers for purpose."
        to "Vorsicht beim Überqueren. Die Brücke fragt Reisende nach ihrem Zweck.",
    "Bridges should ask for tolls. Like honest criminals."
        to "Brücken sollten Maut verlangen. Wie ehrliche Kriminelle.",
    "A man said he was 'just passing through.' The Questbook marked him as temporary infrastructure."
        to "Ein Mann sagte, er sei 'nur auf der Durchreise.' Das Questbuch markierte ihn als temporäre Infrastruktur.",
    "Where is he?"
        to "Wo ist er?",
    "Second plank from the left."
        to "Zweite Planke von links.",
    "No, this is Stokeport."
        to "Nein, das ist Stokeport.",
    "If you hear singing under the bridge, do not answer."
        to "Wenn Ihr Gesang unter der Brücke hört, antwortet nicht.",
    "Is it a troll?"
        to "Ist es ein Troll?",
    "Worse. A bard with unresolved metaphor."
        to "Schlimmer. Ein Barde mit ungelöster Metapher.",
    "QUEST NOTED: INSPECT BRIDGE PURPOSE."
        to "QUEST VERMERKT: BRÜCKENZWECK ÜBERPRÜFEN.",
    "Ignore it."
        to "Ignoriert es.",
    "I love how that has become our strategy."
        to "Ich liebe, wie das zu unserer Strategie geworden ist.",

    // ─── CHAPTER1_OUTRO_LINES ────────────────────────────────────────────
    "The first page settles into the Questbook with a damp administrative sigh."
        to "Die erste Seite legt sich mit einem feuchten administrativen Seufzer ins Questbuch.",
    "Objective complete."
        to "Ziel erfüllt.",
    "Do we get paid?"
        to "Werden wir bezahlt?",
    "We learned something."
        to "Wir haben etwas gelernt.",
    "That is not currency."
        to "Das ist keine Währung.",
    "QUEST UPDATED: REPORT TO CIVIC AUTHORITY."
        to "QUEST AKTUALISIERT: AN STAATSBEHÖRDE MELDEN.",
    "Town guard."
        to "Stadtgarde.",
    "Nothing good starts with those two words."
        to "Nichts Gutes beginnt mit diesen zwei Wörtern.",

    // ─── CHAPTER2_OUTRO_LINES ────────────────────────────────────────────
    "The guardhouse falls quiet. Several laws stop contradicting themselves out loud."
        to "Das Wachhaus wird still. Mehrere Gesetze hören auf, sich laut zu widersprechen.",
    "Order restored."
        to "Ordnung wiederhergestellt.",
    "Order lightly embarrassed."
        to "Ordnung leicht in Verlegenheit gebracht.",
    "The Questbook is learning the shape of authority."
        to "Das Questbuch lernt die Form von Autorität.",
    "Does it have to?"
        to "Muss es das?",
    "QUEST UPDATED: FOLLOW THE FOREST TRAIL."
        to "QUEST AKTUALISIERT: DEM WALDPFAD FOLGEN.",
    "The trail has never met us. It should be afraid."
        to "Der Pfad hat uns nie getroffen. Er sollte Angst haben.",

    // ─── CHAPTER3_OUTRO_LINES ────────────────────────────────────────────
    "The forest stops rearranging itself, which somehow feels judgmental."
        to "Der Wald hört auf, sich neu anzuordnen, was sich irgendwie urteilend anfühlt.",
    "The page is real."
        to "Die Seite ist echt.",
    "The tree was too helpful. I feel violated by advice."
        to "Der Baum war zu hilfsbereit. Ich fühle mich von Ratschlägen verletzt.",
    "We ignored the correct path."
        to "Wir ignorierten den richtigen Weg.",
    "And therefore found the right one."
        to "Und fanden daher den richtigen.",
    "I hate when wisdom sounds like budget design."
        to "Ich hasse es, wenn Weisheit wie Budgetplanung klingt.",
    "QUEST COMPLETE: DIRECTIONS ACQUIRED."
        to "QUEST ABGESCHLOSSEN: WEGBESCHREIBUNG ERWORBEN.",
    "Good. Can we acquire lunch?"
        to "Gut. Können wir auch Mittagessen erwerben?"
)
