package com.pixelcare.domain.user.service;

import com.pixelcare.domain.user.dto.LoginRequestDto;
import com.pixelcare.domain.user.dto.SignupRequestDto;
import com.pixelcare.domain.user.dto.UserResponseDto;
import com.pixelcare.domain.user.entity.User;
import com.pixelcare.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponseDto signup(SignupRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일 계정입니다: " + request.getEmail());
        }

        String nickname = (request.getNickname() != null && !request.getNickname().isBlank())
                ? request.getNickname()
                : request.getEmail().split("@")[0];

        User user = new User(request.getEmail(), nickname, "USER");
        User saved = userRepository.save(user);
        return UserResponseDto.fromEntity(saved);
    }

    public UserResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    // 미등록 유저 로그인 시 자동 생성 (편의 기능)
                    String nickname = request.getEmail().split("@")[0];
                    User newUser = new User(request.getEmail(), nickname, "USER");
                    return userRepository.save(newUser);
                });

        return UserResponseDto.fromEntity(user);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        return UserResponseDto.fromEntity(user);
    }
}
