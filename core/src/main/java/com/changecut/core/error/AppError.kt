package com.changecut.core.error

sealed class AppError(
    val message: String,
    val throwable: Throwable? = null
) {
    class Network(message: String, throwable: Throwable? = null) : AppError(message, throwable)
    class Database(message: String, throwable: Throwable? = null) : AppError(message, throwable)
    class Validation(message: String, throwable: Throwable? = null) : AppError(message, throwable)
    class Unknown(message: String, throwable: Throwable? = null) : AppError(message, throwable)
    class FileError(message: String, throwable: Throwable? = null) : AppError(message, throwable)
    class ExportError(message: String, throwable: Throwable? = null) : AppError(message, throwable)
}
