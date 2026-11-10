package ru.kbzhu.tracker.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kbzhu.tracker.domain.model.ActivityLevel
import ru.kbzhu.tracker.domain.model.Gender
import ru.kbzhu.tracker.domain.model.Goal
import ru.kbzhu.tracker.domain.model.MacroSplit
import ru.kbzhu.tracker.domain.model.UserProfile

class NutritionCalculatorTest {

    private val man = UserProfile(
        gender = Gender.MALE, age = 30, heightCm = 180, weightKg = 80.0,
        goal = Goal.LOSE, targetWeightKg = 75.0, weeklyRateKg = 0.5,
        activityLevel = ActivityLevel.MODERATE, macroSplit = MacroSplit(30, 30, 40)
    )

    @Test
    fun bmr_mifflinStJeor() {
        // 10·80 + 6,25·180 − 5·30 + 5 = 1780
        assertEquals(1780.0, NutritionCalculator.bmr(Gender.MALE, 80.0, 180, 30), 0.01)
        // 10·60 + 6,25·165 − 5·25 − 161 = 1345,25
        assertEquals(1345.25, NutritionCalculator.bmr(Gender.FEMALE, 60.0, 165, 25), 0.01)
    }

    @Test
    fun lose_halfKgPerWeek() {
        val r = NutritionCalculator.calculate(man)
        assertEquals(2759.0, r.tdee, 0.01)          // 1780 × 1,55
        assertEquals(2210, r.targets.calories)       // 2759 − 550 ≈ 2209 → округление до 10
        assertEquals(166, r.targets.protein)         // 2210·0,3/4
        assertEquals(74, r.targets.fat)              // 2210·0,3/9
        assertEquals(221, r.targets.carbs)           // 2210·0,4/4
        assertEquals(false, r.clampedToMinimum)
        assertEquals(-549, r.dailyAdjustment)        // 2210 − 2759
        assertEquals(11, r.weeksToGoal)              // 5 кг / 0,499 кг в неделю = 10,02 → 11
    }

    @Test
    fun lose_isClampedToSafeMinimum() {
        val small = UserProfile(
            Gender.FEMALE, 45, 155, 52.0, Goal.LOSE, 48.0, 1.0, ActivityLevel.SEDENTARY
        )
        val r = NutritionCalculator.calculate(small)
        assertTrue(r.clampedToMinimum)
        assertTrue(r.targets.calories >= NutritionCalculator.MIN_KCAL_FEMALE)
    }

    @Test
    fun maintain_hasNoAdjustment() {
        val r = NutritionCalculator.calculate(man.copy(goal = Goal.MAINTAIN))
        assertEquals(2760, r.targets.calories)
        assertNull(r.weeksToGoal)
    }

    @Test
    fun water_isRoundedAndBounded() {
        assertEquals(2400, NutritionCalculator.recommendedWaterMl(80.0))
        assertEquals(1500, NutritionCalculator.recommendedWaterMl(30.0))
    }
}
