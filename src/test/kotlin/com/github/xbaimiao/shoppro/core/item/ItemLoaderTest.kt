package com.github.xbaimiao.shoppro.core.item

import com.github.xbaimiao.shoppro.core.item.impl.MythicItem
import com.github.xbaimiao.shoppro.core.item.impl.VanillaShopItem
import com.github.xbaimiao.shoppro.core.shop.ShopImpl
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit

class ItemLoaderTest {

    @BeforeEach
    fun setUp() {
        MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `missing optional plugins are detected without loading their APIs`() {
        val manager = ItemLoaderManager()
        val materials = mapOf(
            "MM:KingsCrown" to "MythicMobs",
            "Zaphkiel:example_sword" to "Zaphkiel",
            "MMO_ITEM:SWORD:starter" to "MMOItems",
            "NeigeItems:example" to "NeigeItems"
        )
        for ((material, dependency) in materials) {
            val loader = manager.findLoader(material)
            val exception = assertThrows(MissingItemPluginException::class.java) { loader.checkDependency() }
            assertEquals(dependency, exception.pluginName)
        }
    }

    @Test
    fun `disabled plugins count as missing dependencies`() {
        val plugin = MockBukkit.createMockPlugin("MythicMobs")
        assertDoesNotThrow { MythicItem.checkDependency() }
        plugin.server.pluginManager.disablePlugin(plugin)
        assertThrows(MissingItemPluginException::class.java) { MythicItem.checkDependency() }
    }

    @Test
    fun `material prefixes require a colon and vanilla items need no integration`() {
        val manager = ItemLoaderManager()
        for (material in listOf("STONE", "DIAMOND_SPEAR", "MM", "MMO_ITEM")) {
            assertSame(VanillaShopItem, manager.findLoader(material))
        }
        assertEquals("MMOItems", manager.findLoader("MMO_ITEM:SWORD:starter").requiredPlugin)
    }

    @Test
    fun `shop still loads vanilla items when optional example dependencies are missing`() {
        val config = shopConfiguration()
        config.set("items.A.material", "STONE")
        config.set("items.A.name", "Stone")
        config.set("items.C.material", "Zaphkiel:example_sword")
        config.set("items.D.material", "MM:KingsCrown")
        config.set("items.E.material", "MMO_ITEM:SWORD:starter")
        val items = ShopImpl(config).getItems()
        assertEquals(listOf(Material.STONE), items.map { it.material })
    }

    @Test
    fun `disabled items are skipped before parsing their material or dependency`() {
        val config = shopConfiguration()
        config.set("items.A.enabled", false)
        assertTrue(ShopImpl(config).getItems().isEmpty())
    }

    @Test
    fun `MythicMobs item manager can be used without a version specific entrypoint`() {
        val expected = ItemStack(Material.GOLDEN_HELMET)
        val plugin = FakeMythicPlugin(FakeItemManager(expected))
        assertEquals(expected, MythicItem.readItemStack(plugin, "KingsCrown"))
        assertThrows(IllegalStateException::class.java) { MythicItem.readItemStack(plugin, "missing") }
    }

    private fun shopConfiguration() = YamlConfiguration().apply {
        set("name", "Dependency regression shop")
        set("type", "buy")
        set("title", "Regression")
        set("slots", listOf("ACDE     "))
        createSection("items")
    }

    // Isolated test doubles for the common public method signatures in MythicMobs 4 and 5.
    class FakeMythicPlugin(private val manager: FakeItemManager) {
        fun getItemManager() = manager
    }

    class FakeItemManager(private val item: ItemStack) {
        fun getItemStack(id: String): ItemStack? = item.takeIf { id == "KingsCrown" }
    }
}
