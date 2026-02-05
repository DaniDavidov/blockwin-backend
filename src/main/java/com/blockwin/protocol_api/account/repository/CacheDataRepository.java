package com.blockwin.protocol_api.account.repository;

import com.blockwin.protocol_api.account.model.entity.CacheData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheDataRepository extends CrudRepository<CacheData, String> {}
