package org.jetbrains.kotlin.spec.viewer

import js.externals.jquery.JQuery
import js.externals.jquery.`$`
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window

object Sidebar {
    private const val TOC = "#TOC"
    private const val OFFSET_BEFORE_SCROLLED_ELEMENT = 100

    private fun expandItemsHierarchy(sectionMenuItem: JQuery) {
        if (sectionMenuItem.length == 0) return

        // Clean previously set classes on another elements
        `$`(".toc-element").removeClass("active")

        sectionMenuItem.addClass("active").addClass("toggled")
        sectionMenuItem.next("ul").find("> li").show()

        sectionMenuItem.parents("$TOC ul").find("> li").show()
        sectionMenuItem.parents("li").find(" > a").addClass("toggled")
    }

    private fun expandItemsHierarchy(sectionId: String) {
        val escapedSectionId = sectionId.replace(".", "\\.")
        val sectionMenuItem = `$`("#toc-element-$escapedSectionId")

        expandItemsHierarchy(sectionMenuItem)
    }

    private fun expandItemsHierarchyByUrl(shouldScrollToItem: Boolean = false) {
        val sectionIdFromHash = window.location.hash.removePrefix("#")
        val sectionIdFromPath = window.location.pathname.split("/").last().removeSuffix(".html")

        expandItemsHierarchy(if (sectionIdFromHash.isNotBlank()) sectionIdFromHash else sectionIdFromPath)

        if (shouldScrollToItem) {
            scrollToActiveItem()
        }
    }

    private fun scrollToActiveItem() {
        if (`$`("$TOC .active").length != 0) {
            val tocSelector = `$`(TOC)
            tocSelector.scrollTop(
                    tocSelector.scrollTop().toInt() - tocSelector.offset().top.toInt() + `$`("$TOC .active").offset().top.toInt() - OFFSET_BEFORE_SCROLLED_ELEMENT
            )
        }
    }

    private fun toggleItem(element: JQuery) {
        element.find("> li").toggle()
        element.parent("li").find("> a").toggleClass("toggled")
    }

    private fun showSidebar() {
        `$`(TOC).toggleClass("active")
        `$`(".icon-menu").toggleClass("active")
    }

    fun init() {
        expandItemsHierarchyByUrl(shouldScrollToItem = true)

        `$`(window).on("hashchange") { _, _ -> expandItemsHierarchyByUrl() }
        `$`("$TOC > ul > li, #TOC > ul > li > ul > li").show() // show "Kotlin core" subsections by default
        `$`(TOC).addClass("loaded")
        `$`("$TOC > ul > li ul").on("click") { e, _ ->
            e.stopPropagation()
            val target = `$`(e.target)
            if (e.offsetY.toInt() < 0 && !target.`is`("#TOC > ul > li > ul")) {
                toggleItem(`$`(e.target))
            }
        }

        `$`(document.body ?: return).prepend("""
            <div class="icon-menu">
                <span class="divide"></span>
                <span class="divide"></span>
                <span class="divide"></span>
            </div>
        """.trimIndent())

        `$`(".icon-menu").on("click") { _, _ -> showSidebar() }

        installSearchBar()

        addPdfLinks()

        addLinkToMainWebsitePage()
    }

    private var currSearchString = ""
    private var currResultIdx = 0

    private fun installSearchBar() {
        val tocRoot = `$`(TOC)
        val searchBar = document.createElement("input") as? HTMLInputElement ?: return

        searchBar.id = "toc-search-bar"
        searchBar.placeholder = "Search..."
        searchBar.title = "Currently, only exact matches are supported"

        searchBar.addEventListener("keyup", callback = cb@ { e ->
            // If key isn't "Enter" then return
            if ((e as? KeyboardEvent)?.which != 13) return@cb

            val searchString = (e.target as? HTMLInputElement)?.value ?: return@cb

            if (searchString.isBlank()) return@cb

            val foundItem = `$`("$TOC .toc-element:contains($searchString)")

            if (foundItem.length == 0) return@cb

            if (currSearchString != searchString) {
                currSearchString = searchString
                currResultIdx = -1
            }

            currResultIdx += 1
            currResultIdx %= foundItem.length.toInt()

            val currItem = foundItem.eq(currResultIdx)

            expandItemsHierarchy(currItem)
            scrollToActiveItem()
        })

        tocRoot.prepend(searchBar)
    }

    private fun addLinkToMainWebsitePage() {
        `$`("$TOC ul:first").prepend("<a href=\"http://kotlinlang.org\" class=\"toc-element toggled underlined\">Main page</a>")
    }

    private fun addPdfLinks() {
        `$`("$TOC ul:first").prepend("<a href=\"./pdf/kotlin-spec.pdf\" target=\"_blank\" class=\"toc-element toggled underlined\">Download full PDF</a>")
        `$`("$TOC > ul > li > ul > li").each { _, el ->
            val sectionName = `$`(el).find("> a").attr("href").substringBefore(".html")
            `$`(el).prepend("<a href=\"./pdf/sections/$sectionName.pdf\" target=\"_blank\" class=\"download-section-as-pdf\"></a>")
        }

        `$`("h2").each { _, el ->
            val sectionName = `$`(el).attr("id")
            `$`(el).after("<a href=\"./pdf/sections/$sectionName.pdf\" target=\"_blank\" class=\"download-section-as-pdf-text-link\">Download PDF</a>")
        }
    }
}
