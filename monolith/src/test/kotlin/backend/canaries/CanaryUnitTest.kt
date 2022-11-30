@file:Suppress("unused")

package backend.canaries

import kotlin.test.assertEquals


class CanaryUnitTest {
    //    @kotlin.test.Test @kotlin.test.Ignore
    fun addition_isCorrect() = assertEquals(
        expected = 4,
        actual = 2 + 2
    )
}