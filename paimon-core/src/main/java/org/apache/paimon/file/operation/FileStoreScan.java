/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.file.operation;

import org.apache.paimon.file.io.DataFileMeta;
import org.apache.paimon.file.manifest.FileKind;
import org.apache.paimon.file.manifest.ManifestEntry;
import org.apache.paimon.file.manifest.ManifestFileMeta;
import org.apache.paimon.file.predicate.Predicate;
import org.apache.paimon.utils.Filter;

import org.apache.paimon.data.BinaryRow;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Scan operation which produces a plan. */
public interface FileStoreScan {

    FileStoreScan withPartitionFilter(Predicate predicate);

    FileStoreScan withPartitionFilter(List<BinaryRow> partitions);

    FileStoreScan withBucket(int bucket);

    FileStoreScan withSnapshot(long snapshotId);

    FileStoreScan withManifestList(List<ManifestFileMeta> manifests);

    FileStoreScan withKind(ScanKind scanKind);

    FileStoreScan withLevelFilter(Filter<Integer> levelFilter);

    /** Produce a {@link Plan}. */
    Plan plan();

    /** Result plan of this scan. */
    interface Plan {

        /**
         * Snapshot id of this plan, return null if the table is empty or the manifest list is
         * specified.
         */
        @Nullable
        Long snapshotId();

        /** Result {@link ManifestEntry} files. */
        List<ManifestEntry> files();

        /** Result {@link ManifestEntry} files with specific file kind. */
        default List<ManifestEntry> files(FileKind kind) {
            return files().stream().filter(e -> e.kind() == kind).collect(Collectors.toList());
        }

        /** Return a map group by partition and bucket. */
        static Map<BinaryRow, Map<Integer, List<DataFileMeta>>> groupByPartFiles(
                List<ManifestEntry> files) {
            Map<BinaryRow, Map<Integer, List<DataFileMeta>>> groupBy = new LinkedHashMap<>();
            for (ManifestEntry entry : files) {
                groupBy.computeIfAbsent(entry.partition(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(entry.bucket(), k -> new ArrayList<>())
                        .add(entry.file());
            }
            return groupBy;
        }
    }
}