package com.mentos.backend.service

import com.mentos.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GachaService(
    private val userRepository: UserRepository
) {
    @Transactional
    fun performAttendanceGacha(userId: Long): Map<String, Any> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        val today = LocalDate.now()
        
        if (user.lastAttendanceDate == today) {
            throw IllegalStateException("오늘은 이미 출석 가챠를 진행했습니다.")
        }
        
        user.lastAttendanceDate = today
        userRepository.save(user)
        
        // TODO: 실제 랜덤 보상 로직 연동
        return mapOf(
            "success" to true,
            "message" to "출석 가챠가 완료되었습니다.",
            "reward" to "출석 보상"
        )
    }
    
    fun getAttendanceStatus(userId: Long): Map<String, Any> {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        val today = LocalDate.now()
        
        return mapOf(
            "usedToday" to (user.lastAttendanceDate == today)
        )
    }
}
