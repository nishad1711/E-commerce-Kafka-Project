package com.Ecommerce.user_service.service;

import com.Ecommerce.user_service.dto.AuthResponse;
import com.Ecommerce.user_service.dto.LoginRequest;
import com.Ecommerce.user_service.dto.RegisterRequest;
import com.Ecommerce.user_service.dto.UserResponse;
import com.Ecommerce.user_service.entity.Role;
import com.Ecommerce.user_service.entity.User;
import com.Ecommerce.user_service.repository.UserRepository;
import com.Ecommerce.user_service.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    void register_shouldCreateUserSuccessfully() {

        RegisterRequest request = new RegisterRequest(
                " Nishad ",
                " NISHAD@example.com ",
                "password123"
        );

        when(userRepository.existsByEmail("nishad@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(1L, "nishad@example.com", "USER"))
                .thenReturn("test-token");

        AuthResponse response = userService.register(request);

        assertEquals("test-token", response.token());
        assertEquals(1L, response.userId());
        assertEquals("nishad@example.com", response.email());
        assertEquals("USER", response.role());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Nishad", savedUser.getName());
        assertEquals("nishad@example.com", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole());

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_shouldRejectExistingEmail() {

        RegisterRequest request = new RegisterRequest(
                "Nishad",
                "nishad@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("nishad@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.register(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void register_shouldHandleDuplicateEmailRaceCondition() {

        RegisterRequest request = new RegisterRequest(
                "Nishad",
                "nishad@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("nishad@example.com"))
                .thenReturn(false, true);

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.register(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(userRepository).save(any(User.class));
        verify(userRepository, times(2)).existsByEmail("nishad@example.com");
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {

        LoginRequest request = new LoginRequest(
                " NISHAD@example.com ",
                "password123"
        );

        User user = User.builder()
                .id(1L)
                .name("Nishad")
                .email("nishad@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("nishad@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);
        when(jwtService.generateToken(1L, "nishad@example.com", "USER"))
                .thenReturn("test-token");

        AuthResponse response = userService.login(request);

        assertEquals("test-token", response.token());
        assertEquals(1L, response.userId());
        assertEquals("nishad@example.com", response.email());
        assertEquals("USER", response.role());
    }

    @Test
    void login_shouldRejectUnknownEmail() {

        LoginRequest request = new LoginRequest(
                "unknown@example.com",
                "password123"
        );

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void login_shouldRejectIncorrectPassword() {

        LoginRequest request = new LoginRequest(
                "nishad@example.com",
                "wrongPassword"
        );

        User user = User.builder()
                .id(1L)
                .name("Nishad")
                .email("nishad@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("nishad@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verifyNoInteractions(jwtService);
    }

    @Test
    void getUser_shouldReturnUserDetails() {

        User user = User.builder()
                .id(1L)
                .name("Nishad")
                .email("nishad@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("nishad@example.com"))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getUser("nishad@example.com");

        assertEquals(1L, response.id());
        assertEquals("Nishad", response.name());
        assertEquals("nishad@example.com", response.email());
        assertEquals("USER", response.role());
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {

        User user1 = User.builder()
                .id(1L)
                .name("Nishad")
                .email("nishad@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Admin")
                .email("admin@example.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> responses = userService.getAllUsers();

        assertEquals(2, responses.size());
        assertEquals("Nishad", responses.get(0).name());
        assertEquals("USER", responses.get(0).role());
        assertEquals("Admin", responses.get(1).name());
        assertEquals("ADMIN", responses.get(1).role());
    }
}
