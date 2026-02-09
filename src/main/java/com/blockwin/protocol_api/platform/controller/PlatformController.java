package com.blockwin.protocol_api.platform.controller;

import com.blockwin.protocol_api.platform.model.dto.PlatformDTO;
import com.blockwin.protocol_api.platform.model.dto.RegisterPlatformRequest;
import com.blockwin.protocol_api.platform.model.dto.RegisterPlatformResponse;
import com.blockwin.protocol_api.platform.service.PlatformService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/platform")
@CrossOrigin("*")
public class PlatformController {

    private final PlatformService platformService;

    @PostMapping("/register")
    public ResponseEntity<RegisterPlatformResponse> registerPlatform(@RequestBody RegisterPlatformRequest registerRequest) {
        RegisterPlatformResponse response = platformService.registerPlatform(registerRequest);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/update")
    public ResponseEntity<RegisterPlatformResponse> updatePlatform(@RequestParam("id") UUID id, @RequestBody RegisterPlatformRequest registerRequest) {
        RegisterPlatformResponse response = platformService.updatePlatform(id, registerRequest);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<PlatformDTO> getPlatform(@RequestParam("id") UUID id) {
        return ResponseEntity.ok().body(this.platformService.getById(id));
    }

}
