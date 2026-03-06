package engine.controller.defaults

import engine.model.ScoringRuleBook
import kotlinx.coroutines.CoroutineScope

object ScoreProvider {
    private val tracking = mutableMapOf<String, ScoreTracker>()

    class Builder(private val gameId: String) {
        private var scope: CoroutineScope? = null
        private var ruleBook: ScoringRuleBook? = null

        fun defaultRuleBook(): Builder {
            this.ruleBook = DefaultRulebook()
            return this
        }

        fun withScope(scope: CoroutineScope): Builder {
            this.scope = scope
            return this
        }

        fun withRuleBook(ruleBook: ScoringRuleBook): Builder {
            this.ruleBook = ruleBook
            return this
        }

        fun build(): ScoreTracker {
            assert(scope != null) { "Scope must not be null" }
            assert(ruleBook != null) { "Rulebook must not be null" }
            val scoreTracker = ScoreTracker(ruleBook!!, scope!!)
            tracking[gameId] = scoreTracker
            return scoreTracker
        }

    }

    fun customerBuilder(gameId: String): Builder {
        return Builder(gameId)
    }

    fun defaultBuilder(gameId: String): Builder {
        return Builder(gameId).defaultRuleBook()
    }

    fun getTracker(gameId: String): ScoreTracker {
        return tracking[gameId] ?: error("No tracker found for $gameId")
    }
}