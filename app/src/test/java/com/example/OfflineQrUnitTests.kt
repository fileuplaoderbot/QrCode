package com.example

import com.example.generator.QrGenerator
import com.example.model.ContentType
import com.example.model.ParsedPayload
import com.example.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQrUnitTests {

    @Test
    fun testWifiParsing_standard() {
        val payload = "WIFI:T:WPA;S:MyHomeWifi;P:SecretPassword123;;;"
        val wifi = ParsedPayload.parseWifiPayload(payload)
        assertEquals("MyHomeWifi", wifi.ssid)
        assertEquals("SecretPassword123", wifi.password)
        assertEquals("WPA", wifi.securityType)
        assertFalse(wifi.isHidden)
    }

    @Test
    fun testWifiParsing_escapedSpecialCharacters() {
        val payload = "WIFI:T:WPA;S:Office\\;Network;P:pass\\:word\\;123;H:true;;"
        val wifi = ParsedPayload.parseWifiPayload(payload)
        assertEquals("Office;Network", wifi.ssid)
        assertEquals("pass:word;123", wifi.password)
        assertTrue(wifi.isHidden)
    }

    @Test
    fun testWifiGenerator_escaping() {
        val generated = QrGenerator.buildWifiPayload(
            ssid = "Test;SSID",
            pass = "My:Secret;Pass",
            type = "WPA",
            hidden = true
        )
        assertTrue(generated.contains("S:Test\\;SSID;"))
        assertTrue(generated.contains("P:My\\:Secret\\;Pass;"))
        assertTrue(generated.contains("H:true;"))
    }

    @Test
    fun testContentTypeDetection_safeUrl() {
        val payload = ParsedPayload.parse("https://example.com/search?q=test")
        assertEquals(ContentType.URL, payload.type)
        assertEquals("https://example.com/search?q=test", payload.safeUrl)
    }

    @Test
    fun testContentTypeDetection_dangerousSchemesAreNotUrls() {
        val jsPayload = ParsedPayload.parse("javascript:alert(1)")
        assertEquals(ContentType.TEXT, jsPayload.type)

        val filePayload = ParsedPayload.parse("file:///etc/passwd")
        assertEquals(ContentType.TEXT, filePayload.type)
    }

    @Test
    fun testContentTypeDetection_phoneAndSms() {
        val telPayload = ParsedPayload.parse("tel:+15551234567")
        assertEquals(ContentType.PHONE, telPayload.type)
        assertEquals("+15551234567", telPayload.phoneNumber)

        val smsPayload = ParsedPayload.parse("smsto:5551234:Hello from OfflineQR")
        assertEquals(ContentType.SMS, smsPayload.type)
        assertEquals("5551234", smsPayload.smsData?.number)
        assertEquals("Hello from OfflineQR", smsPayload.smsData?.message)
    }

    @Test
    fun testContentTypeDetection_vCard() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Ada Lovelace
            ORG:Computing Pioneers
            TEL;TYPE=CELL:+15550009999
            EMAIL:ada@example.com
            END:VCARD
        """.trimIndent()

        val parsed = ParsedPayload.parse(vcard)
        assertEquals(ContentType.VCARD, parsed.type)
        assertEquals("Ada Lovelace", parsed.vCardData?.fullName)
        assertEquals("+15550009999", parsed.vCardData?.phone)
        assertEquals("ada@example.com", parsed.vCardData?.email)
    }

    @Test
    fun testPinSecurityHashingAndSalting() {
        val pin = "1234"
        val salt1 = SecurityManager.generateSalt()
        val hash1 = SecurityManager.hashPin(pin, salt1)

        val salt2 = SecurityManager.generateSalt()
        val hash2 = SecurityManager.hashPin(pin, salt2)

        // Ensure salt randomness
        assertFalse(hash1 == hash2)

        // Ensure verifyPin succeeds with matching salt and hash
        assertTrue(SecurityManager.verifyPin("1234", salt1, hash1))
        assertFalse(SecurityManager.verifyPin("9999", salt1, hash1))
        assertFalse(SecurityManager.verifyPin("1234", salt2, hash1))
    }

    @Test
    fun testSixDigitPinAndEightDigitPin() {
        val pin6 = "839201"
        val salt = SecurityManager.generateSalt()
        val hash = SecurityManager.hashPin(pin6, salt)
        assertTrue(SecurityManager.verifyPin("839201", salt, hash))
        assertFalse(SecurityManager.verifyPin("839202", salt, hash))

        val pin8 = "12345678"
        val hash8 = SecurityManager.hashPin(pin8, salt)
        assertTrue(SecurityManager.verifyPin("12345678", salt, hash8))
        assertFalse(SecurityManager.verifyPin("1234567", salt, hash8))
    }

    @Test
    fun testAlphabetPasswordLock() {
        val password = "MySecurePassword#2026!"
        val salt = SecurityManager.generateSalt()
        val hash = SecurityManager.hashSecret(password, salt)
        assertTrue(SecurityManager.verifySecret("MySecurePassword#2026!", salt, hash))
        assertFalse(SecurityManager.verifySecret("WrongPassword", salt, hash))
    }

    @Test
    fun testPatternLock() {
        val pattern = "0,1,2,4,6,7,8"
        val salt = SecurityManager.generateSalt()
        val hash = SecurityManager.hashSecret(pattern, salt)
        assertTrue(SecurityManager.verifySecret("0,1,2,4,6,7,8", salt, hash))
        assertFalse(SecurityManager.verifySecret("0,1,2,3,4", salt, hash))
    }
}
