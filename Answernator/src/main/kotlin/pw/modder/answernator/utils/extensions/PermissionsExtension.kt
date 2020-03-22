package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions

fun Permissions.containsAny(vararg permissions: Permission): Boolean = permissions.any { contains(it) }
fun Permissions.asList(): List<Permission> = Permission.values().filter { contains(it) }