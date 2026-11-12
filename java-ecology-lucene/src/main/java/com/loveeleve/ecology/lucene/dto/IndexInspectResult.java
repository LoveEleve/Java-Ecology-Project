package com.loveeleve.ecology.lucene.dto;

import java.util.List;
import java.util.Map;

public record IndexInspectResult(
        String directoryType,
        String indexPath,
        long numDocs,
        long maxDoc,
        int segmentCount,
        long generation,
        Map<String, String> commitUserData,
        List<FileInfo> files,
        List<SegmentInfo> segments
) {

    public record FileInfo(
            String name,
            long size
    ) {
    }

    public record SegmentInfo(
            String name,
            int maxDoc,
            int numDocs,
            int deletedDocs,
            boolean compoundFile
    ) {
    }
}
