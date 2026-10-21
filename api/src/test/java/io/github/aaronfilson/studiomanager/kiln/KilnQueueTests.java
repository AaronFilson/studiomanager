package io.github.aaronfilson.studiomanager.kiln;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class KilnQueueTests {

    private static final UUID PRODUCTION_POTTER = UUID.randomUUID();
    private static final UUID HOBBYIST = UUID.randomUUID();

    record Waiting(String name, UUID maker) {
    }

    @Test
    void capsEachMemberPerLoadAndKeepsTheOrder() {
        List<Waiting> queue = List.of(
                new Waiting("bowl 1", PRODUCTION_POTTER),
                new Waiting("bowl 2", PRODUCTION_POTTER),
                new Waiting("mug", HOBBYIST),
                new Waiting("bowl 3", PRODUCTION_POTTER));

        List<KilnQueue.Entry<Waiting>> plan = KilnQueue.plan(queue, Waiting::maker, 2);

        assertThat(plan).extracting(e -> e.piece().name()).containsExactly("bowl 1", "bowl 2", "mug", "bowl 3");
        assertThat(plan).extracting(KilnQueue.Entry::fitsNextLoad).containsExactly(true, true, true, false);
    }

    @Test
    void withoutACapEverythingFits() {
        List<KilnQueue.Entry<Waiting>> plan = KilnQueue.plan(
                List.of(new Waiting("a", HOBBYIST), new Waiting("b", HOBBYIST)), Waiting::maker, null);

        assertThat(plan).allMatch(KilnQueue.Entry::fitsNextLoad);
    }

}
