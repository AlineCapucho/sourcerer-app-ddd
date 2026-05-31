package app.infrastructure.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

@Parameters(separators = "=",
            commandDescription = "Remove a repository from tracking list")
class CommandRemove {
    val name = "remove"

    @Parameter(description = "REPOPATH")
    var path: String? = null
}
