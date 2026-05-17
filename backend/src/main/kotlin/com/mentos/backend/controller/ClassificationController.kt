package com.mentos.backend.controller

import com.mentos.backend.service.AiClassificationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/classification")
class ClassificationController(
    private val aiClassificationService: AiClassificationService
) {

    @GetMapping("/categorize")
    fun categorizeMerchant(@RequestParam merchantName: String): ResponseEntity<Map<String, String>> {
        val category = aiClassificationService.classifyMerchant(merchantName)
        return ResponseEntity.ok(mapOf(
            "merchantName" to merchantName,
            "category" to category
        ))
    }
}
