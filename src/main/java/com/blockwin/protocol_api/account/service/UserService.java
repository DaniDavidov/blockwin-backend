package com.blockwin.protocol_api.account.service;

import com.blockwin.protocol_api.account.model.dto.AuthenticationResponse;
import com.blockwin.protocol_api.account.model.dto.LoginRequest;
import com.blockwin.protocol_api.account.model.dto.RegisterRequest;
import com.blockwin.protocol_api.account.model.dto.UserDTO;
import com.blockwin.protocol_api.account.model.entity.CacheData;
import com.blockwin.protocol_api.account.model.entity.UserEntity;
import com.blockwin.protocol_api.account.model.entity.enums.TokenType;
import com.blockwin.protocol_api.account.model.entity.enums.UserRoleEnum;
import com.blockwin.protocol_api.account.model.error.UserNotFoundException;
import com.blockwin.protocol_api.account.repository.CacheDataRepository;
import com.blockwin.protocol_api.account.repository.TokenRepository;
import com.blockwin.protocol_api.account.repository.UserRepository;
import com.blockwin.protocol_api.account.repository.UserRoleRepository;
import com.blockwin.protocol_api.account.model.entity.Token;
import com.blockwin.protocol_api.common.utils.UserStringMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CacheDataRepository cacheDataRepository;
    private final UserRoleRepository userRoleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            CacheDataRepository cacheDataRepository,
            UserRoleRepository userRoleRepository,
            TokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.cacheDataRepository = cacheDataRepository;
        this.userRoleRepository = userRoleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthenticationResponse register(RegisterRequest request) {
        UserEntity user = UserEntity.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .userRoles(Set.of(this.userRoleRepository.findByName(UserRoleEnum.USER).get()))
                .build();

        UserEntity savedUser = userRepository.save(user);
        UserDetails userDetails = UserStringMapper.mapToUserDetails(savedUser);
        String jwt = jwtService.generateToken(userDetails);
        Token savedJwtToken = saveToken(user, jwt);
        return AuthenticationResponse.builder()
                .token(savedJwtToken.getToken())
                .userId(savedUser.getId())
                .build();
    }


    public AuthenticationResponse login(LoginRequest request) {
        this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserEntity userEntity = this.userRepository.findByEmail(request.email()).orElseThrow(() -> new UsernameNotFoundException(request.email()));

        this.cacheDataRepository.save(new CacheData(userEntity.getEmail(), UserStringMapper.map(userEntity)));

        List<Token> validTokens = this.tokenRepository.findAllValidTokenByUser(userEntity.getId());
        if (!validTokens.isEmpty()) {
            String token = validTokens.stream().findFirst().get().getToken();
            return AuthenticationResponse.builder().userId(userEntity.getId()).token(token).build();
        }
        String jwt = this.jwtService.generateToken(new User(userEntity.getEmail(), userEntity.getPassword(), new ArrayList<>()));
        Token savedToken = saveToken(userEntity, jwt);
        return AuthenticationResponse.builder().userId(userEntity.getId()).token(savedToken.getToken()).build();
    }

    public AuthenticationResponse updateUser(Long id, RegisterRequest request) {
        UserEntity userEntity = this.userRepository
                .findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(request.email()));

        List<Token> jwts = this.tokenRepository.findAllValidTokenByUser(userEntity.getId())
                .stream()
                .peek(token ->
                        {
                            token.setRevoked(true);
                            token.setExpired(true);
                        }
                ).toList();
        this.tokenRepository.saveAll(jwts);

        userEntity.setEmail(request.email());
        userEntity.setPassword(passwordEncoder.encode(request.password()));
        userEntity.setFirstName(request.firstName());
        userEntity.setLastName(request.lastName());
        userEntity.setPhoneNumber(request.phoneNumber());
        userEntity.setCreatedAt(userEntity.getCreatedAt());
        userEntity.setUpdatedAt(LocalDate.now());
        UserEntity savedUser = this.userRepository.save(userEntity);

        String jwt = this.jwtService.generateToken(UserStringMapper.mapToUserDetails(savedUser));
        Token savedJwtToken = saveToken(savedUser, jwt);

        return AuthenticationResponse.builder()
                .userId(savedUser.getId())
                .token(savedJwtToken.getToken()).build();
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

    private Token saveToken(UserEntity user, String jwt) {
        Token jwtEntity = Token.builder()
                .token(jwt)
                .tokenType(TokenType.BEARER)
                .revoked(false)
                .expired(false)
                .user(user)
                .build();
        return this.tokenRepository.save(jwtEntity);
    }
}
