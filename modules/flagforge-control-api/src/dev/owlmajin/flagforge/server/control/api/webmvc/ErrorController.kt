package dev.owlmajin.flagforge.server.control.api.webmvc

import dev.owlmajin.flagforge.server.model.api.v1.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolationException
import org.apache.kafka.common.KafkaException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice("dev.owlmajin.flagforge.server.control.api.webmvc.v1")
class ErrorController {

	private val log = KotlinLogging.logger { javaClass }

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleMethodArgNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
		val errors = ex.bindingResult.fieldErrors
			.joinToString(separator = "; ") { fieldError -> "${fieldError.field}: ${fieldError.defaultMessage ?: "invalid"}" }

		val body = ErrorResponse(
			status = "ERROR",
			errorCode = "VALIDATION_ERROR",
			message = errors.ifBlank { "Validation failed" },
		)

		log.debug { "Validation failed: $errors" }

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
	}

	@ExceptionHandler(ConstraintViolationException::class)
	fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
		val message = ex.constraintViolations
			.joinToString(separator = "; ") { cv -> "${cv.propertyPath}: ${cv.message}" }

		val body = ErrorResponse(
			status = "ERROR",
			errorCode = "VALIDATION_ERROR",
			message = message.ifBlank { "Validation failed" },
		)

		log.debug { "Constraint violations: $message" }

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
	}

	@ExceptionHandler(HttpMessageNotReadableException::class, MissingServletRequestParameterException::class)
	fun handleBadRequest(ex: Exception): ResponseEntity<ErrorResponse> {
		val message = when (ex) {
			is HttpMessageNotReadableException -> ex.localizedMessage ?: "Malformed request body"
			is MissingServletRequestParameterException -> "Missing parameter: ${ex.parameterName}"
			else -> ex.localizedMessage ?: "Bad request"
		}

		val body = ErrorResponse(
			status = "ERROR",
			errorCode = "MALFORMED_REQUEST",
			message = message,
		)

		log.debug { "Bad request: $message" }

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
	}

	@ExceptionHandler(ResponseStatusException::class)
	fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
		val body = ErrorResponse(
			status = "ERROR",
			errorCode = ex.statusCode.value().toString(),
			message = ex.reason ?: ex.message,
		)

		log.debug { "ResponseStatusException: ${ex.statusCode} ${ex.reason}" }

		return ResponseEntity.status(ex.statusCode).body(body)
	}

	@ExceptionHandler(KafkaException::class)
	fun handleKafka(ex: KafkaException): ResponseEntity<ErrorResponse> {
		val body = ErrorResponse(
			status = "ERROR",
			errorCode = "KAFKA_ERROR",
			message = ex.message ?: "Failed to write to Kafka",
		)

		log.error(ex) { "Kafka error while handling request" }

		val headers = HttpHeaders()
		headers.add("Retry-After", "5")

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).headers(headers).body(body)
	}

	@ExceptionHandler(Throwable::class)
	fun handleGeneric(ex: Throwable): ResponseEntity<ErrorResponse> {
		log.error(ex) { "Unhandled exception" }

		val body = ErrorResponse(
			status = "ERROR",
			errorCode = "INTERNAL_ERROR",
			message = ex.message ?: "Internal server error",
		)

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
	}

}