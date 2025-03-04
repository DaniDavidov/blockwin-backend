package com.blockwin.protocol_api.repository;

import com.blockwin.protocol_api.token.Token;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends CrudRepository<Token, Long> {

    @Query("""
    select t from Token t inner join t.user u on t.user.id = u.id
    where u.id = :userId and (t.expired = false and t.revoked = false)
""")
    List<Token> findAllValidTokenByUser(Long userId);

    Optional<Token> findByToken(String token);


}
