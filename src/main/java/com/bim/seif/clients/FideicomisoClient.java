package com.bim.seif.clients;

import com.bim.seif.config.ClientConfig;
import com.bim.seif.dto.FideicomisoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "FIDEICOMISOS-API", url = "${gestor.api.url}", configuration = ClientConfig.class)
public interface FideicomisoClient {
    @GetMapping(value = "/{clienteEmail}")
    List<FideicomisoDto> obtenerFideicomisos(@PathVariable String clienteEmail);
}
