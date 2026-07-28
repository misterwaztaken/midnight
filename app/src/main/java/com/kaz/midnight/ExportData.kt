package com.kaz.midnight

data class ExportData(
    val dreams: List<Dream>,
    val tags: List<Tag>,
    val crossRefs: List<DreamTagCrossRef>
)
