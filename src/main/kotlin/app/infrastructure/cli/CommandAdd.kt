package app.infrastructure.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

@Parameters(separators = "=",
            commandDescription = "Add a repository to tracking list")
class CommandAdd {
    val name = "add"

    @Parameter(names = arrayOf("-r", "--recursive"),
               description = "Should search recursively in the specified directory")
    var recursive: Boolean = false

    @Parameter(description = "REPOPATH")
    var paths: List<String> = arrayListOf()

    @Parameter(names = arrayOf("-a", "--all"),
               description = "Hash commits of all contributors.")
    var hashAll: Boolean = false
}
