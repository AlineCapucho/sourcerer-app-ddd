package app.infrastructure.cli

import com.beust.jcommander.Parameters

@Parameters(separators = "=",
            commandDescription = "List tracked repositories")
class CommandList {
    val name = "list"
}
