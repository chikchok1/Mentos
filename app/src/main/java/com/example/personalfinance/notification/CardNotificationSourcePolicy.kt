package com.example.personalfinance.notification

object CardNotificationSourcePolicy {
    private val supportedPaymentPackages = setOf(
        "viva.republica.toss",
        "com.kakao.talk",
        "com.kakaopay.app",
        "com.nhn.android.search",
        "com.naverfin.payapp",
        "kr.co.samsungcard.mpocket",
        "com.shcard.smartpay",
        "com.kbcard.cxh.appcard",
        "com.hyundaicard.appcard",
        "com.wooricard.wcard",
        "com.hanaskcard.paycla",
        "com.lcacApp",
        "com.bccard.bcapp"
    )

    fun isSupported(packageName: String, appPackageName: String): Boolean =
        packageName == appPackageName || packageName in supportedPaymentPackages
}
