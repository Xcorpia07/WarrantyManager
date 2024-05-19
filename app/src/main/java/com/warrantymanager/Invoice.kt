package com.warrantymanager

import java.util.Date

data class Invoice(
    val id: String = "",
    var manufacturer: String = "",
    var productName: String = "",
    var price: Double = 0.0,
    var supplier: String = "",
    var purchaseDate: Date = Date(),
    var warrantyDate: Date = Date(),
    var invoiceFileUrl: String = ""
)
