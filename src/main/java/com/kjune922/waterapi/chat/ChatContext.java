package com.kjune922.waterapi.chat;
import com.kjune922.waterapi.question.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.List;
@Getter
@RequiredArgsConstructor
public class ChatContext {
    private final Long facilityId;
    private final String facilityName;
    private final long matchedCount;
    private final List<RecordEvidence> evidence;
    private final List<String> previousTurns;
}
