package core

import com.github.salomonbrys.kodein.instance
import com.google.gson.Gson
import core.Register.set
import gs.property.I18n

data class Announcement(
        val shouldAnnounce: Boolean = false,
        val index: Int = 0,
        val id: String = "",
        val contentUrl: String = "",
        val title: String = "",
        val tagline: String = "",
        var lastCheck: Long = 0L,
        var displayedIndex: Int = -1
)

fun initAnnouncement() {
    Register.sourceFor(Announcement::class.java, PaperSource("announcement"), default = Announcement())
}

fun maybeCheckForAnnouncement() {
    val ann = get(Announcement::class.java)
    val validity = (86400 * 1000) / 2 // twice a day
    if (ann.lastCheck + validity  < System.currentTimeMillis()) {
        v("checking for announcement")
        requestAnnouncement()
    }
}

fun hasNewAnnouncement(): Boolean {
    val ann = get(Announcement::class.java)
    return when {
        !ann.shouldAnnounce -> false
        ann.index == 0 -> false
        ann.displayedIndex >= ann.index -> false
        ann.contentUrl.isBlank() -> false
        ann.title.isBlank() -> false
        else -> return true
    }
}

fun markAnnouncementAsSeen() {
    val ann = get(Announcement::class.java)
    ann.displayedIndex = ann.index
    set(Announcement::class.java, ann)
    v("marked announcement as seen", ann.id)
}

fun getAnnouncementUrl(): String {
    val ann = get(Announcement::class.java)
    return if (ann.contentUrl.startsWith("http")) ann.contentUrl
    else {
        val ctx = getActiveContext()!!
        val di = ctx.ktx("announcement").di()
        val i18n = di.instance<I18n>()
        "%s/%s".format(i18n.contentUrl(), ann.contentUrl)
    }
}

fun getAnnouncementContent(): Pair<String, String> {
    val ann = get(Announcement::class.java)
    return ann.title to ann.tagline
}

private fun requestAnnouncement() {
    return try {
        val ctx = getActiveContext()!!
        val di = ctx.ktx("announcement").di()
        val url = di.instance<Pages>().announcement
        val fetchTimeout = 10 * 1000
        val data = loadAsString(openUrl(url(), fetchTimeout))
        val announcement = Gson().fromJson(data, Announcement::class.java)
        announcement.lastCheck = System.currentTimeMillis()
        v("announcement info refreshed")
        set(Announcement::class.java, announcement)
    } catch (ex: Exception) {
        e("failed fetching announcement", ex)
    }
}