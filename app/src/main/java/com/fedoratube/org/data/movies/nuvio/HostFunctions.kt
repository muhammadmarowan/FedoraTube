package com.fedoratube.org.data.movies.nuvio

import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.delay

private const val MAX_PLUGIN_TIMER_DELAY_MS = 60_000L
internal const val NUVIO_DEFAULT_TMDB_API_KEY = "439c478a771f35c05022f9feabcca01c"

internal class HostFunctions(
    private val scraperId: String,
    private val scraperSettingsJson: String,
    private val callArgsJson: String = "{}",
    private val onResult: (String) -> Unit,
) : HostModule {
    private val tag = "PluginRuntime"

    override fun register(runtime: QuickJs) {
        runtime.asyncFunction("__plugin_sleep") { args: Array<Any?> ->
            val durationMs = (args.getOrNull(0) as? Number)
                ?.toLong()
                ?.coerceIn(0L, MAX_PLUGIN_TIMER_DELAY_MS)
                ?: 0L
            delay(durationMs)
            null
        }

        runtime.define("console") {
            function("log") { args ->
                Log.d(tag, "Plugin:$scraperId ${args.joinToString(" ") { it?.toString() ?: "null" }}")
                null
            }
            function("error") { args ->
                Log.e(tag, "Plugin:$scraperId ${args.joinToString(" ") { it?.toString() ?: "null" }}")
                null
            }
            function("warn") { args ->
                Log.w(tag, "Plugin:$scraperId ${args.joinToString(" ") { it?.toString() ?: "null" }}")
                null
            }
            function("info") { args ->
                Log.i(tag, "Plugin:$scraperId ${args.joinToString(" ") { it?.toString() ?: "null" }}")
                null
            }
            function("debug") { args ->
                Log.d(tag, "Plugin:$scraperId ${args.joinToString(" ") { it?.toString() ?: "null" }}")
                null
            }
        }

        runtime.function<String>("__get_scraper_id") { scraperId }
        runtime.function<String>("__get_scraper_settings") { scraperSettingsJson }
        runtime.function<String>("__get_tmdb_api_key") { NUVIO_DEFAULT_TMDB_API_KEY }
        runtime.function<String>("__get_call_args") { callArgsJson }
        runtime.function<Any?>("__capture_result") { args ->
            onResult(args.getOrNull(0)?.toString() ?: "[]")
            null
        }
    }
}
