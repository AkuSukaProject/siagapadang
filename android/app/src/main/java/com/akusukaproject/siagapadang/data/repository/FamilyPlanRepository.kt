package com.akusukaproject.siagapadang.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.akusukaproject.siagapadang.data.model.FamilyMember
import com.akusukaproject.siagapadang.data.model.FamilyPlan
import org.json.JSONArray
import org.json.JSONObject

/**
 * Abstraksi media penyimpanan rencana keluarga untuk memudahkan pengujian unit.
 */
interface FamilyPlanStorage {
    fun loadPlanJson(): String?
    fun savePlanJson(json: String)
}

class SharedPreferencesFamilyPlanStorage(context: Context) : FamilyPlanStorage {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadPlanJson(): String? = prefs.getString(KEY_PLAN, null)

    override fun savePlanJson(json: String) {
        prefs.edit().putString(KEY_PLAN, json).apply()
    }

    companion object {
        const val PREFS_NAME = "siaga_padang_family_plan"
        const val KEY_PLAN = "family_plan"
    }
}

/**
 * Menyimpan rencana titik temu keluarga (F-07) di penyimpanan aplikasi, terpisah dari
 * `ranah_siaga.db` yang bersifat read-only. Seluruh operasi berjalan tanpa jaringan.
 */
class FamilyPlanRepository(
    private val storage: FamilyPlanStorage,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    constructor(context: Context) : this(SharedPreferencesFamilyPlanStorage(context))

    @Synchronized
    fun load(): FamilyPlan {
        val json = storage.loadPlanJson() ?: return FamilyPlan()
        return runCatching { decode(JSONObject(json)) }.getOrDefault(FamilyPlan())
    }

    @Synchronized
    fun setMeetingPoint(name: String?): FamilyPlan =
        update { plan -> plan.copy(meetingPointName = name?.takeIf(String::isNotBlank)) }

    @Synchronized
    fun saveMember(member: FamilyMember): FamilyPlan = update { plan ->
        val cleaned = member.copy(
            name = member.name.trim(),
            routineLocation = member.routineLocation.trim(),
            destinationName = member.destinationName?.takeIf(String::isNotBlank),
        )
        require(cleaned.name.isNotEmpty()) { "Nama anggota keluarga wajib diisi" }
        val exists = plan.members.any { it.id == cleaned.id }
        plan.copy(
            members = if (exists) {
                plan.members.map { if (it.id == cleaned.id) cleaned else it }
            } else {
                plan.members + cleaned
            },
        )
    }

    @Synchronized
    fun removeMember(memberId: String): FamilyPlan =
        update { plan -> plan.copy(members = plan.members.filterNot { it.id == memberId }) }

    private fun update(transform: (FamilyPlan) -> FamilyPlan): FamilyPlan {
        val updated = transform(load()).copy(updatedAtMillis = clock())
        storage.savePlanJson(encode(updated).toString())
        return updated
    }

    private fun encode(plan: FamilyPlan): JSONObject = JSONObject().apply {
        put(KEY_VERSION, FORMAT_VERSION)
        put(KEY_MEETING_POINT, plan.meetingPointName ?: JSONObject.NULL)
        put(KEY_UPDATED_AT, plan.updatedAtMillis ?: JSONObject.NULL)
        put(
            KEY_MEMBERS,
            JSONArray().apply {
                plan.members.forEach { member ->
                    put(
                        JSONObject().apply {
                            put(KEY_ID, member.id)
                            put(KEY_NAME, member.name)
                            put(KEY_ROUTINE_LOCATION, member.routineLocation)
                            put(KEY_DESTINATION, member.destinationName ?: JSONObject.NULL)
                        },
                    )
                }
            },
        )
    }

    private fun decode(json: JSONObject): FamilyPlan {
        val membersJson = json.optJSONArray(KEY_MEMBERS) ?: JSONArray()
        val members = (0 until membersJson.length()).map { index ->
            val member = membersJson.getJSONObject(index)
            FamilyMember(
                id = member.getString(KEY_ID),
                name = member.getString(KEY_NAME),
                routineLocation = member.optString(KEY_ROUTINE_LOCATION),
                destinationName = member.optNullableString(KEY_DESTINATION),
            )
        }
        return FamilyPlan(
            meetingPointName = json.optNullableString(KEY_MEETING_POINT),
            members = members,
            updatedAtMillis = if (json.isNull(KEY_UPDATED_AT)) null else json.optLong(KEY_UPDATED_AT),
        )
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private companion object {
        const val FORMAT_VERSION = 1
        const val KEY_VERSION = "version"
        const val KEY_MEETING_POINT = "meeting_point_name"
        const val KEY_UPDATED_AT = "updated_at_millis"
        const val KEY_MEMBERS = "members"
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_ROUTINE_LOCATION = "routine_location"
        const val KEY_DESTINATION = "destination_name"
    }
}
