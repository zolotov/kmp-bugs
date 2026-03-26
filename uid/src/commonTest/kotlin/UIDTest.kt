package fleet.util.test

import fleet.util.UID
import kotlin.test.Test
import kotlin.test.assertTrue

class UIDTest {
  @Test
  fun `test toString`() {
    val uid = UID.random()
    assertTrue(uid.toString().matches(Regex("[a-z0-9]{1,20}+")), uid.toString())
  }
}