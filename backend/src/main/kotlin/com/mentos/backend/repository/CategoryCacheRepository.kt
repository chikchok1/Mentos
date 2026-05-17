package com.mentos.backend.repository

import com.mentos.backend.entity.CategoryCache
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryCacheRepository : JpaRepository<CategoryCache, Long> {
    fun findByMerchantName(merchantName: String): CategoryCache?
}
