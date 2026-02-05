package com.blockwin.protocol_api.account.service;

import com.blockwin.protocol_api.account.model.entity.UserEntity;
import com.blockwin.protocol_api.account.model.entity.UserRoleEntity;
import com.blockwin.protocol_api.account.repository.CacheDataRepository;
import com.blockwin.protocol_api.account.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final CacheDataRepository cacheDataRepository;

    public CustomUserDetailsService(UserRepository userRepository, CacheDataRepository cacheDataRepository) {
        this.userRepository = userRepository;
        this.cacheDataRepository = cacheDataRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        Optional<CacheData> cachedUser = this.cacheDataRepository.findById(email);
//        if (cachedUser.isPresent()) {
//            UserDetails userDetails = mapToUserDetails(UserStringMapper.map(cachedUser.get().getValue()));
//            return userDetails;
//        }
        return this.userRepository
                .findByEmail(email)
                .map(this::mapToUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }

    public UserDetails mapToUserDetails(UserEntity userEntity) {
        return new User(
                userEntity.getEmail(),
                userEntity.getPassword(),
                extractAuthorities(userEntity)
        );
    }

    private List<GrantedAuthority> extractAuthorities(UserEntity userEntity) {
        return userEntity.getUserRoles()
                .stream()
                .map(this::mapRole)
                .toList();
    }

    private GrantedAuthority mapRole(UserRoleEntity role) {
        return new SimpleGrantedAuthority("ROLE_" + role.getName().name());
    }
}
