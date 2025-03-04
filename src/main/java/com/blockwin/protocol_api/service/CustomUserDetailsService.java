package com.blockwin.protocol_api.service;

import com.blockwin.protocol_api.model.entity.CacheData;
import com.blockwin.protocol_api.model.entity.UserEntity;
import com.blockwin.protocol_api.model.entity.UserRoleEntity;
import com.blockwin.protocol_api.repository.CacheDataRepository;
import com.blockwin.protocol_api.repository.UserRepository;
import com.blockwin.protocol_api.utils.UserStringMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static com.blockwin.protocol_api.utils.UserStringMapper.mapToUserDetails;

public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final CacheDataRepository cacheDataRepository;

    public CustomUserDetailsService(UserRepository userRepository, CacheDataRepository cacheDataRepository) {
        this.userRepository = userRepository;
        this.cacheDataRepository = cacheDataRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<CacheData> cachedUser = this.cacheDataRepository.findById(email);
        if (cachedUser.isPresent()) {
            UserDetails userDetails = mapToUserDetails(UserStringMapper.map(cachedUser.get().getValue()));
            return userDetails;
        }
        return this.userRepository
                .findByEmail(email)
                .map(UserStringMapper::mapToUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }
}
