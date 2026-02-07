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

    @PutMapping("{id}/update")
    public ResponseEntity<RegisterPlatformResponse> updatePlatform(@PathVariable UUID id, @RequestBody RegisterPlatformRequest registerRequest) {
        RegisterPlatformResponse response = platformService.updatePlatform(id, registerRequest);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlatformDTO> getPlatform(@PathVariable UUID id) {
        return ResponseEntity.ok().body(this.platformService.getById(id));
    }

}
