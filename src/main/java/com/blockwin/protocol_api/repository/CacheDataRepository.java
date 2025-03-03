package com.blockwin.protocol_api.repository;

import com.blockwin.protocol_api.model.entity.CacheData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheDataRepository extends CrudRepository<CacheData, String> {}
