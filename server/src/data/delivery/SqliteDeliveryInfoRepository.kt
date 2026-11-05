package com.usuallycoding.opensecretsanta.server.data.delivery

import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfo
import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfoRepository
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.Database
import java.sql.ResultSet

class SqliteDeliveryInfoRepository(private val database: Database) : DeliveryInfoRepository {

    override fun find(groupId: Long, username: Username): DeliveryInfo =
        findAll(groupId, listOf(username))[username] ?: DeliveryInfo.EMPTY

    override fun findAll(groupId: Long, usernames: Collection<Username>): Map<Username, DeliveryInfo> {
        if (usernames.isEmpty()) return emptyMap()
        val placeholders = List(usernames.size) { "?" }.joinToString(", ")
        return database.connection().use { c ->
            c.prepareStatement(
                "SELECT username, full_name, email, phone, address, delivery_info FROM group_delivery_info " +
                    "WHERE group_id = ? AND username IN ($placeholders)",
            ).use { s ->
                s.setLong(1, groupId)
                usernames.forEachIndexed { index, username -> s.setString(index + 2, username.value) }
                s.executeQuery().use { rows ->
                    generateSequence { if (rows.next()) Username.parse(rows.getString("username")) to toInfo(rows) else null }.toMap()
                }
            }
        }
    }

    override fun save(groupId: Long, username: Username, info: DeliveryInfo) {
        database.connection().use { c ->
            c.prepareStatement(
                "INSERT INTO group_delivery_info (group_id, username, full_name, email, phone, address, delivery_info) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (group_id, username) DO UPDATE SET " +
                    "full_name = excluded.full_name, email = excluded.email, phone = excluded.phone, " +
                    "address = excluded.address, delivery_info = excluded.delivery_info",
            ).use { s ->
                s.setLong(1, groupId)
                s.setString(2, username.value)
                s.setString(3, info.fullName)
                s.setString(4, info.email)
                s.setString(5, info.phone)
                s.setString(6, info.address)
                s.setString(7, info.deliveryInfo)
                s.executeUpdate()
            }
        }
    }

    private fun toInfo(rows: ResultSet) = DeliveryInfo(
        fullName = rows.getString("full_name"),
        email = rows.getString("email"),
        phone = rows.getString("phone"),
        address = rows.getString("address"),
        deliveryInfo = rows.getString("delivery_info"),
    )
}
