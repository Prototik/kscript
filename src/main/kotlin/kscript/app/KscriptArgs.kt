package kscript.app

import com.xenomachina.argparser.*

sealed class KscriptArgs(parser: ArgParser) {
    val interactive by parser.flagging(
        "-i", "--interactive", help = "Create interactive shell with dependencies as declared in script"
    )

    val text by parser.flagging(
        "-t", "--text", help = "Enable stdin support API for more streamlined text processing"
    )

    val idea by parser.flagging(
        "--idea", help = "Open script in temporary Intellij session"
    )

    val silent by parser.flagging(
        "-s", "--silent", help = "Suppress status logging to stderr"
    )

    val pack by parser.flagging(
        "--package", help = "Package script and dependencies into self-dependent binary"
    )

    val addBootstrapHeader by parser.flagging(
        "--add-bootstrap-header", help = "Prepend bash header that installs kscript if necessary"
    )

    val clearCache by parser.flagging(
        "--clear-cache", help = "Wipe cached script jars and urls"
    )

    abstract val script: String
    abstract val scriptArgs: List<String>

    class General(parser: ArgParser) : KscriptArgs(parser) {
        override val script by parser.positional(
            "SCRIPT", help = """
            A script file (*kts), a script URL, - for stdin, a *.kt source file with a main method, or some kotlin code.
            """.trimIndent()
        )

        override val scriptArgs by parser.positionalList(
            "SCRIPT_ARGS", help = "A script arguments"
        ).default { emptyList() }
    }

    class StdinHack(parser: ArgParser, override val scriptArgs: List<String>) : KscriptArgs(parser) {
        override val script get() = "-"
    }
}
