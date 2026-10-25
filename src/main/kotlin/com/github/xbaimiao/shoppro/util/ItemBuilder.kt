package com.github.xbaimiao.shoppro.util

import com.destroystokyo.paper.profile.ProfileProperty
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.block.banner.Pattern
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionType
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.colored
import taboolib.platform.util.isAir
import java.util.*


/**
 * 通过现有物品构建新的物品
 *
 * @param itemStack 原始物品
 * @param builder 构建器
 * @return 新的物品
 * @throws IllegalArgumentException 如果物品为空气
 */
fun buildItem(itemStack: ItemStack, builder: ItemBuilder.() -> Unit = {}): ItemStack {
    if (itemStack.isAir()) {
        error("air")
    }
    return ItemBuilder(itemStack).also(builder).build()
}

/**
 * 通过 [Material] 材质构建新的物品
 *
 * @param material 材质
 * @param builder 构建器
 * @return 新的物品
 * @throws IllegalArgumentException 如果材质为空气
 */
fun buildItem(material: Material, builder: ItemBuilder.() -> Unit = {}): ItemStack {
    // 在低版本, 并没有 Material.isAir() 方法
    if (material == Material.AIR) {
        error("air")
    }
    return ItemBuilder(material).also(builder).build()
}

/**
 * 通过 [XMaterial] 材质构建新的物品
 *
 * @param material 材质
 * @param builder 构建器
 * @return 新的物品
 * @throws IllegalArgumentException 如果材质为空气
 */
fun buildItem(material: XMaterial, builder: ItemBuilder.() -> Unit = {}): ItemStack {
    // 在低版本, 并没有 Material.isAir() 方法
    if (material == XMaterial.AIR) {
        error("air")
    }
    return ItemBuilder(material.parseMaterial() ?: error("material.parseMaterial() is null")).also(builder).build()
}

open class ItemBuilder {

    class SkullTexture(val textures: String, val uuid: UUID? = UUID.randomUUID())

    /**
     * 物品材质
     */
    var material: Material

    /**
     * 数量
     */
    var amount = 1

    /**
     * 附加值（损伤值）
     */
    var damage = 0

    /**
     * 展示名称
     */
    var name: String? = null

    /**
     * 描述
     */
    val lore = ArrayList<String>()

    /**
     * 标签
     */
    val flags = ArrayList<ItemFlag>()

    /**
     * 附魔
     */
    val enchants = HashMap<Enchantment, Int>()

    /**
     * 旗帜花纹
     */
    val patterns = ArrayList<Pattern>()

    /**
     * 颜色
     */
    var color: Color? = null

    /**
     * 药水效果
     */
    val potions = ArrayList<PotionEffect>()

    /**
     * 基础药水效果
     */
    var potionType: PotionType? = null

    /**
     * 生物类型
     */
    var spawnType: EntityType? = null

    /**
     * 头颅信息
     */
    var skullOwner: String? = null

    /**
     * 头颅材质信息
     */
    var skullTexture: SkullTexture? = null

    /**
     * 无法破坏
     */
    var isUnbreakable = false

    /**
     * CustomModelData
     */
    var customModelData = -1

    /**
     * 原始数据
     * 尝试修复自定义 nbt 失效的问题
     */
    var originMeta: ItemMeta? = null

    /**
     * 当构建完成时，做出最后修改
     */
    var finishing: (ItemStack) -> Unit = {}


    /**
     * 使其发光
     */
    fun shiny() {
        flags += ItemFlag.HIDE_ENCHANTS
        enchants[Enchantment.LURE] = 1
    }

    /**
     * 隐藏所有附加信息（赋予所有 ItemFlag）
     */
    fun hideAll() {
        flags.addAll(ItemFlag.values())
    }

    open fun getSkinValue(skull: ItemMeta): SkullTexture? {
        val profile = (skull as? SkullMeta)?.playerProfile ?: return null
        val texture = profile.properties.firstOrNull { it.name == "textures" && it.value.isNotEmpty() }
            ?: return null
        return SkullTexture(texture.value, profile.id)
    }

    /**
     * 上色
     */
    fun colored() {
        if (name != null) {
            name = try {
                name!!.colored()
            } catch (ex: NoClassDefFoundError) {
                ChatColor.translateAlternateColorCodes('&', name!!)
            }
        }
        if (lore.isNotEmpty()) {
            val newLore = try {
                lore.colored()
            } catch (ex: NoClassDefFoundError) {
                lore.map { ChatColor.translateAlternateColorCodes('&', it) }
            }
            lore.clear()
            lore += newLore
        }
    }

    /**
     * 构建物品
     */
    open fun build(): ItemStack {
        val itemStack = ItemStack(material)
        itemStack.amount = amount
        val itemMeta = originMeta?.clone() ?: itemStack.itemMeta ?: return itemStack
        if (itemMeta is Damageable) {
            itemMeta.damage = damage
        }
        itemMeta.setDisplayName(name)
        itemMeta.lore = lore
        itemMeta.addItemFlags(*flags.mapNotNull { it.toBukkit() }.toTypedArray())
        if (itemMeta is EnchantmentStorageMeta) {
            enchants.forEach { (e, lvl) -> itemMeta.addStoredEnchant(e, lvl, true) }
        } else {
            enchants.forEach { (e, lvl) -> itemMeta.addEnchant(e, lvl, true) }
        }
        when (itemMeta) {
            is LeatherArmorMeta -> {
                color?.let { itemMeta.setColor(it) }
            }

            is PotionMeta -> {
                itemMeta.color = color
                potions.forEach { itemMeta.addCustomEffect(it, true) }
                potionType?.let { itemMeta.basePotionType = it }
            }

            is SkullMeta -> {
                if (skullTexture != null) {
                    val texture = skullTexture!!
                    itemMeta.playerProfile = Bukkit.createProfile(texture.uuid ?: UUID.randomUUID()).also {
                        it.setProperty(ProfileProperty("textures", texture.textures))
                    }
                } else if (skullOwner != null && itemMeta.owningPlayer?.name != skullOwner) {
                    itemMeta.playerProfile = Bukkit.createProfile(skullOwner!!)
                }
            }
        }
        itemMeta.isUnbreakable = isUnbreakable
        if (spawnType != null && itemMeta is SpawnEggMeta) {
            itemMeta.customSpawnedType = spawnType
        }
        if (patterns.isNotEmpty() && itemMeta is BannerMeta) {
            itemMeta.patterns = patterns
        }
        // Preserve the full modern component when the legacy integer has not changed.
        if (customModelData != -1 && (!itemMeta.hasCustomModelData() || itemMeta.customModelData != customModelData)) {
            itemMeta.setCustomModelData(customModelData)
        }
        itemStack.itemMeta = itemMeta
        finishing(itemStack)
        return itemStack
    }

    constructor(material: Material) {
        this.material = material
    }

    /**
     * 通过现有物品构建新的物品
     * 读取基本信息
     */
    constructor(item: ItemStack) {
        material = item.type
        amount = item.amount
        // 如果物品没有 ItemMeta 则不进行后续操作
        val itemMeta = item.itemMeta ?: return
        damage = (itemMeta as? Damageable)?.damage ?: 0
        originMeta = itemMeta
        name = itemMeta.displayName.takeIf { itemMeta.hasDisplayName() }
        lore += itemMeta.lore ?: emptyList()
        flags += itemMeta.itemFlags.mapNotNull { runCatching { ItemFlag.valueOf(it.name) }.getOrNull() }
        enchants += if (itemMeta is EnchantmentStorageMeta) {
            itemMeta.storedEnchants
        } else {
            itemMeta.enchants
        }
        when (itemMeta) {
            is LeatherArmorMeta -> {
                color = itemMeta.color
            }

            is PotionMeta -> {
                color = itemMeta.color
                potions += itemMeta.customEffects
                potionType = itemMeta.basePotionType
            }

            is SkullMeta -> {
                // originMeta retains the full profile, including signatures and names.
                skullOwner = itemMeta.owningPlayer?.name
            }
        }
        customModelData = if (itemMeta.hasCustomModelData()) itemMeta.customModelData else -1
        isUnbreakable = itemMeta.isUnbreakable
        if (itemMeta is BannerMeta) {
            patterns += itemMeta.patterns
        }
    }
}
