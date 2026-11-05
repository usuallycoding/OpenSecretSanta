package com.usuallycoding.opensecretsanta.domain.email

import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfoRepository
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** One plain-text email to one person. */
data class EmailMessage(val to: String, val subject: String, val text: String)

/** How the emails of one draw went: how many were sent, how many failed and how many participants have no address. */
data class EmailSummary(val sent: Int, val failed: Int, val noAddress: Int)

/**
 * Sends email. [send] answers, for each message in the same order, whether it was accepted for delivery; it reports
 * ordinary failures (the service unreachable, a refusal, a timeout) as `false` and does not throw for them.
 */
interface Mailer {
    /** False when no email service is configured: nothing is built or sent. */
    val enabled: Boolean

    fun send(messages: List<EmailMessage>): List<Boolean>
}

/** The mailer of a server without email settings. */
object DisabledMailer : Mailer {
    override val enabled: Boolean = false

    override fun send(messages: List<EmailMessage>): List<Boolean> = messages.map { false }
}

/**
 * Emails every participant of a freshly drawn group, who set an email in their delivery info for it, the group's details and who they are Secret Santa
 * for, and nothing else: no other person's receiver and none of the receiver's postal details. It runs after the draw
 * is stored and never throws, so that the draw cannot depend on it; null means that email is off.
 */
class SendDrawEmails(
    private val deliveryInfos: DeliveryInfoRepository,
    private val groups: GroupRepository,
    private val mailer: Mailer,
    private val appUrl: String? = null,
) {
    operator fun invoke(group: Group): EmailSummary? {
        if (!mailer.enabled) return null
        val infos = deliveryInfos.findAll(group.id, group.participants)
        val receivers = groups.assignments(group.id).associate { it.santa to it.receiver }

        var noAddress = 0
        var unbuilt = 0
        val messages = ArrayList<EmailMessage>()
        for (participant in group.participants) {
            val email = infos[participant]?.email
            if (email == null) {
                noAddress++
                continue
            }
            val receiver = receivers[participant]
            if (receiver == null) {
                unbuilt++
                continue
            }
            messages += drawEmail(email, participant, group.id, group.details, receiver, infos[receiver]?.fullName, appUrl)
        }
        val results = try {
            mailer.send(messages).let { if (it.size == messages.size) it else messages.map { false } }
        } catch (_: Exception) {
            messages.map { false }
        }
        return EmailSummary(sent = results.count { it }, failed = results.count { !it } + unbuilt, noAddress = noAddress)
    }
}

/** The email that tells [santa] who they give to; [receiverFullName] is the receiver's full name in the group, when they set one. */
fun drawEmail(
    to: String,
    santa: Username,
    groupId: Long,
    group: GroupDetails,
    receiver: Username,
    receiverFullName: String?,
    appUrl: String?,
): EmailMessage {
    val receiverName = receiverFullName?.let { "${receiver.value} ($it)" } ?: receiver.value
    val text = buildString {
        appendLine("Hi ${santa.value},")
        appendLine()
        appendLine("The names for \"${oneLine(group.name)}\" have been drawn.")
        appendLine()
        appendLine("You are Secret Santa for: $receiverName")
        appendDetailsAndLink(group, groupLink(appUrl, groupId))
        appendLine()
        append("Keep it secret!")
    }
    return EmailMessage(to, "Your Secret Santa draw: ${oneLine(group.name)}", text)
}

/** The email that gives [message], written by the group's owner, to [to]: the draw email's layout without the draw. */
fun announcementEmail(to: String, recipient: Username, groupId: Long, group: GroupDetails, title: String, message: String, appUrl: String?): EmailMessage {
    val text = buildString {
        appendLine("Hi ${recipient.value},")
        appendLine()
        appendLine(message)
        appendDetailsAndLink(group, groupLink(appUrl, groupId))
        appendLine()
        append("Keep it secret!")
    }
    return EmailMessage(to, "${oneLine(title)}: ${oneLine(group.name)}", text)
}

/** The link to a group in the web app, `<app address>/#/groups/<id>`, or null when no app address is configured. */
internal fun groupLink(appUrl: String?, groupId: Long): String? = appUrl?.let { "${it.trimEnd('/')}/#/groups/$groupId" }

/** The group's description, when, where and budget, if set, and then the link to the group, each block after an empty line. */
private fun StringBuilder.appendDetailsAndLink(group: GroupDetails, link: String?) {
    val details = buildList {
        group.description?.let { add(it) }
        group.date?.let { date -> add("When: " + whenText(date, group.time)) }
        if (group.date == null) group.time?.let { add("When: ${timeText(it)}") }
        group.location?.let { add("Where: $it") }
        group.budget?.let { add("Budget: $it ${group.currency ?: GroupDetails.DEFAULT_CURRENCY}") }
    }
    if (details.isNotEmpty()) {
        appendLine()
        details.forEach { appendLine(it) }
    }
    if (link != null) {
        appendLine()
        appendLine("Open the app: $link")
    }
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun whenText(date: LocalDate, time: LocalTime?): String {
    val day = "${date.day} ${MONTHS[date.month.ordinal]} ${date.year}"
    return if (time != null) "$day, ${timeText(time)}" else day
}

private fun timeText(time: LocalTime): String = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

/** A group name goes into a subject line: line breaks are replaced so that it stays one line. */
internal fun oneLine(text: String): String = text.replace(Regex("[\\r\\n]+"), " ")
