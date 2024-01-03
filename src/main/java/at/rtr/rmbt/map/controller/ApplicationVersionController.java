package at.rtr.rmbt.map.controller;


import at.rtr.rmbt.map.constant.URIConstants;
import at.rtr.rmbt.map.dto.ApplicationVersionResponse;
import at.rtr.rmbt.map.service.ApplicationVersionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ApplicationVersionController {

    private final ApplicationVersionService applicationVersionService;

    @Operation(summary = "Get version of application")
    @GetMapping(URIConstants.VERSION)
    public ApplicationVersionResponse getApplicationVersion() {
        return applicationVersionService.getApplicationVersion();
    }
}
