package app.infrastructure.git

/**
 * Vendor path conventions to exclude from analysis.
 */
val VendorConventions = listOf(
    Regex("(^|/)node_modules/"),
    Regex("(^|/)vendor/"),
    Regex("(^|/)third_party/"),
    Regex("(^|/)3rd_party/"),
    Regex("(^|/)external/"),
    Regex("(^|/)deps/"),
    Regex("(^|/)Pods/"),
    Regex("(^|/)Carthage/"),
    Regex("(^|/)bower_components/"),
    Regex("(^|/)dist/"),
    Regex("(^|/)build/"),
    Regex("(^|/)target/"),
    Regex("(^|/)generated/"),
    Regex("(^|/)Godeps/"),
    Regex("\\.min\\.js$"),
    Regex("\\.min\\.css$"),
    Regex("-min\\.js$"),
    Regex("-min\\.css$"),
    Regex("\\.bundle\\.js$"),
    Regex("\\.pack\\.js$"),
    Regex("(^|/)package-lock\\.json$"),
    Regex("(^|/)yarn\\.lock$"),
    Regex("(^|/)Gemfile\\.lock$"),
    Regex("(^|/)Podfile\\.lock$"),
    Regex("(^|/)Cargo\\.lock$"),
    Regex("(^|/)composer\\.lock$")
)
