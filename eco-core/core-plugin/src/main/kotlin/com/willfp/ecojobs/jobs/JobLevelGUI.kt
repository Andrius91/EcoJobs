package com.willfp.ecojobs.jobs

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuLayer
import com.willfp.eco.core.gui.onLeftClick
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.toNiceString
import com.willfp.ecojobs.api.getJobLevel
import com.willfp.ecomponent.components.LevelComponent
import com.willfp.ecomponent.components.LevelState
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class JobLevelGUI(
    plugin: EcoPlugin,
    private val job: Job
) {
    private val menu: Menu

    init {
        val maskPattern = plugin.configYml.getStrings("level-gui.mask.pattern").toTypedArray()
        val maskItems = MaskItems.fromItemNames(plugin.configYml.getStrings("level-gui.mask.materials"))

        val progressionPattern = plugin.configYml.getStrings("level-gui.progression-slots.pattern")

        val component = object : LevelComponent(progressionPattern, job.maxLevel) {
            override fun getLevelItem(player: Player, menu: Menu, level: Int, levelState: LevelState): ItemStack {
                val key = levelState.name.lowercase().replace("_", "-")

                return ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.$key.item")))
                    .setDisplayName(
                        PlaceholderAPI.setPlaceholders(player,
                            plugin.configYml.getFormattedString("level-gui.progression-slots.$key.name")
                                .replace("%job%", job.name)
                                .replace("%level%", level.toString())
                                .replace("%level_numeral%", NumberUtils.toNumeral(level))
                        )
                    )
                    .addLoreLines(
                        // Process each line of the lore through PlaceholderAPI
                        job.injectPlaceholdersInto(
                            plugin.configYml.getFormattedStrings("level-gui.progression-slots.$key.lore"),
                            player,
                            forceLevel = level
                        ).map { line -> PlaceholderAPI.setPlaceholders(player, line) }
                    )
                    .setAmount(
                        if (plugin.configYml.getBool("level-gui.progression-slots.level-as-amount")) level else 1
                    )
                    .build()
            }


            override fun getLevelState(player: Player, level: Int): LevelState {
                return when {
                    level <= player.getJobLevel(job) -> LevelState.UNLOCKED
                    level == player.getJobLevel(job) + 1 -> LevelState.IN_PROGRESS
                    else -> LevelState.LOCKED
                }
            }
        }

        menu = menu(plugin.configYml.getInt("level-gui.rows")) {
            title = job.name

            maxPages(component.pages)

            setMask(
                FillerMask(
                    maskItems,
                    *maskPattern
                )
            )

            addComponent(1, 1, component)

            // Instead of the page changer, this will show up when on the first page
            addComponent(
                MenuLayer.LOWER,
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.prev-page.material")))
                        .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.prev-page.name"))
                        .build()
                ) {
                    onLeftClick { player, _, _, _ -> JobsGUI.open(player) }
                }
            )

            addComponent(
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.column"),
                PageChanger(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.prev-page.material")))
                        .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.prev-page.name"))
                        .build(),
                    PageChanger.Direction.BACKWARDS
                )
            )

            addComponent(
                plugin.configYml.getInt("level-gui.progression-slots.next-page.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.next-page.location.column"),
                PageChanger(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.next-page.material")))
                        .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.next-page.name"))
                        .build(),
                    PageChanger.Direction.FORWARDS
                )
            )

            setSlot(
                plugin.configYml.getInt("level-gui.progression-slots.close.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.close.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.close.material")))
                        .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.close.name"))
                        .build()
                ) {
                    onLeftClick { event, _ ->
                        event.whoClicked.closeInventory()
                    }
                }
            )

            for (config in plugin.configYml.getSubsections("level-gui.custom-slots")) {
                setSlot(
                    config.getInt("row"),
                    config.getInt("column"),
                    ConfigSlot(config)
                )
            }
        }
    }

    fun open(player: Player) {
        menu.open(player)
    }
}
