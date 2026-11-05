package dev.akhsanuw.kmppref

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform