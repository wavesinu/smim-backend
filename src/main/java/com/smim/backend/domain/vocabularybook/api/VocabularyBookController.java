package com.smim.backend.domain.vocabularybook.api;

import com.smim.backend.domain.vocabularybook.dto.VocabularyBookCreateRequest;
import com.smim.backend.domain.vocabularybook.dto.VocabularyBookResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyBookUpdateRequest;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryCreateRequest;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryMoveRequest;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryMoveResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntrySaveResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryStatusResponse;
import com.smim.backend.domain.vocabularybook.dto.VocabularyEntryStatusUpdateRequest;
import com.smim.backend.domain.vocabularybook.service.VocabularyBookService;
import com.smim.backend.global.auth.UserPrincipal;
import com.smim.backend.global.common.response.ApiResponse;
import com.smim.backend.global.common.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/vocabulary-books")
@RequiredArgsConstructor
public class VocabularyBookController {

    private final VocabularyBookService vocabularyBookService;

    @PostMapping
    public ResponseEntity<ApiResponse<VocabularyBookResponse>> createVocabularyBook(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody VocabularyBookCreateRequest request
    ) {
        VocabularyBookResponse response = vocabularyBookService.createBook(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VocabularyBookResponse>>> getVocabularyBooks(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<VocabularyBookResponse> response = vocabularyBookService.getBooks(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{bookId}")
    public ResponseEntity<ApiResponse<VocabularyBookResponse>> updateVocabularyBook(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookId,
            @Valid @RequestBody VocabularyBookUpdateRequest request
    ) {
        VocabularyBookResponse response = vocabularyBookService.updateBook(userPrincipal.getId(), bookId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteVocabularyBook(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookId
    ) {
        vocabularyBookService.deleteBook(userPrincipal.getId(), bookId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/entries")
    public ResponseEntity<ApiResponse<VocabularyEntrySaveResponse>> addEntries(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookId,
            @Valid @RequestBody VocabularyEntryCreateRequest request
    ) {
        VocabularyEntrySaveResponse response = vocabularyBookService.addEntries(userPrincipal.getId(), bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{bookId}/entries")
    public ResponseEntity<ApiResponse<PageResponse<VocabularyEntryResponse>>> getEntries(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String keyword
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, toSort(sort));
        Page<VocabularyEntryResponse> entryPage = vocabularyBookService.getEntries(
                userPrincipal.getId(),
                bookId,
                pageRequest,
                keyword
        );
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(entryPage)));
    }

    @DeleteMapping("/{bookId}/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookId,
            @PathVariable Long entryId
    ) {
        vocabularyBookService.deleteEntry(userPrincipal.getId(), bookId, entryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/entries/move")
    public ResponseEntity<ApiResponse<VocabularyEntryMoveResponse>> moveEntries(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookId,
            @RequestBody VocabularyEntryMoveRequest request
    ) {
        VocabularyEntryMoveResponse response = vocabularyBookService.moveEntries(userPrincipal.getId(), bookId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{bookId}/entries/{entryId}/status")
    public ResponseEntity<ApiResponse<VocabularyEntryStatusResponse>> updateEntryStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long bookId,
            @PathVariable Long entryId,
            @Valid @RequestBody VocabularyEntryStatusUpdateRequest request
    ) {
        VocabularyEntryStatusResponse response = vocabularyBookService.updateEntryStatus(
                userPrincipal.getId(),
                bookId,
                entryId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Sort toSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }
}
