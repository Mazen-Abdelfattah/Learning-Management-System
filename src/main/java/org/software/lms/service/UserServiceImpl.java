package org.software.lms.service;

import org.software.lms.dto.ProfileUpdateDto;
import org.software.lms.dto.UserDto;
import org.software.lms.exception.ResourceNotFoundException;
import org.software.lms.model.User;
import org.software.lms.model.Role;
import org.software.lms.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDto createUser(UserDto userDto) {
        // Check if email already exists
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        BeanUtils.copyProperties(userDto, user);

        // Set a default role if not specified
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
        }

        // Encode password before saving
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        User savedUser = userRepository.save(user);

        UserDto savedUserDto = new UserDto();
        BeanUtils.copyProperties(savedUser, savedUserDto);

        return savedUserDto;
    }

    @Cacheable(value = "users", key = "#id")
    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(user, userDto);

        return userDto;
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(user, userDto);

        return userDto;
    }

    @Override
    public List<UserDto> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(user -> {
                    UserDto userDto = new UserDto();
                    BeanUtils.copyProperties(user, userDto);
                    return userDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> getAllUsers() {
       List <UserDto> usersDto = new ArrayList<>();
       List<User> users = userRepository.findAll();
       for (User user : users) {
           UserDto userDto = new UserDto();
           BeanUtils.copyProperties(user, userDto);
           usersDto.add(userDto);
       }

        return usersDto;

    }

    @CacheEvict(value = "users", key = "#id")
    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Copy non-null properties
        if (userDto.getFirstName() != null) existingUser.setFirstName(userDto.getFirstName());
        if (userDto.getLastName() != null) existingUser.setLastName(userDto.getLastName());

        User updatedUser = userRepository.save(existingUser);

        UserDto updatedUserDto = new UserDto();
        BeanUtils.copyProperties(updatedUser, updatedUserDto);

        return updatedUserDto;
    }

    @CacheEvict(value = "users", key = "#id")
    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.delete(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserDto viewProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(user, userDto);
        return userDto;
    }

    @CacheEvict(value = "users", key = "#id")
    @Override
    public UserDto updateProfile(Long id, ProfileUpdateDto profileUpdateDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Validate email if it's being updated
        if (profileUpdateDto.getEmail() != null &&
                !profileUpdateDto.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(profileUpdateDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Update fields if they are not null
        if (profileUpdateDto.getFirstName() != null) {
            user.setFirstName(profileUpdateDto.getFirstName());
        }
        if (profileUpdateDto.getLastName() != null) {
            user.setLastName(profileUpdateDto.getLastName());
        }
        if (profileUpdateDto.getEmail() != null) {
            user.setEmail(profileUpdateDto.getEmail());
        }

        User updatedUser = userRepository.save(user);
        UserDto updatedUserDto = new UserDto();
        BeanUtils.copyProperties(updatedUser, updatedUserDto);

        return updatedUserDto;
    }
}