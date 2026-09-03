package com.mysportsapp.imports;

import com.mysportsapp.imports.dto.ImportBatchResultDto;
import com.mysportsapp.imports.dto.ProviderInfoDto;
import com.mysportsapp.provider.DataProviderRegistry;
import com.mysportsapp.provider.spi.DataProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final ImportService importService;
    private final DataProviderRegistry providerRegistry;

    public ImportController(ImportService importService, DataProviderRegistry providerRegistry) {
        this.importService = importService;
        this.providerRegistry = providerRegistry;
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderInfoDto>> listProviders() {
        List<ProviderInfoDto> providers = providerRegistry.listAll().stream()
                .sorted(Comparator.comparing(DataProvider::getProviderId))
                .map(p -> new ProviderInfoDto(
                        p.getProviderId(),
                        p.getDisplayName(),
                        p.getSupportedFileExtensions().stream().sorted().toList()))
                .toList();
        return ResponseEntity.ok(providers);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchResultDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("providerId") String providerId) {

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read uploaded file", e);
        }

        ImportService.Outcome outcome = importService.importFile(providerId, file.getOriginalFilename(), content);

        HttpStatus httpStatus = outcome.hardParseFailure() ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK;
        return ResponseEntity.status(httpStatus).body(outcome.result());
    }
}
