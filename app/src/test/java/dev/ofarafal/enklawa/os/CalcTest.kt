package dev.ofarafal.enklawa.os

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {
    @Test fun precedence() = assertEquals(14.0, Calc.eval("2+3*4")!!, 0.0)
    @Test fun parentheses() = assertEquals(20.0, Calc.eval("(2+3)×4")!!, 0.0)
    @Test fun division() = assertEquals(2.5, Calc.eval("10÷4")!!, 0.0)
    @Test fun unaryMinus() = assertEquals(-3.0, Calc.eval("-5+2")!!, 0.0)
    @Test fun unicodeMinus() = assertEquals(1.0, Calc.eval("3−2")!!, 0.0)
    @Test fun divisionByZeroIsNull() = assertNull(Calc.eval("1/0"))
    @Test fun incompleteIsNull() { assertNull(Calc.eval("2+")); assertNull(Calc.eval("2×(3")); assertNull(Calc.eval("")) }
    @Test fun garbageIsNull() = assertNull(Calc.eval("2a+3"))
    @Test fun formatsFloatNoise() = assertEquals("0.3", Calc.format(Calc.eval("0.1+0.2")!!))
    @Test fun formatsIntegers() = assertEquals("-12", Calc.format(-12.0))
}
