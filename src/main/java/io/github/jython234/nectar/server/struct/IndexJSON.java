package io.github.jython234.nectar.server.struct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents an Index in the Index JSON array
 * returned by fts/checksumIndex.
 *
 * @author jython234
 */
@RequiredArgsConstructor
public class IndexJSON {
    @Getter private final String path;
    @Getter private final String checksum;
    @Getter private final String lastUpdatedBy;
}
