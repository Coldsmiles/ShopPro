package com.github.xbaimiao.shoppro.core.item

import com.github.xbaimiao.shoppro.core.shop.Shop
import com.github.xbaimiao.shoppro.core.vault.Currency
import com.github.xbaimiao.shoppro.core.vault.CurrencyType
import com.github.xbaimiao.shoppro.core.vault.VaultImpl
import org.bukkit.Material
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import com.github.xbaimiao.shoppro.util.parseShopMaterial
import taboolib.module.chat.colored

abstract class ItemLoader {

    abstract val prefix: String?
    open val requiredPlugin: String? = null
    abstract fun formSection(char: Char, section: ConfigurationSection, shop: Shop): Item

    fun checkDependency() {
        val pluginName = requiredPlugin ?: return
        if (!Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            throw MissingItemPluginException(pluginName)
        }
    }

    open fun parseToMaterial(section: ConfigurationSection): Material {
        return section.getString("material")!!.parseShopMaterial()
    }

    fun ConfigurationSection.toItemSetting(char: Char, shop: Shop): ShopItem.ItemSetting {
        var currency: Currency = VaultImpl
        this.getString("currency")?.let {
            currency = CurrencyType.formString(it).func.invoke(it)
        }
        val limitMap = HashMap<String, Long>()

        this.getConfigurationSection("limitMap")?.let { limitSection ->
            limitSection.getKeys(false).forEach { key ->
                limitMap[key] = limitSection.getLong(key)
            }
        }

        return ShopItem.ItemSetting(
            key = char,
            material = parseToMaterial(this),
            name = this.getString("name")!!.colored(),
            lore = this.getStringList("lore").colored(),
            vanilla = this.getBoolean("vanilla", true),
            commands = this.getStringList("commands"),
            data = this.getInt("data"),
            shop = shop,
            script = this.getString("condition"),
            price = this.getDouble("price"),
            limitServer = this.getLong("limit", 99999999),
            limitPlayer = this.getLong("limit-player", 99999999),
            currency = currency,
            conditionIcon = this.getString("condition-icon")?.parseShopMaterial(),
            conditionLore = this.getStringList("condition-lore"),
            conditionName = this.getString("condition-name"),
            limitPermissionMap = limitMap,
            enableRight = this.getBoolean("right_click", true)
        )
    }

}

class MissingItemPluginException(val pluginName: String) : IllegalStateException("未启用依赖插件 $pluginName")
