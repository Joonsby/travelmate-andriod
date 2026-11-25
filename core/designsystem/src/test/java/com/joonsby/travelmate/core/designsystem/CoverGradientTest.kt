package com.joonsby.travelmate.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverGradientTest {
    /** 기대값은 iOS 공식(unicodeScalars, &* 31 &+, & 0x7FFFFFFF, % 6)을 그대로 계산한 것 */
    @Test
    fun `같은 여행지는 iOS 와 같은 색 조합을 고른다`() {
        assertEquals(4, CoverGradient.index("도쿄"))
        assertEquals(1, CoverGradient.index("제주도 여행"))
        assertEquals(1, CoverGradient.index("Paris"))
        assertEquals(1, CoverGradient.index("김여행"))
        // 이모지는 UTF-16 두 글자지만 유니코드 한 글자로 센다.
        assertEquals(3, CoverGradient.index("🍣 오사카"))
        assertEquals(0, CoverGradient.index(""))
    }
}
