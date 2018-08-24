package com.github.charleslzq.hwr.hicloud

sealed class HciException(
        val errorCode: Int,
        message: String = ""
) : RuntimeException(message)

class HciSysInitException(
        errorCode: Int,
        errorMessage: String = ""
) : HciException(errorCode, errorMessage)

class HciAuthFailException(
        errorCode: Int,
        errorMessage: String = ""
) : HciException(errorCode, errorMessage)

class HciHwrInitException(
        errorCode: Int,
        errorMessage: String = ""
) : HciException(errorCode, errorMessage)

class HciSessionException(
        errorCode: Int,
        errorMessage: String = ""
) : HciException(errorCode, errorMessage)

class HciRecogFailException(
        errorCode: Int,
        errorMessage: String = ""
) : HciException(errorCode, errorMessage)

class HciAssociateFailException(
        errorCode: Int,
        errorMessage: String = ""
) : HciException(errorCode, errorMessage)