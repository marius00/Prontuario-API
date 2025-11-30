package prontuario.al.logger

import com.github.ksuid.KsuidGenerator
import org.apache.commons.lang3.exception.ExceptionUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.MDC
import prontuario.al.database.IBaseModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.full.memberProperties

object LoggingUtil {
    private const val MAX_FIELD_VALUE_LENGTH = 28000

    @JvmStatic
    fun getCurrentFullProfile(): String {
        val fullProfile: String? =
            System.getProperty("spring.profiles.active") ?: System.getenv("SPRING_PROFILES_ACTIVE")
        return if (fullProfile.isNullOrBlank()) {
            "local"
        } else {
            fullProfile
        }
    }

    @JvmStatic
    fun getCurrentMainProfile(): String {
        val profile: String = getCurrentFullProfile()
        return profile.split(",").firstOrNull() ?: "local"
    }

    @JvmStatic
    fun isLocal(): Boolean =
        when (getCurrentMainProfile()) {
            "local" -> true
            else -> false
        }

    @JvmStatic
    fun generateUUID(): String = KsuidGenerator.generate()

    @JvmStatic
    fun String?.convertUrlEncodedToUrlDecoded(): String {
        this ?: return ""
        return URLDecoder.decode(this, StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun getClassNameMethodNamePair(joinPoint: ProceedingJoinPoint): Pair<String, String> {
        val className = joinPoint.signature.declaringTypeName
            .split(".")
            .last()
        val methodName = (joinPoint.signature as MethodSignature).method.name

        return Pair(className, methodName)
    }

    fun getValueFromValueClass(obj: Any): Any? {
        if (!obj::class.isValue) {
            return null
        }

        // Get the first (and only) property of the value class
        val property = obj::class.memberProperties.firstOrNull()
        return property?.call(obj)
    }

    fun getNameFromClass(obj: Any): String? = obj::class.simpleName?.replaceFirstChar { s -> s.lowercase() }

    @JvmStatic
    fun put(vararg objects: Any) {
        objects.forEach {
            if (it::class.isValue) {
                MDC.put(getNameFromClass(it), getValueFromValueClass(it).toString())
            } else if (it is IBaseModel) {
                val value = it.getId()
                if (value != null) {
                    if (value::class.isValue) {
                        MDC.put(getNameFromClass(it) + "Id", getValueFromValueClass(value).toString())
                    } else {
                        MDC.put(getNameFromClass(it) + "Id", value.toString())
                    }
                }
            } else {
                MDC.put(getNameFromClass(it), it.toString())
            }
        }
    }

    @JvmStatic
    fun mdcPut(
        key: String?,
        value: Int?,
    ) {
        key ?: return
        value ?: return

        MDC.remove(key)
        MDC.put(key, value.toString())
    }

    @JvmStatic
    fun mdcPut(
        key: String?,
        value: Long?,
    ) {
        key ?: return
        value ?: return

        MDC.remove(key)
        MDC.put(key, value.toString())
    }

    @JvmStatic
    fun mdcPut(
        key: String?,
        value: Boolean?,
    ) {
        key ?: return
        value ?: return

        MDC.remove(key)
        MDC.put(key, value.toString())
    }

    @JvmStatic
    fun mdcPut(
        key: String?,
        value: String?,
    ) {
        key ?: return
        value ?: return

        MDC.remove(key)
        MDC.put(key, value)
    }

    @JvmStatic
    fun mdcPut(
        key: String?,
        value: Instant?,
    ) {
        key ?: return
        value ?: return

        MDC.remove(key)
        MDC.put(key, value.toString())
    }

    @JvmStatic
    fun mdcPutTrimmed(
        key: String?,
        value: String?,
    ) {
        key ?: return
        value ?: return

        if (value.length <= MAX_FIELD_VALUE_LENGTH) {
            mdcPut(key, value)
            return
        }

        mdcPut(key, value.substring(0, MAX_FIELD_VALUE_LENGTH - 1))
    }

    @JvmStatic
    fun mdcPutRawMessage(rawMessage: String?) {
        rawMessage ?: return

        mdcPutCumulative("raw_message", rawMessage, "\n")
    }

    @JvmStatic
    fun mdcPutCumulative(
        fieldName: String,
        fieldValue: String,
        separator: String = "\n",
    ) {
        val existsFieldValue = MDC.get(fieldName)
        if (existsFieldValue.isNullOrEmpty()) {
            MDC.put(fieldName, fieldValue)
            return
        }
        when (existsFieldValue.toByteArray().size > MAX_FIELD_VALUE_LENGTH) {
            true -> {
                val currentIndexNo = fieldName.substringAfterLast('_')
                val newIndexNo = AtomicInteger(1)
                val hasIndexNo = currentIndexNo.matches("[0-9]+".toRegex())
                val currentFieldNameWithoutIndexNo = when (hasIndexNo) {
                    true -> {
                        newIndexNo.addAndGet(Integer.valueOf(currentIndexNo))
                        fieldName.substringBeforeLast('_')
                    }

                    else -> fieldName
                }
                val newFieldName = "${currentFieldNameWithoutIndexNo}_$newIndexNo"
                MDC.put(newFieldName, fieldValue)
            }

            else -> MDC.put(fieldName, existsFieldValue + separator + fieldValue)
        }
    }

    @JvmStatic
    fun mdcPutRequestBody(requestBody: String?) {
        requestBody ?: return
        mdcPut("request_body", requestBody)
    }

    @JvmStatic
    fun mdcPutException(
        ex: Throwable?,
        prefix: String? = null,
    ) {
        ex ?: return

        val prefixString: String = if (prefix.isNullOrBlank()) {
            ""
        } else {
            "${prefix}_"
        }

        mdcPut("${prefixString}error_exception", ex.javaClass.name)
        mdcPutTrimmed("${prefixString}error_message", ex.message)

        ExceptionUtils.getRootCause(ex)?.let {
            mdcPut("${prefixString}error_root_exception", it.javaClass.name)
            mdcPutTrimmed("${prefixString}error_root_message", ExceptionUtils.getMessage(it))
        }
        mdcPutTrimmed("${prefixString}error_trace", ExceptionUtils.getStackTrace(ex))
    }

    @JvmStatic
    fun mdcClearException() {
        MDC.remove("error_exception")
        MDC.remove("error_message")
        MDC.remove("error_root_exception")
        MDC.remove("error_root_message")
        MDC.remove("error_trace")
    }

    @JvmStatic
    fun mergeMdcContextMap(mdcContextMap: Map<String, String>?) {
        if (mdcContextMap.isNullOrEmpty()) return

        MDC.setContextMap(MDC.getCopyOfContextMap() + mdcContextMap)
    }

    @JvmStatic
    fun clearAndPutMdcContextMap(mdcContextMap: Map<String, String>?) {
        if (mdcContextMap.isNullOrEmpty()) return

        MDC.clear()
        MDC.setContextMap(mdcContextMap)
    }
}
