import kotlinx.serialization.Serializable

@Serializable
sealed interface ChatMessageChunk {
    sealed interface Function : ChatMessageChunk {
        @Serializable
        data class Question(
            val question: String,
            val variants: List<String>,
            val descriptions: List<String> = emptyList(),
            val answer: String? = null,
            val questionType: Type? = null,
        ) : Function {
            @Serializable
            sealed class Type {
                @Serializable
                @Deprecated("Use `MultiChoice` instead")
                object Confirmation : Type()

                @Serializable
                object MultiChoice : Type()
            }
        }
    }
}