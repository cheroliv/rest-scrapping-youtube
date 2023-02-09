package webapp

data class ProblemsModel(
    val type: String,
    val title: String,
    val status: Int,
    val path: String,
    val message: String,
    val fieldErrors: MutableSet<Map<String, String>> = mutableSetOf()
) {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        const val PROBLEM_OBJECT_NAME = "objectName"
        const val PROBLEM_FIELD = "field"
        const val PROBLEM_MESSAGE = "message"
        val detailsKeys = setOf(
            PROBLEM_OBJECT_NAME,
            PROBLEM_FIELD,
            PROBLEM_MESSAGE
        )
    }
}