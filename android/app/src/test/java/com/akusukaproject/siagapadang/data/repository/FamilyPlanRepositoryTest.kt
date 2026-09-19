package com.akusukaproject.siagapadang.data.repository

import com.akusukaproject.siagapadang.data.model.FamilyMember
import com.akusukaproject.siagapadang.data.model.FamilyPlan
import com.akusukaproject.siagapadang.ui.familyplan.FamilyPlanShareText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyPlanRepositoryTest {
    private class InMemoryStorage(var json: String? = null) : FamilyPlanStorage {
        override fun loadPlanJson(): String? = json
        override fun savePlanJson(json: String) {
            this.json = json
        }
    }

    private val ibu = FamilyMember(
        id = "a",
        name = "  Ibu ",
        routineLocation = " Pasar Raya ",
        destinationName = "MESJID RAYA IKUR KOTO",
    )

    @Test
    fun emptyStorageReturnsEmptyPlan() {
        val plan = FamilyPlanRepository(InMemoryStorage()).load()

        assertTrue(plan.isEmpty)
        assertNull(plan.updatedAtMillis)
    }

    @Test
    fun savedPlanSurvivesNewRepositoryInstance() {
        val storage = InMemoryStorage()
        FamilyPlanRepository(storage, clock = { 1_000L }).apply {
            setMeetingPoint("GOR H. AGUS SALIM")
            saveMember(ibu)
        }

        val reloaded = FamilyPlanRepository(storage).load()

        assertEquals("GOR H. AGUS SALIM", reloaded.meetingPointName)
        assertEquals(1, reloaded.members.size)
        assertEquals("Ibu", reloaded.members.single().name)
        assertEquals("Pasar Raya", reloaded.members.single().routineLocation)
        assertEquals("MESJID RAYA IKUR KOTO", reloaded.members.single().destinationName)
        assertEquals(1_000L, reloaded.updatedAtMillis)
    }

    @Test
    fun savingExistingMemberReplacesInsteadOfDuplicating() {
        val repository = FamilyPlanRepository(InMemoryStorage())
        repository.saveMember(ibu)

        val plan = repository.saveMember(ibu.copy(destinationName = null))

        assertEquals(1, plan.members.size)
        assertNull(plan.members.single().destinationName)
    }

    @Test
    fun removeMemberKeepsOthers() {
        val repository = FamilyPlanRepository(InMemoryStorage())
        repository.saveMember(ibu)
        repository.saveMember(ibu.copy(id = "b", name = "Adi"))

        val plan = repository.removeMember("a")

        assertEquals(listOf("Adi"), plan.members.map { it.name })
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankNameIsRejected() {
        FamilyPlanRepository(InMemoryStorage()).saveMember(ibu.copy(name = "   "))
    }

    @Test
    fun corruptStorageFallsBackToEmptyPlan() {
        val plan = FamilyPlanRepository(InMemoryStorage("{bukan json")).load()

        assertTrue(plan.isEmpty)
    }

    @Test
    fun shareTextNamesDestinationsWithoutCoordinates() {
        val text = FamilyPlanShareText.build(
            FamilyPlan(
                meetingPointName = "GOR H. AGUS SALIM",
                members = listOf(ibu.copy(name = "Ibu", routineLocation = "Pasar Raya")),
            ),
        )

        assertTrue(text.contains("Titik temu keluarga: GOR H. AGUS SALIM"))
        assertTrue(text.contains("- Ibu (Pasar Raya) -> MESJID RAYA IKUR KOTO"))
        assertTrue(text.contains("berjalan cepat"))
        assertFalse(text.contains("lari", ignoreCase = true))
        assertFalse(text.contains("-0.9"))
    }
}
