package com.dongbacsaigon.backend.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MediaUsageServiceTest {

    @Test
    void reportsReferencedWhenAnyCheckerFindsUsage() {
        UUID mediaId = UUID.randomUUID();
        MediaReferenceChecker first = mock(MediaReferenceChecker.class);
        MediaReferenceChecker second = mock(MediaReferenceChecker.class);
        when(first.isReferenced(mediaId)).thenReturn(false);
        when(second.isReferenced(mediaId)).thenReturn(true);

        assertThat(new MediaUsageService(List.of(first, second)).isReferenced(mediaId)).isTrue();
        verify(first).isReferenced(mediaId);
        verify(second).isReferenced(mediaId);
    }
}
