package com.warrantymanager

import java.util.Date

data class Invoice(
    val id: String = "",
    val providerName: String = "",
    val amount: Double = 0.0,
    val date: Date = Date(),
    val imageUrl: String = ""
)
