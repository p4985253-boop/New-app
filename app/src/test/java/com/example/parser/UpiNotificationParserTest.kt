package com.example.parser

import com.example.data.model.UpiProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiNotificationParserTest {

    // =========================================================================
    // 1. Google Pay Test Cases
    // =========================================================================

    @Test
    fun testGooglePay_standardPaidYouFormat() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_GPAY,
            title = "Google Pay",
            text = "Amit Patel paid you ₹1,500.00"
        )
        assertNotNull("Payment should not be null", result)
        assertEquals(1500.0, result!!.amount, 0.001)
        assertEquals("Amit Patel", result.senderName)
        assertEquals(UpiProvider.GOOGLE_PAY, result.provider)
        assertEquals("1,500", result.formattedAmount)
    }

    @Test
    fun testGooglePay_titleIsSenderName() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_GPAY,
            title = "Amit Patel",
            text = "Paid you ₹1,500.00"
        )
        assertNotNull(result)
        assertEquals(1500.0, result!!.amount, 0.001)
        assertEquals("Amit Patel", result.senderName)
        assertEquals(UpiProvider.GOOGLE_PAY, result.provider)
    }

    @Test
    fun testGooglePay_withCommasAndDecimals() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_GPAY,
            title = "Pooja Shah",
            text = "Paid you ₹2,50,000.50"
        )
        assertNotNull(result)
        assertEquals(250000.50, result!!.amount, 0.001)
        assertEquals("Pooja Shah", result.senderName)
    }

    @Test
    fun testGooglePay_receivedFromFormat() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_GPAY,
            title = "Google Pay",
            text = "Received ₹250 from Amit Patel"
        )
        assertNotNull(result)
        assertEquals(250.0, result!!.amount, 0.001)
        assertEquals("Amit Patel", result.senderName)
    }

    // =========================================================================
    // 2. PhonePe Test Cases
    // =========================================================================

    @Test
    fun testPhonePe_receivedFromFormat() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PHONEPE,
            title = "PhonePe",
            text = "Received ₹250 from Amit Patel"
        )
        assertNotNull(result)
        assertEquals(250.0, result!!.amount, 0.001)
        assertEquals("Amit Patel", result.senderName)
        assertEquals(UpiProvider.PHONEPE, result.provider)
    }

    @Test
    fun testPhonePe_paymentReceivedFromFormat() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PHONEPE,
            title = "Payment Received",
            text = "Payment of ₹500 from Rajesh Kumar received."
        )
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertEquals("Rajesh Kumar", result.senderName)
        assertEquals(UpiProvider.PHONEPE, result.provider)
    }

    @Test
    fun testPhonePe_sentYouFormat() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PHONEPE,
            title = "PhonePe",
            text = "Amit Patel sent you ₹250"
        )
        assertNotNull(result)
        assertEquals(250.0, result!!.amount, 0.001)
        assertEquals("Amit Patel", result.senderName)
    }

    @Test
    fun testPhonePe_rejectOutgoingDebit() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PHONEPE,
            title = "PhonePe",
            text = "Paid ₹250 to Grocery Store"
        )
        assertNull("Outgoing debit payment should be rejected", result)
    }

    @Test
    fun testPhonePe_rejectSentTo() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PHONEPE,
            title = "PhonePe",
            text = "Sent ₹500 to Suresh Kumar"
        )
        assertNull(result)
    }

    // =========================================================================
    // 3. Paytm Test Cases
    // =========================================================================

    @Test
    fun testPaytm_receivedFromViaPaytmUpi() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PAYTM,
            title = "Paytm",
            text = "Received ₹350 from Suresh Kumar via Paytm UPI"
        )
        assertNotNull(result)
        assertEquals(350.0, result!!.amount, 0.001)
        assertEquals("Suresh Kumar", result.senderName)
        assertEquals(UpiProvider.PAYTM, result.provider)
    }

    @Test
    fun testPaytm_withRsNotation() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PAYTM,
            title = "Money Received",
            text = "Received Rs 500 from Rahul Sharma"
        )
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertEquals("Rahul Sharma", result.senderName)
    }

    @Test
    fun testPaytm_amountFirstFormat() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PAYTM,
            title = "Paytm",
            text = "₹1,200 received from Anita Desai"
        )
        assertNotNull(result)
        assertEquals(1200.0, result!!.amount, 0.001)
        assertEquals("Anita Desai", result.senderName)
    }

    @Test
    fun testPaytm_rejectDebited() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PAYTM,
            title = "Paytm",
            text = "Debited ₹350 for Order #1234"
        )
        assertNull(result)
    }

    // =========================================================================
    // 4. Generic Banking and Fallbacks
    // =========================================================================

    @Test
    fun testGenericBank_creditedAccount() {
        val result = UpiNotificationParser.parse(
            packageName = "com.sbi.upi",
            title = "SBI UPI",
            text = "Your account is credited with INR 1,000.00 from Amit Patel"
        )
        assertNotNull(result)
        assertEquals(1000.0, result!!.amount, 0.001)
        assertEquals("Amit Patel", result.senderName)
        assertEquals(UpiProvider.GENERIC_BANK, result.provider)
    }

    @Test
    fun testGenericBank_creditedByFormat() {
        val result = UpiNotificationParser.parse(
            packageName = "com.hdfc.bank",
            title = "HDFC Bank Alert",
            text = "A/C *1234 credited by ₹500.00 on 05-Nov-26 by Suresh."
        )
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertTrue(result.senderName.contains("Suresh"))
    }

    // =========================================================================
    // 5. Zero-Amount, Non-Payment, and Crash Resilience
    // =========================================================================

    @Test
    fun testResilience_nullInputs() {
        val result = UpiNotificationParser.parse(null, null, null)
        assertNull(result)
    }

    @Test
    fun testResilience_emptyString() {
        val result = UpiNotificationParser.parse(UpiNotificationParser.PACKAGE_GPAY, "", "")
        assertNull(result)
    }

    @Test
    fun testResilience_otpNotification() {
        val result = UpiNotificationParser.parse(
            packageName = "com.bank.app",
            title = "Bank OTP",
            text = "Your OTP for transaction of ₹500 is 987654. Do not share it."
        )
        assertNull("OTP notifications must not trigger soundbox payment announcements", result)
    }

    @Test
    fun testResilience_billPaymentDebit() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_PHONEPE,
            title = "Bill Payment",
            text = "Electricity bill payment of ₹1,450 successful"
        )
        assertNull(result)
    }

    @Test
    fun testResilience_zeroAmount() {
        val result = UpiNotificationParser.parse(
            packageName = UpiNotificationParser.PACKAGE_GPAY,
            title = "Google Pay",
            text = "Paid you ₹0.00"
        )
        assertNull(result)
    }

    @Test
    fun testResilience_randomChatNotification() {
        val result = UpiNotificationParser.parse(
            packageName = "com.whatsapp",
            title = "Ramesh",
            text = "Hey, are you at the shop today?"
        )
        assertNull(result)
    }
}
