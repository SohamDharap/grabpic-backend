package com.grabpic.backend.service.impl;

import com.grabpic.backend.dto.request.CreatePhotographerDto;
import com.grabpic.backend.dto.response.PhotographerResponseDto;
import com.grabpic.backend.entity.UserDetails;
import com.grabpic.backend.enums.UserRole;
import com.grabpic.backend.repository.UserRepository;
import com.grabpic.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    @Override
    public PhotographerResponseDto createPhotographer(CreatePhotographerDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        UserDetails photographer = UserDetails.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .role(UserRole.PHOTOGRAPHER)
                .gender(request.getGender())
                .age(request.getAge())
                .isActive(true)
                .build();

        UserDetails savedPhotographer = userRepository.save(photographer);
        return mapToResponseDto(savedPhotographer);
    }

    @Override
    public PhotographerResponseDto activatePhotographer(Long photographerId) {
        UserDetails photographer = userRepository.findById(photographerId)
                .orElseThrow(() -> new RuntimeException("Photographer not found with id: " + photographerId));

        if (photographer.getRole() != UserRole.PHOTOGRAPHER) {
            throw new RuntimeException("User is not a photographer");
        }

        photographer.setIsActive(true);
        UserDetails updatedPhotographer = userRepository.save(photographer);
        return mapToResponseDto(updatedPhotographer);
    }

    @Override
    public PhotographerResponseDto deactivatePhotographer(Long photographerId) {
        UserDetails photographer = userRepository.findById(photographerId)
                .orElseThrow(() -> new RuntimeException("Photographer not found with id: " + photographerId));

        if (photographer.getRole() != UserRole.PHOTOGRAPHER) {
            throw new RuntimeException("User is not a photographer");
        }

        photographer.setIsActive(false);
        UserDetails updatedPhotographer = userRepository.save(photographer);
        return mapToResponseDto(updatedPhotographer);
    }

    @Override
    public List<PhotographerResponseDto> getAllPhotographers() {
        return userRepository.findByRole(UserRole.PHOTOGRAPHER).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private PhotographerResponseDto mapToResponseDto(UserDetails user) {
        PhotographerResponseDto response = new PhotographerResponseDto();
        response.setId(user.getId());
        response.setFirstname(user.getFirstname());
        response.setLastname(user.getLastname());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole());
        response.setGender(user.getGender());
        response.setAge(user.getAge());
        response.setIsActive(user.getIsActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
