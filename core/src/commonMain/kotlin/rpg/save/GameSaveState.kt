package rpg.save

/**
 * A serialisable snapshot of a run: progress flags, economy, party HP and the
 * chosen party name. Pure data with no platform dependencies.
 *
 * Persistence note: the project brief suggested kotlinx-serialization, but the
 * top-level task guard forbids touching build files, and `:core` is deliberately
 * dependency-light (no Compose/KorGE/platform code). To honour both, this type
 * ships a small, self-contained JSON round-trip ([toJson]/[fromJson]) instead of
 * adding a serialization plugin. Swap in `@Serializable` later if the build
 * gains the kotlinx-serialization dependency.
 */
data class GameSaveState(
    val phaseOrdinal: Int,
    val gold: Int,
    val potionCounts: Map<String, Int>,
    val equippedWeapons: Set<String>,
    val partyHp: List<Int>,
    val hasReturnedFromSewer: Boolean,
    val chapter2Complete: Boolean,
    val shrineActivated: Boolean,
    val partyName: String?
) {
    /** Serialises this save state to a JSON object string. */
    fun toJson(): String = buildString {
        append('{')
        append("\"phaseOrdinal\":").append(phaseOrdinal).append(',')
        append("\"gold\":").append(gold).append(',')
        append("\"potionCounts\":")
        append('{')
        potionCounts.entries.forEachIndexed { i, (k, v) ->
            if (i > 0) append(',')
            append(Json.quote(k)).append(':').append(v)
        }
        append('}').append(',')
        append("\"equippedWeapons\":")
        append('[')
        equippedWeapons.forEachIndexed { i, w ->
            if (i > 0) append(',')
            append(Json.quote(w))
        }
        append(']').append(',')
        append("\"partyHp\":")
        append('[')
        partyHp.forEachIndexed { i, hp ->
            if (i > 0) append(',')
            append(hp)
        }
        append(']').append(',')
        append("\"hasReturnedFromSewer\":").append(hasReturnedFromSewer).append(',')
        append("\"chapter2Complete\":").append(chapter2Complete).append(',')
        append("\"shrineActivated\":").append(shrineActivated).append(',')
        append("\"partyName\":").append(if (partyName == null) "null" else Json.quote(partyName))
        append('}')
    }

    companion object {
        /** Parses a JSON string produced by [toJson] back into a [GameSaveState]. */
        fun fromJson(json: String): GameSaveState {
            @Suppress("UNCHECKED_CAST")
            val root = Json.parse(json) as? Map<String, Any?>
                ?: error("Invalid GameSaveState JSON: expected an object")

            @Suppress("UNCHECKED_CAST")
            val potions = (root["potionCounts"] as? Map<String, Any?> ?: emptyMap())
                .mapValues { (it.value as Number).toInt() }

            @Suppress("UNCHECKED_CAST")
            val weapons = (root["equippedWeapons"] as? List<Any?> ?: emptyList())
                .map { it as String }
                .toSet()

            @Suppress("UNCHECKED_CAST")
            val hp = (root["partyHp"] as? List<Any?> ?: emptyList())
                .map { (it as Number).toInt() }

            return GameSaveState(
                phaseOrdinal = (root["phaseOrdinal"] as Number).toInt(),
                gold = (root["gold"] as Number).toInt(),
                potionCounts = potions,
                equippedWeapons = weapons,
                partyHp = hp,
                hasReturnedFromSewer = root["hasReturnedFromSewer"] as Boolean,
                chapter2Complete = root["chapter2Complete"] as Boolean,
                shrineActivated = root["shrineActivated"] as Boolean,
                partyName = root["partyName"] as String?
            )
        }
    }
}

/**
 * Minimal JSON reader/writer for the small, well-typed shapes [GameSaveState]
 * needs (objects, arrays, strings, numbers, booleans, null). Not a general-
 * purpose JSON library, but correct for the values this module produces.
 */
private object Json {

    fun quote(s: String): String = buildString {
        append('"')
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> if (c < ' ') {
                    append("\\u")
                    val hex = c.code.toString(16)
                    repeat(4 - hex.length) { append('0') }
                    append(hex)
                } else {
                    append(c)
                }
            }
        }
        append('"')
    }

    fun parse(input: String): Any? {
        val p = Parser(input)
        p.skipWhitespace()
        val value = p.parseValue()
        p.skipWhitespace()
        require(p.atEnd()) { "Trailing characters in JSON at index ${p.pos}" }
        return value
    }

    private class Parser(private val s: String) {
        var pos = 0

        fun atEnd(): Boolean = pos >= s.length

        fun skipWhitespace() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        fun parseValue(): Any? {
            skipWhitespace()
            require(pos < s.length) { "Unexpected end of JSON" }
            return when (s[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') { pos++; return map }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when (next()) {
                    ',' -> continue
                    '}' -> break
                    else -> error("Expected ',' or '}' in object at index ${pos - 1}")
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') { pos++; return list }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                when (next()) {
                    ',' -> continue
                    ']' -> break
                    else -> error("Expected ',' or ']' in array at index ${pos - 1}")
                }
            }
            return list
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                require(pos < s.length) { "Unterminated string" }
                val c = s[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val esc = s[pos++]
                        when (esc) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'u' -> {
                                val hex = s.substring(pos, pos + 4)
                                pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> error("Invalid escape \\$esc")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun parseBoolean(): Boolean = when {
            s.startsWith("true", pos) -> { pos += 4; true }
            s.startsWith("false", pos) -> { pos += 5; false }
            else -> error("Invalid literal at index $pos")
        }

        private fun parseNull(): Any? {
            require(s.startsWith("null", pos)) { "Invalid literal at index $pos" }
            pos += 4
            return null
        }

        private fun parseNumber(): Any {
            val start = pos
            if (peek() == '-') pos++
            while (pos < s.length && (s[pos].isDigit() || s[pos] in ".eE+-")) pos++
            val token = s.substring(start, pos)
            return if (token.any { it == '.' || it == 'e' || it == 'E' }) {
                token.toDouble()
            } else {
                token.toLong()
            }
        }

        private fun peek(): Char {
            skipWhitespace()
            require(pos < s.length) { "Unexpected end of JSON" }
            return s[pos]
        }

        private fun next(): Char {
            skipWhitespace()
            require(pos < s.length) { "Unexpected end of JSON" }
            return s[pos++]
        }

        private fun expect(c: Char) {
            skipWhitespace()
            require(pos < s.length && s[pos] == c) { "Expected '$c' at index $pos" }
            pos++
        }
    }
}
