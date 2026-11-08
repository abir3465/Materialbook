package com.eepiemi.materialbook.utils

private val MESSAGES_URL_REGEX = Regex(
    """^https?://(?:(?:www|m|web|mbasic)\.)?(?:facebook\.com/messages(?:[/?#]|$)|messenger\.com(?:[/?#]|$))""",
    RegexOption.IGNORE_CASE
)

private val MOBILE_HOST_REGEX = Regex(
    """^(https?://)(?:m|web|mbasic)\.facebook\.com""",
    RegexOption.IGNORE_CASE
)

/** True for the Messages section (facebook.com/messages and messenger.com). */
fun isMessagesUrl(url: String): Boolean = MESSAGES_URL_REGEX.containsMatchIn(url)

/** Moves a mobile Facebook host (m., web., mbasic.) to www so the desktop site is served. */
fun toDesktopMessagesUrl(url: String): String =
    MOBILE_HOST_REGEX.replaceFirst(url, "$1www.facebook.com")
