package com.bulletin.hyundai.data

import android.content.Context
import com.bulletin.hyundai.core.CoachItem
import com.bulletin.hyundai.core.DriveRecord
import com.bulletin.hyundai.core.ItemTally
import com.bulletin.hyundai.core.MemoryNote
import com.bulletin.hyundai.core.Phrases
import org.json.JSONArray
import org.json.JSONObject

/** 주행 기록과 "다음 운전 때 기억할 것"을 기기에 저장한다. */
class DriveRepository(context: Context) {

    private val prefs = context.getSharedPreferences("coach", Context.MODE_PRIVATE)

    fun records(): List<DriveRecord> {
        val arr = JSONArray(prefs.getString(KEY_RECORDS, "[]"))
        return (0 until arr.length()).mapNotNull { i -> parseRecord(arr.getJSONObject(i)) }
    }

    /** 같은 길(출발지 → 목적지)의 기록, 오래된 것부터 */
    fun history(routeKey: String): List<DriveRecord> =
        records().filter { it.routeKey == routeKey }.sortedBy { it.startMs }

    fun save(record: DriveRecord) {
        val arr = JSONArray(prefs.getString(KEY_RECORDS, "[]"))
        arr.put(toJson(record))
        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply()
    }

    fun memory(): List<MemoryNote> {
        val arr = JSONArray(prefs.getString(KEY_MEMORY, "[]"))
        return (0 until arr.length()).mapNotNull { i ->
            itemOf(arr.getString(i))?.let { MemoryNote(it, Phrases.tip(it)) }
        }
    }

    fun saveMemory(notes: List<MemoryNote>) {
        val arr = JSONArray().apply { notes.forEach { put(it.item.name) } }
        prefs.edit().putString(KEY_MEMORY, arr.toString()).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun toJson(r: DriveRecord) = JSONObject().apply {
        put("start", r.startMs)
        put("key", r.routeKey)
        put("name", r.routeName)
        put("t", JSONObject().apply {
            r.tallies.forEach { (item, t) -> put(item.name, JSONArray().put(t.opportunities).put(t.misses)) }
        })
    }

    private fun parseRecord(o: JSONObject): DriveRecord? = runCatching {
        val t = o.getJSONObject("t")
        val tallies = t.keys().asSequence().mapNotNull { k ->
            val item = itemOf(k) ?: return@mapNotNull null
            val a = t.getJSONArray(k)
            item to ItemTally(a.getInt(0), a.getInt(1))
        }.toMap()
        DriveRecord(o.getLong("start"), o.getString("key"), o.getString("name"), tallies)
    }.getOrNull()

    private fun itemOf(name: String) = CoachItem.entries.firstOrNull { it.name == name }

    private companion object {
        const val KEY_RECORDS = "records"
        const val KEY_MEMORY = "memory"
    }
}
