package com.aguiar_marcel.hyperativa_api.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "pan_hash", nullable = false, unique = true, columnDefinition = "BINARY(32)")
    private byte[] panHash;

    @Column(name = "pan_enc", nullable = false)
    private byte[] panEncrypted;

    @Column(name = "pan_iv", nullable = false)
    private byte[] panIv;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
