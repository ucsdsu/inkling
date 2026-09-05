package dev.inkling.speech

class FakeRecognizer : RecognitionInput {
    data class Attempt(
        val onResult: (String, Float) -> Unit,
        val onError: (String) -> Unit,
    )

    override val available = true
    override val lastMode = "offline"
    val attempts = mutableListOf<Attempt>()
    var cancellations = 0

    override fun listen(onResult: (String, Float) -> Unit, onError: (String) -> Unit) {
        attempts += Attempt(onResult, onError)
    }

    override fun cancel() { cancellations++ }
    override fun destroy() {}
}

class FakeSpeaker : SpeechSpeaker {
    data class Line(
        val onWord: (Int) -> Unit,
        val onDone: () -> Unit,
        val onUnavailable: (String) -> Unit,
    )

    var queuesLine = true
    var unavailableMessage: String? = null
    val lines = mutableListOf<Line>()
    var stops = 0

    override fun speakLine(
        line: String,
        onWord: (Int) -> Unit,
        onDone: () -> Unit,
        onUnavailable: (String) -> Unit,
    ): Boolean {
        lines += Line(onWord, onDone, onUnavailable)
        unavailableMessage?.let(onUnavailable)
        return queuesLine
    }

    override fun speakChunks(
        chunks: List<String>,
        word: String,
        onDone: () -> Unit,
        onUnavailable: (String) -> Unit,
    ): Boolean = true

    override fun stop() { stops++ }
    override fun shutdown() {}
}
