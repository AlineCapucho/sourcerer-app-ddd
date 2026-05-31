package app.infrastructure.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

@Parameters(separators = "=",
            commandDescription = "Set configuration key-value pair")
class CommandConfig {
    val name = "config"

    @Parameter(description = "KEY VALUE")
    var pair: List<String> = arrayListOf()
}
