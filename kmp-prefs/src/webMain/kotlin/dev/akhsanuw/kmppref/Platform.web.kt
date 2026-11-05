package dev.akhsanuw.kmppref

class WebPlatform: Platform {
    override val name: String = "Web Platform"
}

actual fun getPlatform(): Platform = WebPlatform()