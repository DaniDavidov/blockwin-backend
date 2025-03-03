package com.blockwin.protocol_api.service;

import com.blockwin.protocol_api.model.dto.LoginRequest;
import com.blockwin.protocol_api.model.dto.RegisterRequest;
import com.blockwin.protocol_api.model.dto.Response;
import com.blockwin.protocol_api.model.dto.UserDTO;
import com.blockwin.protocol_api.model.entity.UserEntity;
import com.blockwin.protocol_api.model.entity.UserRoleEntity;
import com.blockwin.protocol_api.model.entity.enums.UserRoleEnum;
import com.blockwin.protocol_api.model.error.UserNotFoundException;
import com.blockwin.protocol_api.repository.UserRepository;
import com.blockwin.protocol_api.repository.UserRoleRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public long register(RegisterRequest request) {
        UserEntity user = new UserEntity(
                request.email(),
                this.passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                LocalDate.now(),
                LocalDate.now(),
                new HashSet<>(Set.of(this.userRoleRepository.findByName(UserRoleEnum.USER).get()))
        );

        UserEntity saved = userRepository.save(user);
        return saved.getId();
    }


    public String login(LoginRequest request) {
        this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserEntity userEntity = this.userRepository.findByEmail(request.email()).orElseThrow(() -> new UsernameNotFoundException(request.email()));

        return this.jwtService.generateToken(new User(userEntity.getEmail(), userEntity.getPassword(), new ArrayList<>()));
    }

    public UserDTO getUserById(Long id) {
        UserEntity userEntity = this.userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        return new UserDTO(
                userEntity.getEmail(),
                userEntity.getFirstName(),
                userEntity.getLastName(),
                userEntity.getPhoneNumber()
        );
    }
}
