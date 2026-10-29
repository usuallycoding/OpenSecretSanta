package com.usuallycoding.opensecretsanta.server

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64

/** A service-account key file in the shape Firebase issues, with a freshly generated RSA key; returns the file and the key pair. */
fun writeServiceAccountKey(
    tokenUri: String = "https://oauth2.example.org/token",
    projectId: String = "test-project",
    type: String = "service_account",
): Pair<Path, KeyPair> {
    val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    val pem = "-----BEGIN PRIVATE KEY-----\n" +
        Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(pair.private.encoded) +
        "\n-----END PRIVATE KEY-----\n"
    val file = Files.createTempDirectory("santa-fcm").resolve("key.json")
    Files.writeString(
        file,
        """
        {
          "type": "$type",
          "project_id": "$projectId",
          "private_key_id": "abc",
          "private_key": ${kotlinx.serialization.json.JsonPrimitive(pem)},
          "client_email": "push@$projectId.iam.gserviceaccount.com",
          "token_uri": "$tokenUri"
        }
        """.trimIndent(),
    )
    return file to pair
}
