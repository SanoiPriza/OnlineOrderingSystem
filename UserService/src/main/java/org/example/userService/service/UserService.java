
package org.example.userService.service;

import org.example.userService.dto.UserDto;
import org.example.userService.model.Role;
import org.example.userService.model.User;
import org.example.userService.repository.RoleRepository;
import org.example.userService.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto createUser(User user, Set<String> roleNames) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setCreatedAt(LocalDateTime.now());

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        return convertToDto(savedUser);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToDto);
    }

    public Optional<UserDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToDto);
    }

    @Transactional
    public Optional<UserDto> updateUser(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setFirstName(userDetails.getFirstName());
                    user.setLastName(userDetails.getLastName());
                    user.setEmail(userDetails.getEmail());
                    user.setUpdatedAt(LocalDateTime.now());
                    return convertToDto(userRepository.save(user));
                });
    }

    @Transactional
    public Optional<UserDto> updateUserRoles(Long id, Set<String> roleNames) {
        return userRepository.findById(id)
                .map(user -> {
                    Set<Role> roles = new HashSet<>();
                    for (String roleName : roleNames) {
                        Role role = roleRepository.findByName(roleName)
                                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                        roles.add(role);
                    }
                    user.setRoles(roles);
                    user.setUpdatedAt(LocalDateTime.now());
                    return convertToDto(userRepository.save(user));
                });
    }

    @Transactional
    public Optional<UserDto> changePassword(Long id, String currentPassword, String newPassword) {
        return userRepository.findById(id)
                .map(user -> {
                    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                        throw new RuntimeException("Current password is incorrect");
                    }
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setUpdatedAt(LocalDateTime.now());
                    return convertToDto(userRepository.save(user));
                });
    }

    @Transactional
    public Optional<UserDto> deactivateUser(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(false);
                    user.setUpdatedAt(LocalDateTime.now());
                    return convertToDto(userRepository.save(user));
                });
    }

    @Transactional
    public Optional<UserDto> activateUser(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(true);
                    user.setUpdatedAt(LocalDateTime.now());
                    return convertToDto(userRepository.save(user));
                });
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        dto.setRoles(roleNames);
        
        return dto;
    }
}
