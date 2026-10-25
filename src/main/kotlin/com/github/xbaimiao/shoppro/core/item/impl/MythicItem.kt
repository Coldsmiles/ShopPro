package com.github.xbaimiao.shoppro.core.item.impl

import com.github.xbaimiao.shoppro.core.item.Item
import com.github.xbaimiao.shoppro.core.item.ItemLoader
import com.github.xbaimiao.shoppro.core.shop.Shop
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @author 小白
 * @date 2023/5/15 17:59
 **/
class MythicItem(
    private val itemStack: ItemStack,
    itemSetting: ItemSetting
) : VanillaShopItem(itemSetting) {

    override val material: Material
        get() = itemStack.type

    override fun vanillaItem(player: Player): ItemStack {
        return itemStack.clone()
    }

    override fun equal(itemStack: ItemStack): Boolean {
        return itemStack.isSimilar(this.itemStack)
    }

    companion object : ItemLoader() {

        override val prefix: String = "MM"
        override val requiredPlugin: String = "MythicMobs"

        override fun formSection(char: Char, section: ConfigurationSection, shop: Shop): Item {
            checkDependency()
            val mmID = section.getString("material")!!.substring(prefix.length + 1)
            val plugin = Bukkit.getPluginManager().getPlugin(requiredPlugin)!!
            return MythicItem(readItemStack(plugin, mmID), section.toItemSetting(char, shop))
        }

        /** Both MythicMobs 4 and 5 expose these methods, under different package names. */
        internal fun readItemStack(plugin: Any, id: String): ItemStack {
            val manager = plugin.javaClass.getMethod("getItemManager").invoke(plugin)
            val item = manager.javaClass.getMethod("getItemStack", String::class.java).invoke(manager, id) as? ItemStack
            return item?.takeUnless { it.type.isAir } ?: error("MythicMobs 物品 $id 不存在")
        }

        override fun parseToMaterial(section: ConfigurationSection): Material {
            return Material.AIR
        }

    }

}
