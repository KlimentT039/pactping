package com.klt.pactping

import com.klt.pactping.domain.Record
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordTest {
  @Test fun ratio_is_zero_when_no_history() {
    assertEquals(0f, Record(0, 0).keptRatio)
  }

  @Test fun ratio_reflects_kept_share() {
    val r = Record(kept = 13, broken = 3)
    assertEquals(16, r.total)
    assertEquals(13f / 16f, r.keptRatio)
  }
}
