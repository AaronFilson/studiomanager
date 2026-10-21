package io.github.aaronfilson.studiomanager.kiln;

import java.time.Instant;
import java.time.ZoneOffset;

import io.github.aaronfilson.studiomanager.contract.FiringDto;
import io.github.aaronfilson.studiomanager.contract.FiringKindDto;
import io.github.aaronfilson.studiomanager.contract.FiringStatusDto;
import io.github.aaronfilson.studiomanager.contract.KilnDto;
import io.github.aaronfilson.studiomanager.contract.KilnTypeDto;
import io.github.aaronfilson.studiomanager.piece.PieceDtos;

/** Converts kilns and firings to the HTTP contract. */
public final class KilnDtos {

    private KilnDtos() {
    }

    public static KilnDto toDto(KilnView kiln) {
        return new KilnDto()
                .id(kiln.id())
                .name(kiln.name())
                .site(kiln.site())
                .kind(KilnTypeDto.valueOf(kiln.kind().name()))
                .maxPiecesPerMember(kiln.maxPiecesPerMember())
                .totalFirings(kiln.totalFirings())
                .available(kiln.available())
                .downReason(kiln.downReason())
                .version(kiln.version());
    }

    public static FiringDto toDto(FiringView firing) {
        Instant now = Instant.now();
        return new FiringDto()
                .id(firing.id())
                .kilnId(firing.kilnId())
                .kilnName(firing.kilnName())
                .kind(FiringKindDto.valueOf(firing.kind().name()))
                .cone(firing.cone())
                .status(FiringStatusDto.valueOf(firing.status().name()))
                .notes(firing.notes())
                .createdAt(firing.createdAt().atOffset(ZoneOffset.UTC))
                .startedAt(firing.startedAt() == null ? null : firing.startedAt().atOffset(ZoneOffset.UTC))
                .finishedAt(firing.finishedAt() == null ? null : firing.finishedAt().atOffset(ZoneOffset.UTC))
                .pieceCount(firing.pieceCount())
                .version(firing.version())
                .pieces(firing.pieces().stream().map(piece -> PieceDtos.toDto(piece, now)).toList());
    }

}
