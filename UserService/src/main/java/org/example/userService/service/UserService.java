
package org.example.userService.service;

import org.example.userService.dto.UserCreateRequest;
import org.example.userService.dto.UserDto;
import org.example.userService.dto.UserUpdateRequest;
import org.example.userService.mapper.UserMapper;
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
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserDto createUser(UserCreateRequest request, Set<String> roleNames) {
        User user = userMapper.toEntity(request);
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
        return userMapper.toDto(savedUser);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    public Optional<UserDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDto);
    }

    @Transactional
    public Optional<UserDto> updateUser(Long id, UserUpdateRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    userMapper.updateEntityFromRequest(request, user);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userMapper.toDto(userRepository.save(user));
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
                    return userMapper.toDto(userRepository.save(user));
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
                    return userMapper.toDto(userRepository.save(user));
                });
    }

    @Transactional
    public Optional<UserDto> deactivateUser(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(false);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userMapper.toDto(userRepository.save(user));
                });
    }

    @Transactional
    public Optional<UserDto> activateUser(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(true);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userMapper.toDto(userRepository.save(user));
                });
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
