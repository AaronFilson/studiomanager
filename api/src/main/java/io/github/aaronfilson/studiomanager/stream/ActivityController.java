package io.github.aaronfilson.studiomanager.stream;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.ActivityApi;
import io.github.aaronfilson.studiomanager.contract.ActivityWeekDto;
import io.github.aaronfilson.studiomanager.contract.ActivityWeekFeesInnerDto;
import io.github.aaronfilson.studiomanager.contract.ActivityWeekKilnsInnerDto;
import io.github.aaronfilson.studiomanager.contract.WeeklyActivityDto;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ActivityController implements ActivityApi {

    private final ActivityProjection projection;
    private final boolean streaming;

    ActivityController(ActivityProjection projection, @Value("${studiomanager.stream.enabled}") boolean streaming) {
        this.projection = projection;
        this.streaming = streaming;
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<WeeklyActivityDto> getWeeklyActivity(UUID studioId, @Nullable Integer weeks) {
        Map<java.time.LocalDate, ActivityWeekDto> byWeek = new LinkedHashMap<>();
        for (ActivityProjection.Row row : projection.since(weeks == null ? 12 : weeks)) {
            ActivityWeekDto week = byWeek.computeIfAbsent(row.weekStart(), start -> new ActivityWeekDto()
                    .weekStart(start).firings(0).piecesFired(0).membersJoined(0)
                    .kilns(new ArrayList<>()).fees(new ArrayList<>()));
            switch (row.metric()) {
                case FIRINGS -> {
                    week.firings(week.getFirings() + row.count());
                    week.addKilnsItem(new ActivityWeekKilnsInnerDto().kiln(row.dimension()).firings(row.count()));
                }
                case PIECES_FIRED -> week.piecesFired(row.count());
                case MEMBERS_JOINED -> week.membersJoined(row.count());
                case FEES -> week.addFeesItem(new ActivityWeekFeesInnerDto().currency(row.dimension())
                        .amount(row.amount().toPlainString()).count(row.count()));
            }
        }
        return ResponseEntity.ok(new WeeklyActivityDto().streaming(streaming).weeks(List.copyOf(byWeek.values())));
    }

}
