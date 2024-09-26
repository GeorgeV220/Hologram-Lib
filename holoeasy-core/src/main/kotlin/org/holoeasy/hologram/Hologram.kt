package org.holoeasy.hologram

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.holoeasy.line.ILine
import org.holoeasy.pool.IHologramPool
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class Hologram(val plugin: Plugin, location: Location, val loader: IHologramLoader) {

    companion object {

        @JvmStatic
        fun create(location: Location): Builder {

            val builder = Builder(location)
            return builder
        }
    }

    class Builder(val location: Location) {

        val lines = mutableListOf<ILine<*>>()

        @JvmOverloads
        fun build(plugin: Plugin, loader: IHologramLoader = TextBlockStandardLoader()): Hologram {
            val hologram = Hologram(plugin, location, loader)

            if(lines.isEmpty()) {
                throw RuntimeException("its not possible to create an empty hologram")
            }
            hologram.load(*lines.toTypedArray<ILine<*>>())

            return hologram
        }

        @JvmOverloads
        fun buildAndLoad(pool: IHologramPool, loader: IHologramLoader = TextBlockStandardLoader()): Hologram {
            val hologram = build(pool.plugin, loader = loader)
            pool.takeCareOf(hologram)
            return hologram
        }
    }



    val id = UUID.randomUUID()!!

    var location: Location = location
        private set

    private val hLines: MutableList<ILine<*>> =
        CopyOnWriteArrayList() // writes are slow and Iterators are fast and consistent.

    val lines: MutableList<ILine<*>>
        get() = hLines

    val seeingPlayers: MutableSet<Player> = ConcurrentHashMap.newKeySet() // faster writes

    private var showEvent: ShowEvent? = null
    private var hideEvent: HideEvent? = null

    fun <T : ILine<*>> lineAt(index: Int): T {
        return hLines[index] as T
    }

    fun onShow(showEvent: ShowEvent): Hologram {
        this.showEvent = showEvent
        return this
    }

    fun onHide(hideEvent: HideEvent): Hologram {
        this.hideEvent = hideEvent
        return this
    }

    fun load(vararg lines: ILine<*>) {
        hLines.clear()

        lines.forEach { it.pvt.hologram = this }
        loader.load(this, lines)
    }

    fun teleport(to: Location) {
        this.location = to.clone()
        loader.teleport(this)
    }

    fun isShownFor(player: Player): Boolean {
        return seeingPlayers.contains(player)
    }

    fun show(player: Player) {
        seeingPlayers.add(player)
        for (line in this.hLines) {
            line.show(player)
        }

        showEvent?.onShow(player)
    }

    fun hide(player: Player) {
        for (line in this.hLines) {
            line.hide(player)
        }
        seeingPlayers.remove(player)

        hideEvent?.onHide(player)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hologram

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}
