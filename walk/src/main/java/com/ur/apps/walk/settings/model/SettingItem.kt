package com.ur.apps.walk.settings.model

sealed class SettingItem {
    data class Section(
        var id: String,
        var title: String,
        var items: List<Item>,
        var isExpanded: Boolean = false
    )

    sealed class Item {
        abstract val id: String
        abstract val title: String
        
        data class Switch(
            override val id: String,
            override val title: String,
            var isChecked: Boolean
        ) : Item()
        
        data class ColorPicker(
            override val id: String,
            override val title: String,
            var selectedColor: Int,
            val onClick: () -> Unit
        ) : Item()
        
        data class Action(
            override val id: String,
            override val title: String,
            val onClick: () -> Unit
        ) : Item()
        
        data class Value(
            override val id: String,
            override val title: String,
            var value: String,
            val onClick: () -> Unit
        ) : Item()
        
        data class Distance(
            override val id: String,
            override val title: String,
            var isKm: Boolean
        ) : Item()
    }
}