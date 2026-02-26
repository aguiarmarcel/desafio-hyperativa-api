package com.aguiar_marcel.hyperativa_api.adapters.out.persistence;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CardBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public int[] batchInsert(List<CardBatchRecord> records) {

        String sql = """
            INSERT IGNORE INTO card
            (id, pan_hash, pan_enc, pan_iv, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CardBatchRecord record = records.get(i);

                ps.setBytes(1, uuidToBytes(record.id()));
                ps.setBytes(2, record.panHash());
                ps.setBytes(3, record.panEncrypted());
                ps.setBytes(4, record.panIv());
                ps.setObject(5, Instant.now());
            }

            @Override
            public int getBatchSize() {
                return records.size();
            }
        });
    }

    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    public record CardBatchRecord(
            UUID id,
            byte[] panHash,
            byte[] panEncrypted,
            byte[] panIv
    ) {}
}
