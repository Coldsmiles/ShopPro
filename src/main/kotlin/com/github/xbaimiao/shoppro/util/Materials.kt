package com.github.xbaimiao.shoppro.util

import org.bukkit.Material
import taboolib.library.xseries.XMaterial

/** Prefer the server registry so newly added materials do not depend on XSeries updates. */
fun String.parseShopMaterial(): Material {
    return Material.matchMaterial(this)
        ?: XMaterial.matchXMaterial(this).orElse(null)?.parseMaterial()
        ?: error("Unknown item material: $this")
}
