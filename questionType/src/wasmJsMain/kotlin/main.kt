import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement


fun main() {
    with(document) {
        val outputDiv = getElementById("output")!!.unsafeCast<HTMLDivElement>()
        val button = getElementById("goButton")!!.unsafeCast<HTMLButtonElement>()
        button.onclick = {
            outputDiv.textContent = createQuestion(it.metaKey)
        }
    }
}

private fun createQuestion(confirmation: Boolean): String {
    val questionType = if (confirmation) ChatMessageChunk.Function.Question.Type.Confirmation else ChatMessageChunk.Function.Question.Type.MultiChoice
    // the following line fixes the issue
    // val questionType: ChatMessageChunk.Function.Question.Type = if (confirmation) ChatMessageChunk.Function.Question.Type.Confirmation else ChatMessageChunk.Function.Question.Type.MultiChoice
    return ChatMessageChunk.Function.Question(
        question = "",
        variants = emptyList(),
        descriptions = emptyList(),
        questionType = questionType,
    ).toString()
}