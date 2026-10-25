package com.github.xbaimiao.shoppro.util

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.DyeColor
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import java.util.Base64
import java.util.UUID

class ItemBuilderTest {

    @BeforeEach
    fun setUp() {
        MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `damage survives building and rebuilding`() {
        val original = buildItem(Material.DIAMOND_SWORD) { damage = 17 }
        assertEquals(17, (original.itemMeta as Damageable).damage)
        val rebuilt = buildItem(original) { name = "Shop sword" }
        assertEquals(17, (rebuilt.itemMeta as Damageable).damage)
        assertEquals(17, (original.itemMeta as Damageable).damage)
        assertFalse(original.itemMeta.hasDisplayName())
    }

    @Test
    fun `modern potion type survives rebuilding`() {
        val potion = buildItem(Material.POTION) { potionType = PotionType.LONG_SWIFTNESS }
        val rebuilt = buildItem(potion) { lore += "Shop potion" }
        assertEquals(PotionType.LONG_SWIFTNESS, (rebuilt.itemMeta as PotionMeta).basePotionType)
    }

    @Test
    fun `head textures use the public profile API`() {
        val texture = Base64.getEncoder().encodeToString(
            """{"textures":{"SKIN":{"url":"https://textures.minecraft.net/texture/${"a".repeat(64)}"}}}""".toByteArray()
        )
        val uuid = UUID.randomUUID()
        val head = buildItem(Material.PLAYER_HEAD) { skullTexture = ItemBuilder.SkullTexture(texture, uuid) }
        val rebuilt = buildItem(head) { lore += "Shop head" }
        for (item in listOf(head, rebuilt)) {
            val profile = (item.itemMeta as SkullMeta).playerProfile!!
            assertEquals(uuid, profile.id)
            assertEquals(texture, profile.properties.single { it.name == "textures" }.value)
        }
    }

    @Test
    fun `custom model data and persistent data are preserved`() {
        val item = ItemStack(Material.PAPER)
        val key = NamespacedKey("shoppro", "regression")
        item.itemMeta = item.itemMeta.apply {
            setCustomModelData(42)
            persistentDataContainer.set(key, PersistentDataType.STRING, "retained")
        }
        val rebuilt = buildItem(item) { lore += "Shop lore" }
        assertEquals(42, rebuilt.itemMeta.customModelData)
        assertEquals("retained", rebuilt.itemMeta.persistentDataContainer.get(key, PersistentDataType.STRING))
        assertFalse(item.itemMeta.hasLore())
    }

    @Test
    fun `legacy potion flag maps without discarding valid flags`() {
        val item = buildItem(Material.STONE) {
            flags += ItemFlag.HIDE_POTION_EFFECTS
            flags += ItemFlag.HIDE_ENCHANTS
        }
        assertTrue(item.itemMeta.hasItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP))
        assertTrue(item.itemMeta.hasItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS))
    }

    @Test
    fun `new materials can be built`() {
        for (name in listOf("COPPER_CHEST", "DIAMOND_SPEAR")) {
            val material = name.parseShopMaterial()
            assertEquals(name, material.name)
            assertEquals(material, buildItem(material).type)
        }
        assertEquals(Material.DIAMOND, "minecraft:diamond".parseShopMaterial())
    }

    @Test
    fun `rebuilding banners does not duplicate patterns`() {
        val banner = buildItem(Material.WHITE_BANNER) {
            patterns += Pattern(DyeColor.RED, PatternType.CROSS)
        }
        val rebuilt = buildItem(banner) { name = "Shop banner" }
        assertEquals((banner.itemMeta as BannerMeta).patterns, (rebuilt.itemMeta as BannerMeta).patterns)
        assertEquals(1, (rebuilt.itemMeta as BannerMeta).numberOfPatterns())
    }
}
