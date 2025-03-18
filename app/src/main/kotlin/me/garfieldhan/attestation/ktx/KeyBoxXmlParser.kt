package me.garfieldhan.attestation.ktx

import android.util.Base64
import android.util.Xml
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.sec.ECPrivateKey
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.StringReader
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec

class KeyBoxXmlParser {
    private val parser: XmlPullParser = Xml.newPullParser()
    private val certificateFactory: CertificateFactory? = CertificateFactory.getInstance("X.509")
    private val chain: MutableList<Certificate> = ArrayList()
    private var privateKey: PrivateKey? = null

    fun parse(keybox: String): KeyStore.PrivateKeyEntry {
        try {
            parser.setInput(StringReader(keybox))
            chain.clear()
            privateKey = null
            readAndroidAttestation()
        } catch (e: XmlPullParserException) {
            throw IOException(e)
        }
        if (privateKey == null || chain.isEmpty()) {
            throw IOException("No key found")
        }
        return KeyStore.PrivateKeyEntry(privateKey, chain.toTypedArray<Certificate>())
    }

    private fun readAndroidAttestation() {
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            val algorithm = parser.getAttributeValue(null, "algorithm")
            if ("Key" == name && "ecdsa" == algorithm) {
                parser.nextTag()
                readECKey()
                break
            }
        }
    }

    private fun readECKey() {
        while (!(parser.eventType == XmlPullParser.END_TAG && "Key" == parser.name)) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                parser.next()
                continue
            }
            val format = parser.getAttributeValue(null, "format")
            when (parser.name) {
                "PrivateKey" -> {
                    if ("pem" == format) {
                        parser.next()
                        readPrivateKey(parser.text)
                        parser.next()
                    } else {
                        return
                    }
                }

                "Certificate" -> {
                    if ("pem" == format) {
                        parser.next()
                        readCertificateChain(parser.text)
                        parser.next()
                    } else {
                        return
                    }
                }

                else -> parser.next()
            }
        }
    }

    private fun readPrivateKey(text: String) {
        try {
            val sequence = ASN1Sequence.getInstance(stringToBytes(text))
            val ecKey = ECPrivateKey.getInstance(sequence)
            val id = AlgorithmIdentifier(
                X9ObjectIdentifiers.id_ecPublicKey,
                ecKey!!.parametersObject
            )
            val data = PrivateKeyInfo(id, ecKey).encoded
            val keySpec = PKCS8EncodedKeySpec(data)
            val keyFactory = KeyFactory.getInstance("EC")
            privateKey = keyFactory.generatePrivate(keySpec)
        } catch (e: GeneralSecurityException) {
            throw IOException(e)
        }
    }

    private fun stringToBytes(text: String): ByteArray {
        val sb = StringBuilder()
        for (s in text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val line = s.trim { it <= ' ' }
            if (line.isEmpty()) continue
            if (line[0] == '-') continue
            sb.append(line)
            sb.append("\n")
        }
        return Base64.decode(sb.toString(), 0)
    }

    private fun readCertificateChain(text: String) {
        try {
            val data = ByteArrayInputStream(stringToBytes(text))
            chain.add(certificateFactory!!.generateCertificate(data))
        } catch (e: CertificateException) {
            throw IOException(e)
        }
    }
}