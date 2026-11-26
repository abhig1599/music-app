package com.abhi.musicapp

import com.abhi.musicapp.analysis.MixPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.abs
import kotlin.math.min

/**
 * SYNTHETIC mix-quality harness: made-up beat grids only, never real songs.
 *
 * Sweeps outgoing tempo, tempo relation, incoming grid first-beat offset and per-beat jitter through
 * [MixPlanner.plan], prints aggregate planned-vs-fallback rates to the unit-test log (prefix MIXQA), and
 * asserts the plan contract on every returned plan. The printed numbers measure the planner MATHS on
 * synthetic grids: they are not measured library fallback rates and say nothing about how mixes sound.
 */
class MixQualityHarnessTest {

    private fun beats(bpm: Float, startMs: Double, count: Int, jitterMs: Double, rng: Random): List<Long> {
        val period = 60000.0 / bpm
        val out = ArrayList<Long>(count)
        var last = -1L
        for (k in 0 until count) {
            var t = startMs + k * period
            if (jitterMs > 0.0) t += rng.nextGaussian() * jitterMs
            val v = maxOf(0L, Math.round(t))
            if (v > last) { out.add(v); last = v }
        }
        return out
    }

    private data class Cell(val runs: Int, val planned: Int, val medianResidualMs: Double?)

    @Test fun syntheticSweepHonoursThePlanContract() {
        val outBpms = floatArrayOf(90f, 100f, 110f, 120f, 128f, 130f, 140f, 150f)
        val rels = listOf("same" to 1.0f, "+2%" to 1.02f, "-2%" to 0.98f, "+5%" to 1.05f,
            "+7%" to 1.07f, "half" to 0.5f, "double" to 2.0f, "+20%" to 1.20f)
        val deltaFracs = doubleArrayOf(0.0, 0.02, 0.05, 0.10, 0.25, 0.50, 1.00)
        val jitters = doubleArrayOf(0.0, 10.0)
        val outDur = 210_000L
        val contentEnd = 198_700L
        val inDur = 200_000L
        val fadeMs = 12_000L
        val cap = min(fadeMs, min(outDur / 3, inDur / 3))

        val byDelta = LinkedHashMap<Double, Pair<Int, Int>>() // frac -> (planned, runs)
        val byJitter = LinkedHashMap<Double, Pair<Int, Int>>()
        var totalRuns = 0
        var totalPlanned = 0

        for (oBpm in outBpms) {
            val oPeriod = 60000.0 / oBpm
            val ob = beats(oBpm, 400.0, (outDur / oPeriod).toInt() + 2, 0.0, Random(1))
            val obSet = ob.toSet()
            for ((relName, relFactor) in rels) {
                val iBpm = oBpm * relFactor
                if (iBpm < 60f || iBpm > 180f) continue
                val iPeriod = 60000.0 / iBpm
                for (frac in deltaFracs) {
                    val deltaMs = frac * iPeriod
                    for (jit in jitters) {
                        val trials = if (jit > 0.0) 25 else 1
                        var planned = 0
                        val residuals = ArrayList<Double>()
                        for (t in 0 until trials) {
                            val rng = Random(1000L * t + 7)
                            val ib = beats(iBpm, deltaMs, 90, jit, rng)
                            val p = MixPlanner.plan(ob, oBpm, .9f, outDur, contentEnd,
                                ib, iBpm, .9f, inDur, fadeMs, null, null)
                            totalRuns++
                            if (p == null) continue
                            planned++
                            totalPlanned++
                            residuals.add(p.maxResidualMs)
                            // Plan contract, checked on every synthetic plan.
                            assertTrue("start is an outgoing beat", p.outStartMs in obSet)
                            assertEquals("ends exactly at content end", contentEnd, p.outEndMs)
                            assertTrue("overlap in window", p.overlapMs in 500L..cap)
                            assertTrue("residual in phase budget: ${p.maxResidualMs}",
                                p.maxResidualMs <= MixPlanner.MAX_PHASE_ERROR_MS + 1e-9)
                            assertTrue("nudge within cap: ${p.incomingSpeed}",
                                abs(p.incomingSpeed - 1f) <= 0.06f + 1e-6f)
                            assertEquals("no RMS evidence -> unity trim", 1f, p.inGain, 0f)
                        }
                        val med = residuals.sorted().let { if (it.isEmpty()) null else it[it.size / 2] }
                        byDelta.merge(frac, planned to trials) { a, b -> (a.first + b.first) to (a.second + b.second) }
                        byJitter.merge(jit, planned to trials) { a, b -> (a.first + b.first) to (a.second + b.second) }
                        if (trials == 1 && med != null) {
                            // quiet
                        }
                        Cell(trials, planned, med)
                    }
                }
            }
        }

        println("MIXQA[SYNTHETIC] total planned $totalPlanned/$totalRuns = ${pct(totalPlanned, totalRuns)}")
        for ((frac, pr) in byDelta) {
            println("MIXQA[SYNTHETIC] byGridOffset delta=${frac}P planned ${pr.first}/${pr.second} = ${pct(pr.first, pr.second)}")
        }
        for ((jit, pr) in byJitter) {
            println("MIXQA[SYNTHETIC] byJitter sigma=${jit}ms planned ${pr.first}/${pr.second} = ${pct(pr.first, pr.second)}")
        }
        assertTrue("sweep actually ran", totalRuns > 1000)
    }

    @Test fun fixedAnchors() {
        val outDur = 210_000L
        val contentEnd = 198_700L
        val ob = beats(120f, 400.0, 500, 0.0, Random(1))
        // Ideal aligned grids always plan.
        assertNotNull(MixPlanner.plan(ob, 120f, .9f, outDur, contentEnd,
            beats(120f, 0.0, 90, 0.0, Random(2)), 120f, .9f, 200_000L, 12_000L, null, null))
        // A half-period incoming grid offset never plans under index-locked matching.
        assertNull(MixPlanner.plan(ob, 120f, .9f, outDur, contentEnd,
            beats(120f, 250.0, 90, 0.0, Random(3)), 120f, .9f, 200_000L, 12_000L, null, null))
        // Tempo outside the 6% nudge cap or the half/double window never plans.
        assertNull(MixPlanner.plan(ob, 120f, .9f, outDur, contentEnd,
            beats(128.4f, 0.0, 96, 0.0, Random(4)), 128.4f, .9f, 200_000L, 12_000L, null, null))
        assertNull(MixPlanner.plan(ob, 120f, .9f, outDur, contentEnd,
            beats(144f, 0.0, 108, 0.0, Random(5)), 144f, .9f, 200_000L, 12_000L, null, null))
    }

    @Test fun planningIsDeterministicForSameInputs() {
        val outDur = 210_000L
        val contentEnd = 198_700L
        val ob = beats(128f, 400.0, 450, 0.0, Random(11))
        val mkIn = { beats(128f, 3.0, 90, 10.0, Random(99)) }
        val a = MixPlanner.plan(ob, 128f, .9f, outDur, contentEnd, mkIn(), 128f, .9f, 200_000L, 12_000L, null, null)
        val b = MixPlanner.plan(ob, 128f, .9f, outDur, contentEnd, mkIn(), 128f, .9f, 200_000L, 12_000L, null, null)
        if (a == null) assertNull(b) else {
            assertNotNull(b); b!!
            assertEquals(a.outStartMs, b.outStartMs)
            assertEquals(a.overlapMs, b.overlapMs)
            assertEquals(a.maxResidualMs, b.maxResidualMs, 0.0)
            assertEquals(a.incomingSpeed, b.incomingSpeed, 0f)
        }
    }

    /** Detector imperfections, applied to made-up grids: missed beats. */
    private fun dropped(base: List<Long>, p: Double, rng: Random): List<Long> =
        if (p <= 0.0) base else base.filter { rng.nextDouble() >= p }

    /** Detector imperfections: spurious half-interval ghost beats. */
    private fun extras(base: List<Long>, p: Double, rng: Random): List<Long> {
        if (p <= 0.0) return base
        val out = ArrayList<Long>(base.size * 2)
        for (k in base.indices) {
            out.add(base[k])
            if (k + 1 < base.size && rng.nextDouble() < p) out.add((base[k] + base[k + 1]) / 2)
        }
        return out.distinct().sorted()
    }

    /** Swung timing: every second beat delayed by [swing] of a period. Reported bpm stays constant. */
    private fun swung(bpm: Float, startMs: Double, count: Int, swing: Double): List<Long> {
        val period = 60000.0 / bpm
        return (0 until count).map { maxOf(0L, Math.round(startMs + it * period + if (it % 2 == 1) swing * period else 0.0)) }
    }

    /** Tempo drift: true bpm ramps linearly to bpm*(1+[driftFrac]) by the last beat; reported bpm constant. */
    private fun drifted(bpm: Float, startMs: Double, count: Int, driftFrac: Double): List<Long> {
        val out = ArrayList<Long>(count)
        var t = startMs
        out.add(maxOf(0L, Math.round(t)))
        for (k in 1 until count) {
            val inst = bpm * (1.0 + driftFrac * (k - 1).toDouble() / (count - 1))
            t += 60000.0 / inst
            out.add(Math.round(t))
        }
        return out.distinct().sorted()
    }

    @Test fun swingBeyondThePhaseBudgetNeverPlans() {
        val ob = beats(128f, 400.0, 500, 0.0, Random(1))
        // 30% swing at 128bpm shifts every second beat by ~141ms: far beyond the 45ms budget, must fall back.
        assertNull(MixPlanner.plan(ob, 128f, .9f, 210_000L, 198_700L,
            swung(128f, 0.0, 90, 0.30), 128f, .9f, 200_000L, 12_000L, null, null))
        // The same fixture unswung is an aligned grid and must plan.
        assertNotNull(MixPlanner.plan(ob, 128f, .9f, 210_000L, 198_700L,
            swung(128f, 0.0, 90, 0.0), 128f, .9f, 200_000L, 12_000L, null, null))
    }

    /**
     * Detector-realistic imperfections, one dimension at a time over a reduced base sweep. SYNTHETIC:
     * printed rates measure planner maths on made-up grids, never the real library or detector.
     */
    @Test fun detectorImperfectionSweeps() {
        val outDur = 210_000L
        val contentEnd = 198_700L
        val inDur = 200_000L
        val fadeMs = 12_000L
        val cap = min(fadeMs, min(outDur / 3, inDur / 3))
        val outBpms = floatArrayOf(110f, 128f, 140f)
        val rels = listOf(1.0f, 1.02f, 0.5f)
        val deltas = doubleArrayOf(0.0, 0.02)
        val trials = 15
        val dims = linkedMapOf(
            "DroppedBeats" to doubleArrayOf(0.0, 0.02, 0.05),
            "ExtraBeats" to doubleArrayOf(0.0, 0.02, 0.05),
            "TempoDrift" to doubleArrayOf(0.0, 0.005, 0.01),
            "Swing" to doubleArrayOf(0.0, 0.10, 0.30))
        var totalRuns = 0
        for ((dim, levels) in dims) {
            for (level in levels) {
                var runs = 0
                var planned = 0
                for (oBpm in outBpms) {
                    val oPeriod = 60000.0 / oBpm
                    val ob = beats(oBpm, 400.0, (outDur / oPeriod).toInt() + 2, 0.0, Random(1))
                    val obSet = ob.toSet()
                    for (rel in rels) {
                        val iBpm = oBpm * rel
                        if (iBpm < 60f || iBpm > 180f) continue
                        val iPeriod = 60000.0 / iBpm
                        for (frac in deltas) {
                            val deltaMs = frac * iPeriod
                            for (t in 0 until trials) {
                                val rng = Random(5000L * t + 13)
                                var ib = when (dim) {
                                    "TempoDrift" -> drifted(iBpm, deltaMs, 90, level)
                                    "Swing" -> swung(iBpm, deltaMs, 90, level)
                                    else -> beats(iBpm, deltaMs, 90, 10.0, rng)
                                }
                                ib = when (dim) {
                                    "DroppedBeats" -> dropped(ib, level, rng)
                                    "ExtraBeats" -> extras(ib, level, rng)
                                    else -> ib
                                }
                                val p = MixPlanner.plan(ob, oBpm, .9f, outDur, contentEnd,
                                    ib, iBpm, .9f, inDur, fadeMs, null, null)
                                runs++
                                totalRuns++
                                if (p == null) continue
                                planned++
                                assertTrue("start is an outgoing beat", p.outStartMs in obSet)
                                assertEquals("ends exactly at content end", contentEnd, p.outEndMs)
                                assertTrue("overlap in window", p.overlapMs in 500L..cap)
                                assertTrue("residual in phase budget: ${p.maxResidualMs}",
                                    p.maxResidualMs <= MixPlanner.MAX_PHASE_ERROR_MS + 1e-9)
                                assertTrue("nudge within cap: ${p.incomingSpeed}",
                                    abs(p.incomingSpeed - 1f) <= 0.06f + 1e-6f)
                                assertEquals("no RMS evidence -> unity trim", 1f, p.inGain, 0f)
                            }
                        }
                    }
                }
                println("MIXQA[SYNTHETIC] by$dim level=$level planned $planned/$runs = ${pct(planned, runs)}")
            }
        }
        assertTrue("sweep actually ran", totalRuns > 500)
    }

    private fun pct(n: Int, d: Int): String = "%.1f%%".format(100.0 * n / d)
}
