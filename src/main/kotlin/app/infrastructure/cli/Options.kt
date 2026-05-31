package app.infrastructure.cli

import com.beust.jcommander.Parameter

class Options {
    @Parameter(names = arrayOf("-u", "--username"),
               description = "Sourcerer account username", order = 0)
    var username: String = ""

    @Parameter(names = arrayOf("-p", "--password"),
               description = "Sourcerer account password", order = 1)
    var password: String = ""

    @Parameter(names = arrayOf("-h", "--help"),
               description = "List options and commands", order = 2)
    var help: Boolean = false

    @Parameter(names = arrayOf("--setup"),
               description = "Cleanup configs and run setup again", order = 3)
    var setup: Boolean = false

    @Parameter(names = arrayOf("--uninstall"),
               description = "Remove Sourcerer App", order = 4)
    var uninstall: Boolean = false
}
