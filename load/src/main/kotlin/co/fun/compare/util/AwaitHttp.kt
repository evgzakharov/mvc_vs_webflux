@file:Suppress("RedundantVisibilityModifier")
package co.`fun`.compare.util

import io.netty.handler.codec.http.EmptyHttpHeaders
import io.netty.handler.codec.http.HttpHeaders
import java.util.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.asynchttpclient.AsyncHandler
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.HttpResponseBodyPart
import org.asynchttpclient.HttpResponseStatus
import org.asynchttpclient.Request

data class HttpResponse(
    val status: HttpResponseStatus,
    val headers: HttpHeaders = EmptyHttpHeaders.INSTANCE,
    val rawBody: ByteArray? = null
) {
    val isSuccessful: Boolean = status.statusCode in 200..399
}

public suspend fun AsyncHttpClient.executeAndAwaitRequest(request: Request): HttpResponse {
    return suspendCancellableCoroutine { continuation ->
        this.executeRequest(request, object : AsyncHandler<Unit> {
            private var status: HttpResponseStatus? = null
            private var headers: HttpHeaders = EmptyHttpHeaders.INSTANCE
            private val bodyParts = ArrayList<HttpResponseBodyPart>(1)
            private var cancel = false

            init {
                continuation.invokeOnCancellation { cancel = true }
            }

            override fun onHeadersReceived(headers: HttpHeaders): AsyncHandler.State {
                this.headers = headers
                return if (!cancel) AsyncHandler.State.CONTINUE else AsyncHandler.State.ABORT
            }

            override fun onStatusReceived(responseStatus: HttpResponseStatus): AsyncHandler.State {
                this.status = responseStatus
                return if (!cancel) AsyncHandler.State.CONTINUE else AsyncHandler.State.ABORT
            }

            override fun onBodyPartReceived(bodyPart: HttpResponseBodyPart): AsyncHandler.State {
                bodyParts += bodyPart
                return if (!cancel) AsyncHandler.State.CONTINUE else AsyncHandler.State.ABORT
            }

            override fun onCompleted() {
                if (status == null) {
                    continuation.resumeWithException(RuntimeException("status is empty"))
                }

                val bodyRawLength = bodyParts.sumOf { it.length() }
                val body = if (bodyParts.size == 1)
                    bodyParts.first().bodyPartBytes
                else
                    bodyParts.fold(ByteArray(bodyRawLength) to 0) { (acc, offset), part ->
                        part.bodyPartBytes.copyInto(
                            acc,
                            offset
                        ); acc to offset + part.bodyPartBytes.size
                    }.first

                continuation.resume(
                    HttpResponse(
                        status = status!!,
                        headers = headers,
                        rawBody = body,
                    )
                )
            }

            override fun onThrowable(t: Throwable) {
                continuation.resumeWithException(t)
            }
        })
    }
}
