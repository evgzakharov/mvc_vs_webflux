package co.`fun`.compare.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.UUID
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Dsl
import org.asynchttpclient.Param
import org.asynchttpclient.RequestBuilder
import org.asynchttpclient.request.body.multipart.StringPart
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.UnsupportedOperationException

enum class Method {
    POST, PUT, PATCH, DELETE, GET, POST_FORM, POST_MULTIPART
}

const val MAX_LINE_LIMIT = 5000

val SPECIAL_METHODS = setOf(Method.POST_FORM, Method.POST_MULTIPART)

data class Response<T>(
    val status: Boolean,
    val response: T?,
    val errorResponse: String? = null
)

@Suppress("BlockingMethodInNonBlockingContext", "UNCHECKED_CAST")
class HttpRequester(
    val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
    val client: AsyncHttpClient = Dsl.asyncHttpClient(
        DefaultAsyncHttpClientConfig.Builder()
            .setConnectTimeout(30000)
            .setRequestTimeout(30000)
    )
) {
    val log: Logger = LoggerFactory.getLogger(HttpRequester::class.java)

    suspend inline fun <reified T : Any> sendRequest(
        url: String,
        method: Method = Method.GET,
        data: Any? = null,
        params: Map<String, Any> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): T? {
        return sendRequestWithStatus<T>(url, method, data, params, headers).response
    }

    suspend fun <T : Any> sendRequest(
        url: String,
        method: Method = Method.GET,
        data: Any? = null,
        params: Map<String, Any> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        clazz: Class<T>
    ): T? {
        return sendRequestWithStatus(url, method, data, params, headers, clazz).response
    }

    suspend inline fun <reified T : Any> sendRequestWithStatus(
        url: String,
        method: Method,
        data: Any? = null,
        params: Map<String, Any> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Response<T?> {
        return sendRequestWithStatus(url, method, data, params, headers, T::class.java)
    }

    suspend fun <T : Any> sendRequestWithStatus(
        url: String,
        method: Method,
        data: Any? = null,
        params: Map<String, Any> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        clazz: Class<T>
    ): Response<T?> {
        var buildUrl = url
        params.forEach { (key, value) -> buildUrl = buildUrl.replace(key, value.toString()) }

        val requestId = UUID.randomUUID().toString()

        val request = if (method !in SPECIAL_METHODS) {
            RequestBuilder()
                .setMethod(method.name)
                .addHeader("User-RequestUUID", requestId)
                .setUrl(buildUrl).run {
                    if (data is String) {
                        setBodyWithHeaders(data)
                    } else {
                        when (val dataRaw = data?.let { objectMapper.writeValueAsString(it) }) {
                            null -> this
                            else -> setBodyWithHeaders(dataRaw)
                        }
                    }
                }
        } else {
            RequestBuilder()
                .setMethod(when(method) {
                    Method.POST_FORM, Method.POST_MULTIPART -> "POST"
                    else -> throw UnsupportedOperationException("unknown $method")
                })
                .addHeader("User-RequestUUID", requestId)
                .setUrl(buildUrl).apply {
                    if (method == Method.POST_FORM)
                        setFormBodyWithHeaders(data as Map<String, String>)

                    if (method == Method.POST_MULTIPART)
                        setMultipartBodyWithHeaders(data as Map<String, String>)
                }
        }


        headers.forEach { (header, value) ->
            request.setHeader(header, value)
        }

        client.executeAndAwaitRequest(request.build()).let { response ->
            if (response.isSuccessful) {
                if (clazz.isInstance(Unit)) return Response(true, Unit as T)
                val rawBody = response.rawBody ?: return Response(true, null)

                if (log.isDebugEnabled) {
                    val body = String(rawBody, charset("UTF-8"))
                    log.debug("id=$requestId, rawResponse=${body}")
                }
                return if (clazz == String::class.java) {
                    return Response(true, String(rawBody, charset("UTF-8")) as T)
                } else if (clazz == ByteArray::class.java) {
                    return Response(true, rawBody as T)
                } else {
                    Response(true, objectMapper.readValue(rawBody, clazz))
                }
            } else {
                val errorResponse = response.rawBody?.let { String(it, charset("UTF-8")) } ?: "<none>"
                val extendBodyInfo = if (method != Method.GET) ", body=$errorResponse" else ""
                log.debug("id=$requestId, code=${response.status.statusCode}, method=$method, url=$buildUrl$extendBodyInfo")
                return Response(false, null, errorResponse)
            }
        }
    }
}

private fun RequestBuilder.setBodyWithHeaders(data: String?): RequestBuilder = this.apply {
    setBody(data)
    addHeader("Content-Type", "application/json;charset=UTF-8")
    addHeader("Content-Length", data?.toByteArray()?.size ?: 0)
}

private fun RequestBuilder.setFormBodyWithHeaders(data: Map<String, Any>): RequestBuilder = this.apply {
    setFormParams(data.map { Param(it.key, it.value.toString()) })
    addHeader("Content-Type", "application/x-www-form-urlencoded")
}

private fun RequestBuilder.setMultipartBodyWithHeaders(data: Map<String, Any>): RequestBuilder = this.apply {
    setBodyParts(data.map { StringPart(it.key, it.value.toString()) })
    addHeader("Content-Type", "multipart/form-data")
}
