package com.mentos.backend.controller

import com.mentos.backend.dto.SocialLoginRequest
import com.mentos.backend.dto.AuthResponse
import com.mentos.backend.entity.User
import com.mentos.backend.repository.UserRepository
import com.mentos.backend.security.JwtProvider
import com.mentos.backend.service.OAuthService
import com.mentos.backend.service.OAuthUserInfo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val oAuthService: OAuthService,
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider
) {

    @PostMapping("/social-login")
    fun socialLogin(@RequestBody request: SocialLoginRequest): ResponseEntity<AuthResponse> {
        val userInfo: OAuthUserInfo = when (request.provider) {
            "GOOGLE" -> oAuthService.verifyGoogleToken(request.socialToken)
            "KAKAO" -> oAuthService.verifyKakaoToken(request.socialToken)
            else -> throw IllegalArgumentException("지원하지 않는 로그인 방식입니다.")
        }

        // DB에서 사용자 조회, 없으면 회원가입 처리
        val user = userRepository.findBySocialIdAndProvider(userInfo.socialId, userInfo.provider)
            ?: userRepository.save(User(
                socialId = userInfo.socialId,
                email = userInfo.email,
                provider = userInfo.provider
            ))

        // 자체 JWT 발급
        val accessToken = jwtProvider.generateAccessToken(user.id)
        val refreshToken = jwtProvider.generateRefreshToken(user.id)

        // 클라이언트(안드로이드)에 JWT 응답
        return ResponseEntity.ok(AuthResponse(accessToken, refreshToken))
    }
}
