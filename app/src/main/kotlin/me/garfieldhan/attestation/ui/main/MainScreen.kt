package me.garfieldhan.attestation.ui.main

import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.DIGEST_SHA256
import android.security.keystore.KeyProtection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.garfieldhan.attestation.R
import me.garfieldhan.attestation.compat.BuildCompat
import me.garfieldhan.attestation.ktx.KeyBoxXmlParser
import org.bouncycastle.util.encoders.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec
import java.util.Date
import javax.security.auth.x500.X500Principal

private const val hanKeyboxBase64 =
    "PD94bWwgdmVyc2lvbj0iMS4wIj8+CjxBbmRyb2lkQXR0ZXN0YXRpb24+CiAgICA8TnVtYmVyT2ZLZXlib3hlcz4xPC9OdW1iZXJPZktleWJveGVzPgogICAgPEtleWJveCBEZXZpY2VJRD0ic3ciPgogICAgICAgIDxLZXkgYWxnb3JpdGhtPSJlY2RzYSI+CiAgICAgICAgICAgIDxQcml2YXRlS2V5IGZvcm1hdD0icGVtIj4KICAgICAgICAgICAgICAgIC0tLS0tQkVHSU4gRUMgUFJJVkFURSBLRVktLS0tLQogICAgICAgICAgICAgICAgTUhjQ0FRRUVJRzEvWmpPVUd0REh5SUs0ZXM0NHBGQzVYTnRUcWU2eXBHSXRvSU8rdHRkbG9Bb0dDQ3FHU000OQogICAgICAgICAgICAgICAgQXdFSG9VUURRZ0FFZ1JHN2VtYmJtaGRuSHBNVGZ0ZUZoM1B6Mk5hcSt6bkdDdk9IRUJIYmpFN2xJcmZ0SGZQWgogICAgICAgICAgICAgICAgTjRlNE5QSkt0dFYycTdSLytFbmUzZ3g3N2R2ZU51ZjVDQT09CiAgICAgICAgICAgICAgICAtLS0tLUVORCBFQyBQUklWQVRFIEtFWS0tLS0tCiAgICAgICAgICAgIDwvUHJpdmF0ZUtleT4KICAgICAgICAgICAgPENlcnRpZmljYXRlQ2hhaW4+CiAgICAgICAgICAgICAgICA8TnVtYmVyT2ZDZXJ0aWZpY2F0ZXM+MTwvTnVtYmVyT2ZDZXJ0aWZpY2F0ZXM+CiAgICAgICAgICAgICAgICA8Q2VydGlmaWNhdGUgZm9ybWF0PSJwZW0iPgogICAgICAgICAgICAgICAgICAgIC0tLS0tQkVHSU4gQ0VSVElGSUNBVEUtLS0tLQogICAgICAgICAgICAgICAgICAgIE1JSUNTRENDQWU4Q0ZFc2NUWEhIVXJHS2RWQnJJeWhaVFN3QWhaNmxNQW9HQ0NxR1NNNDlCQU1DTUlHbE1Rc3cKICAgICAgICAgICAgICAgICAgICBDUVlEVlFRR0V3SkRUakVSTUE4R0ExVUVDQXdJVTJoaGJtZG9ZV2t4RVRBUEJnTlZCQWNNQ0ZOb1lXNW5hR0ZwCiAgICAgICAgICAgICAgICAgICAgTVJRd0VnWURWUVFLREF0SFlYSm1hV1ZzWkVoaGJqRVJNQThHQTFVRUN3d0lNV1l5TURBelpEVXhIekFkQmdOVgogICAgICAgICAgICAgICAgICAgIEJBTU1Ga2RoY21acFpXeGtTR0Z1SUNneFpqSXdNRE5rTlNreEpqQWtCZ2txaGtpRzl3MEJDUUVXRjNCeWFYWXUKICAgICAgICAgICAgICAgICAgICBNV1l5TURBelpEVkFaMjFoYVd3dVkyOXRNQ0FYRFRJMU1ETXhPREV3TVRBeU1sb1lEekl3TlRJd09EQXlNVEF4CiAgICAgICAgICAgICAgICAgICAgTURJeVdqQ0JwVEVMTUFrR0ExVUVCaE1DUTA0eEVUQVBCZ05WQkFnTUNGTm9ZVzVuYUdGcE1SRXdEd1lEVlFRSAogICAgICAgICAgICAgICAgICAgIERBaFRhR0Z1WjJoaGFURVVNQklHQTFVRUNnd0xSMkZ5Wm1sbGJHUklZVzR4RVRBUEJnTlZCQXNNQ0RGbU1qQXcKICAgICAgICAgICAgICAgICAgICBNMlExTVI4d0hRWURWUVFEREJaSFlYSm1hV1ZzWkVoaGJpQW9NV1l5TURBelpEVXBNU1l3SkFZSktvWklodmNOCiAgICAgICAgICAgICAgICAgICAgQVFrQkZoZHdjbWwyTGpGbU1qQXdNMlExUUdkdFlXbHNMbU52YlRCWk1CTUdCeXFHU000OUFnRUdDQ3FHU000OQogICAgICAgICAgICAgICAgICAgIEF3RUhBMElBQklFUnUzcG0yNW9YWng2VEUzN1hoWWR6ODlqV3F2czV4Z3J6aHhBUjI0eE81U0szN1IzejJUZUgKICAgICAgICAgICAgICAgICAgICB1RFR5U3JiVmRxdTBmL2hKM3Q0TWUrM2IzamJuK1Fnd0NnWUlLb1pJemowRUF3SURSd0F3UkFJZ1UrSWNRU3ViCiAgICAgICAgICAgICAgICAgICAgRjU1NjBsLzl5WS9lcG5nS3VjclhKdG9zUzFOYWtWRmduMW9DSUhrdTJiRm1ZWGxmUlRsZW1kTFJVclFJa0pxMgogICAgICAgICAgICAgICAgICAgIGhmVGxlT1NQM3Y2dzFJbnUKICAgICAgICAgICAgICAgICAgICAtLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCiAgICAgICAgICAgICAgICA8L0NlcnRpZmljYXRlPgogICAgICAgICAgICA8L0NlcnRpZmljYXRlQ2hhaW4+CiAgICAgICAgPC9LZXk+CiAgICAgICAgPEtleSBhbGdvcml0aG09InJzYSI+CiAgICAgICAgICAgIDxQcml2YXRlS2V5IGZvcm1hdD0icGVtIj4KICAgICAgICAgICAgICAgIC0tLS0tQkVHSU4gUFJJVkFURSBLRVktLS0tLQogICAgICAgICAgICAgICAgTUlJSlJBSUJBREFOQmdrcWhraUc5dzBCQVFFRkFBU0NDUzR3Z2drcUFnRUFBb0lDQVFERjRrSmt6VlJLL3N4MAogICAgICAgICAgICAgICAgd0tvRjhaSWt2ZkpMdklXL3JDdWZzQ0NGTWJVVXRvb2tHc3kxa1I3cStWODlYc20rRTdHVGxJZy9jaHZXV2NneQogICAgICAgICAgICAgICAgdno0RFlCRzN3bnltMDZHdnhDRDlnNyttcXRsV09UdlE2YVBHUzNseG16QVY0MUpRdWU3RXI4ZHMrQ1laNzJKaQogICAgICAgICAgICAgICAgampiYkhYZGlVeVZ3OExRMFFLWjNWekpLL3BGUTRsNGNhck9lMHhGQ1ZZOEt1R3dRQUxlenJLVG4rOFBNU3habwogICAgICAgICAgICAgICAgQTA2Rm9PS3pOTVJuTjZYK0R3WkVBTEpuRWlaQWxJUWRIUHd4VGYxMVI4bXpVRUQrRTJBOUZSd2xXVDFvUUd1awogICAgICAgICAgICAgICAgcTlUejYzRjVuclRrYTkrTVcwMnhuS2U4Q0JNZGtJeGp6TFFiMi9sS3NqRXcxV1J3aGQyOGdIZXQvV1lveUV3MAogICAgICAgICAgICAgICAgL25FRDBKeTZEbDFWT3JKZHcyWGl5SG5mSlFtcHl1eEFiaEJkdnN6RTV3b3hXYXVzTVhFamRlRmk1dXJ3R1hUZAogICAgICAgICAgICAgICAgSnpNNDloejRnbmVxOVduZXF5d0U0MVlOVGFiUkhRbHl3bGc0U2RJUnZYRUxuNUpCaWpoOUxPRHNvQ0xxM3ptSAogICAgICAgICAgICAgICAgMVU1bm8zZmVyR1BCcE82OURWVVBTZmVVYVdsR0psTWVTTGJua2hGOFlzSksrOFZCTTRWT3JYa1R5VE1aRHFHWAogICAgICAgICAgICAgICAgK0FDN1hOeEVhejFxek5xUHNERUZtaGZFZG9OMEhnZVpnalUvVWpZbDI2T01GbGJpU0NqTjV2QXhuUjYrNWJOZwogICAgICAgICAgICAgICAgbVdrWm50M2pQajRDWFpSSG5DKzVLM3E5QWg1Tys2TllYK1U0Rk56Q1FzUEVrdVFVbHJyNjBBME5IOGhxNWxOaAogICAgICAgICAgICAgICAgUENKTU43UG9UWnNBZmwzNGt0OGpndUM3bXhlQkd3SURBUUFCQW9JQ0FRQ0toeTJCa2JoNzA4bmZ4ZXlBd3E5VAogICAgICAgICAgICAgICAgWjJIU29VUmtmU3dBMjNTNHFhYWh2SFFTZUd4c2d3NUk0cEJJenkyZUVmMVRXanpUWlplVlJEOEJQNTR6RUxMMgogICAgICAgICAgICAgICAgbUlGWTdIaVpLazBLREl4REtnR0IrZ2trNUJyUnU0NWwrSVQrSGFoSGVSSnN3TDEwVEhlbjJreGV3RDRvd0lVTgogICAgICAgICAgICAgICAgNFRVcHptZm8ySVZ3c0NzMkV1WTU0RzZQcUNiY2ZkZnNQeWNuRXhvYlRkVk1SQVVobmI5aEtnT2lod1lXU0IzTAogICAgICAgICAgICAgICAgNUpuRnhGQkEvZFdlRDNyVTZ3QzZYdjNwdjV2UkRFN3VKblpPUUZWdlNMTXJyN2FIY3VTcE1jZnBmSjFVc0xNOAogICAgICAgICAgICAgICAgdDl6Y3Q2UEVoenBwcDBoSk9zSGZCWTB5WkdDRnpFWEZnOThwMTRrN3VJc1U1ajVpSUMyL1NSRUlYRU5UcjF1MwogICAgICAgICAgICAgICAgSy9WNE16Z0srTXZqUmZjTlorbi82UDZ6SWN1ZmFRTk56N2hCRjhwUXdLV0RtejVLc3dreEg0aG04VzNEaHovZgogICAgICAgICAgICAgICAgeEVpb0JjcGpOZzBFZldtdUNBY3ROWlEvejkyeWVUbVJqRzVFOEorN1V3WTVNVG5xQWZ4aFdLUHFrZVhjNmQ1awogICAgICAgICAgICAgICAgQWtZWGNhSU16d1NjMjY0bW5heU55OE0yL2RDbktmcktoSmpLNDlzOStWYytSdDFhai9aZkQ4eXBqYWhaYjMxTAogICAgICAgICAgICAgICAgbWh3MEI5NnVmcnp1QndEb2o1Lzh2ci96VGEyWXlwUVZ0M0NDeUsveU1QQ2J3SDlBeHY3SVNWM1VDcDJWQ3F6SgogICAgICAgICAgICAgICAgVmRDUGpBS2ZRRlBPa1dZdk92SVRkKzgyRFhqRkoyMGtBZTZlU2hJUzFObTVGZGg3ayszbWtCTmRjUEhBNDc5aAogICAgICAgICAgICAgICAgcG5zL01FVmFSU2traEpMZHRWL05JUUtDQVFFQTliVXVFajRkUWczZGt4aGkycmRpamgvSTREbUxqU1JhWXlvSgogICAgICAgICAgICAgICAgRUNIRXU0cHNEMlg3SEMzWk5tMDRKM3dhdk9yWVRsNXBqcW02V2ZFb3BKVStUWlkxbUlSVWJZbkdvY1RFeCtJNgogICAgICAgICAgICAgICAgWUEycDIva0lVdTJ5SHJnVGd0SUg0VVYzUVlpaXZ6SnUrUXVGWDFHOEFBRHdtcUliSUZUcmJ6NnBTaDRONTdROAogICAgICAgICAgICAgICAgbVk5N1J3bEU3VnJCR3puWWE0cjZ4U1p5Q2ZUR1E3dGJwb1U2eGcwT0pYRXMweE1Sc1pXRWFrcjRwS3phQlRSYQogICAgICAgICAgICAgICAgd2lNNk9SUThBRjNxUkxFTVY1dzk5UEh1c0F0MmRJbkdBeThtMzJiMVRjaE5sUUdOYnFMVDl4bVZGWTBCS2VLSAogICAgICAgICAgICAgICAgY0ZuaU42L1ZXaE5WUjBKYjRZWjRta1ZDS3JTaVdWSUR2V2x2bmhURkZMaW9IL1dSTVFLQ0FRRUF6aXcrczRyMAogICAgICAgICAgICAgICAgbThFVzRZQ0RZMXlZc3NzWCtTaWNLbmVNWDdINVA1MkgrN0hJdTVxYVd1bWR0eUt1bWxNZGpGV0FrcS9KeFlHTAogICAgICAgICAgICAgICAgN1l5bHZRSXNzYU5tN0VVZTJBODRGV0padzl2WWc0UXc1K3NxVkZFTm5Eb0JFS0lvK3dCMVp6WWR2YjJkUGlEVwogICAgICAgICAgICAgICAgNG9vdVVnc0c4OGh0THJXYkJxV0I3QzhuN3BGeTV0Mmc1Ry9sKzVSeWEvM2pwYXhoemZEWjJ1MUhWR0NIWDYzMAogICAgICAgICAgICAgICAgZzVYUjhxOG5nZWwwZUhCRFYzY1NLaDBkL3ZGdjlrNEloOFRlVVJZRFRZUkZySGpsL0l2V2FhenNJOXZtdGE1aAogICAgICAgICAgICAgICAgR0tKeTAvcHNiZGpmMDhZTHF2T2hRVnFCRzBhVVlrYmw4RE9QMXA4a2x3N0RhcmtpNG5qUjEza3A4YVY1MWRsUgogICAgICAgICAgICAgICAgbmhaNWo3TmpSRDJFQ3dLQ0FRRUFzc2dnTzduaWhCdWVxOWRFcHF4bEU5RGcwdlpNS21qWE0reDBKNzdLYlVOdAogICAgICAgICAgICAgICAgR1U4MUJlMytiUEhQdURzL0lWTW9ETzM1bElxL1E0Nkw2aVdGN2VKc0lRdTRldk9UQ2tZb2dPQ0ZaVHJMNjNrcQogICAgICAgICAgICAgICAgcTF5MURGRVlNVW90M2FZei9nVXpsamRXTS9SRUdYVlpmQTh6QXZFTkY5dHlhOEdHWUoyNXRHeWE0NGxaUS9qawogICAgICAgICAgICAgICAgcEdneXRtTENySTIxcGhveFJEbE13aC9qc2lpaStWU3FoR2Z6RzZwMloyYkpzS1hZZ2NvYjRVbVUrRW0xcGE2cAogICAgICAgICAgICAgICAgY2dMajRST0tUWDBzUDd5QjFxcWdpRzl2R3dZR0hPdkhkYzlYVkNLTERJT0NBL3dqVkVrREhienNtNzFyTXVYSQogICAgICAgICAgICAgICAgTWdCN1BweDBTb2NUM2Z5akJudmszV1VsdjM2bWJybmlKUSt2bFlHVElRS0NBUUVBazkzaHZtMUxrQ1hGSGtoSQogICAgICAgICAgICAgICAgYzB2bTdGeUN2aDBsRzhRUDlsWXptQytJWnJrVWFaQ3hZY0NwYjc0OUVjdklwbXh3UVVRNFp5SEpWTDhKNDJ3YQogICAgICAgICAgICAgICAgMS9DRWtiSGxORHArRThBczl5N0dzMVJzcXZqYnJBdFJ3cGxMU0QyMVBJZ1FxOHlCT2lNekVtc0ZFd2JkdjlQTwogICAgICAgICAgICAgICAgMGFzT3UzQmVWajcvMW4vZm1OZlNXOGZhYTl2aFN6VWxkMVJwSDlwcGJ3VnRpZm1ocmQ5cFYvTHdLaXVhbnY2RwogICAgICAgICAgICAgICAgVEVNK1dGN1R2WGhWbThTMFRiT2xoRFpsSWRMTlM4U0k2K0NSQStqVnNmSXMvaSthVVFHWER2aVpCTTBGcEFpNAogICAgICAgICAgICAgICAgR1hSdzIzYnZ5VlRnYzNuUW5OcGE4NzA3anJHOFdNWmN2TVpHMjFVYXN1aktQMHJVRFU2TCtoYloxWkx5N21CUAogICAgICAgICAgICAgICAgTXBNWUR3S0NBUUJMYmk5TVFZTTczakgvRGNKRVpWUjBLdXRtTCt1dmw2RlJXZWlmSHZCa3AwRFM4U1BGZjk0OQogICAgICAgICAgICAgICAgNzVUMGVNNDg3RzlYSzdWTW1nR25QMTZ5MTE1ZkVOWjRKTEFvUnVzYVBDeW54dWpLL25ITlNBa2hJUktMSVNaegogICAgICAgICAgICAgICAgZUU3Q3RxQVg3bzhNaSsxVFpSQnM5TTlKM2pXdFIwRThRK0ZscXhBSDRvbmFaNktNS2RsZEJrN1AwZ2pSMlIzZgogICAgICAgICAgICAgICAgeCs1S3lnTk9xOStRVEk2TjBBK0t5OFlzUlR6WmtVUW55d1FQd3NKcUluUzRNak5rVW9oeEQyd0FFajZqeUdLeQogICAgICAgICAgICAgICAgMW96ZzVpTnRaTG9nUUZpdlZmd3d4N1pFaGduaGE4d015VjVWRnJUU1h5Z1pwYlNuUS9rTkc4dXBpS3JyRG5FMAogICAgICAgICAgICAgICAgMVpCMSt4UEFRN052ajM1SjUvUnQ3cys2SXUwQXUwTzMKICAgICAgICAgICAgICAgIC0tLS0tRU5EIFBSSVZBVEUgS0VZLS0tLS0KICAgICAgICAgICAgPC9Qcml2YXRlS2V5PgogICAgICAgICAgICA8Q2VydGlmaWNhdGVDaGFpbj4KICAgICAgICAgICAgICAgIDxOdW1iZXJPZkNlcnRpZmljYXRlcz4xPC9OdW1iZXJPZkNlcnRpZmljYXRlcz4KICAgICAgICAgICAgICAgIDxDZXJ0aWZpY2F0ZSBmb3JtYXQ9InBlbSI+CiAgICAgICAgICAgICAgICAgICAgLS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCiAgICAgICAgICAgICAgICAgICAgTUlJRjFUQ0NBNzBDRkd6eDJVQ0JYZWlLV083K3dKN25XMHlKMnRSUE1BMEdDU3FHU0liM0RRRUJDd1VBTUlHbAogICAgICAgICAgICAgICAgICAgIE1Rc3dDUVlEVlFRR0V3SkRUakVSTUE4R0ExVUVDQXdJVTJoaGJtZG9ZV2t4RVRBUEJnTlZCQWNNQ0ZOb1lXNW4KICAgICAgICAgICAgICAgICAgICBhR0ZwTVJRd0VnWURWUVFLREF0SFlYSm1hV1ZzWkVoaGJqRVJNQThHQTFVRUN3d0lNV1l5TURBelpEVXhIekFkCiAgICAgICAgICAgICAgICAgICAgQmdOVkJBTU1Ga2RoY21acFpXeGtTR0Z1SUNneFpqSXdNRE5rTlNreEpqQWtCZ2txaGtpRzl3MEJDUUVXRjNCeQogICAgICAgICAgICAgICAgICAgIGFYWXVNV1l5TURBelpEVkFaMjFoYVd3dVkyOXRNQ0FYRFRJMU1ETXhPREV3TURRd01Gb1lEekl3TlRJd09EQXkKICAgICAgICAgICAgICAgICAgICBNVEF3TkRBd1dqQ0JwVEVMTUFrR0ExVUVCaE1DUTA0eEVUQVBCZ05WQkFnTUNGTm9ZVzVuYUdGcE1SRXdEd1lECiAgICAgICAgICAgICAgICAgICAgVlFRSERBaFRhR0Z1WjJoaGFURVVNQklHQTFVRUNnd0xSMkZ5Wm1sbGJHUklZVzR4RVRBUEJnTlZCQXNNQ0RGbQogICAgICAgICAgICAgICAgICAgIE1qQXdNMlExTVI4d0hRWURWUVFEREJaSFlYSm1hV1ZzWkVoaGJpQW9NV1l5TURBelpEVXBNU1l3SkFZSktvWkkKICAgICAgICAgICAgICAgICAgICBodmNOQVFrQkZoZHdjbWwyTGpGbU1qQXdNMlExUUdkdFlXbHNMbU52YlRDQ0FpSXdEUVlKS29aSWh2Y05BUUVCCiAgICAgICAgICAgICAgICAgICAgQlFBRGdnSVBBRENDQWdvQ2dnSUJBTVhpUW1UTlZFcit6SFRBcWdYeGtpUzk4a3U4aGIrc0s1K3dJSVV4dFJTMgogICAgICAgICAgICAgICAgICAgIGlpUWF6TFdSSHVyNVh6MWV5YjRUc1pPVWlEOXlHOVpaeURLL1BnTmdFYmZDZktiVG9hL0VJUDJEdjZhcTJWWTUKICAgICAgICAgICAgICAgICAgICBPOURwbzhaTGVYR2JNQlhqVWxDNTdzU3Z4Mno0SmhudlltS09OdHNkZDJKVEpYRHd0RFJBcG5kWE1rcitrVkRpCiAgICAgICAgICAgICAgICAgICAgWGh4cXM1N1RFVUpWandxNGJCQUF0N09zcE9mN3c4eExGbWdEVG9XZzRyTTB4R2MzcGY0UEJrUUFzbWNTSmtDVQogICAgICAgICAgICAgICAgICAgIGhCMGMvREZOL1hWSHliTlFRUDRUWUQwVkhDVlpQV2hBYTZTcjFQUHJjWG1ldE9ScjM0eGJUYkdjcDd3SUV4MlEKICAgICAgICAgICAgICAgICAgICBqR1BNdEJ2YitVcXlNVERWWkhDRjNieUFkNjM5WmlqSVREVCtjUVBRbkxvT1hWVTZzbDNEWmVMSWVkOGxDYW5LCiAgICAgICAgICAgICAgICAgICAgN0VCdUVGMit6TVRuQ2pGWnE2d3hjU04xNFdMbTZ2QVpkTjBuTXpqMkhQaUNkNnIxYWQ2ckxBVGpWZzFOcHRFZAogICAgICAgICAgICAgICAgICAgIENYTENXRGhKMGhHOWNRdWZra0dLT0gwczRPeWdJdXJmT1lmVlRtZWpkOTZzWThHazdyME5WUTlKOTVScGFVWW0KICAgICAgICAgICAgICAgICAgICBVeDVJdHVlU0VYeGl3a3I3eFVFemhVNnRlUlBKTXhrT29aZjRBTHRjM0VSclBXck0ybyt3TVFXYUY4UjJnM1FlCiAgICAgICAgICAgICAgICAgICAgQjVtQ05UOVNOaVhibzR3V1Z1SklLTTNtOERHZEhyN2xzMkNaYVJtZTNlTStQZ0pkbEVlY0w3a3JlcjBDSGs3NwogICAgICAgICAgICAgICAgICAgIG8xaGY1VGdVM01KQ3c4U1M1QlNXdXZyUURRMGZ5R3JtVTJFOElrdzNzK2hObXdCK1hmaVMzeU9DNEx1YkY0RWIKICAgICAgICAgICAgICAgICAgICBBZ01CQUFFd0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dJQkFKelEvUTBRZDk0eGkxL0tNU1Rudlgxd3luRlRsOE5NCiAgICAgICAgICAgICAgICAgICAgZ0tSM2FhYjdZR3g4NWJJcXdodGcyRXprd29Pc2k4Q2VCNFJjZ3RBYm5hT05HcVM3ZWI3VDF1cjRpUS9iMlowQQogICAgICAgICAgICAgICAgICAgIHkxNlNWaXB5bGltTWUvWmdITEZUSkV4a3orTFl3Lys0S1REZG04dzgzMmpNNVVpV1dyZEszNjlia0ZqaUlJWGsKICAgICAgICAgICAgICAgICAgICAxNXExMm5MUU1hemxOOXdiclB5ZDJsYTVEaHFQWUNrVnlBWEpReDQ3SWt6aXdMRzhuOGJIMTB6UStZU29tdWVMCiAgICAgICAgICAgICAgICAgICAgR3FYaHNxbFM2MXo4c0ErdDkrRHRIdDdnamlZMXdjOTFnY2tTaXliUGJsa2EvTDJ2Mk9jaTN5Uk1yVDRMNEN4SgogICAgICAgICAgICAgICAgICAgIG1jS29vakNDdmlUN3FtV21yankzekQ3T21RZjk1NU1rQXpkMGxIZDVJbEtHQXBDU3cwbGwxeFlxY3laTDc5MloKICAgICAgICAgICAgICAgICAgICBVWTcwZTVqNDErR3crV3pUd0JvY0J1bFpxVWk3cE1zYjVCNm8wTmZNeGswK0xVMkZDekVtcWI0bmJ5ZEc3ZVZRCiAgICAgICAgICAgICAgICAgICAgOE5YRmdLRWMxK0loU1o4SUJpdUdnai9aVVpwbklHazFGV1RpNkFqWVc5NHlBY01mbDdFU3U2UkM0RmwyYTlzaAogICAgICAgICAgICAgICAgICAgIG1SMnU5amt6eHVFbGtQdEo1TWxROTBwRjhaY3U2WCs2TjR0WkhqYkNvT09Mb1lic0VvRm9kaGhuMnZRb0EvOHoKICAgICAgICAgICAgICAgICAgICB6eGl3OUEvWFBHV2owV3FWVS8zVThuQ1NiSVFzbFcwSXRlY1RMVG1GeUFtZENHVWVIUURyVFRQRnBLelFwR3IwCiAgICAgICAgICAgICAgICAgICAgbzdYZFZwNyt6NUFlWkdHcEE0RVNHV0pHVGVtcFIvak1SaXdTMzh2SjBzaWVzeFBiakY4TDIrUVNZdHJwTDVDTwogICAgICAgICAgICAgICAgICAgIGwwV3Q3NEowSTNUZwogICAgICAgICAgICAgICAgICAgIC0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0KICAgICAgICAgICAgICAgIDwvQ2VydGlmaWNhdGU+CiAgICAgICAgICAgIDwvQ2VydGlmaWNhdGVDaGFpbj4KICAgICAgICA8L0tleT4KICAgIDwvS2V5Ym94Pgo8L0FuZHJvaWRBdHRlc3RhdGlvbj4K"

@Composable
fun MainScreen() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showLoading by remember { mutableStateOf(true) }
    val attestationResultList = mutableListOf<String>()
    val packageManager = LocalContext.current.packageManager
    val isAttestKeySupported =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && packageManager.hasSystemFeature(
            PackageManager.FEATURE_KEYSTORE_APP_ATTEST_KEY
        )

    Scaffold(
        topBar = {
            TopBar(scrollBehavior = scrollBehavior)
        }) { contentPadding ->
        Box(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(contentPadding)
                .fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            if (showLoading) {
                CircularProgressIndicator()
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        if (BuildCompat.atLeastS && isAttestKeySupported) {
                            val keyStore = KeyStore.getInstance("AndroidKeyStore")
                            keyStore.load(null)
                            val attestKeyAlias = "GarfieldHan_persistent"
                            if (keyStore.containsAlias(attestKeyAlias)) {
                                keyStore.deleteEntry(attestKeyAlias)
                            }
                            val keyPairGenerator = KeyPairGenerator.getInstance(
                                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
                            )
                            val now = Date()
                            val spec = KeyGenParameterSpec.Builder(
                                attestKeyAlias, KeyProperties.PURPOSE_ATTEST_KEY
                            ).setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                                .setAttestationChallenge(now.toString().toByteArray())
                                .setDigests(DIGEST_SHA256).setCertificateNotBefore(now)
                                .setCertificateSubject(X500Principal("CN=App Attest Key")).build()
                            keyPairGenerator.initialize(spec)
                            try {
                                keyPairGenerator.generateKeyPair()
                            } catch (e: Exception) {
                                attestationResultList.add("Failed to doAttestation: $e")
                            }
                            val key =
                                KeyBoxXmlParser().parse(String(Base64.decode(hanKeyboxBase64)))
                            keyStore.setEntry(
                                attestKeyAlias,
                                key,
                                KeyProtection.Builder(KeyProperties.PURPOSE_ATTEST_KEY)
                                    .setDigests(DIGEST_SHA256).build()
                            )
                            val certificateFromKeybox = keyStore.getCertificate(attestKeyAlias)
                            if (certificateFromKeybox == null) {
                                attestationResultList.add("Something Wrong")
                            }
                            if (certificateFromKeybox.toString().contains("GarfieldHan")) {
                                attestationResultList.add("Normal Attestation Key")
                            } else {
                                attestationResultList.add("Tampered Attestation Key")
                            }
                        } else {
                            attestationResultList.add("Unsupported device")
                        }
                    }
                    withContext(Dispatchers.Main) {
                        showLoading = false
                    }
                }
            } else {
                Text(
                    text = attestationResultList.joinToString("\n"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline.copy(0.5f)
                )
                Text(
                    text = "Author: GarfieldHan (1f2003d5)\nContact: https://t.me/real1f2003d5\nEmail: priv.1f2003d5@gmail.com",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline.copy(0.7f),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = { Text(text = stringResource(id = R.string.app_name)) }, scrollBehavior = scrollBehavior
)