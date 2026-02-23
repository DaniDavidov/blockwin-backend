package com.blockwin.protocol_api;

import com.blockwin.protocol_api.blockchain.config.BlockchainConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BlockchainConfig.class)
public class ProtocolApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProtocolApiApplication.class, args);
	}

}
