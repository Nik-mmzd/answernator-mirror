package pw.modder.answernator.utils.locale

import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.util.*
import java.util.ResourceBundle as JavaResourceBundle


object UTF8Control : JavaResourceBundle.Control() {
    override fun newBundle(
        baseName: String?,
        locale: Locale?,
        format: String?,
        loader: ClassLoader,
        reload: Boolean
    ): JavaResourceBundle? { // The below is a copy of the default implementation.
        val bundleName: String = toBundleName(baseName, locale)
        val resourceName: String = toResourceName(bundleName, "properties")
        var bundle: JavaResourceBundle? = null
        var stream: InputStream? = null
        if (reload) {
            val url: URL? = loader.getResource(resourceName)
            if (url != null) {
                val connection: URLConnection = url.openConnection()
                connection.useCaches = false
                stream = connection.getInputStream()
            }
        } else {
            stream = loader.getResourceAsStream(resourceName)
        }
        if (stream != null) {
            bundle = stream.use { PropertyResourceBundle(it.reader(Charsets.UTF_8)) }
        }
        return bundle
    }

    override fun getFallbackLocale(p0: String?, p1: Locale?): Locale? {
        return if (p0 == null) {
            throw NullPointerException()
        } else {
            val p2 = Locale.ROOT
            if (p1 == p2) null else p2
        }
    }
}
