package com.example.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regresiones de dictado operativo, fusión y correcciones puntuales de placas.
 * Pruebas puras: no requieren micrófono, red ni dispositivo Android.
 */
class AviParserVoiceQualityTest {
    private val vias = setOf(101, 102)

    @Test
    fun `a targeted last digit correction preserves action and lane`() {
        val initial = AviParser.parse("fuga vía 101 placa I1L111", vias)
        assertTrue(initial.valido)

        val corrected = AviParser.mergeCorrection(
            initial,
            "corrige último dígito negativo",
            vias
        )
        assertTrue(corrected.errores.joinToString(), corrected.valido)
        assertEquals("I1L110", corrected.placa)
        assertEquals(101, corrected.via)
        assertEquals("FUGA", corrected.accion)
    }

    @Test
    fun `a targeted prefix correction accepts a spoken NATO letter`() {
        val initial = AviParser.parse("liberado vía 102 placa I1L110", vias)
        assertTrue(initial.valido)

        val corrected = AviParser.mergeCorrection(
            initial,
            "cambia segundo carácter alfa",
            vias
        )
        assertTrue(corrected.errores.joinToString(), corrected.valido)
        assertEquals("IAL110", corrected.placa)
        assertEquals(102, corrected.via)
        assertEquals("LIBERADO", corrected.accion)
    }

    @Test
    fun `invalid character correction does not silently accept the old plate`() {
        val initial = AviParser.parse("derivado vía 101 placa I1L110", vias)
        val corrected = AviParser.mergeCorrection(
            initial,
            "corrige último dígito lima",
            vias
        )
        assertFalse(corrected.valido)
        assertEquals("I1L110", corrected.placa)
        assertTrue(corrected.errores.any { it.contains("carácter") })
    }

    @Test
    fun `short plate hypotheses do not contaminate votes for a full plate`() {
        val result = AviParser.fuseHypotheses(
            candidates = listOf(
                "fuga vía 101 placa I1L11",
                "fuga vía 101 placa I1L110"
            ),
            allowedVias = vias
        )
        assertTrue(result.parsed.errores.joinToString(), result.parsed.valido)
        assertEquals("I1L110", result.parsed.placa)
    }

    @Test
    fun `conflicting final digits show reduced consensus instead of false certainty`() {
        val result = AviParser.fuseHypotheses(
            candidates = listOf(
                "fuga vía 101 placa I1L110",
                "fuga vía 101 placa I1L111"
            ),
            allowedVias = vias
        )
        assertTrue(AviParser.isValidPeruPlate(result.parsed.placa))
        assertEquals(6, result.platePositionConfidence.size)
        assertTrue(result.platePositionConfidence[5] < 0.60f)
        assertTrue(result.platePositionConfidence[0] >= 0.99f)
    }

    @Test
    fun `full plate correction still replaces all six characters`() {
        val initial = AviParser.parse("fuga vía 101 placa I1L111", vias)
        val corrected = AviParser.mergeCorrection(
            initial, "placa alfa primero lima primero primero negativo", vias
        )
        assertTrue(corrected.errores.joinToString(), corrected.valido)
        assertEquals("A1L110", corrected.placa)
    }
}
