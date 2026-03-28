package dev.jwillert.ddd.exceptions

open class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ValidationException(message: String) : DomainException(message)
class NotFoundException(message: String) : DomainException(message)
