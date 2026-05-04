import platform.Foundation.NSLog

private fun nslog(message: String) {
    NSLog(message.replace("%", "%%"))
}

fun main() {
    val level = "ERROR"
    val message = "My fancy error"
    NSLog("%@ %@", level, message)
}
