package com.mentos.backend.repository

import com.mentos.backend.entity.NotificationParseLog
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationParseLogRepository : JpaRepository<NotificationParseLog, Long>
